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

package de.walware.ecommons.ltk.internal.core;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;


public class AdapterFactory {
	
	
	private static final String ADAPTER_ELEMENT_NAME = "adapter"; //$NON-NLS-1$
	
	private static final String MODEL_TYPE_ID_ATTRIBUTE_NAME = "modelTypeId"; //$NON-NLS-1$
	private static final String TYPE_ATTRIBUTE_NAME = "type"; //$NON-NLS-1$
	private static final String CLASS_ATTRIBUTE_NAME = "class"; //$NON-NLS-1$
	
	
	private static interface AdapterContribution {
		
		Object getAdapter(String modelTypeId, Class<?> required);
		
	}
	
	private static class ClassContribution implements AdapterContribution {
		
		private IConfigurationElement fConfigurationElement;
		
		public ClassContribution(final IConfigurationElement contributionElement) {
			fConfigurationElement = contributionElement;
		}
		
		@Override
		public Object getAdapter(final String modelTypeId, final Class<?> required) {
			try {
				return fConfigurationElement.createExecutableExtension(CLASS_ATTRIBUTE_NAME);
			}
			catch (final CoreException e) {
				LTKCorePlugin.getDefault().log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID, 0,
						NLS.bind("An error occurred when loading adapter class for model ''{0}''.", modelTypeId),
						e ));
				fConfigurationElement = null;
			}
			return null;
		}
		
	}
	
	private static class FactoryContribution implements AdapterContribution {
		
		private IConfigurationElement fConfigurationElement;
		private IAdapterFactory fFactory;
		
		public FactoryContribution(final IConfigurationElement configurationElement) {
			fConfigurationElement = configurationElement;
		}
		
		@Override
		public Object getAdapter(final String modelTypeId, final Class<?> required) {
			synchronized (this) {
				if (fConfigurationElement != null) {
					try {
						fFactory = (IAdapterFactory) fConfigurationElement.createExecutableExtension(CLASS_ATTRIBUTE_NAME);
						fConfigurationElement = null;
					}
					catch (final CoreException e) {
						LTKCorePlugin.getDefault().log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID, 0,
								NLS.bind("An error occurred when loading adapter factory for model ''{0}''.", modelTypeId),
								e ));
						fConfigurationElement = null;
					}
				}
			}
			if (fFactory != null) {
				return fFactory.getAdapter(modelTypeId, required);
			}
			return null;
		}
		
	}
	
	
	private final String fExtensionPointId;
	
	private final Map<String, Map<String, AdapterContribution>> fMap = new IdentityHashMap<String,
			Map<String, AdapterContribution>>();
	
	
	public AdapterFactory(final String extensionPointId) {
		fExtensionPointId = extensionPointId;
		load();
	}
	
	
	private void load() {
		final IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		final IConfigurationElement[] elements = extensionRegistry.getConfigurationElementsFor(
				fExtensionPointId);
		for (final IConfigurationElement contributionElement : elements) {
			String modelTypeId = contributionElement.getAttribute(
					MODEL_TYPE_ID_ATTRIBUTE_NAME);
			if (modelTypeId != null) {
				modelTypeId = modelTypeId.intern();
				Map<String, AdapterContribution> map = fMap.get(modelTypeId);
				if (map == null) {
					map = new HashMap<String, AdapterContribution>();
					fMap.put(modelTypeId, map);
				}
				final IConfigurationElement[] adapterElements = contributionElement.getChildren();
				for (final IConfigurationElement adapterElement : adapterElements) {
					if (adapterElement.getName().equals(ADAPTER_ELEMENT_NAME)) {
						final String type = adapterElement.getAttribute(TYPE_ATTRIBUTE_NAME);
						if (type != null) {
							if (contributionElement.getName().equals("adapterClass")) { //$NON-NLS-1$
								map.put(type, new ClassContribution(contributionElement));
							}
							else if (contributionElement.getName().equals("adapterFactory")) { //$NON-NLS-1$
								map.put(type, new FactoryContribution(contributionElement));
							}
						}
					}
				}
			}
		}
	}
	
	public Object get(final String modelTypeId, final Class<?> required) {
		AdapterContribution contribution;
		synchronized (fMap) {
			final Map<String, AdapterContribution> map = fMap.get(modelTypeId);
			if (map == null) {
				return null;
			}
			contribution = map.get(required.getName());
		}
		return (contribution != null) ? contribution.getAdapter(modelTypeId, required) : null;
	}
	
}
