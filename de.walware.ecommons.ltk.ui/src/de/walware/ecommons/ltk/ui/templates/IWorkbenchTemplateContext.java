/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateException;

import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


/**
 * Can extend {@link org.eclipse.jface.text.templates.TemplateContext} and similar contexts.
 */
public interface IWorkbenchTemplateContext {
	
	
	public ISourceUnit getSourceUnit();
	
	public ISourceEditor getEditor();
	
	public String evaluateInfo(Template template) throws BadLocationException, TemplateException;
	
}
