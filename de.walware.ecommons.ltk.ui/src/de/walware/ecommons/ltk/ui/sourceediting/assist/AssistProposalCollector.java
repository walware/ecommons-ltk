/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;


public class AssistProposalCollector<T> {
	
	
	private final Class<T> type;
	
	protected final Map<T, T> proposals;
	
	
	public AssistProposalCollector(final Class<T> type) {
		this.type= type;
		this.proposals= new HashMap<>();
	}
	
	
	public void add(final T proposal) {
		this.proposals.put(proposal, proposal);
	}
	
	public int getCount() {
		return this.proposals.size();
	}
	
	public T[] toArray() {
		@SuppressWarnings("unchecked")
		final T[] array= (T[]) Array.newInstance(this.type, this.proposals.size());
		this.proposals.values().toArray(array);
		return array;
	}
	
}
