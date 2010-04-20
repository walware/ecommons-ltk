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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Manages shared instances of source units.
 */
public interface ISourceUnitManager {
	
	
	ISourceUnit getSourceUnit(String modelTypeId, WorkingContext context, Object from,
			boolean create, IProgressMonitor monitor);
	
	List<ISourceUnit> getOpenSourceUnits(String modelTypeId, WorkingContext context);
	
}
