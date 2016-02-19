/*=============================================================================#
 # Copyright (c) 2015-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import java.util.ArrayList;
import java.util.List;

import de.walware.jcommons.collections.ImCollections;

import de.walware.ecommons.ltk.IElementName;


public final class NameAccessAccumulator<TName extends IElementName> {
	
	
	private final String label;
	
	private List<TName> list;
	
	
	public NameAccessAccumulator(final String label) {
		if (label == null) {
			throw new NullPointerException("label"); //$NON-NLS-1$
		}
		this.label= label;
		this.list= new ArrayList<>(8);
	}
	
	
	public void finish() {
		this.list= ImCollections.toList(this.list);
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public List<TName> getList() {
		return this.list;
	}
	
}
