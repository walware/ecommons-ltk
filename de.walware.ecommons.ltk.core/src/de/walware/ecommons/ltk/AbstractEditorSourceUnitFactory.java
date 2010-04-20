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

import org.eclipse.core.filesystem.IFileStore;


/**
 * Abstract factory for {@link LTK#EDITOR_CONTEXT}.
 */
public abstract class AbstractEditorSourceUnitFactory implements ISourceUnitFactory {
	
	
	public String createId(final Object from) {
		if (from instanceof IFileStore) {
			return AbstractFilePersistenceSourceUnitFactory.createResourceId(((IFileStore) from).toURI());
		}
		return null;
	}
	
	public ISourceUnit createSourceUnit(final String id, final Object from, final ISourceUnitStateListener callback) {
		if (from instanceof ISourceUnit) {
			return createSourceUnit(id, (ISourceUnit) from, callback);
		}
		if (from instanceof IFileStore) {
			return createSourceUnit(id, (IFileStore) from, callback);
		}
		return null;
	}
	
	
	protected abstract ISourceUnit createSourceUnit(String id, ISourceUnit su, ISourceUnitStateListener callback);
	
	protected abstract ISourceUnit createSourceUnit(String id, IFileStore file, ISourceUnitStateListener callback);
	
}
