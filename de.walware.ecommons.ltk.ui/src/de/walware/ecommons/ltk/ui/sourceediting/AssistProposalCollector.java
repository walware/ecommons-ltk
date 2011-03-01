/*******************************************************************************
 * Copyright (c) 2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;


public class AssistProposalCollector<T> {
	
	
	private final Class<T> fType;
	
	protected final Map<T, T> fProposals;
	
	
	public AssistProposalCollector(final Class<T> type) {
		fType = type;
		fProposals = new HashMap<T, T>();
	}
	
	
	public void add(final T proposal) {
		fProposals.put(proposal, proposal);
	}
	
	public int getCount() {
		return fProposals.size();
	}
	
	public T[] toArray() {
		@SuppressWarnings("unchecked")
		final T[] array = (T[]) Array.newInstance(fType, fProposals.size());
		fProposals.values().toArray(array);
		return array;
	}
	
}
