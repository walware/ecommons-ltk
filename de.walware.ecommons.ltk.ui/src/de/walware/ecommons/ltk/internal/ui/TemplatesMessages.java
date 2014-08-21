/*=============================================================================#
 # Copyright (c) 2005-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.ui;

import org.eclipse.osgi.util.NLS;


public class TemplatesMessages extends NLS {
	
	
	public static String Templates_Variable_ToDo_description;
	
	public static String Templates_Variable_EnclosingProject_description;
	public static String Templates_Variable_File_description;
	public static String Templates_Variable_SelectionBegin_description;
	public static String Templates_Variable_SelectionEnd_description;
	
	public static String Templates_Variable_SelectedLines_description;
	
	public static String Preview_label;
	
	public static String Config_error_Read_message;
	public static String Config_error_Write_message;
	public static String Config_RestoreDefaults_title;
	public static String Config_RestoreDefaults_Completely_label;
	public static String Config_RestoreDefaults_Deleted_label;
	
	public static String Config_Import_title;
	public static String Config_Export_title;
	public static String Config_Export_filename;
	public static String Config_Export_error_title;
	public static String Config_Export_error_Hidden_message;
	public static String Config_Export_error_CanNotWrite_message;
	public static String Config_Export_Exists_title;
	public static String Config_Export_Exists_message;
	
	public static String Config_Preview_label;
	
	public static String NewDocWizardPage_title;
	public static String NewDocWizardPage_description;
	public static String NewDocWizardPage_Template_group;
	
	
	static {
		NLS.initializeMessages(TemplatesMessages.class.getName(), TemplatesMessages.class);
	}
	private TemplatesMessages() {}
	
}
