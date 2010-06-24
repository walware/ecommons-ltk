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

package de.walware.ecommons.ltk;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Manages shared instances of source units.
 */
public interface ISourceUnitManager {
	
	/**
	 * Returns the source unit for the given object in the given LTK model and working context.
	 * 
	 * The supported object types depends on the model and working context. Typically for
	 * the workspace context it is IFile, for the editor context an existing source unit
	 * (a working copy is created) or IFileStore (URI).
	 * The returned source unit is already connected. If it is a working copy of an existing source
	 * unit (if <code>from</code> was instance of ISourceUnit), the parent is disconnected.
	 * 
	 * @param modelTypeId the model type id
	 * @param context the working context
	 * @param from the object to get the source unit for
	 * @param create whether a new source unit object should be created, if it does not yet exists
	 * @param monitor
	 * @return the source unit or <code>null</code>
	 */
	ISourceUnit getSourceUnit(String modelTypeId, WorkingContext context, Object from,
			boolean create, IProgressMonitor monitor);
	
	List<ISourceUnit> getOpenSourceUnits(String modelTypeId, WorkingContext context);
	
}
