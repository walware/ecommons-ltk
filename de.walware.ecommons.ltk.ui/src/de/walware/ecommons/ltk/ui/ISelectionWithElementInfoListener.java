/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui;


/**
 * Interface to listen to {@link LTKInputData} state changes providing selection and model
 * information.
 * 
 * @see {@link PostSelectionWithElementInfoController}
 */
public interface ISelectionWithElementInfoListener {
	
	
	public void inputChanged();
	
	public void stateChanged(LTKInputData state);
	
	
}
