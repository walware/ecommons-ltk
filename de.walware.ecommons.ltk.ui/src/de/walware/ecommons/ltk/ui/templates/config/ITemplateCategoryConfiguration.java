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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;

import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.templates.TemplateVariableProcessor;

import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;


public interface ITemplateCategoryConfiguration {
	
	
	ITemplateContribution getTemplates();
	
	Preference<String> getDefaultPref();
	
	
	ContextTypeRegistry getContextTypeRegistry();
	
	String getDefaultContextTypeId();
	
	
	String getViewerConfigId(TemplatePersistenceData data);
	
	SourceEditorViewerConfigurator createViewerConfiguator(String viewerConfigId, TemplatePersistenceData data,
			TemplateVariableProcessor templateProcessor, IProject project);
	
}
