/*=============================================================================#
 # Copyright (c) 2007-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.model;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ISynchronizable;

import de.walware.ecommons.text.core.sections.IDocContentSections;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.ISourceUnitManager;
import de.walware.ecommons.ltk.SourceDocumentRunnable;
import de.walware.ecommons.ltk.WorkingContext;
import de.walware.ecommons.ltk.core.SourceContent;


/**
 * A source unit provides a document for source code
 * <p>
 * The typical example for a source unit is a text file.</p>
 * <p>
 * Source units should be created using the {@link ISourceUnitManager}.
 * For the progress monitors of the methods the SubMonitor pattern is applied.</p>
 */
public interface ISourceUnit extends IModelElement, IAdaptable {
	
	
	long UNKNOWN_MODIFICATION_STAMP = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
	
	
	WorkingContext getWorkingContext();
	ISourceUnit getUnderlyingUnit();
	
	/**
	 * If the content is synchronized with the underlying unit.
	 * 
	 * @return <code>true</code> if synchronized, otherwise <code>false</code>
	 */
	boolean isSynchronized();
	
	/**
	 * The file resource of the source unit. The type depends on the source unit.
	 * 
	 * @return the resource or <code>null</code> if without resource
	 */
	Object getResource();
	
	/**
	 * Checks if the source is modifiable.
	 * 
	 * @param validate if validate state finally
	 * @param monitor progress monitor
	 * @return <code>true</code> if not yet validated or validated and modifiable,
	 *     otherwise <code>false</code> (finally not modifiable).
	 */
	boolean checkState(boolean validate, IProgressMonitor monitor);
	
	/**
	 * Access to the document with the content of this source unit
	 * 
	 * You must be connected to the source unit. The document object is shared 
	 * and reused as long one task is connect. 
	 * Document changes should be executed using {@link #syncExec(SourceDocumentRunnable)}.
	 * 
	 * @param monitor progress monitor (optional but recommended)
	 * @return the shared document
	 */
	AbstractDocument getDocument(IProgressMonitor monitor);
	
	/**
	 * Returns the information about partitioning and content sections types of the document for 
	 * the content type of this source unit.
	 * 
	 * @return the document content information
	 */
	IDocContentSections getDocumentContentInfo();
	
	/**
	 * Returns the current stamp of the content of this source unit.
	 * 
	 * The stamp is identical with the stamp of the SourceContent returned by
	 * {@link #getContent(IProgressMonitor)}, or <code>0</code> if unkown.
	 * 
	 * @param monitor
	 * @return the current stamp
	 */
	long getContentStamp(IProgressMonitor monitor);
	
	/**
	 * Access to the current content of this source unit.
	 * 
	 * The content represents a snapshot usually recreated by each access.
	 * @param monitor progress monitor (optional but recommended)
	 * @return the current content
	 */
	SourceContent getContent(IProgressMonitor monitor);
	
	/**
	 * Runs {@link SourceDocumentRunnable} with checks (modification stamp) and 
	 * the required 'power' (thread, synch), if necessary. The calling thread is
	 * blocked (syncExec) until the runnable finished
	 * 
	 * For usual editor documents, this is equivalent running in Display thread and synchronize
	 * using {@link ISynchronizable#getLockObject()}, if possible).
	 * 
	 * @param runnable the runnable
	 * @throws InvocationTargetException forwarded from runnable
	 */
	void syncExec(SourceDocumentRunnable runnable) throws InvocationTargetException;
	
	AstInfo getAstInfo(String type, boolean ensureSync, IProgressMonitor monitor);
	
	ISourceUnitModelInfo getModelInfo(String type, int flags, IProgressMonitor monitor);
	
	void connect(IProgressMonitor monitor);
	void disconnect(IProgressMonitor monitor);
	boolean isConnected();
	
}
