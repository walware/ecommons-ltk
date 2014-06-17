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

package de.walware.ecommons.ltk.ast;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.text.IRegion;

import de.walware.ecommons.collections.ImList;


/**
 * AST node.
 * <p>
 * The interface must be implemented by the language specific AST classes.</p>
 */
public interface IAstNode extends IRegion {
	
	
	int getStatusCode();
	
	@Override
	int getOffset();
	int getStopOffset();
	@Override
	int getLength();
	
	void accept(ICommonAstVisitor visitor) throws InvocationTargetException;
	void acceptInChildren(ICommonAstVisitor visitor) throws InvocationTargetException;
	
	IAstNode getParent();
	IAstNode getRoot();
	boolean hasChildren();
	int getChildCount();
	IAstNode getChild(int index);
//	IAstNode[] getChildren();
	int getChildIndex(IAstNode element);
	
	void addAttachment(Object data);
	void removeAttachment(Object data);
	ImList<Object> getAttachments();
	
}
