/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
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


/**
 * Lines in a text (document)
 * 
 * All indexes are 0-based
 */
public interface ILineInformation {
	
	/**
	 * Returns the number of lines.
	 * 
	 * @return count
	 */
	int getNumberOfLines();
	
	/**
	 * Returns the line of the specified offset.
	 * 
	 * @param offset 
	 * @return the line of the offset
	 * @throws BadLocationException if offset is out of bounds
	 */
	int getLineOfOffset(int offset) throws BadLocationException;
	
	/**
	 * Returns the offset of the specified line.
	 * 
	 * @param line
	 * @return the offset of the line
	 * @throws BadLocationException if line is out of bounds
	 */
	int getLineOffset(int line) throws BadLocationException;
	
	/**
	 * Returns the end offset of the specified line.
	 * 
	 * Equivalent to <code>getLineOffset(line) + getLineLength(line)</code>
	 * 
	 * @param line
	 * @return the end offset of the line
	 * @throws BadLocationException if line is out of bounds
	 */
	int getLineEndOffset(int line) throws BadLocationException;
	
	/**
	 * Returns the length (including the line delimiters) of the specified line.
	 * 
	 * @param line
	 * @return the offset of the line
	 * @throws BadLocationException if line is out of bounds
	 */
	int getLineLength(int line) throws BadLocationException;
	
}
