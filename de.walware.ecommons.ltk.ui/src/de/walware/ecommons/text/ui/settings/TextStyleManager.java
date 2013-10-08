/*******************************************************************************
 * Copyright (c) 2007-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

import de.walware.ecommons.text.ui.presentation.ITextPresentationConstants;
import de.walware.ecommons.ui.ColorManager;


/**
 * Manages text style tokens for a highlighting scanner.
 */
public class TextStyleManager {
	
	
	protected ColorManager colorManager;
	protected IPreferenceStore preferenceStore;
	protected String[] tokenNames;
	private final String stylesGroupId;
	
	private final Map<String, Token> tokenMap = new HashMap<String, Token>();
	
	
	public TextStyleManager(final ColorManager colorManager, final IPreferenceStore preferenceStore,
			final String stylesGroupId) {
		super();
		this.colorManager = colorManager;
		this.preferenceStore = preferenceStore;
		this.stylesGroupId = stylesGroupId;
	}
	
	
	/**
	 * Token access for styles.
	 * 
	 * @param key id and prefix for preference keys
	 * @return token with text style attribute
	 */
	public IToken getToken(final String key) {
		Token token = this.tokenMap.get(key);
		if (token == null) {
			token = new Token(createTextAttribute(key));
			this.tokenMap.put(key, token);
		}
		return token;
	}
	
	protected String resolveUsedKey(final String key) {
		String use = key;
		while (true) {
			final String test = this.preferenceStore.getString(use+ITextPresentationConstants.TEXTSTYLE_USE_SUFFIX);
			if (test == null || test.equals("") || test.equals(use)) { //$NON-NLS-1$
				return use;
			}
			use = test;
		}
	}
	
	/**
	 * Create a text attribute based on the given color, bold, italic, strikethrough and underline preference keys.
	 * 
	 * @param rootKey the italic preference key
	 * @return the created text attribute
	 * @since 3.0
	 */
	protected Object createTextAttribute(String rootKey) {
		rootKey = resolveUsedKey(rootKey);
		
		final RGB rgb = PreferenceConverter.getColor(this.preferenceStore, rootKey + ITextPresentationConstants.TEXTSTYLE_COLOR_SUFFIX);
		int style = this.preferenceStore.getBoolean(rootKey + ITextPresentationConstants.TEXTSTYLE_BOLD_SUFFIX) ?
				SWT.BOLD : SWT.NORMAL;
		if (this.preferenceStore.getBoolean(rootKey + ITextPresentationConstants.TEXTSTYLE_ITALIC_SUFFIX)) {
			style |= SWT.ITALIC;
		}
		if (this.preferenceStore.getBoolean(rootKey + ITextPresentationConstants.TEXTSTYLE_UNDERLINE_SUFFIX)) {
			style |= TextAttribute.UNDERLINE;
		}
		if (this.preferenceStore.getBoolean(rootKey + ITextPresentationConstants.TEXTSTYLE_STRIKETHROUGH_SUFFIX)) {
			style |= TextAttribute.STRIKETHROUGH;
		}
		
		return new TextAttribute(this.colorManager.getColor(rgb), null, style);
	}
	
	public void handleSettingsChanged(final Set<String> groupIds, final Map<String, Object> options) {
		if (groupIds.contains(this.stylesGroupId)) {
			for (final Map.Entry<String, Token> token : this.tokenMap.entrySet()) {
				token.getValue().setData(createTextAttribute(token.getKey()));
			}
			options.put(ITextPresentationConstants.SETTINGSCHANGE_AFFECTSPRESENTATION_KEY, Boolean.TRUE);
		}
	}
	
}
