/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates.config;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;


public class CodeTemplateConfigurationRegistry {
	
	
	private static final String CATEGORY_ELEMENT= "category"; //$NON-NLS-1$
	
	
	private final String extensionPointId;
	
	private final List<TemplateCategory> categories;
	
	
	public CodeTemplateConfigurationRegistry(final String extensionPointId) {
		this.extensionPointId= extensionPointId;
		
		this.categories= new ArrayList<>();
		load(this.categories);
	}
	
	
	protected void load(final List<TemplateCategory> categories) {
		final IConfigurationElement[] elements= Platform.getExtensionRegistry()
				.getConfigurationElementsFor(this.extensionPointId);
		for (int i= 0; i < elements.length; i++) {
			switch (elements[i].getName()) {
			case CATEGORY_ELEMENT:
				createCategory(elements[i]);
				break;
			}
		}
	}
	
	private void createCategory(final IConfigurationElement element) {
		final String id= loadRequiredText(element, "id"); //$NON-NLS-1$
		final ImageDescriptor image= loadImage(element, "image"); //$NON-NLS-1$
		final String label= loadRequiredText(element, "label"); //$NON-NLS-1$
		final ImageDescriptor itemImage= loadImage(element, "itemImage"); //$NON-NLS-1$
		
		if (id != null && label != null) {
			this.categories.add(new RegistryTemplateCategory(id.intern(), image, label, itemImage,
					element ));
		}
	}
	
	
	String loadRequiredText(final IConfigurationElement element, final String attrName) {
		final String value= element.getAttribute(attrName);
		if (value != null && !value.isEmpty()) {
			return value;
		}
		return null;
	}
	
	ImageDescriptor loadImage(final IConfigurationElement element, final String attrName) {
		final String path= element.getAttribute(attrName);
		if (path != null) {
			return AbstractUIPlugin.imageDescriptorFromPlugin(element.getContributor().getName(), path);
		}
		return null;
	}
	
	
	public List<? extends TemplateCategory> getCategories() {
		return this.categories;
	}
	
}
