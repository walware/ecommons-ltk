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

package de.walware.ecommons.ltk.ui;

import org.eclipse.jface.resource.ImageRegistry;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


public class LTKUI {
	
	
	public static final String OBJ_TEXT_TEMPLATE = LTKUIPlugin.PLUGIN_ID + "/image/obj/text.template"; //$NON-NLS-1$
	public static final String OBJ_TEXT_AT_TAG = LTKUIPlugin.PLUGIN_ID + "/image/obj/text.at_tag"; //$NON-NLS-1$
	
	public static final String OBJ_TEXT_LINKEDRENAME = LTKUIPlugin.PLUGIN_ID + "/image/obj/assist.linked_rename"; //$NON-NLS-1$
	
	
	public static ImageRegistry getImages() {
		return LTKUIPlugin.getDefault().getImageRegistry();
	}
	
}
