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

package de.walware.ecommons.ltk.internal.ui;

import java.util.Set;

import org.eclipse.jface.text.source.AnnotationPainter.ITextStyleStrategy;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

import de.walware.ecommons.IDisposable;
import de.walware.ecommons.preferences.SettingsChangeNotifier;


public class OverwriteTextStyleStrategy implements ITextStyleStrategy, IDisposable, SettingsChangeNotifier.ChangeListener {
	
	
	private Color fColor;
	
	
	public OverwriteTextStyleStrategy() {
//		PreferencesUtil.getSettingsChangeNotifier().addChangeListener(this);
	}
	
	
	@Override
	public void applyTextStyle(final StyleRange styleRange, final Color annotationColor) {
		Color color = fColor;
		if (fColor == null) {
			fColor = color = initColor(annotationColor);
		}
		styleRange.strikeout = true;
		styleRange.strikeoutColor = color;
		styleRange.foreground = color;
	}
	
	
	public Color initColor(final Color fallback) {
//		final RGB rgb = ;
//		if (rgb != null) {
//			return ECommonsUI.getColors().getColor(rgb);
//		}
		return fallback;
	}
	
	@Override
	public void settingsChanged(final Set<String> groupIds) {
//		if (groupIds.contains(GROUP_ID)) {
//			fColor = null;
//		}
	}
	
	@Override
	public void dispose() {
//		PreferencesUtil.getSettingsChangeNotifier().removeChangeListener(this);
	}
	
}
