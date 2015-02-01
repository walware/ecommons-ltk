/*=============================================================================#
 # Copyright (c) 2014-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     erka - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates;

import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;


public class WaContributionContextTypeRegistry extends ContributionContextTypeRegistry {
	
	
	private final String id;
	
	
	public WaContributionContextTypeRegistry(final String id) {
		super(id);
		
		this.id= id;
	}
	
	
	@Override
	public void addContextType(final String id) {
		assert (id != null);
		if (super.getContextType(id) != null) {
			return;
		}
		final TemplateContextType type= loadContextType(id);
		if (type != null) {
			addContextType(type);
		}
	}
	
	protected TemplateContextType loadContextType(final String id) {
		final TemplateContextType type= createContextType(id);
		if (type != null) {
			if (type instanceof IWaTemplateContextTypeExtension1) {
				((IWaTemplateContextTypeExtension1) type).init();
			}
			addContextType(type);
		}
		return type;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb= new StringBuilder("ContributionContextTypeRegistry ("); //$NON-NLS-1$
		sb.append("id= ").append(this.id); //$NON-NLS-1$
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}
	
}
