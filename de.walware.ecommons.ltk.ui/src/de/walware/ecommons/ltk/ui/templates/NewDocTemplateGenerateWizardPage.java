/*=============================================================================#
 # Copyright (c) 2012-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.core.Preference;
import de.walware.ecommons.ui.util.LayoutUtil;

import de.walware.ecommons.ltk.internal.ui.TemplatesMessages;
import de.walware.ecommons.ltk.ui.templates.config.ITemplateCategoryConfiguration;


public class NewDocTemplateGenerateWizardPage extends WizardPage {
	
	
	private final String categoryId;
	private final ITemplateCategoryConfiguration config;
	
	private TemplateSelectionComposite templateSelectComposite;
	
	
	public NewDocTemplateGenerateWizardPage(final ITemplateCategoryConfiguration config,
			final String categoryId) {
		super("CodeGen");
		this.config= config;
		this.categoryId= categoryId;
		
		setTitle(TemplatesMessages.NewDocWizardPage_title);
		setDescription(TemplatesMessages.NewDocWizardPage_description);
	}
	
	
	public ContextTypeRegistry getContextTypeRegistry() {
		return this.config.getContextTypeRegistry();
	}
	
	public Template getTemplate() {
		if (this.templateSelectComposite != null) {
			return this.templateSelectComposite.getSelectedTemplate();
		}
		else {
			return this.config.getTemplates().findTemplate(this.categoryId,
					getDefaultTemplateName() );
		}
	}
	
	private String getDefaultTemplateName() {
		final Preference<String> defaultPref= this.config.getDefaultPref();
		if (defaultPref == null) {
			return null;
		}
		return PreferencesUtil.getInstancePrefs().getPreferenceValue(defaultPref);
	}
	
	protected List<Template> getAvailableTemplates() {
		final List<TemplatePersistenceData> templateDatas= this.config.getTemplates().getTemplates(this.categoryId);
		final List<Template> templates= new ArrayList<>(templateDatas.size());
		for (final TemplatePersistenceData templateData : templateDatas) {
			final Template template= templateData.getTemplate();
			if (getCategoryId(template).equals(this.categoryId)) {
				templates.add(template);
			}
		}
		return templates;
	}
	
	protected String getCategoryId(final Template template) {
		// de.walware.ecommons.ltk.ui.templates.config.CodeTemplateConfigurationBlock.getCategoryId(Template)
		final String name= template.getName();
		final int idx= name.indexOf(':');
		return (idx > 0) ? name.substring(0, idx) : name;
	}
	
	@Override
	public void createControl(final Composite parent) {
		final Group group= new Group(parent, SWT.NONE);
		group.setLayout(LayoutUtil.createGroupGrid(1));
		group.setText(TemplatesMessages.NewDocWizardPage_Template_group);
		
		this.templateSelectComposite= new TemplateSelectionComposite(group);
		this.templateSelectComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.templateSelectComposite.setConfigurator(this.config.createViewerConfiguator(null, null,
				this.templateSelectComposite.getPreview().getTemplateVariableProcessor(),
				null ));
		this.templateSelectComposite.setInput(getAvailableTemplates(), true,
				getContextTypeRegistry() );
		this.templateSelectComposite.setSelection(getDefaultTemplateName());
		
		setControl(group);
	}
	
}
