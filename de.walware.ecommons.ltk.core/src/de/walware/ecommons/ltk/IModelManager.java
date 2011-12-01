/*******************************************************************************
 * Copyright (c) 2007-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;

import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.ecommons.ltk.core.impl.SourceUnitModelContainer;


/**
 * 
 */
public interface IModelManager {
	
	int NONE =                                              0x00000000;
	
	int AST =                                               0x00000001;
	int MODEL_FILE =                                        0x00000002;
	int MODEL_DEPENDENCIES =                                0x00000003;
	
	int RECONCILER =                                        0x10000000; 
	
	
	/**
	 * Refreshes the model info of all loaded source units in given context.
	 */
	void refresh(WorkingContext context);
	
	void addElementChangedListener(IElementChangedListener listener, WorkingContext context);
	void removeElementChangedListener(IElementChangedListener listener, WorkingContext context);
	
	void registerDependentUnit(ISourceUnit su);
	void deregisterDependentUnit(ISourceUnit su);
	
	void reconcile(SourceUnitModelContainer<?, ?> adapter, int level,
			IProgressMonitor monitor);
	
}
