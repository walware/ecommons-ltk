/*******************************************************************************
 * Copyright (c) 2009-2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;

import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.ecommons.ltk.ast.IAstNode;


public abstract class SourceUnitModelContainer<NodeT extends IAstNode> {
	
	
	private final ISourceUnit fSourceUnit;
	
	private AstInfo<NodeT> fAstInfo;
	
	private ISourceUnitModelInfo fModelInfo;
	
	
	public SourceUnitModelContainer(final ISourceUnit su) {
		fSourceUnit = su;
	}
	
	
	public ISourceUnit getSourceUnit() {
		return fSourceUnit;
	}
	
	public AstInfo<NodeT> getAstInfo(final boolean ensureSync, final IProgressMonitor monitor) {
		if (ensureSync) {
			getModelManager().reconcile(fSourceUnit, IModelManager.AST, false, monitor);
		}
		return fAstInfo;
	}
	
	public ISourceUnitModelInfo getModelInfo(final int syncLevel, final IProgressMonitor monitor) {
		return fModelInfo;
	}
	
	protected abstract IModelManager getModelManager();
	
	public AstInfo<NodeT> getCurrentAst() {
		return fAstInfo;
	}
	
	public void setAst(final AstInfo<NodeT> ast) {
		fAstInfo = ast;
	}
	
	public ISourceUnitModelInfo getCurrentModel() {
		return fModelInfo;
	}
	
	public void setModel(final ISourceUnitModelInfo modelInfo) {
		fModelInfo = modelInfo;
	}
	
}
