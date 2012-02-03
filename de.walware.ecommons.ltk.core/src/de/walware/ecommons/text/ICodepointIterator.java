/*******************************************************************************
 * Copyright (c) 2011-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
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


/**
 * Iterator over Unicode character code points of a document or a region in a document (or other 
 * text content).
 * 
 * The content region of the iterator inside the document is specified by {@link #getBeginIndex()} 
 * and {@link #getEndIndex()}. Also all other index properties including 
 * {@link #getCurrentIndex()} and {@link #getCurrentLength()} relate to the document.
 */
public interface ICodepointIterator {
	
	/**
	 * Indicates that the iterator overrun the begin/end index of the content in the requested 
	 * direction.
	 */
	int EOF = -1;
	
	
	/**
	 * Hint for forward iteration.
	 */
	byte PREPARE_FORWARD =                                  0x1;
	
	/**
	 * Hint for backward iteration.
	 */
	
	byte PREPARE_BACKWARD =                                 0x2;
	
	/**
	 * If the iterator should fix the position, if <code>index</code> points not to the 
	 * first index of a character.
	 */
	byte PREPARE_FIX =                                      0x4;
	
	
	/**
	 * Resets the iterator to the first position and returns its character.
	 * 
	 * If the content is empty, the position is set to {@link #getBeginIndex()} and returns 
	 * {@link #EOF}.
	 * 
	 * @return the code point at the first position or {@link #EOF}
	 */
	int first();
	
	/**
	 * Resets the iterator to the last position and returns its character.
	 * 
	 * If the content is empty, the position is set to {@link #getBeginIndex()} and returns 
	 * {@link #EOF}.
	 * 
	 * @return the code point at the last position or {@link #EOF}
	 */
	int last();
	
	/**
	 * Returns the character at the current position.
	 * 
	 * @return the code point at the current position or {@link #EOF}
	 */
	int current();
	
	/**
	 * Sets the iterator to the next position and returns its character.
	 * 
	 * If the iterator is at the end of the content, it sets the position to
	 * {@link #getEndIndex()} and returns {@link #EOF}.
	 * 
	 * @return the code point at the current position or {@link #EOF}
	 */
	int next();
	
	/**
	 * Sets the iterator to the previous position and returns its character.
	 * 
	 * If the iterator is at the begin of the content, it sets the position to
	 * {@link #getBeginIndex()} and returns {@link #EOF}.
	 * 
	 * @return the code point at the current position or {@link #EOF}
	 */
	int previous();
	
	/**
	 * Sets the iterator to the specified position.
	 * 
	 * @param index the index of the new position in the document
	 * @param prepare an optional set of prepare flags configuring the iterator
	 * @throws BadLocationException if <code>index</code> &lt; {@link #getBeginIndex()} or 
	 *     <code>index</code> &gt; {@link #getEndIndex()}
	 */
	void setIndex(int index, byte prepare) throws BadLocationException;
	
	/**
	 * The index of the first position of the content.
	 * 
	 * @return the begin index in the document
	 */
	int getBeginIndex();
	
	/**
	 * The index behind the last position of the content.
	 * 
	 * @return the end index in the document
	 */
	int getEndIndex();
	
	/**
	 * The index of the current position.
	 * 
	 * @return the current index in the document
	 */
	int getCurrentIndex();
	
	/**
	 * The length of the current position (code point).
	 * 
	 * Usually the length of a code point is 1 or 2 chars, but for encoded content also other
	 * values are possible.
	 * 
	 * @return the current length in the document
	 */
	int getCurrentLength();
	
}
