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

import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * Interface for factories creation {@link ISourceUnit}.
 * Can be registered via extension point <code>de.walware.ecommons.ltk.workingContexts</code>
 */
public interface ISourceUnitFactory {
	
	
	String createId(Object from);
	
	ISourceUnit createSourceUnit(String id, Object from);
	
}
