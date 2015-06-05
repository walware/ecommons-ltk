/*=============================================================================#
 # Copyright (c) 2005-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.IDocumentProvider;

import de.walware.ecommons.text.core.IPartitionConstraint;

import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ast.AstSelection;
import de.walware.ecommons.ltk.core.ISourceModelStamp;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.ui.ISelectionWithElementInfoListener;
import de.walware.ecommons.ltk.ui.LTKInputData;


public abstract class AbstractMarkOccurrencesProvider implements ISourceEditorAddon,
		ISelectionWithElementInfoListener {
	
	
	private static final int CLEAR = -1;
	private static final int KEEP = 1;
	private static final int UPDATE = 2;
	
	
	public final class RunData {
		
		public final AbstractDocument doc;
		public ISourceModelStamp stamp;
		
		private Annotation[] annotations;
		private Point range;
		
		private int set = 0;
		private Map<Annotation, Position> todo;
		
		
		RunData(final AbstractDocument doc, final ISourceModelStamp stamp) {
			this.doc= doc;
			this.stamp= stamp;
		}
		
		
		public boolean isValid() {
			final Point currentSelection = fEditor.fCurrentSelection;
			return (this.range != null && currentSelection.x >= this.range.x
					&& currentSelection.x+currentSelection.y <= this.range.y
					&& this.doc.getModificationStamp() == this.stamp.getSourceStamp() );
		}
		
		public boolean accept(final Point range) {
			this.range = range;
			if (isValid()) {
				return true;
			}
			this.range = null;
			return false;
		}
		
		public void set(final Map<Annotation, Position> annotations) {
			this.set = UPDATE;
			this.todo = annotations;
		}
		
		public void keep() {
			this.set = KEEP;
		}
		
		public void clear() {
			this.set = CLEAR;
		}
		
	}
	
	
	private final SourceEditor1 fEditor;
	
	private final String fPartitioning;
	private final IPartitionConstraint fToleratePartitions;
	
	private boolean fIsMarkEnabled;
	private RunData fLastRun;
	
	
	public AbstractMarkOccurrencesProvider(final SourceEditor1 editor,
			final IPartitionConstraint toleratePartitions) {
		if (editor == null) {
			throw new NullPointerException("editor");
		}
		if (toleratePartitions == null) {
			throw new NullPointerException("validPartitions");
		}
		fEditor = editor;
		fPartitioning= fEditor.getDocumentContentInfo().getPartitioning();
		fToleratePartitions = toleratePartitions;
	}
	
	@Override
	public void install(final ISourceEditor editor) {
		fIsMarkEnabled = true;
		fEditor.addPostSelectionWithElementInfoListener(this);
	}
	
	@Override
	public void uninstall() {
		fIsMarkEnabled = false;
		fEditor.removePostSelectionWithElementInfoListener(this);
		removeAnnotations();
	}
	
	
	@Override
	public void inputChanged() {
		fLastRun = null;
	}
	
	@Override
	public void stateChanged(final LTKInputData state) {
		final ISelection selection = state.getSelection();
		final boolean ok = update((ISourceUnit) state.getInputElement(), state.getAstSelection(),
				(selection instanceof ITextSelection) ? (ITextSelection) selection : null );
		if (!ok && state.isStillValid()) {
			removeAnnotations();
		}
	}
	
	/**
	 * Updates the occurrences annotations based on the current selection.
	 * 
	 * @return <code>true</code> if the annotation is ok (still valid or updated), 
	 *     otherwise <code>false</code>
	 */
	protected boolean update(final ISourceUnit inputElement, final AstSelection astSelection,
			final ITextSelection orgSelection) {
		if (!fIsMarkEnabled) {
			return false;
		}
		try {
			final ISourceUnitModelInfo info = inputElement.getModelInfo(fEditor.getModelTypeId(),
					IModelManager.NONE, new NullProgressMonitor() );
			if (fEditor.getSourceUnit() != inputElement || info == null || astSelection == null) {
				return false;
			}
			final RunData run = new RunData(inputElement.getDocument(null), info.getStamp());
			if (run.doc == null) {
				return false;
			}
			if (fLastRun != null && fLastRun.isValid() && fLastRun.stamp.equals(run.stamp)) {
				return true;
			}
			
			doUpdate(run, info, astSelection, orgSelection);
			if (!fIsMarkEnabled) {
				return false;
			}
			
			if (run.set == 0) {
				checkKeep(run, orgSelection);
			}
			switch (run.set) {
			case KEEP:
				return true;
			case UPDATE:
				updateAnnotations(run);
				return true;
			default:
				removeAnnotations();
				return true;
			}
		}
		catch (final BadLocationException e) {
		}
		catch (final BadPartitioningException e) {
		}
		catch (final UnsupportedOperationException e) {
		}
		return false;
	}
	
	protected abstract void doUpdate(RunData run, ISourceUnitModelInfo info,
			AstSelection astSelection, ITextSelection orgSelection) 
			throws BadLocationException, BadPartitioningException, UnsupportedOperationException;
	
	
	protected void checkKeep(final RunData run, final ITextSelection selection)
			throws BadLocationException, BadPartitioningException {
		if (fLastRun == null || !fLastRun.stamp.equals(run.stamp)) {
			run.clear();
			return;
		}
		if (selection instanceof ITextSelection) {
			final ITextSelection textSelection = selection;
			final Point currentSelection = fEditor.fCurrentSelection;
			final int offset = textSelection.getOffset();
			final int docLength = run.doc.getLength();
			final ITypedRegion partition = run.doc.getPartition(fPartitioning, offset, false);
			if (docLength > 0 &&
					(	(currentSelection.y > 0)
					||	(offset != currentSelection.x)
					||	(textSelection.getLength() == 0
						&& partition != null && fToleratePartitions.matches(partition.getType())
						&& (offset <= 0 || !Character.isLetterOrDigit(run.doc.getChar(offset-1)) )
						&& (offset >= docLength || Character.isWhitespace(run.doc.getChar(offset)) ) )
					)) {
				run.keep();
				return;
			}
		}
		return;
	}
	
	protected IAnnotationModel getAnnotationModel() {
		final IDocumentProvider documentProvider = fEditor.getDocumentProvider();
		if (documentProvider == null) {
			throw new UnsupportedOperationException();
		}
		final IAnnotationModel annotationModel = documentProvider.getAnnotationModel(
				fEditor.getEditorInput());
		if (annotationModel == null || !(annotationModel instanceof IAnnotationModelExtension)) {
			throw new UnsupportedOperationException();
		}
		return annotationModel;
	}
	
	protected void updateAnnotations(final RunData run) throws BadLocationException {
		if (!run.isValid()) {
			return;
		}
		
		// Add occurrence annotations
		final IAnnotationModel annotationModel = getAnnotationModel();
//			create diff ?
//			if (fLastRun != null && Arrays.equals(run.name, fLastRun.name)) {
//			}
		final Annotation[] lastAnnotations = (fLastRun != null) ? fLastRun.annotations : null;
		synchronized (SourceEditor1.getLockObject(annotationModel)) {
			if (!run.isValid()) {
				return;
			}
			((IAnnotationModelExtension) annotationModel).replaceAnnotations(lastAnnotations, run.todo);
			run.annotations = run.todo.keySet().toArray(new Annotation[run.todo.keySet().size()]);
			run.todo = null;
			fLastRun = run;
		}
	}
	
	protected void removeAnnotations() {
		final IAnnotationModel annotationModel = getAnnotationModel();
		synchronized (SourceEditor1.getLockObject(annotationModel)) {
			if (fLastRun == null) {
				return;
			}
			((IAnnotationModelExtension) annotationModel).replaceAnnotations(fLastRun.annotations, null);
			fLastRun = null;
		}
	}
	
}
