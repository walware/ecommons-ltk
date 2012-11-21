/*******************************************************************************
 * Copyright (c) 2010-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.AbstractDocument;


public interface ISourceFragment extends IAdaptable {
	
	
	public String getId();
	
	public String getName();
	
	public String getFullName();
	
	public AbstractDocument getDocument();
	
}
