/*=============================================================================#
 # Copyright (c) 2007-2016 Stephan Wahlbrink (WalWare.de) and others.
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

import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.text.core.ITextRegion;


/**
 * AST node.
 * <p>
 * The interface must be implemented by the language specific AST classes.</p>
 */
public interface IAstNode extends ITextRegion {
	
	
	int NA_OFFSET= Integer.MIN_VALUE;
	
	
	int getStatusCode();
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Obligatory offset property: {@link #NA_OFFSET} not supported.</p>
	 */
	@Override
	int getOffset();
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Obligatory offset property: {@link #NA_OFFSET} not supported.</p>
	 */
	@Override
	int getEndOffset();
	
	@Override
	int getLength();
	
	void accept(ICommonAstVisitor visitor) throws InvocationTargetException;
	void acceptInChildren(ICommonAstVisitor visitor) throws InvocationTargetException;
	
	IAstNode getParent();
	boolean hasChildren();
	int getChildCount();
	IAstNode getChild(int index);
//	IAstNode[] getChildren();
	int getChildIndex(IAstNode element);
	
	void addAttachment(Object data);
	void removeAttachment(Object data);
	ImList<Object> getAttachments();
	
}
