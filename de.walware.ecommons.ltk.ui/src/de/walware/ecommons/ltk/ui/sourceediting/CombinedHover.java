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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IInformationControlCreator;


/**
 * Special information hover showing the first match of the specified hover types.
 */
public class CombinedHover implements IInfoHover {
	
	
	private List<InfoHoverDescriptor> fDescriptors;
	private List<IInfoHover> fInstantiatedHovers;
	
	private IInfoHover fBestHover;
	
	
	public CombinedHover() {
	}
	
	
	public void setHovers(final List<InfoHoverDescriptor> descriptors) {
		fDescriptors = descriptors;
		fInstantiatedHovers = new ArrayList<IInfoHover>(descriptors.size());
	}
	
	
	@Override
	public Object getHoverInfo(final AssistInvocationContext context) {
		fBestHover = null;
		if (fDescriptors == null) {
			return null;
		}
		
		for (int i = 0; i < fDescriptors.size(); i++) {
			if (i == fInstantiatedHovers.size()) {
				fInstantiatedHovers.add(fDescriptors.get(i).createHover());
			}
			final IInfoHover hover = fInstantiatedHovers.get(i);
			if (hover != null) {
				final Object info = hover.getHoverInfo(context);
				if (info != null) {
					fBestHover = hover;
					return info;
				}
			}
		}
		return null;
	}
	
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (fBestHover != null) {
			return fBestHover.getHoverControlCreator();
		}
		return null;
	}
	
}
