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

package de.walware.ecommons.ltk;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.DocumentRewriteSessionType;

import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * Runnable to execute a document operation in a special context.
 * 
 * @see ISourceUnit#syncExec(SourceDocumentRunnable)
 */
public abstract class SourceDocumentRunnable {
	
	
	private AbstractDocument document;
	private long stamp;
	private final DocumentRewriteSessionType rewriteSessionType;
	
	
	public SourceDocumentRunnable(final AbstractDocument document, final long assertedStamp, final DocumentRewriteSessionType rewriteSessionType) {
		this.document= document;
		this.stamp= assertedStamp;
		this.rewriteSessionType= rewriteSessionType;
	}
	
	
	public final DocumentRewriteSessionType getRewriteSessionType() {
		return this.rewriteSessionType;
	}
	
	public final void setNext(final AbstractDocument document, final long assertedStamp) {
		this.document= document;
		this.stamp= assertedStamp;
	}
	
	public final AbstractDocument getDocument() {
		return this.document;
	}
	
	public final long getStampAssertion() {
		return this.stamp;
	}
	
	
	public abstract void run() throws InvocationTargetException;
	
}
