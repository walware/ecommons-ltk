/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.model;

import java.util.List;


/**
 * Represents Source structure instead of model structure
 */
public interface ISourceStructElement extends ISourceElement {
	
	
	ISourceStructElement getSourceParent();
	boolean hasSourceChildren(IModelElement.Filter filter);
	List<? extends ISourceStructElement> getSourceChildren(IModelElement.Filter filter);
	
}
