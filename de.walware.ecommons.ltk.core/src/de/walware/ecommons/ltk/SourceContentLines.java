/*******************************************************************************
 * Copyright (c) 2011-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;

import de.walware.ecommons.text.ILineInformation;


public class SourceContentLines extends SourceContent {
	
	
	public final ILineInformation lines;
	
	
	public SourceContentLines(final long stamp, final String text, final ILineInformation lines) {
		super(stamp, text);
		this.lines = lines;
	}
	
	public SourceContentLines(final SourceContent content, final ILineInformation lines) {
		super(content.stamp, content.text);
		this.lines = lines;
	}
	
	
}
