/*=============================================================================#
 # Copyright (c) 2014-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filebuffers.IDocumentSetupParticipantExtension;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.eclipse.ui.texteditor.IDocumentProviderExtension2;
import org.eclipse.ui.texteditor.IDocumentProviderExtension3;
import org.eclipse.ui.texteditor.IDocumentProviderExtension4;
import org.eclipse.ui.texteditor.IDocumentProviderExtension5;
import org.eclipse.ui.texteditor.IElementStateListener;

import de.walware.ecommons.ltk.IDocumentModelProvider;
import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * Forward provider for {@link SourceDocumentProvider}s allowing special document setup
 * (similar to {@link org.eclipse.ui.editors.text.ForwardingDocumentProvider}).
 */
public class ForwardSourceDocumentProvider implements IDocumentProvider,
		IDocumentProviderExtension, IDocumentProviderExtension2, IDocumentProviderExtension3, IDocumentProviderExtension4, IDocumentProviderExtension5,
		IStorageDocumentProvider, IDocumentModelProvider {
	
	
	private final SourceDocumentProvider<?> parentProvider;
	
	private final IDocumentSetupParticipant documentSetup;
	
	
	public ForwardSourceDocumentProvider(final SourceDocumentProvider<?> parentProvider,
			final IDocumentSetupParticipant documentSetup) {
		if (parentProvider == null) {
			throw new NullPointerException("parentProvider"); //$NON-NLS-1$
		}
		if (documentSetup == null) {
			throw new NullPointerException("documentSetup"); //$NON-NLS-1$
		}
		
		this.parentProvider= parentProvider;
		this.documentSetup= documentSetup;
	}
	
	
	@Override
	public void connect(final Object element) throws CoreException {
		this.parentProvider.connect(element);
		
		final IDocument document= getDocument(element);
		if (this.documentSetup instanceof IDocumentSetupParticipantExtension
				&& element instanceof IFileEditorInput) {
			final IFile file= ((IFileEditorInput) element).getFile();
			if (file != null) {
				((IDocumentSetupParticipantExtension) this.documentSetup).setup(document,
					file.getFullPath(), LocationKind.IFILE);
				return;
			}
		}
		this.documentSetup.setup(document);
	}
	
	@Override
	public void disconnect(final Object element) {
		this.parentProvider.disconnect(element);
	}
	
	@Override
	public IDocument getDocument(final Object element) {
		return this.parentProvider.getDocument(element);
	}
	
	@Override
	public void resetDocument(final Object element) throws CoreException {
		this.parentProvider.resetDocument(element);
	}
	
	@Override
	public void saveDocument(final IProgressMonitor monitor, final Object element,
			final IDocument document, final boolean overwrite) throws CoreException {
		this.parentProvider.saveDocument(monitor, element, document, overwrite);
	}
	
	@Override
	public long getModificationStamp(final Object element) {
		return this.parentProvider.getModificationStamp(element);
	}
	
	@Override
	public long getSynchronizationStamp(final Object element) {
		return this.parentProvider.getSynchronizationStamp(element);
	}
	
	@Override
	public boolean isDeleted(final Object element) {
		return this.parentProvider.isDeleted(element);
	}
	
	@Override
	public boolean mustSaveDocument(final Object element) {
		return this.parentProvider.mustSaveDocument(element);
	}
	
	@Override
	public boolean canSaveDocument(final Object element) {
		return this.parentProvider.canSaveDocument(element);
	}
	
	@Override
	public IAnnotationModel getAnnotationModel(final Object element) {
		return this.parentProvider.getAnnotationModel(element);
	}
	
	@Override
	public void aboutToChange(final Object element) {
		this.parentProvider.aboutToChange(element);
	}
	
	@Override
	public void changed(final Object element) {
		this.parentProvider.changed(element);
	}
	
	@Override
	public void addElementStateListener(final IElementStateListener listener) {
		this.parentProvider.addElementStateListener(listener);
	}
	
	@Override
	public void removeElementStateListener(final IElementStateListener listener) {
		this.parentProvider.removeElementStateListener(listener);
	}
	
	@Override
	public boolean isReadOnly(final Object element) {
		return this.parentProvider.isReadOnly(element);
	}
	
	@Override
	public boolean isModifiable(final Object element) {
		return this.parentProvider.isModifiable(element);
	}
	
	@Override
	public void validateState(final Object element, final Object computationContext) throws CoreException {
		this.parentProvider.validateState(element, computationContext);
	}
	
	@Override
	public boolean isStateValidated(final Object element) {
		return this.parentProvider.isStateValidated(element);
	}
	
	@Override
	public void updateStateCache(final Object element) throws CoreException {
		this.parentProvider.updateStateCache(element);
	}
	
	@Override
	public void setCanSaveDocument(final Object element) {
		this.parentProvider.setCanSaveDocument(element);
	}
	
	@Override
	public IStatus getStatus(final Object element) {
		return this.parentProvider.getStatus(element);
	}
	
	@Override
	public void synchronize(final Object element) throws CoreException {
		this.parentProvider.synchronize(element);
	}
	
	@Override
	public void setProgressMonitor(final IProgressMonitor progressMonitor) {
		this.parentProvider.setProgressMonitor(progressMonitor);
	}
	
	@Override
	public IProgressMonitor getProgressMonitor() {
		return this.parentProvider.getProgressMonitor();
	}
	
	@Override
	public boolean isSynchronized(final Object element) {
		return this.parentProvider.isSynchronized(element);
	}
	
	@Override
	public IContentType getContentType(final Object element) throws CoreException {
		return this.parentProvider.getContentType(element);
	}
	
	@Override
	public boolean isNotSynchronizedException(final Object element, final CoreException ex) {
		return this.parentProvider.isNotSynchronizedException(element, ex);
	}
	
	
	@Override
	public String getDefaultEncoding() {
		return this.parentProvider.getDefaultEncoding();
	}
	
	@Override
	public String getEncoding(final Object element) {
		return this.parentProvider.getEncoding(element);
	}
	
	@Override
	public void setEncoding(final Object element, final String encoding) {
		this.parentProvider.setEncoding(element, encoding);
	}
	
	
	@Override
	public ISourceUnit getWorkingCopy(final Object element) {
		return this.parentProvider.getWorkingCopy(element);
	}
	
}
