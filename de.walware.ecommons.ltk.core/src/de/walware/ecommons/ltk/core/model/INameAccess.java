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

package de.walware.ecommons.ltk.core.model;

import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.ast.IAstNode;


public interface INameAccess<TAstNode extends IAstNode,
		TNameAccess extends INameAccess<TAstNode, ? super TNameAccess>> extends IElementName {
	
	
	TAstNode getNode();
	
	TAstNode getNameNode();
	
	ImList<? extends TNameAccess> getAllInUnit();
	
	
	boolean isWriteAccess();
	
}
