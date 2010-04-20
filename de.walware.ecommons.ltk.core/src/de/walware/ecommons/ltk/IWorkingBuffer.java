/*******************************************************************************
 * Copyright (c) 2007-2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.AbstractDocument;


/**
 * Interface to access documents. 
 * Usually only used inside implementations of {@link ISourceUnit}.
 * <p>
 * For the progress monitors of the methods the SubMonitor pattern is applied.</p>
 */
public interface IWorkingBuffer {
	
	
	public AbstractDocument getDocument(IProgressMonitor monitor);
	public SourceContent getContent(IProgressMonitor monitor);
	public boolean checkState(boolean validate, IProgressMonitor monitor);
	public void saveDocument(IProgressMonitor monitor);
	public void releaseDocument(IProgressMonitor monitor);
	
}
