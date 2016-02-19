/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text;

import java.util.Arrays;

import de.walware.ecommons.text.core.ILineInformation;
import de.walware.ecommons.text.core.LineInformation;


public class LineInformationCreator {
	
	
	private int[] buffer= new int[2048];
	
	
	public LineInformationCreator() {
	}
	
	
	public ILineInformation create(final String text) {
		int[] lines= this.buffer;
		int line= 0;
		lines[0]= 0;
		for (int offset= 0; offset < text.length(); ) {
			final int c= text.charAt(offset++);
			switch (c) {
			case '\r':
				if (offset < text.length() && text.charAt(offset) == '\n') {
					offset++;
				}
				break;
			case '\n':
				break;
			default:
				continue;
			}
			if (++line >= lines.length) {
				lines= this.buffer= Arrays.copyOf(lines, lines.length + 1024);
			}
			lines[line]= offset;
		}
		return new LineInformation(Arrays.copyOf(lines, line + 1), text.length());
	}
	
}
