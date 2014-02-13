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
	
	
	private final int[] offsets;
	private final int textLength;
	
	
	public LineInformation(final int[] offsets, final int textLength) {
		this.offsets= offsets;
		this.textLength= textLength;
	}
	
	
	@Override
	public int getNumberOfLines() {
		return 1+this.offsets.length;
	}
	
	@Override
	public int getLineOfOffset(final int offset) throws BadLocationException {
		if (offset < 0 || offset > this.textLength) {
			throw new BadLocationException("offset= " + offset); //$NON-NLS-1$
		}
		if (this.offsets.length == 0) {
			return 0;
		}
		int low= 0;
		int high= this.offsets.length-1;
		
		while (low <= high) {
			final int mid= (low + high) >> 1;
			final int lineOffset= this.offsets[mid];
			
			if (lineOffset < offset) {
				low= mid + 1;
			} else if (lineOffset > offset) {
				high= mid - 1;
			} else {
				return mid;
			}
		}
		return low-1;
	}
	
	@Override
	public int getLineOffset(final int line) throws BadLocationException {
		if (line < 0 || line >= this.offsets.length) {
			throw new BadLocationException("line= " + line); //$NON-NLS-1$
		}
		return this.offsets[line];
	}
	
	@Override
	public int getLineLength(final int line) throws BadLocationException {
		if (line < 0 || line >= this.offsets.length) {
			throw new BadLocationException("line= " + line); //$NON-NLS-1$
		}
		return (line + 1 == this.offsets.length) ?
				(this.textLength - this.offsets[line]) :
				(this.offsets[line + 1] - this.offsets[line]);
	}
	
}
