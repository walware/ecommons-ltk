/*******************************************************************************
 * Copyright (c) 2007-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.SettingsChangeNotifier.ChangeListener;
import de.walware.ecommons.text.TextUtil;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.IModelElementDelta;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.ast.ICommonAstVisitor;
import de.walware.ecommons.ltk.ui.IModelElementInputListener;


public class FoldingEditorAddon implements ISourceEditorAddon, IModelElementInputListener, ChangeListener {
	
	
	public static final class FoldingStructureComputationContext {
		
		public final AbstractDocument document;
		public final ISourceUnitModelInfo model;
		public final AstInfo ast;
		
		public final boolean isInitial;
		
		private final ProjectionAnnotationModel fAnnnotationModel;
		private final SortedMap<Position, FoldingAnnotation> fTable = new TreeMap<Position, FoldingAnnotation>(TextUtil.POSITION_COMPARATOR);
		
		
		protected FoldingStructureComputationContext(final AbstractDocument document,
				final ISourceUnitModelInfo model, final AstInfo ast,
				final ProjectionAnnotationModel annotationModel, final boolean isInitial) {
			this.document = document;
			this.model = model;
			this.ast = ast;
			
			this.isInitial = isInitial;
			
			fAnnnotationModel = annotationModel;
		}
		
		
		public void addFoldingRegion(final Position position, final FoldingAnnotation ann) {
			if (!fTable.containsKey(position)) {
				fTable.put(position, ann);
			}
		}
		
	}
	
	public static final class FoldingAnnotation extends ProjectionAnnotation {
		
		
		private String fType;
		
		
		public FoldingAnnotation(final String type, final boolean collapse) {
			super(collapse);
			fType = type;
		}
		
	}
	
	public static abstract interface FoldingProvider {
		
		boolean checkConfig(Set<String> groupIds);
		
		boolean requiresModel();
		
		void collectRegions(FoldingStructureComputationContext ctx)
				throws InvocationTargetException;
		
	}
	
	public static interface NodeFoldingProvider extends FoldingProvider {
		
		ICommonAstVisitor createVisitor(FoldingStructureComputationContext ctx);
		
	}
	
	
	private static final class Input {
		
		private final ISourceUnit fUnit;
		private boolean fInitilized;
		private long fUpdateStamp;
		
		Input(final ISourceUnit unit) {
			fUnit = unit;
			fInitilized = false;
			fUpdateStamp = Long.MIN_VALUE;
		}
		
	}
	
	
	private final FoldingProvider fProvider;
	
	private SourceEditor1 fEditor;
	
	private volatile Input fInput;
	
	
	public FoldingEditorAddon(final FoldingProvider provider) {
		fProvider = provider;
	}
	
	
	@Override
	public void install(final ISourceEditor editor) {
		fEditor = (SourceEditor1) editor;
		PreferencesUtil.getSettingsChangeNotifier().addChangeListener(this);
		fProvider.checkConfig(null);
		fEditor.getModelInputProvider().addListener(this);
	}
	
	@Override
	public void elementChanged(final IModelElement element) {
		final Input input = new Input((ISourceUnit) element);
		synchronized (this) {
			fInput = input;
		}
	}
	
	@Override
	public void elementInitialInfo(final IModelElement element) {
		final Input input = fInput;
		if (input.fUnit == element) {
			update(input, -1);
		}
	}
	
	@Override
	public void elementUpdatedInfo(final IModelElement element, final IModelElementDelta delta) {
		final Input input = fInput;
		if (input.fUnit == element) {
			update(input, delta.getNewAst().stamp);
		}
	}
	
	@Override
	public void uninstall() {
		PreferencesUtil.getSettingsChangeNotifier().removeChangeListener(this);
		if (fEditor != null) {
			fEditor.getModelInputProvider().removeListener(this);
			fEditor = null;
		}
	}
	
	protected void refresh() {
		final Input input = fInput;
		if (input != null) {
			update(input, -1);
		}
	}
	
	@Override
	public void settingsChanged(final Set<String> groupIds) {
		if (groupIds != null && fProvider.checkConfig(groupIds)) {
			refresh();
		}
	}
	
	private FoldingStructureComputationContext createCtx(final Input input) {
		final SourceEditor1 editor = fEditor;
		if (editor == null) {
			return null;
		}
		final ProjectionAnnotationModel annotationModel = (ProjectionAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);
		if (input.fUnit == null || annotationModel == null) {
			return null;
		}
		final IProgressMonitor monitor = new NullProgressMonitor();
		
		final ISourceUnitModelInfo modelInfo;
		final AstInfo ast;
		if (fProvider.requiresModel()) {
			modelInfo = input.fUnit.getModelInfo(null, IModelManager.MODEL_FILE, monitor);
			if (modelInfo == null) {
				return null;
			}
			ast = modelInfo.getAst();
		}
		else {
			modelInfo = null;
			ast = input.fUnit.getAstInfo(null, false, monitor);
		}
		final AbstractDocument document = input.fUnit.getDocument(monitor);
		if (ast == null || document == null || ast.stamp != document.getModificationStamp()) {
			return null;
		}
		return new FoldingStructureComputationContext(document, modelInfo, ast,
				annotationModel, !input.fInitilized );
	}
	
	private void update(final Input input, final long stamp) {
		synchronized(input) {
			if (input.fUnit == null
					|| (stamp != -1 && input.fUpdateStamp == stamp)) { // already uptodate
				return;
			}
			FoldingStructureComputationContext ctx;
			if (input != fInput) {
				return;
			}
			ctx = createCtx(input);
			if (ctx == null) {
				return;
			}
			try {
				fProvider.collectRegions(ctx);
			}
			catch (final InvocationTargetException e) {
				return;
			}
			
			ProjectionAnnotation[] deletions;
			if (ctx.isInitial) {
				deletions = null;
				input.fInitilized = true;
			}
			else {
				final List<FoldingAnnotation> del = new ArrayList<FoldingAnnotation>();
				for (final Iterator<FoldingAnnotation> iter = ctx.fAnnnotationModel.getAnnotationIterator(); iter.hasNext(); ) {
					final FoldingAnnotation existingAnn = iter.next();
					final Position position = ctx.fAnnnotationModel.getPosition(existingAnn);
					final FoldingAnnotation newAnn = ctx.fTable.remove(position);
					if (newAnn != null) {
						existingAnn.fType = newAnn.fType;
					}
					else {
						del.add(existingAnn);
					}
				}
				deletions = del.toArray(new FoldingAnnotation[del.size()]);
				if (ctx.document.getModificationStamp() != ctx.ast.stamp || input != fInput) {
					return;
				}
			}
			final LinkedHashMap<FoldingAnnotation, Position> additions = new LinkedHashMap<FoldingAnnotation, Position>();
			for (final Iterator<Entry<Position, FoldingAnnotation>> iter = ctx.fTable.entrySet().iterator(); iter.hasNext(); ) {
				final Entry<Position, FoldingAnnotation> next = iter.next();
				additions.put(next.getValue(), next.getKey());
			}
			ctx.fAnnnotationModel.modifyAnnotations(deletions, additions, null);
			input.fUpdateStamp = ctx.ast.stamp;
		}
	}
	
}
