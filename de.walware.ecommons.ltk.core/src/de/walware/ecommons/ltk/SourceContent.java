/*******************************************************************************
 * Copyright (c) 2007-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;


/**
 * Source code with time stamp.
 */
public class SourceContent {
	
	
	public final long stamp;
	public final String text;
	
	
	public SourceContent(final long stamp, final String text) {
		this.stamp = stamp;
		this.text = text;
	}
	
}
