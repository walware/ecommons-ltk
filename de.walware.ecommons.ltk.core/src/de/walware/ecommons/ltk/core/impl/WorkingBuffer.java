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

package de.walware.ecommons.ltk.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ISynchronizable;

import de.walware.ecommons.ICommonStatusConstants;
import de.walware.ecommons.io.FileUtil;

import de.walware.ecommons.ltk.core.SourceContent;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.internal.core.LTKCorePlugin;


/**
 * Common implementation of {@link IWorkingBuffer} for source units based on
 * an {@link IFile} or an {@link IFileStore}.
 */
public class WorkingBuffer implements IWorkingBuffer {
	
	/** Mode for IFile (in workspace) */
	protected static final byte IFILE= 1;
	/** Mode for IFileStore (URI) */
	protected static final byte FILESTORE= 2;
	
	protected static final byte DOCUMENT= 1;
	
	
	public static SourceContent createContentFromDocument(final IDocument doc) {
		Object lock= null;
		if (doc instanceof ISynchronizable) {
			lock= ((ISynchronizable) doc).getLockObject();
		}
		if (lock != null && doc instanceof IDocumentExtension4) {
			synchronized (lock) {
				return new SourceContent(
						((IDocumentExtension4) doc).getModificationStamp(),
						doc.get() );
			}
		}
		else {
			return new SourceContent(System.currentTimeMillis(), doc.get());
		}
	}
	
	
	protected final ISourceUnit unit;
	private AbstractDocument document;
	
	/**
	 * Mode of this working buffer:<ul>
	 *   <li>= 0 - uninitialized</li>
	 *   <li>< 0 - invalid/no source found</li>
	 *   <li>> 0 - mode constant {@link #IFILE}, {@link #FILESTORE}</li>
	 * </ul>
	 */
	private byte mode;
	
	public WorkingBuffer(final ISourceUnit unit) {
		this.unit= unit;
	}
	
	
	/**
	 * Checks the mode of this working buffer
	 * 
	 * @return <code>true</code> if valid mode, otherwise <code>false</code>
	 */
	protected final byte detectResourceMode() {
		if (this.mode == 0) {
			final Object resource= this.unit.getResource();
			if (resource instanceof IFile) {
				this.mode= IFILE;
			}
			else if (resource instanceof IFileStore 
					&& !((IFileStore) resource).fetchInfo().isDirectory() ) {
				this.mode= FILESTORE;
			}
			if (this.mode == 0) {
				this.mode= -1;
			}
		}
		return this.mode;
	}
	
	protected final byte getResourceMode() {
		return this.mode;
	}
	
	protected byte getContentMode() {
		return 0;
	}
	
	
	@Override
	public long getContentStamp(final IProgressMonitor monitor) {
		{	final AbstractDocument doc= this.document;
			if (doc != null) {
				return doc.getModificationStamp();
			}
		}
		{	final ISourceUnit underlyingUnit= this.unit.getUnderlyingUnit();
			if (underlyingUnit != null) {
				return underlyingUnit.getContentStamp(monitor);
			}
		}
		if (detectResourceMode() == IFILE) {
			final IFile resource= (IFile) this.unit.getResource();
			if (resource != null) {
				return resource.getModificationStamp();
			}
		}
		return 0;
	}
	
	@Override
	public synchronized AbstractDocument getDocument() {
		return this.document;
	}
	
	@Override
	public synchronized AbstractDocument getDocument(final IProgressMonitor monitor) {
		AbstractDocument doc= this.document;
		if (doc == null) {
			final SubMonitor progress= SubMonitor.convert(monitor);
			doc= createDocument(progress);
			checkDocument(doc);
			if ((getContentMode() & DOCUMENT) != 0) {
				this.document= doc;
			}
		}
		return doc;
	}
	
	@Override
	public SourceContent getContent(final IProgressMonitor monitor) {
		final SubMonitor progress= SubMonitor.convert(monitor);
		final IDocument doc= ((getContentMode() & DOCUMENT) != 0) ? getDocument(monitor) : getDocument();
		if (doc != null) {
			return createContentFromDocument(doc);
		}
		return createContent(progress);
	}
	
	@Override
	public void saveDocument(final IProgressMonitor monitor) {
	}
	
	@Override
	public synchronized void releaseDocument(final IProgressMonitor monitor) {
		this.document= null;
	}
	
	@Override
	public boolean checkState(final boolean validate, final IProgressMonitor monitor) {
		{	final ISourceUnit underlyingUnit= this.unit.getUnderlyingUnit();
			if (underlyingUnit != null) {
				return underlyingUnit.checkState(validate, monitor);
			}
		}
		switch (detectResourceMode()) {
		case IFILE:
			{	final IFile resource= (IFile) this.unit.getResource();
				if (!validate) {
					return !resource.getResourceAttributes().isReadOnly();
				}
				else {
					return resource.getWorkspace().validateEdit(new IFile[] { resource }, IWorkspace.VALIDATE_PROMPT).isOK();
				}
			}
		case FILESTORE:
			{	final IFileStore store= (IFileStore) this.unit.getResource();
				try {
					return !store.fetchInfo(EFS.NONE, monitor).getAttribute(EFS.ATTRIBUTE_READ_ONLY);
				}
				catch (final CoreException e) {
					LTKCorePlugin.log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID, ICommonStatusConstants.IO_ERROR,
							"An error occurred when checking modifiable state of the file.", e));
					return false;
				}
			}
		default:
			return false;
		}
	}
	
	
	protected AbstractDocument createDocument(final SubMonitor progress) {
		final IDocument fileDoc= createEmptyDocument();
		if (!(fileDoc instanceof AbstractDocument)) {
			return null;
		}
		final AbstractDocument document= (AbstractDocument) fileDoc;
		
		final ISourceUnit underlyingUnit= this.unit.getUnderlyingUnit();
		if (underlyingUnit != null) {
			final SourceContent underlyingContent= underlyingUnit.getContent(progress);
//			if (document instanceof IDocumentExtension4) {
			document.set(underlyingContent.getText(), underlyingContent.getStamp());
//			}
//			else {
//				document.set(underlyingContent.text);
//			}
		}
		else {
			final Object resource= this.unit.getResource();
			if (resource instanceof IFile) {
				loadDocumentFromFile((IFile) resource, document, progress);
			}
		}
		
		return document;
	}
	
	private IDocument createEmptyDocument() {
		switch (detectResourceMode()) {
		case IFILE:
			return FileBuffers.getTextFileBufferManager().createEmptyDocument(
					((IFile) this.unit.getResource()).getFullPath(),
					LocationKind.IFILE );
		case FILESTORE:
			return FileBuffers.getTextFileBufferManager().createEmptyDocument(
					URIUtil.toPath(((IFileStore) this.unit.getResource()).toURI()),
					LocationKind.LOCATION );
		default:
			return FileBuffers.getTextFileBufferManager().createEmptyDocument(null, null);
		}
	}
	
	protected void checkDocument(final AbstractDocument document) {
		if (document instanceof ISynchronizable) {
			synchronized (document) {
				if (((ISynchronizable) document).getLockObject() == null) {
					((ISynchronizable) document).setLockObject(new Object());
				}
			}
		}
	}
	
	protected final void loadDocumentFromFile(final IFile file, final AbstractDocument document, final SubMonitor progress) {
		try {
			FileUtil.getFileUtil(file).createReadTextFileOp(new FileUtil.ReaderAction() {
				@Override
				public void run(final BufferedReader reader, final IProgressMonitor monitor) throws IOException {
					final StringBuilder buffer= new StringBuilder();
					final char[] readBuffer= new char[2048];
					int n;
					while ((n= reader.read(readBuffer)) > 0) {
						buffer.append(readBuffer, 0, n);
					}
//					if (document instanceof IDocumentExtension4) {
					document.set(buffer.toString(), file.getModificationStamp());
//					}
//					else {
//						document.set(buffer.toString());
//					}
				}
			}).doOperation(progress);
		}
		catch (final OperationCanceledException e) {
		}
		catch (final CoreException e) {
			LTKCorePlugin.log(e.getStatus());
		}
	}
	
	protected SourceContent createContent(final SubMonitor progress) {
		final ISourceUnit underlyingUnit= this.unit.getUnderlyingUnit();
		if (underlyingUnit != null) {
			return underlyingUnit.getContent(progress);
		}
		else {
			final Object resource= this.unit.getResource();
			final AtomicReference<SourceContent> content= new AtomicReference<>();
			if (resource instanceof IFile) {
				loadContentFromFile((IFile) resource, content, progress);
			}
			return content.get();
		}
	}
	
	protected final void loadContentFromFile(final IFile file, final AtomicReference<SourceContent> content, final SubMonitor progress) {
		try {
			FileUtil.getFileUtil(file).createReadTextFileOp(new FileUtil.ReaderAction() {
				@Override
				public void run(final BufferedReader reader, final IProgressMonitor monitor) throws IOException {
					final StringBuilder buffer= new StringBuilder();
					final char[] readBuffer= new char[2048];
					int n;
					while ((n= reader.read(readBuffer)) >= 0) {
						buffer.append(readBuffer, 0, n);
					}
					content.set(new SourceContent(file.getModificationStamp(), buffer.toString()));
				}
			}).doOperation(progress);
		}
		catch (final OperationCanceledException e) {
		}
		catch (final CoreException e) {
			LTKCorePlugin.log(e.getStatus());
		}
	}
	
	@Override
	public boolean isSynchronized() {
		return false;
	}
	
}
