/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     erka - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates.config;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


class RegistryTemplateCategory extends TemplateCategory {
	
	
	private IConfigurationElement extElement;
	
	
	public RegistryTemplateCategory(final String id, final ImageDescriptor image, final String label,
			final ImageDescriptor itemImage, final IConfigurationElement extElement) {
		super(id, image, label, itemImage);
		
		this.extElement= extElement;
	}
	
	
	@Override
	ITemplateCategoryConfiguration getConfiguration() {
		ITemplateCategoryConfiguration configuration= super.getConfiguration();
		if (configuration == null) {
			if (this.extElement != null) {
				try {
					configuration= (ITemplateCategoryConfiguration) this.extElement
							.createExecutableExtension("configurationClass"); //$NON-NLS-1$
					setConfiguration(configuration);
				}
				catch (final CoreException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, 0,
							NLS.bind("An error occurred when loading template category configuration for category {0} of plugin ''{1}''",
									getId(),
									this.extElement.getContributor().getName() ),
							e ));
				}
				finally {
					this.extElement= null;
				}
			}
		}
		return configuration;
	}
	
}
