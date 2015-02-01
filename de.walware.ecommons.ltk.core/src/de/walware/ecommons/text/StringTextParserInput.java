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


/**
 * Accepts a common string as parse input.
 */
public class StringTextParserInput extends TextParserInput implements CharSequence {
	
	
	private final String source;
	
	
	public StringTextParserInput(final String content) {
		this(content, DEFAULT_BUFFER_SIZE);
	}
	
	public StringTextParserInput(final String content, int bufferSize) {
		this.source= content;
		
		if (content.length() < bufferSize) {
			bufferSize= content.length();
		}
		setBuffer(new char[bufferSize], 0, 0);
	}
	
	
	@Override
	protected String getSourceString() {
		return this.source;
	}
	
	@Override
	protected int getSourceLength() {
		return this.source.length();
	}
	
	
	@Override
	protected boolean doUpdateBuffer(final int index, final char[] buffer, final int length) {
		this.source.getChars(index, index + length, buffer, 0);
		setBuffer(buffer, length, 0);
		return true;
	}
	
	
/*- CharSequence ---------------------------------------------------------------------------------*/
	
	@Override
	public int length() {
		return this.source.length();
	}
	
	@Override
	public char charAt(final int index) {
		return this.source.charAt(index);
	}
	
	@Override
	public CharSequence subSequence(final int start, final int end) {
		return this.source.subSequence(start, end);
	}
	
}
