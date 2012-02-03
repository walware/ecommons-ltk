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

package de.walware.ecommons.text;


public final class NoStringCache implements IStringCache {
	
	
	public static final NoStringCache INSTANCE = new NoStringCache();
	
	
	public NoStringCache() {
	}
	
	
	@Override
	public String get(final String s, final boolean isCompact) {
		return (isCompact) ? s : new String(s);
	}
	
	@Override
	public String get(final CharArrayString s) {
		return s.toString();
	}
	
}
