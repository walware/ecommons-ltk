/*=============================================================================#
 # Copyright (c) 2007-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk;


/**
 * Represents a context when working with LTK sources and models.
 * <p>
 * The objects can be used to identify the context (by identity). It has only the
 * key attribute and doesn't provides further functions.</p>
 * 
 * @see LTK
 * @noextend
 */
public final class WorkingContext {
	
	
	private final String fKey;
	
	
	public WorkingContext(final String key) {
		fKey = key;
	}
	
	
	public String getKey() {
		return fKey;
	}
	
}
