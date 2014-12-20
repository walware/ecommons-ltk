/*=============================================================================#
 # Copyright (c) 2007-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;

import de.walware.ecommons.IDisposable;
import de.walware.ecommons.collections.ImCollections;

import de.walware.ecommons.ltk.IExtContentTypeManager;


public class ExtContentTypeServices implements IExtContentTypeManager, IDisposable {
	
	
	private static boolean matches(IContentType type, final String typeId) {
		while (type != null) {
			if (typeId.equals(type.getId())) {
				return true;
			}
			type = type.getBaseType();
		}
		return false;
	}
	
	private static boolean matches(final String[] ids, final String typeId) {
		for (int i = 0; i < ids.length; i++) {
			if (typeId.equals(ids[i])) {
				return true;
			}
		}
		return false;
	}
	
	private static void add(final Map<String, Set<String>> map, final String key, final String value) {
		Set<String> set = map.get(key);
		if (set == null) {
			set = new HashSet<>();
			map.put(key, set);
		}
		set.add(value);
	}
	
	private static Map<String, String[]> copy(final Map<String, Set<String>> from, final Map<String, String[]> to) {
		for (final Map.Entry<String, Set<String>> entry : from.entrySet()) {
			final Set<String> set = entry.getValue();
			to.put(entry.getKey(), set.toArray(new String[set.size()]));
		}
		return to;
	}
	
	
	private static final String CONFIG_CONTENTTYPEACTIVATION_EXTENSIONPOINT_ID = "de.walware.ecommons.ltk.contentTypeActivation"; //$NON-NLS-1$
	private static final String CONFIG_CONTENTTYPE_ELEMENT_NAME = "contentType"; //$NON-NLS-1$
	private static final String CONFIG_ID_ATTRIBUTE_NAME = "id"; //$NON-NLS-1$
	private static final String CONFIG_CONTENTTYPE_ID_ATTRIBUTE_NAME = "contentTypeId"; //$NON-NLS-1$
	private static final String CONFIG_SECONDARY_ID_ATTRIBUTE_NAME = "secondaryId"; //$NON-NLS-1$
	private static final String CONFIG_MODELTYPE_ID_ATTRIBUTE_NAME = "modelTypeId"; //$NON-NLS-1$
	
	
	private Map<String, String[]> fPrimaryToSecondary;
	private Map<String, String[]> fSecondaryToPrimary;
	private Map<String, String> primaryToModel;
	private Map<String, ModelTypeDescriptor> modelDescriptors;
	private final String[] NO_TYPES = new String[0];
	
	
	public ExtContentTypeServices() {
		load();
	}
	
	
	private void load() {
		final IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		
		final Map<String, ModelTypeDescriptor> modelTypes = new HashMap<>();
		
		final Map<String, Set<String>> primaryToSecondary = new HashMap<>();
		final Map<String, Set<String>> secondaryToPrimary = new HashMap<>();
		final Map<String, String> primaryToModel = new HashMap<>();
		
		{	final IConfigurationElement[] elements = extensionRegistry
					.getConfigurationElementsFor(CONFIG_CONTENTTYPEACTIVATION_EXTENSIONPOINT_ID); 
			for (final IConfigurationElement element : elements) {
				if (element.getName().equals(CONFIG_CONTENTTYPE_ELEMENT_NAME)) { 
					String primary = element.getAttribute(CONFIG_ID_ATTRIBUTE_NAME); 
					String secondary = element.getAttribute(CONFIG_SECONDARY_ID_ATTRIBUTE_NAME); 
					if (primary != null && secondary != null
							&& primary.length() > 0 && secondary.length() > 0) {
						primary = primary.intern();
						secondary = secondary.intern();
						add(primaryToSecondary, primary, secondary);
						add(secondaryToPrimary, secondary, primary);
					}
				}
			}
		}
		{	final IConfigurationElement[] elements = extensionRegistry
					.getConfigurationElementsFor("de.walware.ecommons.ltk.modelTypes"); //$NON-NLS-1$
			for (final IConfigurationElement element : elements) {
				if (element.getName().equals("modelType")) { //$NON-NLS-1$
					String id = element.getAttribute(CONFIG_ID_ATTRIBUTE_NAME); 
					if (id != null && !id.isEmpty()) {
						id = id.intern();
						ModelTypeDescriptor descriptor = modelTypes.get(id);
						if (descriptor == null) {
							descriptor = new ModelTypeDescriptor(id);
							modelTypes.put(id, descriptor);
						}
						final IConfigurationElement[] children = element.getChildren();
						for (final IConfigurationElement child : children) {
							if (child.getName().equals("secondaryType")) { //$NON-NLS-1$
								String secondaryId = child.getAttribute(CONFIG_MODELTYPE_ID_ATTRIBUTE_NAME);
								if (secondaryId != null && !secondaryId.isEmpty()) {
									secondaryId = secondaryId.intern();
									if (!descriptor.secondaryTypeIds.contains(secondaryId)) {
										descriptor.secondaryTypeIds.add(secondaryId);
									}
								}
							}
						}
					}
				}
				if (element.getName().equals(CONFIG_CONTENTTYPE_ELEMENT_NAME)) {
					String contentTypeId = element.getAttribute(CONFIG_CONTENTTYPE_ID_ATTRIBUTE_NAME); 
					String modelTypeId = element.getAttribute(CONFIG_MODELTYPE_ID_ATTRIBUTE_NAME); 
					if (contentTypeId != null && !contentTypeId.isEmpty()
							&& modelTypeId != null && !modelTypeId.isEmpty() ) {
						contentTypeId = contentTypeId.intern();
						modelTypeId = modelTypeId.intern();
						primaryToModel.put(contentTypeId, modelTypeId);
					}
				}
			}
		}
		
		checkModelTypes(modelTypes);
		
		this.fPrimaryToSecondary = copy(primaryToSecondary, new HashMap<String, String[]>());
		this.fSecondaryToPrimary = copy(secondaryToPrimary, new HashMap<String, String[]>());
		this.primaryToModel = primaryToModel;
		this.modelDescriptors = modelTypes;
	}
	
	private static void checkModelTypes(final Map<String, ModelTypeDescriptor> modelTypes) {
		final List<String> temp= new ArrayList<>();
		for (final ModelTypeDescriptor descriptor : modelTypes.values()) {
			synchronized (descriptor) {
				temp.clear();
				for (final String sId : descriptor.secondaryTypeIds) {
					if (modelTypes.containsKey(sId)) {
						temp.add(sId);
					}
				}
				descriptor.checkedSecondaryTypeIds= ImCollections.toList(temp);
			}
		}
	}
	
	
	@Override
	public String[] getSecondaryContentTypes(final String primaryContentType) {
		final String[] types = this.fPrimaryToSecondary.get(primaryContentType);
		return (types != null) ? types : this.NO_TYPES;
	}
	
	@Override
	public String[] getPrimaryContentTypes(final String secondaryContentType) {
		final String[] types = this.fSecondaryToPrimary.get(secondaryContentType);
		return (types != null) ? types : this.NO_TYPES;
	}
	
	@Override
	public boolean matchesActivatedContentType(final String primaryContentTypeId, final String activatedContentTypeId, final boolean self) {
		final IContentTypeManager manager = Platform.getContentTypeManager();
		final IContentType primaryContentType = manager.getContentType(primaryContentTypeId);
		IContentType primary = primaryContentType;
		if (self &&
				(primary.getId().equals(activatedContentTypeId)
				|| matches(primary, activatedContentTypeId))) {
			return true;
		}
		while (primary != null) {
			final String[] types = getSecondaryContentTypes(primary.getId());
			if (types != null && matches(types, activatedContentTypeId)) {
				return true;
			}
			primary = primary.getBaseType();
		}
		return false;
	}
	
	
	@Override
	public ModelTypeDescriptor getModelType(final String modelTypeId) {
		return (modelTypeId != null) ? this.modelDescriptors.get(modelTypeId) : null;
	}
	
	@Override
	public ModelTypeDescriptor getModelTypeForContentType(final String contentTypeId) {
		return getModelType(this.primaryToModel.get(contentTypeId));
	}
	
	
	@Override
	public void dispose() {
	}
	
}
