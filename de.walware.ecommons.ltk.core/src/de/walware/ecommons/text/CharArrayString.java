/*******************************************************************************
 * Copyright (c) 2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text;


/**
 * Open string class based on char array.
 * In contrast to the standard string class, it allows the exchange and modification 
 * the array and avoid copying of the source array.
 * 
 * The class is not prepared for concurrent access.
 */
public class CharArrayString implements CharSequence {
	
	
	private static final char[] EMPTY_ARRAY = new char[0];
	
	
	private char[] fArray;
	private int fOffset;
	private int fLength;
	
	private int fHashCode;
	
	
	public CharArrayString() {
		fArray = EMPTY_ARRAY;
	}
	
	public CharArrayString(final char[] array, final int offset, final int length) {
		set(array, offset, length);
	}
	
	public CharArrayString(final String s) {
		set(s);
	}
	
	public void clear() {
		fArray = EMPTY_ARRAY;
		fOffset = fLength = fHashCode = 0;
	}
	
	public void set(final char[] array) {
		fArray = array;
		fOffset = 0;
		fLength = array.length;
		fHashCode = 0;
	}
	
	public void set(final char[] array, final int offset, final int length) {
		fArray = array;
		fOffset = offset;
		fLength = length;
		fHashCode = 0;
	}
	
	public void set(final String s) {
		fArray = s.toCharArray();
		fOffset = 0;
		fLength = fArray.length;
		fHashCode = s.hashCode();
	}
	
	
	@Override
	public int length() {
		return fLength;
	}
	
	@Override
	public char charAt(final int index) {
		return fArray[fOffset+index];
	}
	
	@Override
	public CharSequence subSequence(final int start, final int end) {
		return new CharArrayString(fArray, fOffset+start, end-start);
	}
	
	
	@Override
	public int hashCode() {
		int hashCode = fHashCode;
		if (hashCode == 0) {
			int length = fLength;
			final char[] array = fArray;
			int offset = fOffset;
			while (length-- != 0) {
				hashCode = 31 * hashCode + array[offset++];
			}
		}
		return hashCode;
	}
	
	public boolean contentEquals(final String s) {
		int length = fLength;
		if (length != s.length() || hashCode() != s.hashCode()) {
			return false;
		}
		final char[] array = fArray;
		int offset = fOffset;
		int i = 0;
		while (length-- != 0) {
			if (array[offset++] != s.charAt(i++)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof CharArrayString)) {
			return false;
		}
		final CharArrayString other = (CharArrayString) obj;
		int length = fLength;
		if (length != other.fLength
				|| (fHashCode != 0 && other.fHashCode != 0 && fHashCode != other.fHashCode) ) {
			return false;
		}
		final char[] array1 = fArray;
		int offset1 = fOffset;
		final char[] array2 = other.fArray;
		int offset2 = other.fOffset;
		while (length-- > 0) {
			if (array1[offset1++] != array2[offset2++]) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		return new String(fArray, fOffset, fLength);
	}
	
}
