/*******************************************************************************
 * Copyright (c) 2005-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui.settings;

import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.swt.graphics.Color;

import de.walware.ecommons.preferences.IPreferenceAccess;
import de.walware.ecommons.preferences.Preference.BooleanPref;
import de.walware.ecommons.preferences.Preference.IntPref;
import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.ui.RGBPref;
import de.walware.ecommons.ui.ColorManager;
import de.walware.ecommons.ui.SharedUIResources;


/**
 * Preferences for content and quick assist assistant.
 */
public class AssistPreferences {
	
	
	private final String fGroupId;
	
	/**
	 * Preference for content assist auto activation
	 */
	private final BooleanPref fAutoActivationEnabled;
	
	/**
	 * Preference for content assist auto activation delay
	 */
	private final IntPref fAutoActivationDelay;
	
	/**
	 * Preference for content assist auto insert
	 */
	private final BooleanPref fAutoInsertSingle;
	
	/**
	 * Preference for content assist auto insert
	 */
	private final BooleanPref fAutoInsertPrefix;
	
	/**
	 * Preference key for content assist parameters color.
	 */
	private final RGBPref fInformationBackground;
	
	/**
	 * Preference for content assist parameters color
	 */
	private final RGBPref fInformationForeground;
	
//	/**
//	 * A named preference that holds the background color used in the code
//	 * assist selection dialog to mark replaced code.
//	 */
//	private final RGBPref fReplacementBackground;
//	
//	/**
//	 * A named preference that holds the foreground color used in the code
//	 * assist selection dialog to mark replaced code.
//	 */
//	private final RGBPref fReplacementForeground;
	
	
	public AssistPreferences(final String prefQualifier, final String groupId) {
		fGroupId = groupId;
		
		fAutoActivationEnabled = new BooleanPref(prefQualifier, "AutoActivation.enable"); //$NON-NLS-1$
		fAutoActivationDelay = new IntPref(prefQualifier, "AutoActivation.delay"); //$NON-NLS-1$
		fAutoInsertSingle = new BooleanPref(prefQualifier, "AutoInsert.Single.enable"); //$NON-NLS-1$
		fAutoInsertPrefix = new BooleanPref(prefQualifier, "AutoInsert.Prefix.enable"); //$NON-NLS-1$
		
		fInformationBackground = new RGBPref(prefQualifier, "Parameters.background"); //$NON-NLS-1$
		fInformationForeground = new RGBPref(prefQualifier, "Parameters.foreground"); //$NON-NLS-1$
		
//		fReplacementBackground = new RGBPref(prefQualifier, "CompletionReplacement.background"); //$NON-NLS-1$
//		fReplacementForeground = new RGBPref(prefQualifier, "CompletionReplacement.foreground"); //$NON-NLS-1$
	}
	
	
//	/** Preference key for java content assist auto activation triggers */
//	private final static String AUTOACTIVATION_TRIGGERS_JAVA= PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA;
//	/** Preference key for javadoc content assist auto activation triggers */
//	private final static String AUTOACTIVATION_TRIGGERS_JAVADOC= PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC;
	
//	/** Preference key for visibility of proposals */
//	private final static String SHOW_VISIBLE_PROPOSALS= PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS;
//	/** Preference key for alphabetic ordering of proposals */
//	private final static String ORDER_PROPOSALS= PreferenceConstants.CODEASSIST_ORDER_PROPOSALS;
//	/** Preference key for case sensitivity of proposals */
//	private final static String CASE_SENSITIVITY= PreferenceConstants.CODEASSIST_CASE_SENSITIVITY;
//	/** Preference key for adding imports on code assist */
//	/** Preference key for filling argument names on method completion */
//	private static final String FILL_METHOD_ARGUMENTS= PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES;
//	/** Preference key for prefix completion. */
//	private static final String PREFIX_COMPLETION= PreferenceConstants.CODEASSIST_PREFIX_COMPLETION;
	
	
	public String getGroupId() {
		return fGroupId;
	}
	
	public BooleanPref getAutoActivationEnabledPref() {
		return fAutoActivationEnabled;
	}
	
	public IntPref getAutoActivationDelayPref() {
		return fAutoActivationDelay;
	}
	
	public BooleanPref getAutoInsertSinglePref() {
		return fAutoInsertSingle;
	}
	
	public BooleanPref getAutoInsertPrefixPref() {
		return fAutoInsertPrefix;
	}
	
	public RGBPref getInformationBackgroundPref() {
		return fInformationBackground;
	}
	
	public RGBPref getInformationForegroundPref() {
		return fInformationForeground;
	}
	
//	public RGBPref getReplacementBackgroundPref() {
//		return fReplacementBackground;
//	}
//	
//	public RGBPref getReplacementForegroundPref() {
//		return fReplacementForeground;
//	}
	
	
	/**
	 * Configure the given content assistant according common StatET settings.
	 */
	public void configure(final ContentAssistant assistant) {
		final ColorManager manager = SharedUIResources.getColors();
		final IPreferenceAccess statet = PreferencesUtil.getInstancePrefs();
		
		assistant.enableAutoActivation(statet.getPreferenceValue(fAutoActivationEnabled));
		assistant.setAutoActivationDelay(statet.getPreferenceValue(fAutoActivationDelay));
		assistant.enableAutoInsert(statet.getPreferenceValue(fAutoInsertSingle));
		assistant.enablePrefixCompletion(statet.getPreferenceValue(fAutoInsertPrefix));
		{	final Color c = manager.getColor(statet.getPreferenceValue(fInformationForeground));
			assistant.setContextInformationPopupForeground(c);
			assistant.setContextSelectorForeground(c);
		}
		{	final Color c = manager.getColor(statet.getPreferenceValue(fInformationBackground));
			assistant.setContextInformationPopupBackground(c);
			assistant.setContextSelectorBackground(c);
		}
		
//		assistant.enableColoredLabels(true);
	}
	
//	public static void configureInformationProposalMode(final ContentAssist assistant, final boolean enable) {
//		final ColorManager manager = StatetUIPlugin.getDefault().getColorManager();
//		final IPreferenceAccess statet = PreferencesUtil.getInstancePrefs();
//		
//		assistant.setProposalSelectorForeground(manager.getColor(statet.getPreferenceValue(enable ?
//				PARAMETERS_FOREGROUND : PROPOSALS_FOREGROUND)));
//		assistant.setProposalSelectorBackground(manager.getColor(statet.getPreferenceValue(enable ?
//				PARAMETERS_BACKGROUND : PROPOSALS_BACKGROUND)));
//	}
	
	/**
	 * Configure the given quick assistant according common StatET settings.
	 */
	public void configure(final IQuickAssistAssistant assistant) {
	}
	
}
