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

package de.walware.ecommons.ltk.ui.sourceediting;


/**
 * Correction proposals implement this interface to by invokable by a command.
 * (e.g. keyboard shortcut)
 */
public interface ICommandAccess {
	
	
	/**
	 * Returns the id of the command that should invoke this correction proposal
	 * @return the id of the command.
	 */
	String getCommandId();

}
