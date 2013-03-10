/*******************************************************************************
 * Copyright (c) 2009-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

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
	
	
	public static final String OBJ_TEXT_TEMPLATE_IMAGE_ID = LTKUIPlugin.PLUGIN_ID + "/image/obj/text.template"; //$NON-NLS-1$
	public static final String OBJ_TEXT_AT_TAG_IMAGE_ID = LTKUIPlugin.PLUGIN_ID + "/image/obj/text.at_tag"; //$NON-NLS-1$
	
	public static final String OBJ_TEXT_LINKEDRENAME_IMAGE_ID = LTKUIPlugin.PLUGIN_ID + "/image/obj/assist.linked_rename"; //$NON-NLS-1$
	
	
	public static ImageRegistry getImages() {
		return LTKUIPlugin.getDefault().getImageRegistry();
	}
	
}
