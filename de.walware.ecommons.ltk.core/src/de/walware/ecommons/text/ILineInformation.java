/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
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
	 * Returns the number of lines
	 * 
	 * @return count
	 */
	int getNumberOfLines();
	
	/**
	 * Returns the line of the specified offset
	 * 
	 * @param offset 
	 * @return the line
	 * @throws BadLocationException if offset out of bounds
	 */
	int getLineOfOffset(int offset) throws BadLocationException;
	
	/**
	 * Returns the offset of the specified line
	 * 
	 * @param line
	 * @return the offset or -1, if line out of bounds
	 * @throws BadLocationException if line out of bounds
	 */
	int getLineOffset(int line) throws BadLocationException;
	
}
