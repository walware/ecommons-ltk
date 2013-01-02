/*******************************************************************************
 * Copyright (c) 2012-2013 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text;



public interface IIndentSettings {
	
	
	enum IndentationType {
		TAB, SPACES,
	}
	
	
	String TAB_SIZE_PROP = "tabSize"; //$NON-NLS-1$
	
	int getTabSize();
	
	
	String INDENT_DEFAULT_TYPE_PROP = "indentDefaultType"; //$NON-NLS-1$
	
	IndentationType getIndentDefaultType();
	
	
	String INDENT_SPACES_COUNT_PROP = "indentSpacesCount"; //$NON-NLS-1$
	
	int getIndentSpacesCount();
	
	
	String REPLACE_TABS_WITH_SPACES_PROP = "replaceOtherTabsWithSpaces"; //$NON-NLS-1$
	
	boolean getReplaceOtherTabsWithSpaces();
	
	
	String REPLACE_CONSERVATIVE_PROP = "replaceConservative"; //$NON-NLS-1$
	
	boolean getReplaceConservative();
	
	
	String WRAP_LINE_WIDTH_PROP = "lineWidth"; //$NON-NLS-1$
	
	int getLineWidth();
	
	
}
