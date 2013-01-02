/*******************************************************************************
 * Copyright (c) 2009-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.internal.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import de.walware.ecommons.ui.util.MessageUtil;


/**
 * Map of parameters for the specific content assist command.
 */
public class SpecificContentAssistParameterValues implements IParameterValues {
	
	
	private Map<String, String> fParameterValues;
	
	
	public SpecificContentAssistParameterValues() {
	}
	
	
	@Override
	public Map<String, String> getParameterValues() {
		Map<String, String> map = fParameterValues;
		if (map == null) {
			map = new HashMap<String, String>();
			
			final IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
			final IConfigurationElement[] contributions = extensionRegistry.getConfigurationElementsFor(AdvancedExtensionsInternal.CONTENTASSIST_EXTENSIONPOINT_ID);
			for (final IConfigurationElement config : contributions) {
				if (config.getName().equals(AdvancedExtensionsInternal.CONFIG_CATEGORY_ELEMENT_NAME)) {
					try {
						final String id = AdvancedExtensionsInternal.getCheckedString(config, AdvancedExtensionsInternal.CONFIG_ID_ATTRIBUTE_NAME);
						final String name = AdvancedExtensionsInternal.getCheckedString(config, AdvancedExtensionsInternal.CONFIG_NAME_ATTRIBUTE_NAME);
						map.put(MessageUtil.removeMnemonics(name), id);
					}
					catch (final CoreException e) {
					}
				}
			}
			fParameterValues = map;
		}
		return map;
	}
	
}
