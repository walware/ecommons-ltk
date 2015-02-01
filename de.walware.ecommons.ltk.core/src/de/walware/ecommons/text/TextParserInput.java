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
 * Generic API for input of lexers etc.
 * <p>
 * Subclasses have to fill and update the buffer.</p>
 */
public abstract class TextParserInput {
	
	
	public static final int EOF= -1;
	
	
	protected static final int DEFAULT_BUFFER_SIZE= 2048;
	
	protected static final char[] NO_INPUT= new char[0];
	
	
	private final CharArrayString tmpCharString= new CharArrayString();
	
	private int startIndex;
	private int stopIndex;
	
	private char[] buffer= NO_INPUT;
	private int bufferLength= 0;
	private int indexInBuffer= 0;
	private int index;
	
	
	protected TextParserInput() {
		this.index= Integer.MIN_VALUE;
		this.stopIndex= Integer.MIN_VALUE;
	}
	
	
	public void init() {
		init(0, Integer.MIN_VALUE);
	}
	
	public void init(final int startIndex, int stopIndex) {
		final int length= getSourceLength();
		if (length > 0) {
			if (stopIndex > length) {
				throw new IndexOutOfBoundsException("stopIndex= " + stopIndex); //$NON-NLS-1$
			}
			else if (stopIndex < 0) {
				stopIndex= length;
			}
			if (startIndex < 0 || startIndex > stopIndex) {
				throw new IndexOutOfBoundsException("startIndex= " + startIndex); //$NON-NLS-1$
			}
		}
		
		this.startIndex= startIndex;
		this.stopIndex= stopIndex;
		this.index= startIndex;
		this.indexInBuffer= 0;
		this.bufferLength= 0;
		
		updateBuffer(0);
	}
	
	
	/**
	 * Returns the length of the source text.
	 * 
	 * @return the length or <code>-1</code> for unknown
	 */
	protected int getSourceLength() {
		return -1;
	}
	
	/**
	 * Returns the source text as string, if possible.
	 * 
	 * @return the underlying text
	 */
	protected String getSourceString() {
		return null;
	}
	
	/**
	 * Returns the index of the {@link #getSourceString() source string} in the source text.
	 * 
	 * @return the index of the source string
	 */
	protected int getSourceStringIndex() {
		return 0;
	}
	
	/**
	 * Returns the start index of this input in the source.
	 * 
	 * @return the start index
	 */
	public final int getStartIndex() {
		return this.startIndex;
	}
	
	/**
	 * Returns the stop index of this input in the source (exclusive).
	 * 
	 * @return the stop index
	 */
	public final int getStopIndex() {
		return this.stopIndex;
	}
	
	/**
	 * Returns the current index in the source text.
	 * 
	 * @return the index in the source
	 */
	public final int getIndex() {
		return this.index;
	}
	
	
	/**
	 * Returns the character at the specified offset in the content.
	 * 
	 * @param offset the offset in the content
	 * @return the content character, or {@link #EOF} if outside of the content
	 */
	public final int get(final int offset) {
		int idx= this.indexInBuffer + offset;
		if (idx >= this.bufferLength) {
			if (updateBuffer(offset + 1)) {
				idx= this.indexInBuffer + offset;
			}
			else {
				return EOF;
			}
		}
		return this.buffer[idx];
	}
	
	public final boolean matches(final int offset, final char c1) {
		int idx= this.indexInBuffer + offset;
		if (idx >= this.bufferLength) {
			if (updateBuffer(offset + 1)) {
				idx= this.indexInBuffer + offset;
			}
			else {
				return false;
			}
		}
		return (this.buffer[idx] == c1);
	}
	
	public final boolean matches(final int offset, final char c1, final char c2) {
		int idx= this.indexInBuffer + offset;
		if (idx + 1 >= this.bufferLength) {
			if (updateBuffer(offset + 2)) {
				idx= this.indexInBuffer + offset;
			}
			else {
				return false;
			}
		}
		return (this.buffer[idx] == c1 && this.buffer[++idx] == c2);
	}
	
	public final boolean matches(final int offset, final char c1, final char c2, final char c3) {
		int idx= this.indexInBuffer + offset;
		if (idx + 2 >= this.bufferLength) {
			if (updateBuffer(offset + 3)) {
				idx= this.indexInBuffer + offset;
			}
			else {
				return false;
			}
		}
		return (this.buffer[idx] == c1 && this.buffer[++idx] == c2 && this.buffer[++idx] == c3);
	}
	
	public final boolean matches(int offset, final char[] sequence) {
		final int l= sequence.length;
		int idx= this.indexInBuffer + offset;
		if (idx + l > this.bufferLength) {
			if (updateBuffer(offset + l)) {
				idx= this.indexInBuffer + offset;
			}
			else {
				return false;
			}
		}
		while (offset < sequence.length) {
			if (this.buffer[idx++] != sequence[offset++]) {
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Returns the length in source text of the content from the current index to the specified
	 * offset (exclusive).
	 * 
	 * @param offset the end offset in the content
	 * @return the length in the source
	 */
	public int getLengthInSource(final int offset) {
		return offset;
	}
	
	/**
	 * Forwards the current index to the specified offset.
	 * 
	 * @param offset the offset in content
	 */
	public void consume(final int offset) {
		this.index+= getLengthInSource(offset);
		this.indexInBuffer += offset;
	}
	
	/**
	 * Forwards the current index to the specified offset.
	 * 
	 * @param offset the offset in the content
	 * @param lengthInSource the length in the source
	 */
	public void consume(final int offset, final int lengthInSource) {
		this.index+= lengthInSource;
		this.indexInBuffer+= offset;
	}
	
	protected void setConsume(final int offset, final int index) {
		this.index= index;
		this.indexInBuffer+= offset;
	}
	
	protected boolean updateBuffer(final int endOffset) {
		final int index= this.index;
		if (index + endOffset > this.stopIndex) {
			return false;
		}
		
		char[] buffer= this.buffer;
		int length= buffer.length;
		if (endOffset > length) {
			length= (1 + (endOffset / 1024)) * 1024;
		}
		if (buffer.length < length) {
			buffer= new char[length];
		}
		if (index + length > this.stopIndex) {
			length= this.stopIndex - index;
		}
		return doUpdateBuffer(index, buffer, length);
	}
	
	protected boolean doUpdateBuffer(final int index, final char[] buffer, final int length) {
		setBuffer(NO_INPUT, 0, 0);
		return false;
	}
	
	protected final char[] getBuffer() {
		return this.buffer;
	}
	
	protected final void setBuffer(final char[] buffer, final int length, final int indexInBuffer) {
		this.indexInBuffer= indexInBuffer;
		this.buffer= buffer;
		final int stopInBuffer= this.indexInBuffer - this.index + this.stopIndex;
		this.bufferLength= (this.stopIndex >= 0 && stopInBuffer < length) ? stopInBuffer : length;
		this.tmpCharString.clear();
	}
	
	protected final int getIndexInBuffer() {
		return this.indexInBuffer;
	}
	
	
	public final String getString(final int offset, final int length) {
		return new String(this.buffer, this.indexInBuffer + offset, length);
	}
	
	protected final CharArrayString getTmpString(final int offset, final int length) {
		this.tmpCharString.set(this.buffer, this.indexInBuffer + offset, length);
		return this.tmpCharString;
	}
	
	public final String getString(final int offset, final int length, final IStringCache factory) {
		this.tmpCharString.set(this.buffer, this.indexInBuffer + offset, length);
		return factory.get(this.tmpCharString);
	}
	
	@Override
	public String toString() {
		if (getSourceString() != null) {
			final int offset= getSourceStringIndex();
			if (offset == 0) {
				return getSourceString();
			}
			else if (offset > 0) {
				final StringBuilder sb= new StringBuilder();
				sb.ensureCapacity(offset + getSourceString().length());
				sb.setLength(offset);
				sb.append(getSourceString());
				return sb.toString();
			}
			else {
				return getSourceString().substring(-offset);
			}
		}
		return super.toString();
	}
	
}
