/*=============================================================================#
 # Copyright (c) 2014-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates.config;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.templates.ContextTypeRegistry;

import de.walware.ecommons.preferences.Preference;


public class TemplateCategory {
	
	
	private final String id;
	
	private final ImageDescriptor image;
	private final String label;
	
	private final ImageDescriptor itemImage;
	
	private ITemplateCategoryConfiguration configuration;
	private ITemplateContribution templateContribution;
	private ContextTypeRegistry contextTypeRegistry;
	private boolean defaultPrefCheck;
	
	private Set<String> templateNames;
	
	
	public TemplateCategory(final String id,
			final ImageDescriptor image, final String label, final ImageDescriptor itemImage,
			final ITemplateCategoryConfiguration configuration) {
		if (id == null) {
			throw new NullPointerException("id"); //$NON-NLS-1$
		}
		if (configuration == null) {
			throw new NullPointerException("configuration"); //$NON-NLS-1$
		}
		this.id= id;
		
		this.image= image;
		this.label= label;
		
		this.itemImage= itemImage;
		
		this.configuration= configuration;
	}
	
	TemplateCategory(final String id,
			final ImageDescriptor image, final String label, final ImageDescriptor itemImage) {
		this.id= id;
		
		this.image= image;
		this.label= label;
		
		this.itemImage= itemImage;
	}
	
	
	public String getId() {
		return this.id;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public ImageDescriptor getImage() {
		return this.image;
	}
	
	public ImageDescriptor getItemImage() {
		return this.itemImage;
	}
	
	
	void setConfiguration(final ITemplateCategoryConfiguration configuration) {
		this.configuration= configuration;
	}
	
	ITemplateCategoryConfiguration getConfiguration() {
		return this.configuration;
	}
	
	boolean isTemplateLoaded() {
		return (this.templateContribution != null);
	}
	
	ITemplateContribution getTemplateContrib(final boolean activate) {
		if (activate && this.templateContribution == null) {
			this.templateContribution= getConfiguration().getTemplates();
		}
		return this.templateContribution;
	}
	
	Preference<String> getDefaultPref() {
		return getConfiguration().getDefaultPref();
	}
	
	ContextTypeRegistry getContextTypeRegistry() {
		if (this.contextTypeRegistry == null) {
			this.contextTypeRegistry= getConfiguration().getContextTypeRegistry();
		}
		return this.contextTypeRegistry;
	}
	
	
	boolean initNames() {
		if (this.templateNames == null) {
			this.templateNames= new HashSet<>();
			return true;
		}
		return false;
	}
	
	void clearNames() {
		this.templateNames= null;
	}
	
	void addName(final String name) {
		this.templateNames.add(name);
	}
	
	boolean hasName(final String name) {
		return this.templateNames.contains(name);
	}
	
	
	@Override
	public String toString() {
		return this.id;
	}
	
}
