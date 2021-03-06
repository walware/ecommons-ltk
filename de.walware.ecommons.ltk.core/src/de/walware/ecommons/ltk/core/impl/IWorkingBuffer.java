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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.AbstractDocument;

import de.walware.ecommons.ltk.core.SourceContent;
import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * Interface to access documents. 
 * Usually only used inside implementations of {@link ISourceUnit}.
 * <p>
 * For the progress monitors of the methods the SubMonitor pattern is applied.</p>
 */
public interface IWorkingBuffer {
	
	
	long getContentStamp(IProgressMonitor monitor);
	
	/**
	 * @return the document or <code>null</code>
	 */
	AbstractDocument getDocument();
	
	/**
	 * @return the document
	 */
	AbstractDocument getDocument(IProgressMonitor monitor);
	SourceContent getContent(IProgressMonitor monitor);
	boolean checkState(boolean validate, IProgressMonitor monitor);
	boolean isSynchronized();
	void saveDocument(IProgressMonitor monitor);
	void releaseDocument(IProgressMonitor monitor);
	
}
