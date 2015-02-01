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

package de.walware.ecommons.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;


/**
 * Accepts document ranges as input. Loads the content partial, small footprint.
 */
public class DocumentTextParserInput extends TextParserInput {
	
	
	private IDocument document;
	
	
	public DocumentTextParserInput() {
		this(DEFAULT_BUFFER_SIZE);
	}
	
	public DocumentTextParserInput(final int bufferSize) {
		setBuffer(new char[bufferSize], 0, 0);
	}
	
	
	public void init(final IDocument document) {
		if (document == null) {
			throw new NullPointerException("document"); //$NON-NLS-1$
		}
		this.document= document;
		init(0, Integer.MIN_VALUE);
	}
	
	public void init(final IDocument document, final int offset, final int length) {
		if (document == null) {
			throw new NullPointerException("document"); //$NON-NLS-1$
		}
		this.document= document;
		init(offset, offset + length);
	}
	
	
	public IDocument getDocument() {
		return this.document;
	}
	
	@Override
	protected int getSourceLength() {
		return (this.document != null) ? this.document.getLength() : 0;
	}
	
	@Override
	protected String getSourceString() {
		return (this.document != null) ? this.document.get() : null;
	}
	
	
	@Override
	protected boolean doUpdateBuffer(final int index, final char[] buffer, final int length) {
		try {
			this.document.get(index, length).getChars(0, length, buffer, 0);
			setBuffer(buffer, length, 0);
			return true;
		}
		catch (final BadLocationException e) {
			throw new RuntimeException(e);
		}
	}
	
}
