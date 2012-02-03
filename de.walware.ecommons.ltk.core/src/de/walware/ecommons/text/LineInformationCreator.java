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

import java.util.Arrays;


public class LineInformationCreator {
	
	
	private int[] fBuffer = new int[2048];
	
	
	public ILineInformation create(final String text) {
		int line = 0;
		fBuffer[0] = 0;
		for (int offset = 0; offset < text.length(); ) {
			final int c = text.charAt(offset++);
			switch (c) {
			case '\r':
				if (offset < text.length() && text.charAt(offset) == '\n') {
					offset++;
				}
				break;
			case '\n':
				if (offset < text.length() && text.charAt(offset) == '\r') {
					offset++;
				}
				break;
			default:
				continue;
			}
			if (++line >= fBuffer.length) {
				fBuffer = Arrays.copyOf(fBuffer, fBuffer.length+1024);
			}
			fBuffer[line] = offset;
		}
		return new LineInformation(Arrays.copyOf(fBuffer, line+1), text.length());
	}
	
}
