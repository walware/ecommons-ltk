/*******************************************************************************
 * Copyright (c) 2010-2011 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;


/**
 * Interface for information hover providers.
 * 
 * Intend to be implemented by client and registered to the extension point
 * <code>de.walware.ecommons.ltk.advancedInfoHover</code>.
 */
public interface IInfoHover {
	
	/**
	 * @see ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	Object getHoverInfo(AssistInvocationContext context);
	
	/**
	 * @see ITextHoverExtension#getHoverControlCreator()
	 */
	IInformationControlCreator getHoverControlCreator();
	
}
