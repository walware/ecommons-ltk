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

package de.walware.ecommons.ltk.core.impl;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;

import de.walware.ecommons.ltk.core.SourceContent;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.internal.core.LTKCorePlugin;


/**
 * WorkingBuffer using {@link ITextFileBuffer} and following JFace UI rules.
 * <p>
 * Usually used for editors / the editor context.</p>
 */
public class FileBufferWorkingBuffer extends WorkingBuffer {
	
	
	private ITextFileBuffer fileBuffer;
	
	
	public FileBufferWorkingBuffer(final ISourceUnit unit) {
		super(unit);
	}
	
	
	@Override
	protected AbstractDocument createDocument(final SubMonitor progress) {
		if (detectMode()) {
			if (getMode() == IFILE) {
				final IPath path= ((IFile) this.unit.getResource()).getFullPath();
				try {
					FileBuffers.getTextFileBufferManager().connect(path, LocationKind.IFILE, progress);
					this.fileBuffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path, LocationKind.IFILE);
				}
				catch (final CoreException e) {
					LTKCorePlugin.getSafe().log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID, -1,
							"An error occurred when allocating the document of the file buffer.", e ));
				}
			}
			else if (getMode() == FILESTORE) {
				final IFileStore store= (IFileStore) this.unit.getResource();
				try {
					FileBuffers.getTextFileBufferManager().connectFileStore(store, progress);
					this.fileBuffer= FileBuffers.getTextFileBufferManager().getFileStoreTextFileBuffer(store);
				}
				catch (final CoreException e) {
					LTKCorePlugin.getSafe().log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID, -1,
							"An error occurred when allocating the document of the file buffer.", e ));
				}
			}
			if (this.fileBuffer != null) {
				final IDocument fileDoc= this.fileBuffer.getDocument();
				if (fileDoc instanceof AbstractDocument) {
					return (AbstractDocument) fileDoc;
				}
			}
			return null;
		}
		return super.createDocument(progress);
	}
	
	private ITextFileBuffer getBuffer() {
		synchronized (this) {
			if (this.fileBuffer != null) {
				return this.fileBuffer;
			};
		}
		if (getMode() == IFILE) {
			final IPath path= ((IFile) this.unit.getResource()).getFullPath();
			return FileBuffers.getTextFileBufferManager().getTextFileBuffer(path, LocationKind.IFILE);
		}
		else if (getMode() == FILESTORE) {
			final IFileStore store= (IFileStore) this.unit.getResource();
			return FileBuffers.getTextFileBufferManager().getFileStoreTextFileBuffer(store);
		}
		return null;
	}
	
	@Override
	protected SourceContent createContent(final SubMonitor progress) {
		if (detectMode()) {
			final ITextFileBuffer buffer= getBuffer();
			if (buffer != null) {
				return createContentFromDocument(buffer.getDocument());
			}
		}
		return super.createContent(progress);
	}
	
	@Override
	public void releaseDocument(final IProgressMonitor monitor) {
		if (this.fileBuffer != null) {
			try {
				final SubMonitor progress= SubMonitor.convert(monitor);
				if (getMode() == IFILE) {
					final IPath path= ((IFile) this.unit.getResource()).getFullPath();
					FileBuffers.getTextFileBufferManager().disconnect(path, LocationKind.IFILE, progress);
				}
				else if (getMode() == FILESTORE) {
					final IFileStore store= (IFileStore) this.unit.getResource();
					FileBuffers.getTextFileBufferManager().disconnectFileStore(store, progress);
				}
			}
			catch (final CoreException e) {
				LTKCorePlugin.getSafe().log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID, -1,
						"An error occurred when releasing the document of the file buffer.", e ));
			}
			finally {
				this.fileBuffer= null;
				super.releaseDocument(monitor);
			}
			return;
		}
		else {
			super.releaseDocument(monitor);
		}
	}
	
	@Override
	public boolean checkState(final boolean validate, final IProgressMonitor monitor) {
		final ITextFileBuffer buffer= this.fileBuffer;
		if (buffer != null) {
			if (!validate && !buffer.isStateValidated()) {
				return true;
			}
			if (validate && !buffer.isStateValidated()) {
				try {
					buffer.validateState(monitor, IWorkspace.VALIDATE_PROMPT);
				}
				catch (final CoreException e) {
					LTKCorePlugin.getSafe().log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID, -1,
							"An error occurred when validating file buffer state.", e ));
				}
			}
		}
		return super.checkState(validate, monitor);
	}
	
	@Override
	public boolean isSynchronized() {
		if (detectMode()) {
			final ITextFileBuffer buffer= getBuffer();
			if (buffer != null) {
				return !buffer.isDirty();
			}
		}
		return true;
	}
	
}
