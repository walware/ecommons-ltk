/*=============================================================================#
 # Copyright (c) 2010-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;

import de.walware.ecommons.ui.util.InformationDispatchHandler;


/**
 * Interface for information hover providers.
 * 
 * Intend to be implemented by client and registered to the extension point
 * <code>de.walware.ecommons.ltk.advancedInfoHover</code>.
 */
public interface IInfoHover {
	
	
	int MODE_TOOLTIP = InformationDispatchHandler.MODE_TOOLTIP;
	int MODE_PROPOSAL_INFO = InformationDispatchHandler.MODE_PROPOSAL_INFO;
	
	int MODE_FOCUS = InformationDispatchHandler.MODE_FOCUS;
	
	
	/**
	 * @see ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	Object getHoverInfo(AssistInvocationContext context);
	
	/**
	 * @see ITextHoverExtension#getHoverControlCreator()
	 */
	IInformationControlCreator getHoverControlCreator();
	
}
