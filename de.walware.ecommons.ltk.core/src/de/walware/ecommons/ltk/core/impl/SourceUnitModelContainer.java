/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.IProblemRequestor;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.WorkingContext;
import de.walware.ecommons.ltk.core.SourceContent;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.core.model.IWorkspaceSourceUnit;


public abstract class SourceUnitModelContainer<U extends ISourceUnit, M extends ISourceUnitModelInfo> {
	
	
	private final WorkingContext mode;
	
	private final U unit;
	
	private AstInfo astInfo;
	
	private M modelInfo;
	
	
	public SourceUnitModelContainer(final U unit) {
		this.unit= unit;
		this.mode= getMode(unit);
	}
	
	
	public abstract boolean isContainerFor(String modelTypeId);
	
	public abstract Class<?> getAdapterClass();
	
	
	protected WorkingContext getMode(final U su) {
		if (su instanceof IWorkspaceSourceUnit) {
			return su.getWorkingContext();
		}
		return null;
	}
	
	protected final WorkingContext getMode() {
		return this.mode;
	}
	
	
	public U getSourceUnit() {
		return this.unit;
	}
	
	public SourceContent getParseContent(final IProgressMonitor monitor) {
		return this.unit.getContent(monitor);
	}
	
	public AstInfo getAstInfo(final boolean ensureSync, final IProgressMonitor monitor) {
		if (ensureSync) {
			getModelManager().reconcile(this, IModelManager.AST, monitor);
		}
		return this.astInfo;
	}
	
	public M getModelInfo(final int syncLevel, final IProgressMonitor monitor) {
		if ((syncLevel & 0xf) >= IModelManager.MODEL_FILE) {
			final M currentModel= this.modelInfo;
			if (currentModel == null
					|| currentModel.getStamp() == 0
					|| currentModel.getStamp() != this.unit.getContentStamp(monitor)) {
				getModelManager().reconcile(this, syncLevel, monitor);
			}
		}
		return this.modelInfo;
	}
	
	protected abstract IModelManager getModelManager();
	
	
	public void clear() {
		this.astInfo= null;
		this.modelInfo= null;
	}
	
	public AstInfo getCurrentAst() {
		if (this.mode == LTK.PERSISTENCE_CONTEXT) {
			final M model= getCurrentModel();
			if (model != null) {
				return model.getAst();
			}
			return null;
		}
		return this.astInfo;
	}
	
	public AstInfo getCurrentAst(final long stamp) {
		final AstInfo ast= getCurrentAst();
		if (ast != null && ast.stamp == stamp) {
			return ast;
		}
		return null;
	}
	
	public void setAst(final AstInfo ast) {
		if (this.mode == LTK.PERSISTENCE_CONTEXT) {
			return;
		}
		this.astInfo= ast;
	}
	
	public M getCurrentModel() {
		return this.modelInfo;
	}
	
	public M getCurrentModel(final long stamp) {
		final M model= getCurrentModel();
		if (model != null && model.getStamp() == stamp) {
			return model;
		}
		return null;
	}
	
	public void setModel(final M modelInfo) {
		if (modelInfo != null
				&& (this.astInfo == null || this.astInfo.stamp == modelInfo.getStamp()) ) {
									// otherwise, the ast is probably newer
			setAst(modelInfo.getAst());
		}
		this.modelInfo= modelInfo;
	}
	
	
	public IProblemRequestor createProblemRequestor(final long stamp) {
		return null;
	}
	
}
