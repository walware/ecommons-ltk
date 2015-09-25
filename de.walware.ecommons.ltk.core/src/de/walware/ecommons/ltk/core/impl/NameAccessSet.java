/*=============================================================================#
 # Copyright (c) 2012-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import de.walware.ecommons.collections.ImCollections;
import de.walware.ecommons.collections.ImList;

import de.walware.ecommons.ltk.core.model.INameAccess;
import de.walware.ecommons.ltk.core.model.INameAccessSet;


public final class NameAccessSet<TNameAccess extends INameAccess<?, TNameAccess>>
		implements INameAccessSet<TNameAccess> {
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static final NameAccessSet EMPTY_SET= new NameAccessSet(Collections.EMPTY_MAP);
	
	public static final <TNameAccess extends INameAccess<?, TNameAccess>> NameAccessSet<TNameAccess> emptySet() {
		return EMPTY_SET;
	}
	
	
	private final ImList<String> labelsSorted;
	
	private final Map<String, NameAccessAccumulator<TNameAccess>> map;
	
	
	public NameAccessSet(final Map<String, NameAccessAccumulator<TNameAccess>> map) {
		final String[] labelArray= new String[map.size()];
		int i= 0;
		for (final Map.Entry<String, NameAccessAccumulator<TNameAccess>> entry : map.entrySet()) {
			labelArray[i++]= entry.getKey();
			entry.getValue().finish();
		}
		Arrays.sort(labelArray);
		
		this.labelsSorted= ImCollections.newList(labelArray);
		this.map= map;
	}
	
	
	@Override
	public ImList<String> getNames() {
		return this.labelsSorted;
	}
	
	@Override
	public ImList<TNameAccess> getAllInUnit(final String label) {
		final NameAccessAccumulator<TNameAccess> shared= this.map.get(label);
		return (shared != null) ? (ImList<TNameAccess>) shared.getList() : null;
	}
	
}
