/*******************************************************************************
 * Copyright (c) 2007-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text;


/**
 * The string represents a range of the source.
 */
public class PartialStringParseInput extends SourceParseInput implements CharSequence {
	
	
	private final int fOffset;
	private final char[] fContent;
	
	
	public PartialStringParseInput(final String content, final int offsetInSource) {
		fOffset = offsetInSource;
		fContent = content.toCharArray();
	}
	
	
	@Override
	protected void updateBuffer(final int min) {
		final int index = getIndex();
		if (index < fOffset) {
			throw new IllegalStateException();
		}
		setBuffer(fContent, fContent.length, index-fOffset);
	}
	
	
	@Override
	public int length() {
		return fOffset+fContent.length;
	}
	
	@Override
	public char charAt(final int index) {
		return fContent[index-fOffset];
	}
	
	@Override
	public CharSequence subSequence(final int start, final int end) {
		return new String(fContent, start-fOffset, end-(start-fOffset));
	}
	
	
	@Override
	protected char[] getCharInput() {
		return fContent;
	}
	
	@Override
	protected int getInputOffset() {
		return fOffset;
	}
	
}
