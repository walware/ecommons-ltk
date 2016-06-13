/*=============================================================================#
 # Copyright (c) 2007-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.folding;

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
import de.walware.ecommons.ltk.IModelElementDelta;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.core.ISourceModelStamp;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.core.model.IWorkspaceSourceUnit;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.IModelElementInputListener;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditorAddon;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditor1;


public class FoldingEditorAddon implements ISourceEditorAddon, IModelElementInputListener, ChangeListener {
	
	
	public static final class FoldingStructureComputationContext {
		
		public final AbstractDocument document;
		public final ISourceUnitModelInfo model;
		public final AstInfo ast;
		
		public final boolean isInitial;
		
		private final SortedMap<Position, FoldingAnnotation> table= new TreeMap<>(TextUtil.POSITION_COMPARATOR);
		
		
		protected FoldingStructureComputationContext(final AbstractDocument document,
				final ISourceUnitModelInfo model, final AstInfo ast, final boolean isInitial) {
			this.document= document;
			this.model= model;
			this.ast= ast;
			
			this.isInitial= isInitial;
		}
		
		
		public void addFoldingRegion(final FoldingAnnotation ann) {
			if (!this.table.containsKey(ann.getPosition())) {
				this.table.put(ann.getPosition(), ann);
			}
		}
		
	}
	
	private static final class Input {
		
		private final ISourceUnit fUnit;
		
		private boolean fIsInitilized;
		private ISourceModelStamp fUpdateStamp;
		
		private QualifiedName fSavePropertyName;
		
		public ProjectionAnnotationModel fAnnotationModel;
		
		Input(final ISourceUnit unit) {
			this.fUnit= unit;
			this.fIsInitilized= false;
		}
		
	}
	
	
	private final FoldingProvider provider;
	
	private SourceEditor1 editor;
	
	private volatile Input input;
	
	
	public FoldingEditorAddon(final FoldingProvider provider) {
		this.provider= provider;
	}
	
	
	@Override
	public void install(final ISourceEditor editor) {
		this.editor= (SourceEditor1) editor;
		PreferencesUtil.getSettingsChangeNotifier().addChangeListener(this);
		this.provider.checkConfig(null);
		this.editor.getModelInputProvider().addListener(this);
	}
	
	@Override
	public void elementChanged(final IModelElement element) {
		final Input input= (element != null) ? new Input((ISourceUnit) element) : null;
		synchronized (this) {
			if (this.input != null) {
				saveState(this.input);
			}
			this.input= input;
		}
	}
	
	@Override
	public void elementInitialInfo(final IModelElement element) {
		final Input input= this.input;
		if (input != null && input.fUnit == element) {
			update(input, null);
		}
	}
	
	@Override
	public void elementUpdatedInfo(final IModelElement element, final IModelElementDelta delta) {
		final Input input= this.input;
		if (input != null && input.fUnit == element) {
			update(input, delta.getNewAst().getStamp());
		}
	}
	
	@Override
	public void uninstall() {
		PreferencesUtil.getSettingsChangeNotifier().removeChangeListener(this);
		if (this.editor != null) {
			this.editor.getModelInputProvider().removeListener(this);
			this.editor= null;
		}
	}
	
	protected void refresh() {
		final Input input= this.input;
		if (input != null) {
			update(input, null);
		}
	}
	
	@Override
	public void settingsChanged(final Set<String> groupIds) {
		if (groupIds != null && this.provider.checkConfig(groupIds)) {
			refresh();
		}
	}
	
	private FoldingStructureComputationContext createCtx(final Input input) {
		if (input.fUnit == null) {
			return null;
		}
		final IProgressMonitor monitor= new NullProgressMonitor();
		
		final ISourceUnitModelInfo modelInfo;
		final AstInfo ast;
		if (this.provider.requiresModel()) {
			modelInfo= input.fUnit.getModelInfo(null, IModelManager.MODEL_FILE, monitor);
			if (modelInfo == null) {
				return null;
			}
			ast= modelInfo.getAst();
		}
		else {
			modelInfo= null;
			ast= input.fUnit.getAstInfo(null, false, monitor);
		}
		final AbstractDocument document= input.fUnit.getDocument(monitor);
		if (ast == null || document == null || ast.getStamp().getSourceStamp() != document.getModificationStamp()) {
			return null;
		}
		return new FoldingStructureComputationContext(document, modelInfo, ast, !input.fIsInitilized);
	}
	
	private void update(final Input input, final ISourceModelStamp stamp) {
		synchronized(input) {
			final SourceEditor1 editor= this.editor;
			if (editor == null) {
				return;
			}
			if (input.fUnit == null
					|| (stamp != null && stamp.equals(input.fUpdateStamp)) ) { // already uptodate
				return;
			}
			FoldingStructureComputationContext ctx;
			if (input != this.input) {
				return;
			}
			ctx= createCtx(input);
			if (ctx == null) {
				return;
			}
			try {
				this.provider.collectRegions(ctx);
			}
			catch (final InvocationTargetException e) {
				return;
			}
			
			ProjectionAnnotation[] deletions;
			if (ctx.isInitial) {
				deletions= null;
				input.fAnnotationModel= (ProjectionAnnotationModel) this.editor.getAdapter(ProjectionAnnotationModel.class);
				if (input.fAnnotationModel == null) {
					return;
				}
				input.fIsInitilized= true;
				input.fSavePropertyName= new QualifiedName("de.walware.ecommons.ltk", "FoldingState-" + editor.getSite().getId()); //$NON-NLS-1$ //$NON-NLS-2$
				
				if (this.provider.isRestoreStateEnabled()) {
					loadState(input, ctx.table);
				}
			}
			else {
				final ProjectionAnnotationModel model= input.fAnnotationModel;
				final List<FoldingAnnotation> del= new ArrayList<>();
				for (final Iterator<FoldingAnnotation> iter= (Iterator) model.getAnnotationIterator(); iter.hasNext(); ) {
					final FoldingAnnotation ann= iter.next();
					final Position position= model.getPosition(ann);
					final FoldingAnnotation newAnn= ctx.table.remove(position);
					if (newAnn != null) {
						if (!ann.update(newAnn)) {
							del.add(ann);
							ctx.table.put(newAnn.getPosition(), newAnn);
						}
					}
					else {
						del.add(ann);
					}
				}
				deletions= del.toArray(new FoldingAnnotation[del.size()]);
				if (ctx.document.getModificationStamp() != ctx.ast.getStamp().getSourceStamp()
						|| input != this.input) {
					return;
				}
			}
			final LinkedHashMap<FoldingAnnotation, Position> additions= new LinkedHashMap<>();
			for (final Iterator<Entry<Position, FoldingAnnotation>> iter= ctx.table.entrySet().iterator(); iter.hasNext(); ) {
				final Entry<Position, FoldingAnnotation> next= iter.next();
				additions.put(next.getValue(), next.getKey());
			}
			input.fAnnotationModel.modifyAnnotations(deletions, additions, null);
			input.fUpdateStamp= ctx.ast.getStamp();
		}
	}
	
	
	//---- Persistence ----
	
	private static class EncodedValue {
		
		private static final int I_OFFSET= 0x20;
		private static final int I_SHIFT= 14;
		private static final int I_RADIX= 1 << I_SHIFT;
		private static final int I_MASK= I_RADIX - 1;
		private static final int I_FOLLOW= 1 << (I_SHIFT + 1);
		private static final int I_VALUE= I_FOLLOW - 1;
		
		
		public static void writeInt(final StringBuilder sb, int value) {
			while (true) {
				final int c= (value & I_MASK) + I_OFFSET;
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
				final int c= (int) (value & I_MASK) + I_OFFSET;
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
			this.fValue= value;
		}
		
		
		public boolean hasNext() {
			return this.fOffset < this.fValue.length();
		}
		
		public long readLong() {
			long value= 0;
			int shift= 0;
			while (true) {
				final int c= (this.fValue.charAt(this.fOffset++));
				if ((c & I_FOLLOW) == 0) {
					value |= (long) (c - I_OFFSET) << shift;
					return value;
				}
				value |= ((c & I_VALUE) - I_OFFSET) << shift;
				shift += I_SHIFT;
			}
		}
		
		public int readInt() {
			int value= 0;
			int shift= 0;
			while (true) {
				final int c= (this.fValue.charAt(this.fOffset++));
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
//		StringBuilder sb= new StringBuilder();
////		int start= 0;
////		int stop= 10000000;
//		int start= Integer.MAX_VALUE - 1000000;
//		int stop= Integer.MAX_VALUE;
//		for (int i= start; i < stop; i++) {
//			EncodedValue.writeInt(sb, i);
//		}
//		
//		EncodedValue v= new EncodedValue(sb.toString());
//		for (int i= start; i < stop; i++) {
//			int r= v.readInt();
//			if (i != r) {
//				System.out.println("ERROR " + i + " " + r);
//			}
//		}
//	}
	
	private static final int MAX_PERSISTENT_LENGTH= 2 * 1024;
	private static final int CURRENT_VERSION= 1;
	
	private class SaveJob extends Job {
		
		private final IResource fResource;
		private final QualifiedName fPropertyName;
		
		public SaveJob(final IResource resource, final QualifiedName propertyName) {
			super(NLS.bind("Save Folding State for ''{0}''", resource.toString())); //$NON-NLS-1$
			setSystem(true);
			setUser(false);
			setPriority(Job.LONG);
			
			this.fResource= resource;
			this.fPropertyName= propertyName;
		}
		
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			try {
				if (!this.fResource.exists()) {
					return Status.OK_STATUS;
				}
				String value= (String) this.fResource.getSessionProperty(this.fPropertyName);
				value= checkValue(value);
				if (value != null) {
					this.fResource.setPersistentProperty(this.fPropertyName, value);
				}
				return Status.OK_STATUS;
			}
			catch (final CoreException e) {
				return new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
						NLS.bind("An error occurred when saving the code folding state for {0}", this.fResource.toString()),
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
			final EncodedValue encoded= new EncodedValue(value);
			final StringBuilder sb= new StringBuilder(MAX_PERSISTENT_LENGTH);
			
			if (encoded.readInt() != CURRENT_VERSION) {
				return null;
			}
			EncodedValue.writeInt(sb, CURRENT_VERSION);
			EncodedValue.writeLong(sb, encoded.readLong());
			
			int collapedBegin= -1;
			int collapedEnd= -1;
			while (encoded.hasNext()) {
				final int l= sb.length();
				
				final int offset= encoded.readInt();
				final int length= encoded.readInt();
				final int state= encoded.readInt();
				
				if (offset >= collapedBegin && offset + length <= collapedEnd) {
					continue;
				}
				
				EncodedValue.writeInt(sb, offset);
				EncodedValue.writeInt(sb, length);
				EncodedValue.writeInt(sb, state);
				
				if (sb.length() > MAX_PERSISTENT_LENGTH) {
					return sb.substring(0, l);
				}
				
				if (state == FoldingAnnotation.COLLAPSED_STATE) {
					collapedBegin= offset;
					collapedEnd= offset + length;
				}
			}
			return sb.toString();
		}
		
	}
	
	private void saveState(final Input input) {
		final SourceEditor1 editor= this.editor;
		if (editor == null || !input.fIsInitilized || !input.fUnit.isSynchronized()
				|| !(input.fUnit instanceof IWorkspaceSourceUnit) ) {
			return;
		}
		final IResource resource= ((IWorkspaceSourceUnit) input.fUnit).getResource();
		if (resource == null || !resource.exists()) {
			return;
		}
		
		final String value;
		{	final StringBuilder sb= new StringBuilder(1024);
			
			EncodedValue.writeInt(sb, CURRENT_VERSION);
			EncodedValue.writeLong(sb, resource.getModificationStamp());
			
			final ProjectionAnnotationModel model= input.fAnnotationModel;
			for (final Iterator<FoldingAnnotation> iter= (Iterator) model.getAnnotationIterator(); iter.hasNext(); ) {
				final FoldingAnnotation ann= iter.next();
				final int state= ann.getState();
				if (state != ann.getInitialState()) {
					final Position position= model.getPosition(ann);
					if (position != null) {
						EncodedValue.writeInt(sb, position.getOffset());
						EncodedValue.writeInt(sb, position.getLength());
						EncodedValue.writeInt(sb, state);
					}
				}
			}
			value= sb.toString();
		}
		try {
			final QualifiedName propertyName= input.fSavePropertyName;
			
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
		final SourceEditor1 editor= this.editor;
		if (editor == null || !input.fIsInitilized || !input.fUnit.isSynchronized()
				|| !(input.fUnit instanceof IWorkspaceSourceUnit) ) {
			return;
		}
		final IResource resource= ((IWorkspaceSourceUnit) input.fUnit).getResource();
		if (resource == null || !resource.exists()) {
			return;
		}
		EncodedValue encoded;
		try {
			final QualifiedName propertyName= input.fSavePropertyName;
			
			String s= (String) resource.getSessionProperty(propertyName);
			if (s == null) {
				s= resource.getPersistentProperty(propertyName);
				if (s == null) {
					resource.setSessionProperty(propertyName, ""); //$NON-NLS-1$
					return;
				}
			}
			if (s.isEmpty()) {
				return;
			}
			encoded= new EncodedValue(s);
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
		{	final Position position= new Position(0, 0);
			while (encoded.hasNext()) {
				position.offset= encoded.readInt();
				position.length= encoded.readInt();
				final FoldingAnnotation ann= table.get(position);
				if (ann != null) {
					ann.applyState(encoded.readInt());
				}
			}
		}
	}
	
}
