/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui;

import org.eclipse.jface.resource.ImageRegistry;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


public class LTKUI {
	
	
	/**
	 * ID of command 'insert assignment'.
	 * 
	 * Value: @value
	 */
	public static final String INSERT_ASSIGNMENT_COMMAND_ID = "de.walware.ecommons.ltk.commands.InsertAssignment"; //$NON-NLS-1$
	
	/**
	 * ID of command 'strip comments'.
	 * 
	 * Value: @value
	 */
	public static final String STRIP_COMMENTS_COMMAND_ID = "de.walware.ecommons.ltk.commands.StripComments"; //$NON-NLS-1$
	
	/**
	 * ID of command 'add doc comment'.
	 * 
	 * Value: @value
	 */
	public static final String ADD_DOC_COMMENT_COMMAND_ID = "de.walware.ecommons.ltk.commands.AddDocComment"; //$NON-NLS-1$
	
	/**
	 * ID of command 'generate element comment'.
	 * 
	 * Value: @value
	 */
	public static final String GENERATE_ELEMENT_COMMENT_COMMAND_ID = "de.walware.ecommons.ltk.commands.GenerateElementComment"; //$NON-NLS-1$
	
	/**
	 * ID of command 'correct indentation'.
	 * 
	 * Value: @value
	 */
	public static final String CORRECT_INDENT_COMMAND_ID = "de.walware.ecommons.ltk.commands.CorrectIndent"; //$NON-NLS-1$
	
	
	/**
	 * ID of command to show quick outline.
	 * 
	 * Value: @value
	 */
	public static final String SHOW_QUICK_SOURCE_OUTLINE_COMMAND_ID = "de.walware.ecommons.ltk.commands.ShowQuickSourceOutline"; //$NON-NLS-1$
	
	/**
	 * ID of command to show quick element structure.
	 * 
	 * Value: @value
	 */
	public static final String SHOW_QUICK_ELEMENT_OUTLINE_COMMAND_ID = "de.walware.ecommons.ltk.commands.ShowQuickElementOutline"; //$NON-NLS-1$
	
	/**
	 * ID of command to show quick element hierarchy.
	 * 
	 * Value: @value
	 */
	public static final String SHOW_QUICK_ELEMENT_HIERARCHY_COMMAND_ID = "de.walware.ecommons.ltk.commands.ShowQuickElementHierarchy"; //$NON-NLS-1$
	
	
	/**
	 * ID of command to search for access (all occurrences) of an element.
	 * 
	 * Value: @value
	 */
	public static final String SEARCH_ALL_ELEMENT_ACCESS_COMMAND_ID = "de.walware.ecommons.ltk.commands.SearchAllElementAccess"; //$NON-NLS-1$
	
	/**
	 * ID of command to search for write access of an element.
	 * 
	 * Value: @value
	 */
	public static final String SEARCH_WRITE_ELEMENT_ACCESS_COMMAND_ID = "de.walware.ecommons.ltk.commands.SearchWriteElementAccess"; //$NON-NLS-1$
	
	/**
	 * Name of command parameter defining the scope of a search.
	 * 
	 * Value: @value
	 */
	public static final String SEARCH_SCOPE_PARAMETER_NAME = "scope"; //$NON-NLS-1$
	
	public static final String SEARCH_SCOPE_WORKSPACE_PARAMETER_VALUE = "workspace"; //$NON-NLS-1$
	public static final String SEARCH_SCOPE_PROJECT_PARAMETER_VALUE = "project"; //$NON-NLS-1$
	public static final String SEARCH_SCOPE_FILE_PARAMETER_VALUE = "file"; //$NON-NLS-1$
	
	
	public static final String OBJ_TEXT_TEMPLATE_IMAGE_ID = LTKUIPlugin.PLUGIN_ID + "/image/obj/text.template"; //$NON-NLS-1$
	public static final String OBJ_TEXT_AT_TAG_IMAGE_ID = LTKUIPlugin.PLUGIN_ID + "/image/obj/text.at_tag"; //$NON-NLS-1$
	
	public static final String OBJ_TEXT_LINKEDRENAME_IMAGE_ID = LTKUIPlugin.PLUGIN_ID + "/image/obj/assist.linked_rename"; //$NON-NLS-1$
	
	
	public static ImageRegistry getImages() {
		return LTKUIPlugin.getDefault().getImageRegistry();
	}
	
}
