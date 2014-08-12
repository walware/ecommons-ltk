/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import de.walware.ecommons.text.ui.settings.DecorationPreferences;

import de.walware.workbench.ui.IWaThemeConstants;
import de.walware.workbench.ui.util.ThemeUtil;

import de.walware.ecommons.ltk.ui.LTKUIPreferences;


public class LTKUIPreferenceInitializer extends AbstractPreferenceInitializer {
	
	
	public LTKUIPreferenceInitializer() {
	}
	
	
	@Override
	public void initializeDefaultPreferences() {
		final IScopeContext context= DefaultScope.INSTANCE;
		final ThemeUtil theme = new ThemeUtil();
		
		{	final IEclipsePreferences node= context.getNode(LTKUIPlugin.PLUGIN_ID);
			node.putBoolean(DecorationPreferences.MATCHING_BRACKET_ENABLED_KEY, true);
			node.put(DecorationPreferences.MATCHING_BRACKET_COLOR_KEY,
					theme.getColorPrefValue(IWaThemeConstants.MATCHING_BRACKET_COLOR) );
		}
		{	final IEclipsePreferences node= context.getNode(LTKUIPreferences.ASSIST_PREF_QUALIFIER);
			node.putInt(LTKUIPreferences.CONTENT_ASSIST_DELAY_PREF_KEY, 200);
			node.put(LTKUIPreferences.CONTEXT_INFO_BACKGROUND_COLOR_PREF_KEY,
					theme.getColorPrefValue(IWaThemeConstants.INFORMATION_BACKGROUND_COLOR) );
			node.put(LTKUIPreferences.CONTEXT_INFO_FOREGROUND_COLOR_PREF_KEY,
					theme.getColorPrefValue(IWaThemeConstants.INFORMATION_COLOR) );
		}
	}
	
}
