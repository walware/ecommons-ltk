/*=============================================================================#
 # Copyright (c) 2007-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.SettingsChangeNotifier.ChangeListener;
import de.walware.ecommons.text.TextUtil;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.IModelElementDelta;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.IWorkspaceSourceUnit;
import de.walware.ecommons.ltk.ast.ICommonAstVisitor;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.IModelElementInputListener;


public class FoldingEditorAddon implements ISourceEditorAddon, IModelElementInputListener, ChangeListener {
	
	
	private static final int EXPANDED_STATE = 1;
	private static final int COLLAPSED_STATE = 2;
	
	
	public static final class FoldingStructureComputationContext {
		
		public final AbstractDocument document;
		public final ISourceUnitModelInfo model;
		public final AstInfo ast;
		
		public final boolean isInitial;
		
		private final SortedMap<Position, FoldingAnnotation> fTable = new TreeMap<Position, FoldingAnnotation>(TextUtil.POSITION_COMPARATOR);
		
		
		protected FoldingStructureComputationContext(final AbstractDocument document,
				final ISourceUnitModelInfo model, final AstInfo ast, final boolean isInitial) {
			this.document = document;
			this.model = model;
			this.ast = ast;
			
			this.isInitial = isInitial;
		}
		
		
		public void addFoldingRegion(final Position position, final FoldingAnnotation ann) {
			if (!fTable.containsKey(position)) {
				fTable.put(position, ann);
			}
		}
		
	}
	
	public static final class FoldingAnnotation extends ProjectionAnnotation {
		
		
		private String fType;
		
		private final int fInitialState;
		
		
		public FoldingAnnotation(final String type, final boolean collapse) {
			super(collapse);
			fType = type;
			fInitialState = (collapse) ? COLLAPSED_STATE : EXPANDED_STATE;
		}
		
		
		private int getInitialState() {
			return fInitialState;
		}
		
		private int getState() {
			return (isCollapsed()) ? COLLAPSED_STATE : EXPANDED_STATE;
		}
		
		private void applyState(final int state) {
			switch (state) {
			case EXPANDED_STATE:
				markExpanded();
				break;
			case COLLAPSED_STATE:
				markCollapsed();
				break;
			}
		}
		
	}
	
	public static abstract interface FoldingProvider {
		
		boolean checkConfig(Set<String> groupIds);
		
		boolean isRestoreStateEnabled();
		
		boolean requiresModel();
		
		void collectRegions(FoldingStructureComputationContext ctx)
				throws InvocationTargetException;
		
	}
	
	public static interface NodeFoldingProvider extends FoldingProvider {
		
		ICommonAstVisitor createVisitor(FoldingStructureComputationContext ctx);
		
	}
	
	
	private static final class Input {
		
		private final ISourceUnit fUnit;
		
		private boolean fIsInitilized;
		private long fUpdateStamp;
		
		private QualifiedName fSavePropertyName;
		
		public ProjectionAnnotationModel fAnnotationModel;
		
		Input(final ISourceUnit unit) {
			fUnit = unit;
			fIsInitilized = false;
			fUpdateStamp = Long.MIN_VALUE;
		}
		
	}
	
	
	private final FoldingProvider fProvider;
	
	private SourceEditor1 fEditor;
	
	private volatile Input fInput;
	
	private final List<Input> fInputToSave = new ArrayList<FoldingEditorAddon.Input>();
	
	
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
		final Input input = (element != null) ? new Input((ISourceUnit) element) : null;
		synchronized (this) {
			if (fInput != null) {
				saveState(fInput);
			}
			fInput = input;
		}
	}
	
	@Override
	public void elementInitialInfo(final IModelElement element) {
		final Input input = fInput;
		if (input != null && input.fUnit == element) {
			update(input, -1);
		}
	}
	
	@Override
	public void elementUpdatedInfo(final IModelElement element, final IModelElementDelta delta) {
		final Input input = fInput;
		if (input != null && input.fUnit == element) {
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
		if (input.fUnit == null) {
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
		return new FoldingStructureComputationContext(document, modelInfo, ast, !input.fIsInitilized);
	}
	
	private void update(final Input input, final long stamp) {
		synchronized(input) {
			final SourceEditor1 editor = fEditor;
			if (editor == null) {
				return;
			}
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
				input.fAnnotationModel = (ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
				if (input.fAnnotationModel == null) {
					return;
				}
				input.fIsInitilized = true;
				input.fSavePropertyName = new QualifiedName("de.walware.ecommons.ltk", "FoldingState-" + editor.getSite().getId()); //$NON-NLS-1$ //$NON-NLS-2$
				
				if (fProvider.isRestoreStateEnabled()) {
					loadState(input, ctx.fTable);
				}
			}
			else {
				final ProjectionAnnotationModel model = input.fAnnotationModel;
				final List<FoldingAnnotation> del = new ArrayList<FoldingAnnotation>();
				for (final Iterator<FoldingAnnotation> iter = model.getAnnotationIterator(); iter.hasNext(); ) {
					final FoldingAnnotation ann = iter.next();
					final Position position = model.getPosition(ann);
					final FoldingAnnotation newAnn = ctx.fTable.remove(position);
					if (newAnn != null) {
						ann.fType = newAnn.fType;
					}
					else {
						del.add(ann);
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
			input.fAnnotationModel.modifyAnnotations(deletions, additions, null);
			input.fUpdateStamp = ctx.ast.stamp;
		}
	}
	
	
	//---- Persistence ----
	
	private static class EncodedValue {
		
		private static final int I_OFFSET = 0x20;
		private static final int I_SHIFT = 14;
		private static final int I_RADIX = 1 << I_SHIFT;
		private static final int I_MASK = I_RADIX - 1;
		private static final int I_FOLLOW = 1 << (I_SHIFT + 1);
		private static final int I_VALUE = I_FOLLOW - 1;
		
		
		public static void writeInt(final StringBuilder sb, int value) {
			while (true) {
				final int c = (value & I_MASK) + I_OFFSET;
				value >>>= I_SHIFT;
				if (value == 0) {
					sb.append((char) c);
					return;
				}
				sb.append((char) (I_FOLLOW | c));
			}
		}
		
		public static void writeLong(final StringBuilder sb, long value) {
			while (true) {
				final int c = (int) (value & I_MASK) + I_OFFSET;
				value >>>= I_SHIFT;
				if (value == 0) {
					sb.append((char) c);
					return;
				}
				sb.append((char) (I_FOLLOW | c));
			}
		}
		
		
		private final String fValue;
		
		private int fOffset;
		
		public EncodedValue(final String value) {
			fValue = value;
		}
		
		
		public boolean hasNext() {
			return fOffset < fValue.length();
		}
		
		public long readLong() {
			long value = 0;
			int shift = 0;
			while (true) {
				final int c = (fValue.charAt(fOffset++));
				if ((c & I_FOLLOW) == 0) {
					value |= (long) (c - I_OFFSET) << shift;
					return value;
				}
				value |= ((c & I_VALUE) - I_OFFSET) << shift;
				shift += I_SHIFT;
			}
		}
		
		public int readInt() {
			int value = 0;
			int shift = 0;
			while (true) {
				final int c = (fValue.charAt(fOffset++));
				if ((c & I_FOLLOW) == 0) {
					value |= (c - I_OFFSET) << shift;
					return value;
				}
				value |= ((c & I_VALUE) - I_OFFSET) << shift;
				shift += I_SHIFT;
			}
		}
		
	}
	
//	public static void main(String[] args) {
//		StringBuilder sb = new StringBuilder();
////		int start = 0;
////		int stop = 10000000;
//		int start = Integer.MAX_VALUE - 1000000;
//		int stop = Integer.MAX_VALUE;
//		for (int i = start; i < stop; i++) {
//			EncodedValue.writeInt(sb, i);
//		}
//		
//		EncodedValue v = new EncodedValue(sb.toString());
//		for (int i = start; i < stop; i++) {
//			int r = v.readInt();
//			if (i != r) {
//				System.out.println("ERROR " + i + " " + r);
//			}
//		}
//	}
	
	private static final int MAX_PERSISTENT_LENGTH = 2 * 1024;
	private static final int CURRENT_VERSION = 1;
	
	private class SaveJob extends Job {
		
		private final IResource fResource;
		private final QualifiedName fPropertyName;
		
		public SaveJob(final IResource resource, final QualifiedName propertyName) {
			super(NLS.bind("Save Folding State for '{0}'", resource.toString())); //$NON-NLS-1$
			setSystem(true);
			setUser(false);
			setPriority(Job.LONG);
			
			fResource = resource;
			fPropertyName = propertyName;
		}
		
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			try {
				if (!fResource.exists()) {
					return Status.OK_STATUS;
				}
				String value = (String) fResource.getSessionProperty(fPropertyName);
				value = checkValue(value);
				if (value != null) {
					fResource.setPersistentProperty(fPropertyName, value);
				}
				return Status.OK_STATUS;
			}
			catch (final CoreException e) {
				return new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
						NLS.bind("An error occurred when saving the code folding state for {0}", fResource.toString()),
						e );
			}
		}
		
		private String checkValue(final String value) {
			if (value == null || value.isEmpty()) {
				return null;
			}
			if (value.length() <= MAX_PERSISTENT_LENGTH) {
				return value;
			}
			final EncodedValue encoded = new EncodedValue(value);
			final StringBuilder sb = new StringBuilder(MAX_PERSISTENT_LENGTH);
			
			if (encoded.readInt() != CURRENT_VERSION) {
				return null;
			}
			EncodedValue.writeInt(sb, CURRENT_VERSION);
			EncodedValue.writeLong(sb, encoded.readLong());
			
			int collapedBegin = -1;
			int collapedEnd = -1;
			while (encoded.hasNext()) {
				final int l = sb.length();
				
				final int offset = encoded.readInt();
				final int length = encoded.readInt();
				final int state = encoded.readInt();
				
				if (offset >= collapedBegin && offset + length <= collapedEnd) {
					continue;
				}
				
				EncodedValue.writeInt(sb, offset);
				EncodedValue.writeInt(sb, length);
				EncodedValue.writeInt(sb, state);
				
				if (sb.length() > MAX_PERSISTENT_LENGTH) {
					return sb.substring(0, l);
				}
				
				if (state == COLLAPSED_STATE) {
					collapedBegin = offset;
					collapedEnd = offset + length;
				}
			}
			return sb.toString();
		}
		
	}
	
	private void saveState(final Input input) {
		final SourceEditor1 editor = fEditor;
		if (editor == null || !input.fIsInitilized || !input.fUnit.isSynchronized()
				|| !(input.fUnit instanceof IWorkspaceSourceUnit) ) {
			return;
		}
		final IResource resource = ((IWorkspaceSourceUnit) input.fUnit).getResource();
		if (resource == null || !resource.exists()) {
			return;
		}
		
		final String value;
		{	final StringBuilder sb = new StringBuilder(1024);
			
			EncodedValue.writeInt(sb, CURRENT_VERSION);
			EncodedValue.writeLong(sb, resource.getModificationStamp());
			
			final ProjectionAnnotationModel model = input.fAnnotationModel;
			for (final Iterator<FoldingAnnotation> iter = model.getAnnotationIterator(); iter.hasNext(); ) {
				final FoldingAnnotation ann = iter.next();
				final int state = ann.getState();
				if (state != ann.getInitialState()) {
					final Position position = model.getPosition(ann);
					if (position != null) {
						EncodedValue.writeInt(sb, position.getOffset());
						EncodedValue.writeInt(sb, position.getLength());
						EncodedValue.writeInt(sb, state);
					}
				}
			}
			value = sb.toString();
		}
		try {
			final QualifiedName propertyName = input.fSavePropertyName;
			
			resource.setSessionProperty(propertyName, value);
			
			new SaveJob(resource, propertyName).schedule();
		}
		catch (final CoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
					NLS.bind("An error occurred when saving the code folding state for {0}", resource.toString()),
					e ));
		}
	}
	
	private void loadState(final Input input, final SortedMap<Position, FoldingAnnotation> table) {
		final SourceEditor1 editor = fEditor;
		if (editor == null || !input.fIsInitilized || !input.fUnit.isSynchronized()
				|| !(input.fUnit instanceof IWorkspaceSourceUnit) ) {
			return;
		}
		final IResource resource = ((IWorkspaceSourceUnit) input.fUnit).getResource();
		if (resource == null || !resource.exists()) {
			return;
		}
		EncodedValue encoded;
		try {
			final QualifiedName propertyName = input.fSavePropertyName;
			
			String s = (String) resource.getSessionProperty(propertyName);
			if (s == null) {
				s = resource.getPersistentProperty(propertyName);
				if (s == null) {
					resource.setSessionProperty(propertyName, ""); //$NON-NLS-1$
					return;
				}
			}
			if (s.isEmpty()) {
				return;
			}
			encoded = new EncodedValue(s);
		}
		catch (final CoreException e) {
			return;
		}
		if (encoded.readInt() != CURRENT_VERSION) {
			return;
		}
		if (encoded.readLong() != resource.getModificationStamp()) {
			return;
		}
		{	final Position position = new Position(0, 0);
			while (encoded.hasNext()) {
				position.offset = encoded.readInt();
				position.length = encoded.readInt();
				final FoldingAnnotation ann = table.get(position);
				if (ann != null) {
					ann.applyState(encoded.readInt());
				}
			}
		}
	}
	
}
