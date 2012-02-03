/*******************************************************************************
 * Copyright (c) 2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text;

import org.eclipse.jface.text.BadLocationException;


public abstract class CharCodepointIterator implements ICodepointIterator {
	
	
	private final static byte PREPARE_STEPBACK = (PREPARE_BACKWARD | PREPARE_FIX);
	
	
	private static class StringIterator extends CharCodepointIterator {
		
		
		private final String fString;
		
		private final int fStringOffset;
		
		
		public StringIterator(final String string, final int stringIndex,
				final int beginIndex, final int endIndex) throws BadLocationException {
			super(beginIndex, endIndex);
			fString = string;
			fStringOffset = stringIndex;
		}
		
		
		@Override
		protected char getChar(final int offset, final byte prepare) {
			return fString.charAt(offset - fStringOffset);
		}
		
		
		@Override
		public String toString() {
			return fString.substring(getBeginIndex() - fStringOffset, getEndIndex() - fStringOffset);
		}
		
	}
	
	private static class CharArrayIterator extends CharCodepointIterator {
		
		
		private final char[] fArray;
		
		private final int fArrayOffset;
		
		
		public CharArrayIterator(final char[] array, final int arrayIndex,
				final int beginIndex, final int endIndex) throws BadLocationException {
			super(beginIndex, endIndex);
			fArray = array;
			fArrayOffset = arrayIndex;
		}
		
		
		@Override
		protected char getChar(final int offset, final byte prepare) {
			return fArray[offset - fArrayOffset];
		}
		
		
		@Override
		public String toString() {
			return new String(fArray, getBeginIndex() - fArrayOffset, getEndIndex() - getBeginIndex());
		}
		
	}
	
	
	/**
	 * Creates a new iterator for a string.
	 * 
	 * @param string the string
	 * @param stringIndex the offset of the string in the document
	 * @param beginIndex the begin index of the iterator in the document
	 * @param endIndex the end index of the iterator in the document
	 * @throws BadLocationException if an index is not valid
	 */
	public static CharCodepointIterator create(final String string, final int stringIndex,
			final int beginIndex, final int endIndex)
			throws BadLocationException {
		if (beginIndex > endIndex
				|| beginIndex < stringIndex || endIndex > stringIndex + string.length()) {
			throw new BadLocationException();
		}
		return new StringIterator(string, stringIndex, beginIndex, endIndex);
	}
	
	/**
	 * Creates a new iterator for a char array.
	 * 
	 * @param array the char array
	 * @param arrayIndex the offset of the array in the document
	 * @param beginIndex the begin index of the iterator in the document
	 * @param endIndex the end index of the iterator in the document
	 * @throws BadLocationException if an index is not valid
	 */
	public static CharCodepointIterator create(final char[] array, final int arrayIndex,
			final int beginIndex, final int endIndex)
			throws BadLocationException {
		if (beginIndex > endIndex
				|| beginIndex < arrayIndex || endIndex > arrayIndex + array.length) {
			throw new BadLocationException();
		}
		return new CharArrayIterator(array, arrayIndex, beginIndex, endIndex);
	}
	
	
	private final int fBeginIndex;
	private final int fEndIndex;
	
	private int fCurrentIndex;
	private int fCurrentCodepoint;
	private int fCurrentCharLength;
	
	
	protected CharCodepointIterator(final int beginIndex, final int endIndex) {
		fBeginIndex = beginIndex;
		fEndIndex = endIndex;
	}
	
	
	protected abstract char getChar(int index, byte prepare);
	
	
	@Override
	public final int first() {
		internalSet(fBeginIndex, PREPARE_FORWARD);
		return fCurrentCodepoint;
	}
	
	@Override
	public final int last() {
		internalSet((fBeginIndex < fEndIndex) ? fEndIndex - 1 : fEndIndex, PREPARE_STEPBACK);
		return fCurrentCodepoint;
	}
	
	@Override
	public final int current() {
		return fCurrentCodepoint;
	}
	
	@Override
	public final int next() {
		if (fCurrentIndex < fEndIndex) {
			internalSet(fCurrentIndex + fCurrentCharLength, PREPARE_FORWARD);
			return fCurrentCodepoint;
		}
		else {
			return EOF;
		}
	}
	
	public final int next(int count) {
		while (count > 0 && fCurrentIndex < fEndIndex) {
			internalSet(fCurrentIndex + fCurrentCharLength, PREPARE_FORWARD);
			count--;
		}
		return (count == 0) ? fCurrentCodepoint : EOF;
	}
	
	@Override
	public final int previous() {
		if (fCurrentIndex > fBeginIndex) {
			internalSet(fCurrentIndex - 1, PREPARE_STEPBACK);
			return fCurrentCodepoint;
		}
		else {
			return EOF;
		}
	}
	
	public final int previous(int count) {
		while (count > 0 && fCurrentIndex > fBeginIndex) {
			internalSet(fCurrentIndex - 1, PREPARE_STEPBACK);
			count--;
		}
		return (count == 0) ? fCurrentCodepoint : EOF;
	}
	
	@Override
	public void setIndex(final int index, final byte prepare) throws BadLocationException {
		if (index < fBeginIndex || index > fEndIndex) {
			throw new BadLocationException();
		}
		internalSet(index, prepare);
	}
	
	private final void internalSet(final int index, final byte prepare) {
		fCurrentIndex = index;
		if (fCurrentIndex < fEndIndex) {
			final char c = getChar(fCurrentIndex, prepare);
			char c2;
			if ((prepare & PREPARE_FIX) != 0 && Character.isLowSurrogate(c)
					&& fCurrentIndex > fBeginIndex
					&& Character.isHighSurrogate(c2 = getChar(fCurrentIndex-1, prepare)) ) {
				fCurrentIndex--;
				fCurrentCodepoint = Character.toCodePoint(c2, c);
				fCurrentCharLength = 2;
			}
			else if (Character.isHighSurrogate(c)
					&& (fCurrentIndex+1 < fEndIndex
					&& Character.isLowSurrogate((c2 = getChar(fCurrentIndex+1, prepare)) ))) {
				fCurrentCodepoint = Character.toCodePoint(c, c2);
				fCurrentCharLength = 2;
			}
			else {
				fCurrentCodepoint = c;
				fCurrentCharLength = 1;
			}
		}
		else {
			fCurrentCodepoint = EOF;
			fCurrentCharLength = 0;
		}
	}
	
	@Override
	public final int getBeginIndex() {
		return fBeginIndex;
	}
	
	@Override
	public final int getEndIndex() {
		return fEndIndex;
	}
	
	@Override
	public final int getCurrentIndex() {
		return fCurrentIndex;
	}
	
	@Override
	public final int getCurrentLength() {
		return fCurrentCharLength;
	}
	
}
