/*=============================================================================#
 # Copyright (c) 2014-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;


/**
 * {@link GenericResourceSourceUnit} plus:
 * <ul>
 *   <li>Support for {@link SourceUnitModelContainer}</li>
 * </ul>
 * 
 * @param <M>
 */
public abstract class GenericResourceSourceUnit2<M extends SourceUnitModelContainer<? extends ISourceUnit, ? extends ISourceUnitModelInfo>>
		extends GenericResourceSourceUnit {
	
	
	private final M model;
	
	
	public GenericResourceSourceUnit2(final String id, final IFile file) {
		super(id, file);
		
		this.model= createModelContainer();
	}
	
	
	protected abstract M createModelContainer();
	
	protected final M getModelContainer() {
		return this.model;
	}
	
	
	@Override
	protected void unregister() {
		super.unregister();
		
		this.model.clear();
	}
	
	@Override
	public AstInfo getAstInfo(final String type, final boolean ensureSync,
			final IProgressMonitor monitor) {
		if (type == null || this.model.isContainerFor(type)) {
			final AstInfo ast= this.model.getCurrentAst();
			final long stamp= getResource().getModificationStamp();
			if (ast != null && ast.getStamp().getSourceStamp() == stamp) {
				return ast;
			}
			// TODO
		}
		return null;
	}
	
	@Override
	public ISourceUnitModelInfo getModelInfo(final String type, final int flags,
			final IProgressMonitor monitor) {
		if (type == null || this.model.isContainerFor(type)) {
			if ((flags & IModelManager.RECONCILE) != 0) {
				if ((flags & IModelManager.REFRESH) != 0) {
					this.model.clear();
				}
				// trigger rebuild?
				return null;
			}
			
			return this.model.getModelInfo(flags, monitor);
		}
		return null;
	}
	
	@Override
	public Object getAdapter(final Class required) {
		if (required.equals(this.model.getAdapterClass())) {
			return this.model;
		}
		return super.getAdapter(required);
	}
	
}
