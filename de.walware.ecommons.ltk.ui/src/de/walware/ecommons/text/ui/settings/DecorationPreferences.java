/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.ui.settings;

import de.walware.ecommons.preferences.core.Preference.BooleanPref;
import de.walware.ecommons.preferences.ui.RGBPref;


/**
 * Preferences for source viewer/editor decorations.
 */
public class DecorationPreferences {
	
	
	public static final String MATCHING_BRACKET_ENABLED_KEY = "MatchingBrackets.enabled"; //$NON-NLS-1$
	public static final String MATCHING_BRACKET_COLOR_KEY = "MatchingBrackets.color"; //$NON-NLS-1$
	
	
	private final BooleanPref fMatchingBracketsEnabled;
	private final RGBPref fMatchingBracketsColor;
	
	
	public DecorationPreferences(final String commonQualifier) {
		fMatchingBracketsEnabled = new BooleanPref(commonQualifier, MATCHING_BRACKET_ENABLED_KEY);
		fMatchingBracketsColor = new RGBPref(commonQualifier, MATCHING_BRACKET_COLOR_KEY);
	}
	
	
	
	public BooleanPref getMatchingBracketsEnabled() {
		return fMatchingBracketsEnabled;
	}
	
	public RGBPref getMatchingBracketsColor() {
		return fMatchingBracketsColor;
	}
	
}
