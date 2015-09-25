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

package de.walware.ecommons.ltk.core.model;

import de.walware.ecommons.collections.ImList;


/**
 * Set of INameAccess in a (source) unit.
 * 
 * @param <TNameAccess> type of name access
 * 
 * @see de.walware.ecommons.ltk.core.impl.NameAccessSet
 */
public interface INameAccessSet<TNameAccess extends INameAccess<?, TNameAccess>> {
	
	
	ImList<String> getNames();
	
	ImList<TNameAccess> getAllInUnit(String label);
	
}
