/*******************************************************************************
 * Copyright (c) 2007-2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ast;

import java.lang.reflect.InvocationTargetException;


/**
 * Visitor for AST of {@link IAstNode}s.
 * <p>
 * A visitor can visit an AST (or a subtree of an AST) by calling
 * {@link IAstNode#accept(ICommonAstVisitor)} or {@link IAstNode#acceptInChildren(ICommonAstVisitor)}.
 * It can be used independent of the language (often a language specific visitor pattern is offered
 * too).</p>
 */
public interface ICommonAstVisitor {
	
	
	void visit(final IAstNode node) throws InvocationTargetException;
	
}
