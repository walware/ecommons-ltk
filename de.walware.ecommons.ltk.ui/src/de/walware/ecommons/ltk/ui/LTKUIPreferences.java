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

package de.walware.ecommons.ltk.ui;

import org.eclipse.jface.preference.IPreferenceStore;

import de.walware.ecommons.preferences.core.Preference.IntPref;
import de.walware.ecommons.preferences.ui.RGBPref;
import de.walware.ecommons.text.ui.settings.DecorationPreferences;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


public class LTKUIPreferences {
	
	
	public static IPreferenceStore getPreferenceStore() {
		return LTKUIPlugin.getInstance().getPreferenceStore();
	}
	
	
	private static DecorationPreferences DECORATION_PREFERENCES= new DecorationPreferences(
			LTKUIPlugin.PLUGIN_ID );
	
	public static DecorationPreferences getEditorDecorationPreferences() {
		return DECORATION_PREFERENCES;
	}
	
	
	public static final String ASSIST_GROUP_ID= "LTK/assist"; //$NON-NLS-1$
	
	public static final String ASSIST_PREF_QUALIFIER= LTKUIPlugin.PLUGIN_ID + "/assist"; //$NON-NLS-1$
	
	public static final String CONTENT_ASSIST_DELAY_PREF_KEY= "ContentAssist.AutoActivation.delay"; //$NON-NLS-1$
	public static final IntPref CONTENT_ASSIST_DELAY_PREF= new IntPref(
			ASSIST_PREF_QUALIFIER, CONTENT_ASSIST_DELAY_PREF_KEY);
	
	public static final String CONTEXT_INFO_BACKGROUND_COLOR_PREF_KEY= "ContextInfo.Background.color"; //$NON-NLS-1$
	public static final RGBPref CONTEXT_INFO_BACKGROUND_COLOR_PREF= new RGBPref(
			ASSIST_PREF_QUALIFIER, CONTEXT_INFO_BACKGROUND_COLOR_PREF_KEY);
	
	public static final String CONTEXT_INFO_FOREGROUND_COLOR_PREF_KEY= "ContextInfo.Foreground.color"; //$NON-NLS-1$
	public static final RGBPref CONTEXT_INFO_FOREGROUND_COLOR_PREF= new RGBPref(
			ASSIST_PREF_QUALIFIER, CONTEXT_INFO_FOREGROUND_COLOR_PREF_KEY);
	
}
