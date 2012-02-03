/*******************************************************************************
 * Copyright (c) 2005-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.internal.ui;

import org.eclipse.osgi.util.NLS;


public class TemplatesMessages extends NLS {
	
	
	public static String Templates_Variable_ToDo_description;
	
	public static String Templates_Variable_EnclosingProject_description;
	public static String Templates_Variable_File_description;
	public static String Templates_Variable_SelectionBegin_description;
	public static String Templates_Variable_SelectionEnd_description;
	
	public static String Templates_Variable_SelectedLines_description;
	
	
	static {
		NLS.initializeMessages(TemplatesMessages.class.getName(), TemplatesMessages.class);
	}
	private TemplatesMessages() {}
	
}
