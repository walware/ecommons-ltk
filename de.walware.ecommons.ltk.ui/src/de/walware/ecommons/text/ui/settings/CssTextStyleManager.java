/*=============================================================================#
 # Copyright (c) 2010-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.ui.settings;

import static de.walware.ecommons.text.ui.presentation.ITextPresentationConstants.TEXTSTYLE_BOLD_SUFFIX;
import static de.walware.ecommons.text.ui.presentation.ITextPresentationConstants.TEXTSTYLE_COLOR_SUFFIX;
import static de.walware.ecommons.text.ui.presentation.ITextPresentationConstants.TEXTSTYLE_ITALIC_SUFFIX;
import static de.walware.ecommons.text.ui.presentation.ITextPresentationConstants.TEXTSTYLE_STRIKETHROUGH_SUFFIX;
import static de.walware.ecommons.text.ui.presentation.ITextPresentationConstants.TEXTSTYLE_UNDERLINE_SUFFIX;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;


public class CssTextStyleManager extends TextStyleManager {
	
	
	private static void appendCssColor(final StringBuilder sb, final RGB color) {
		sb.append('#');
		String s = Integer.toHexString(color.red);
		if (s.length() == 1) {
			sb.append('0');
		}
		sb.append(s);
		s = Integer.toHexString(color.green);
		if (s.length() == 1) {
			sb.append('0');
		}
		sb.append(s);
		s = Integer.toHexString(color.blue);
		if (s.length() == 1) {
			sb.append('0');
		}
		sb.append(s);
	}
	
	
	private final String defaultRootKey;
	
	
	public CssTextStyleManager(final IPreferenceStore preferenceStore, final String stylesGroupId,
			final String defaultRootKey) {
		super(null, preferenceStore, stylesGroupId);
		this.defaultRootKey= defaultRootKey;
	}
	
	
	@Override
	protected Object createTextAttribute(String key) {
		if (key != null) {
			key= resolveUsedKey(key);
			
			if (key.equals(this.defaultRootKey)) {
				return null;
			}
		}
		else {
			key = this.defaultRootKey;
		}
		
		final StringBuilder sb = new StringBuilder(32);
		final RGB rgb = PreferenceConverter.getColor(this.preferenceStore, key + TEXTSTYLE_COLOR_SUFFIX);
		sb.append("color: "); //$NON-NLS-1$
		appendCssColor(sb, rgb);
		sb.append("; "); //$NON-NLS-1$
		if (this.preferenceStore.getBoolean(key + TEXTSTYLE_BOLD_SUFFIX)) {
			sb.append("font-weight: bold; "); //$NON-NLS-1$
		}
		if (this.preferenceStore.getBoolean(key + TEXTSTYLE_ITALIC_SUFFIX)) {
			sb.append("font-style: italic; "); //$NON-NLS-1$
		}
		final boolean strikethrough = this.preferenceStore.getBoolean(key + TEXTSTYLE_STRIKETHROUGH_SUFFIX);
		final boolean underline = this.preferenceStore.getBoolean(key + TEXTSTYLE_UNDERLINE_SUFFIX);
		if (strikethrough || underline) {
			sb.append("text-decoration:"); //$NON-NLS-1$
			if (strikethrough) {
				sb.append(" line-through"); //$NON-NLS-1$
			}
			if (underline) {
				sb.append(" underline"); //$NON-NLS-1$
			}
			sb.append("; "); //$NON-NLS-1$
		}
		
		return sb.substring(0, sb.length()-1);
	}
	
}
