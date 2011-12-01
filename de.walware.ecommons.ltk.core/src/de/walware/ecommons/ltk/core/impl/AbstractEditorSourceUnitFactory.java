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

import org.eclipse.core.filesystem.IFileStore;

import de.walware.ecommons.text.ISourceFragment;

import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitFactory;
import de.walware.ecommons.ltk.IWorkspaceSourceUnit;
import de.walware.ecommons.ltk.LTK;


/**
 * Abstract factory for {@link LTK#EDITOR_CONTEXT}.
 */
public abstract class AbstractEditorSourceUnitFactory implements ISourceUnitFactory {
	
	
	@Override
	public String createId(final Object from) {
		if (from instanceof IFileStore) {
			return AbstractFilePersistenceSourceUnitFactory.createResourceId(((IFileStore) from).toURI());
		}
		if (from instanceof ISourceFragment) {
			return ((ISourceFragment) from).getId();
		}
		return null;
	}
	
	@Override
	public ISourceUnit createSourceUnit(final String id, final Object from) {
		if (from instanceof IWorkspaceSourceUnit) {
			return createSourceUnit(id, (IWorkspaceSourceUnit) from);
		}
		if (from instanceof IFileStore) {
			return createSourceUnit(id, (IFileStore) from);
		}
		if (from instanceof ISourceFragment) {
			return createSourceUnit(id, (ISourceFragment) from);
		}
		return null;
	}
	
	
	protected abstract ISourceUnit createSourceUnit(final String id, final IWorkspaceSourceUnit su);
	
	protected abstract ISourceUnit createSourceUnit(final String id, final IFileStore file);
	
	protected ISourceUnit createSourceUnit(final String id, final ISourceFragment fragment) {
		return null;
	}
	
}
