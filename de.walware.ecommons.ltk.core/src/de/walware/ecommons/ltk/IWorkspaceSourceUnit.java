/*******************************************************************************
 * Copyright (c) 2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;

import org.eclipse.core.resources.IResource;

import de.walware.ecommons.text.IMarkerPositionResolver;


/**
 * Source unit of a workspace resource
 */
public interface IWorkspaceSourceUnit extends ISourceUnit {
	
	
	/**
	 * The file resource of the source unit in the workspace.
	 * 
	 * @return the resource
	 */
	@Override
	public IResource getResource();
	
	/**
	 * Returns a resolver for markers in the resource providing the position in the current
	 * document content of the source unit.
	 * 
	 * @return the resolver if required
	 */
	public IMarkerPositionResolver getMarkerPositionResolver();
	
}
