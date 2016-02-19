/*=============================================================================#
 # Copyright (c) 2005-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;

import de.walware.ecommons.text.PartitionerDocumentSetupParticipant;

import de.walware.ecommons.ltk.IDocumentModelProvider;
import de.walware.ecommons.ltk.IProblemRequestor;
import de.walware.ecommons.ltk.ISourceUnitManager;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.IWorkspaceSourceUnit;


public class SourceDocumentProvider<T extends ISourceUnit> extends TextFileDocumentProvider
		implements IDocumentModelProvider {
	
	
	public static class SourceFileInfo extends FileInfo {
		
		private ISourceUnit workingCopy;
		
	}
	
	
	private final String modelTypeId;
	private final PartitionerDocumentSetupParticipant documentSetupParticipant;
	
	
	public SourceDocumentProvider(final String modelTypeId, final PartitionerDocumentSetupParticipant documentSetupParticipant) {
		if (modelTypeId == null) {
			throw new NullPointerException("modelTypeId"); //$NON-NLS-1$
		}
		this.modelTypeId = modelTypeId;
		this.documentSetupParticipant = documentSetupParticipant;
//		final IDocumentProvider provider = new ForwardingDocumentProvider(documentSetupParticipant.getPartitioningId(),
//				fDocumentSetupParticipant, new TextFileDocumentProvider());
//		setParentDocumentProvider(provider);
	}
	
	
	@Override
	protected FileInfo createEmptyFileInfo() {
		return new SourceFileInfo();
	}
	
	@Override
	public void disconnect(final Object element) {
		final FileInfo info = getFileInfo(element);
		if (info instanceof SourceFileInfo) {
			final SourceFileInfo rinfo = (SourceFileInfo) info;
			if (rinfo.fCount == 1 && rinfo.workingCopy != null) {
				final IProgressMonitor monitor = getProgressMonitor();
				final SubMonitor progress = SubMonitor.convert(monitor, 1);
				try {
					rinfo.workingCopy.disconnect(progress.newChild(1));
				}
				finally {
					rinfo.workingCopy = null;
					if (monitor != null) {
						monitor.done();
					}
				}
			}
		}
		super.disconnect(element);
	}
	
	@Override
	protected FileInfo createFileInfo(final Object element) throws CoreException {
		final FileInfo info = super.createFileInfo(element);
		
		if (!(info instanceof SourceFileInfo)) {
			return null;
		}
		
		{	final IDocument document = getDocument(element);
			if (document instanceof AbstractDocument) {
				setupDocument((AbstractDocument) document);
			}
		}
		
		final IAdaptable adaptable = (IAdaptable) element;
		final SourceFileInfo sourceInfo = (SourceFileInfo) info;
		
		final IProgressMonitor monitor = getProgressMonitor();
		final SubMonitor progress = SubMonitor.convert(monitor, 2);
		try {
			final Object ifile = adaptable.getAdapter(IFile.class);
			final ISourceUnitManager suManager = LTK.getSourceUnitManager();
			if (ifile != null) {
				final ISourceUnit pUnit = suManager.getSourceUnit(this.modelTypeId, LTK.PERSISTENCE_CONTEXT, ifile, true, progress.newChild(1));
				sourceInfo.workingCopy = suManager.getSourceUnit(this.modelTypeId, LTK.EDITOR_CONTEXT, pUnit, true, progress.newChild(1));
			}
			else if (element instanceof IURIEditorInput) {
				final IFileStore store;
				try {
					store = EFS.getStore(((IURIEditorInput) element).getURI());
				}
				catch (final CoreException e) {
					return sourceInfo;
				}
				sourceInfo.workingCopy = suManager.getSourceUnit(this.modelTypeId, LTK.EDITOR_CONTEXT, store, true, progress.newChild(1));
			}
		}
		finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		
		return sourceInfo;
	}
	
	protected void setupDocument(final AbstractDocument document) {
		if (this.documentSetupParticipant != null) {
			this.documentSetupParticipant.setup(document);
		}
	}
	
	@Override
	public T getWorkingCopy(final Object element) {
		final FileInfo fileInfo = getFileInfo(element);
		if (fileInfo instanceof SourceFileInfo) {
			return (T) ((SourceFileInfo) fileInfo).workingCopy;
		}
		return null;
	}
	
	@Override
	public IAnnotationModel getAnnotationModel(Object element) {
		if (element instanceof IWorkspaceSourceUnit) {
			element = new FileEditorInput((IFile) ((IWorkspaceSourceUnit) element).getResource());
		}
		return super.getAnnotationModel(element);
	}
	
	public IProblemRequestor createProblemRequestor(final ISourceUnit element) {
		final IAnnotationModel annotationModel = getAnnotationModel(element);
		if (annotationModel instanceof SourceAnnotationModel) {
			return ((SourceAnnotationModel) annotationModel).createProblemRequestor();
		}
		return null;
	}
	
}
