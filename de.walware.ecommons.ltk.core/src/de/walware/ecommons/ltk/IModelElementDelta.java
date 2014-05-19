/*=============================================================================#
 # Copyright (c) 2000-2014 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk;

import de.walware.ecommons.ltk.core.model.IModelElement;


public interface IModelElementDelta {
	
	public IModelElement getModelElement();
	public AstInfo getOldAst();
	public AstInfo getNewAst();
	
}
