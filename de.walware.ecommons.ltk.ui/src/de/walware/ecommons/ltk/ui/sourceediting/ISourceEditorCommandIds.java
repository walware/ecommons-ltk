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

package de.walware.ecommons.ltk.ui.sourceediting;


public interface ISourceEditorCommandIds {
	
	
//--Edit--
	
	/**
	 * Action definition ID of the 'edit' &gt; 'copy (qualified) (element) name' action
	 * 
	 * Value: @value
	 */
	public static final String COPY_ELEMENT_NAME = 		"de.walware.ecommons.base.commands.CopyElementName"; //$NON-NLS-1$
	
//--Navigation--
	
	/**
	 * Action definition ID of the 'navigate' &gt; 'go to matching bracket' action
	 * 
	 * Value: @value
	 */
	public static final String GOTO_MATCHING_BRACKET = 	"de.walware.ecommons.text.commands.GotoMatchingBracket"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select enclosing action
	 * 
	 * Value: @value
	 */
	public static final String SELECT_ENCLOSING = 		"de.walware.ecommons.base.commands.SelectExpandEnclosing"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select next action
	 * 
	 * Value: @value
	 */
	public static final String SELECT_NEXT = 			"de.walware.ecommons.base.commands.SelectExpandNext"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select previous action
	 * 
	 * Value: @value
	 */
	public static final String SELECT_PREVIOUS = 		"de.walware.ecommons.base.commands.SelectExpandPrevious"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select restore last action
	 * 
	 * Value: @value
	 */
	public static final String SELECT_LAST = 			"de.walware.ecommons.base.commands.SelectLast"; //$NON-NLS-1$
	
	/**
	 * ID for command to invoke a specific content assist (with a single content assist category).
	 * 
	 * Command required attribute {@link #SPECIFIC_CONTENT_ASSIST_CATEGORY_PARAMETER_ID}.
	 */
	public static final String SPECIFIC_CONTENT_ASSIST_COMMAND_ID = "de.walware.ecommons.ltk.commands.SpecificContentAssist"; //$NON-NLS-1$
	
	/**
	 * ID for command parameter for {@link #SPECIFIC_CONTENT_ASSIST_COMMAND_ID} specifying the category to show.
	 */
	public static final String SPECIFIC_CONTENT_ASSIST_CATEGORY_PARAMETER_ID = "categoryId"; //$NON-NLS-1$
	
	
//--Source--
	
	/**
	 * Action definition ID of the 'source' &gt; 'toggle comment' action
	 * 
	 * Value: @value
	 */
	public static final String TOGGLE_COMMENT = 		"de.walware.ecommons.ltk.commands.ToggleComment"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the 'correct indentation' action
	 * 
	 * Value: @value
	 */
	public static final String CORRECT_INDENT =        "de.walware.ecommons.ltk.commands.CorrectIndent"; //$NON-NLS-1$
	
}
