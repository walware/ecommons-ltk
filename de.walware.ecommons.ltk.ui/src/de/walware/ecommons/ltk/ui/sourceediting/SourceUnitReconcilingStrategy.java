/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;

import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.ui.sourceediting.EcoReconciler2.ISourceUnitStrategy;


public class SourceUnitReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension, ISourceUnitStrategy {
	
	
	private ISourceUnit input;
	
	private final int flags;
	
	private IProgressMonitor monitor;
	
	
	public SourceUnitReconcilingStrategy() {
		this(IModelManager.MODEL_DEPENDENCIES | IModelManager.RECONCILE);
	}
	
	public SourceUnitReconcilingStrategy(final int flags) {
		this.flags= flags;
	}
	
	
	@Override
	public void initialReconcile() {
		reconcile();
	}
	
	@Override
	public void setDocument(final IDocument document) {
	}
	
	@Override
	public void setInput(final ISourceUnit input) {
		this.input= input;
	}
	
	@Override
	public void reconcile(final IRegion partition) {
		reconcile();
	}
	
	@Override
	public void reconcile(final DirtyRegion dirtyRegion, final IRegion subRegion) {
		reconcile();
	}
	
	@Override
	public void setProgressMonitor(final IProgressMonitor monitor) {
		this.monitor= monitor;
	}
	
	
	protected void reconcile() {
		final ISourceUnit su= this.input;
		if (this.monitor.isCanceled()) {
			return;
		}
		su.getModelInfo(null, this.flags, this.monitor);
	}
	
}
