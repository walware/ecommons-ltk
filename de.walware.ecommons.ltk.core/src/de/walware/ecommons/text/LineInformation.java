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


public class LineInformation implements ILineInformation {
	
	
	private final int[] fOffsets;
	private final int fTextLength;
	
	
	public LineInformation(final int[] offsets, final int textLength) {
		fOffsets = offsets;
		fTextLength = textLength;
	}
	
	
	@Override
	public int getNumberOfLines() {
		return 1+fOffsets.length;
	}
	
	@Override
	public int getLineOfOffset(final int offset) throws BadLocationException {
		if (offset < 0 || offset > fTextLength) {
			throw new BadLocationException("offset " + offset);
		}
		if (fOffsets.length == 0) {
			return 0;
		}
		int low = 0;
		int high = fOffsets.length-1;
		
		while (low <= high) {
			final int mid = (low + high) >> 1;
			final int lineOffset = fOffsets[mid];
			
			if (lineOffset < offset) {
				low = mid + 1;
			} else if (lineOffset > offset) {
				high = mid - 1;
			} else {
				return mid;
			}
		}
		return low-1;
	}
	
	@Override
	public int getLineOffset(final int line) throws BadLocationException {
		if (line < 0 || line >= fOffsets.length) {
			throw new BadLocationException("line " + line);
		}
		return fOffsets[line];
	}
	
}
