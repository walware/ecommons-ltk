/*=============================================================================#
 # Copyright (c) 2012-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.workbench.ui.util;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;


/**
 * Utility for theme settings
 */
public class ThemeUtil {
	
	
	private final ColorRegistry fColorRegistry;
	private final StringBuilder fBuffer;
	
	
	public ThemeUtil() {
		fColorRegistry = (PlatformUI.isWorkbenchRunning()) ?
				PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry() :
				null;
		fBuffer = new StringBuilder();
	}
	
	
	/**
	 * Return the theme color as encoded RGB value as used for preferences
	 * 
	 * @param key key of theme color
	 * @return encoded color
	 */
	public String getColorPrefValue(final String key) {
		final RGB rgb = (fColorRegistry != null) ? fColorRegistry.getRGB(key) : null;
		if (rgb != null) {
			fBuffer.setLength(0);
			fBuffer.append(rgb.red);
			fBuffer.append(',');
			fBuffer.append(rgb.green);
			fBuffer.append(',');
			fBuffer.append(rgb.blue);
			return fBuffer.toString();
		}
		return (key.endsWith("BackgroundColor")) ? "255,255,255" : "0,0,0"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}
