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

package de.walware.ecommons.ltk.internal.ui.config;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
	
	
	public static String StatetBase_description;
	
	public static String Editors_link;
	public static String Editors_Appearance;
	public static String Editors_HighlightMatchingBrackets;
	public static String Editors_AppearanceColors;
	public static String Editors_Color;
	public static String Editors_MatchingBracketsHighlightColor;
	public static String Editors_CodeAssistParametersForegrondColor;
	public static String Editors_CodeAssistParametersBackgroundColor;
	public static String Editors_CodeAssistReplacementForegroundColor;
	public static String Editors_CodeAssistReplacementBackgroundColor;
	public static String Editors_CodeAssist;
	public static String Editors_CodeAssist_AutoTriggerDelay_label;
	public static String Editors_CodeAssist_AutoTriggerDelay_error_message;
	
	
	static {
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}
	private Messages() {}
	
}
