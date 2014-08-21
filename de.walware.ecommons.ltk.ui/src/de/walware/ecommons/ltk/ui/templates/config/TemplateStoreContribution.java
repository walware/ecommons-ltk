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

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import de.walware.ecommons.collections.ImCollections;


public class TemplateStoreContribution implements ITemplateContribution {
	
	
	private final TemplateStore templateStore;
	
	
	public TemplateStoreContribution(final TemplateStore store) {
		this.templateStore= store;
	}
	
	
	@Override
	public List<TemplatePersistenceData> getTemplates(final String categoryId) {
		return ImCollections.newList(this.templateStore.getTemplateData(false));
	}
	
	@Override
	public Template findTemplate(final String contextTypeId, final String name) {
		return this.templateStore.findTemplate(name, contextTypeId);
	}
	
	@Override
	public void add(final String categoryId, final TemplatePersistenceData data) {
		this.templateStore.add(data);
	}
	
	@Override
	public void delete(final TemplatePersistenceData data) {
		this.templateStore.delete(data);
	}
	
	@Override
	public void revertEdits() throws IOException {
		this.templateStore.load();
	}
	
	@Override
	public void saveEdits() throws IOException {
		this.templateStore.save();
	}
	
	@Override
	public boolean hasDeleted() {
		return (this.templateStore.getTemplateData(true).length != this.templateStore.getTemplateData(false).length);
	}
	
	@Override
	public void restoreDeleted() {
		this.templateStore.restoreDeleted();
	}
	
	@Override
	public void restoreDefaults() {
		this.templateStore.restoreDefaults(false);
	}
	
	
	@Override
	public int hashCode() {
		return this.templateStore.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final TemplateStoreContribution other= (TemplateStoreContribution) obj;
		return (this.templateStore == other.templateStore);
	}
	
}
