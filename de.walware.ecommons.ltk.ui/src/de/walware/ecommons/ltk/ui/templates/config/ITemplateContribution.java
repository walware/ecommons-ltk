/*=============================================================================#
 # Copyright (c) 2014-2016 Stephan Wahlbrink (WalWare.de) and others.
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


public interface ITemplateContribution {
	
	
	List<TemplatePersistenceData> getTemplates(String categoryId);
	
	Template findTemplate(String contextTypeId, String name);
	
	void add(String categoryId, TemplatePersistenceData data);
	
	void delete(TemplatePersistenceData data);
	
	boolean hasDeleted();
	
	void restoreDeleted();
	
	void restoreDefaults();
	
	void revertEdits() throws IOException;
	
	void saveEdits() throws IOException;
	
}
