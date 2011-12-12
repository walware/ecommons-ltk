/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

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


public abstract class SourceUnitModelContainer<SuType extends ISourceUnit, ModelType extends ISourceUnitModelInfo> {
	
	
	private final int fMode;
	
	private final SuType fSourceUnit;
	
	private AstInfo fAstInfo;
	
	private ModelType fModelInfo;
	
	
	public SourceUnitModelContainer(final SuType su) {
		fSourceUnit = su;
		fMode = getMode(su);
	}
	
	
	protected int getMode(final SuType su) {
		if (su instanceof IWorkspaceSourceUnit) {
			if (su.getWorkingContext() == LTK.PERSISTENCE_CONTEXT) {
				return 1;
			}
			else if (su.getWorkingContext() == LTK.EDITOR_CONTEXT) {
				return 2;
			}
		}
		return 0;
	}
	
	
	public SuType getSourceUnit() {
		return fSourceUnit;
	}
	
	public SourceContent getParseContent(final IProgressMonitor monitor) {
		return fSourceUnit.getContent(monitor);
	}
	
	public AstInfo getAstInfo(final boolean ensureSync, final IProgressMonitor monitor) {
		if (ensureSync) {
			getModelManager().reconcile(this, IModelManager.AST, monitor);
		}
		return fAstInfo;
	}
	
	public ModelType getModelInfo(final int syncLevel, final IProgressMonitor monitor) {
		if ((syncLevel & 0xf) >= IModelManager.MODEL_FILE) {
			final ModelType currentModel = fModelInfo;
			if (currentModel == null
					|| currentModel.getStamp() == 0
					|| currentModel.getStamp() != fSourceUnit.getContentStamp(monitor)) {
				getModelManager().reconcile(this, syncLevel, monitor);
			}
		}
		return fModelInfo;
	}
	
	protected abstract IModelManager getModelManager();
	
	
	public void clear() {
		fAstInfo = null;
		fModelInfo = null;
	}
	
	public AstInfo getCurrentAst() {
		if (fMode == 1) {
			final ModelType model = getCurrentModel();
			if (model != null) {
				return model.getAst();
			}
			return null;
		}
		return fAstInfo;
	}
	
	public AstInfo getCurrentAst(final long stamp) {
		final AstInfo ast = getCurrentAst();
		if (ast != null && ast.stamp == stamp) {
			return ast;
		}
		return null;
	}
	
	public void setAst(final AstInfo ast) {
		if (fMode == 1) {
			return;
		}
		fAstInfo = ast;
	}
	
	public ModelType getCurrentModel() {
		return fModelInfo;
	}
	
	public ModelType getCurrentModel(final long stamp) {
		final ModelType model = getCurrentModel();
		if (model != null && model.getStamp() == stamp) {
			return model;
		}
		return null;
	}
	
	public void setModel(final ModelType modelInfo) {
		if (modelInfo != null
				&& (fAstInfo == null || fAstInfo.stamp == modelInfo.getStamp()) ) {
									// otherwise, the ast is probably newer
			setAst(modelInfo.getAst());
		}
		fModelInfo = modelInfo;
	}
	
	
	public IProblemRequestor createProblemRequestor(final long stamp) {
		if (fMode == 2) {
			return createEditorContextProblemRequestor(stamp);
		}
		return null;
	}
	
	protected IProblemRequestor createEditorContextProblemRequestor(final long stamp) {
		return null;
	}
	
}
