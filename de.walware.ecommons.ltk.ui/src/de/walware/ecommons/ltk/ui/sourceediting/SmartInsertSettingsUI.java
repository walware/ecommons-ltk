/*=============================================================================#
 # Copyright (c) 2012-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.viewers.LabelProvider;


public class SmartInsertSettingsUI {
	
	
	public static class SettingsLabelProvider extends LabelProvider {
		
		@Override
		public String getText(final Object element) {
			if (element instanceof ISmartInsertSettings.TabAction) {
				switch ((ISmartInsertSettings.TabAction) element) {
				case INSERT_TAB_CHAR:
					return "Insert Tab Char";
				case INSERT_INDENT_LEVEL:
					return "Insert Indent Level";
				case CORRECT_INDENT:
					return "Adjust Indentation";
				}
			}
			return super.getText(element);
		}
		
	}
	
}
