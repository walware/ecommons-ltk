/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui.settings;

import de.walware.ecommons.preferences.Preference.BooleanPref;
import de.walware.ecommons.preferences.ui.RGBPref;


/**
 * Preferences for source viewer/editor decorations.
 */
public class DecorationPreferences {
	
	
	private final BooleanPref fMatchingBracketsEnabled;
	private final RGBPref fMatchingBracketsColor;
	
	
	public DecorationPreferences(final String commonQualifier) {
		fMatchingBracketsEnabled = new BooleanPref(commonQualifier, "MatchingBrackets.enabled");
		fMatchingBracketsColor = new RGBPref(commonQualifier, "MatchingBrackets.color");
	}
	
	
	
	public BooleanPref getMatchingBracketsEnabled() {
		return fMatchingBracketsEnabled;
	}
	
	public RGBPref getMatchingBracketsColor() {
		return fMatchingBracketsColor;
	}
	
}
