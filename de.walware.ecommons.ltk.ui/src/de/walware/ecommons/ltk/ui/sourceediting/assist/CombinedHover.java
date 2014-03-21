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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IInformationControlCreator;


/**
 * Special information hover showing the first match of the specified hover types.
 */
public class CombinedHover implements IInfoHover {
	
	
	private List<InfoHoverDescriptor> descriptors;
	private List<IInfoHover> instantiatedHovers;
	
	private IInfoHover bestHover;
	
	
	public CombinedHover() {
	}
	
	
	public void setHovers(final List<InfoHoverDescriptor> descriptors) {
		this.descriptors= descriptors;
		this.instantiatedHovers= new ArrayList<>(descriptors.size());
	}
	
	
	@Override
	public Object getHoverInfo(final AssistInvocationContext context) {
		this.bestHover= null;
		if (this.descriptors == null) {
			return null;
		}
		
		for (int i= 0; i < this.descriptors.size(); i++) {
			if (i == this.instantiatedHovers.size()) {
				this.instantiatedHovers.add(this.descriptors.get(i).createHover());
			}
			final IInfoHover hover= this.instantiatedHovers.get(i);
			if (hover != null) {
				final Object info= hover.getHoverInfo(context);
				if (info != null) {
					this.bestHover= hover;
					return info;
				}
			}
		}
		return null;
	}
	
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (this.bestHover != null) {
			return this.bestHover.getHoverControlCreator();
		}
		return null;
	}
	
}
