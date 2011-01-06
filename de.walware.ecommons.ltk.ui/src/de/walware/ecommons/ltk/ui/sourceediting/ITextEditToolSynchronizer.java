/*******************************************************************************
 * Copyright (c) 2008-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.text.link.LinkedModeModel;


/**
 * Ensures that not too much effects are enabled.
 * <p>
 * Must used in UI thread only.</p>
 */
public interface ITextEditToolSynchronizer {
	
	
	public void install(final LinkedModeModel model);
	
}
