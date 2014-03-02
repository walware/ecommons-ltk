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
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.IWorkspaceSourceUnit;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.SourceContent;


public abstract class SourceUnitModelContainer<U extends ISourceUnit, ModelType extends ISourceUnitModelInfo> {
	
	
	private static final int MODE_PERSISTENCE= 1;
	private static final int MODE_EDITOR= 2;
	
	
	private final int mode;
	
	private final U unit;
	
	private AstInfo astInfo;
	
	private ModelType modelInfo;
	
	
	public SourceUnitModelContainer(final U unit) {
		this.unit= unit;
		this.mode= getMode(unit);
	}
	
	
	public abstract boolean isContainerFor(String modelTypeId);
	
	public abstract Class<?> getAdapterClass();
	
	
	protected int getMode(final U su) {
		if (su instanceof IWorkspaceSourceUnit) {
			if (su.getWorkingContext() == LTK.PERSISTENCE_CONTEXT) {
				return MODE_PERSISTENCE;
			}
			else if (su.getWorkingContext() == LTK.EDITOR_CONTEXT) {
				return MODE_EDITOR;
			}
		}
		return 0;
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
	
	public ModelType getModelInfo(final int syncLevel, final IProgressMonitor monitor) {
		if ((syncLevel & 0xf) >= IModelManager.MODEL_FILE) {
			final ModelType currentModel= this.modelInfo;
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
		if (this.mode == MODE_PERSISTENCE) {
			final ModelType model= getCurrentModel();
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
		if (this.mode == MODE_PERSISTENCE) {
			return;
		}
		this.astInfo= ast;
	}
	
	public ModelType getCurrentModel() {
		return this.modelInfo;
	}
	
	public ModelType getCurrentModel(final long stamp) {
		final ModelType model= getCurrentModel();
		if (model != null && model.getStamp() == stamp) {
			return model;
		}
		return null;
	}
	
	public void setModel(final ModelType modelInfo) {
		if (modelInfo != null
				&& (this.astInfo == null || this.astInfo.stamp == modelInfo.getStamp()) ) {
									// otherwise, the ast is probably newer
			setAst(modelInfo.getAst());
		}
		this.modelInfo= modelInfo;
	}
	
	
	public IProblemRequestor createProblemRequestor(final long stamp) {
		switch (this.mode) {
		case MODE_PERSISTENCE:
			return createPersistenceContextProblemRequestor(stamp);
		case MODE_EDITOR:
			return createEditorContextProblemRequestor(stamp);
		}
		return null;
	}
	
	protected IProblemRequestor createPersistenceContextProblemRequestor(final long stamp) {
		return null;
	}
	
	protected IProblemRequestor createEditorContextProblemRequestor(final long stamp) {
		return null;
	}
	
}
