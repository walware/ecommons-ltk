/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

import de.walware.ecommons.ICommonStatusConstants;
import de.walware.ecommons.IDisposable;
import de.walware.ecommons.preferences.IPreferenceAccess;
import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.preferences.Preference.StringArrayPref;
import de.walware.ecommons.preferences.Preference.StringSetPref;
import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.SettingsChangeNotifier;
import de.walware.ecommons.preferences.SettingsChangeNotifier.ManageListener;
import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.internal.ui.AdvancedExtensionsInternal;


/**
 * Registry of contributions for {@link ContentAssistProcessor}s for a single content type.
 */
public class ContentAssistComputerRegistry implements ManageListener, IDisposable {
	
	
	public static String DEFAULT_DISABLED= "assist.default.disabled_categories.ids"; //$NON-NLS-1$
	public static String CIRCLING_ORDERED= "assist.circling.ordered_categories.ids:enabled"; //$NON-NLS-1$
	
	
	public static final Bundle getBundle(final IConfigurationElement element) {
		final String namespace= element.getDeclaringExtension().getContributor().getName();
		final Bundle bundle= Platform.getBundle(namespace);
		return bundle;
	}
	
	protected static final Map<String, ContentAssistCategory> createCategoryByIdMap(final List<ContentAssistCategory> categories) {
		final Map<String, ContentAssistCategory> map= new HashMap<>();
		for (final ContentAssistCategory category : categories) {
			map.put(category.getId(), category);
		}
		return map;
	}
	
	
	/**
	 * The description of an {@link IContentAssistComputer}
	 */
	final class ComputerDescriptor {
		
		
		/** The identifier of the extension. */
		private final String id;
		
		private final Set<String> partitions;
		
		/** The configuration element of this extension. */
		private final IConfigurationElement configurationElement;
		
		/** The computer, if instantiated, <code>null</code> otherwise. */
		private IContentAssistComputer computer;
		
		/** Tells whether we tried to load the computer. */
		private boolean triedLoadingComputer= false;
		
		
		/**
		 * Creates a new descriptor with lazy loaded computer
		 */
		ComputerDescriptor(final String id, final Set<String> partitions, final IConfigurationElement configurationElement) {
			this.id= id;
			this.partitions= partitions;
			this.configurationElement= configurationElement;
		}
		
		
		/**
		 * Returns the identifier of the described extension.
		 *
		 * @return Returns the id
		 */
		public String getId() {
			return this.id;
		}
		
		/**
		 * Returns the partition types of the described extension.
		 * 
		 * @return the set of partition types (element type: {@link String})
		 */
		public Set<String> getPartitions() {
			return this.partitions;
		}
		
		/**
		 * Returns a cached instance of the computer
		 */
		public IContentAssistComputer getComputer() {
			if (this.computer == null && !this.triedLoadingComputer && this.configurationElement != null) {
				this.triedLoadingComputer= true;
				try {
					this.computer= (IContentAssistComputer) this.configurationElement.createExecutableExtension(AdvancedExtensionsInternal.CONFIG_CLASS_ATTRIBUTE_NAME);
				}
				catch (final CoreException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, -1,
							NLS.bind("Loading Content Assist Computer ''{0}'' failed (contributed by= '' {1}'').", //$NON-NLS-1$
									this.id, this.configurationElement.getDeclaringExtension().getContributor().getName() ),
							e )); 
				}
			}
			return this.computer;
		}
		
	}
	
	class CategoryPreferences {
		
		public final List<ContentAssistCategory> allCategories;
		public final List<ContentAssistCategory> defaultEnabledCategories;
		public final List<ContentAssistCategory> circlingOrderedCategories;
		public final List<ContentAssistCategory> circlingOrderedEnabledCategories;
		
		
		public CategoryPreferences(final List<ContentAssistCategory> categories) {
			this.allCategories= categories;
			this.defaultEnabledCategories= new ArrayList<>(categories.size());
			this.circlingOrderedCategories= new ArrayList<>(categories.size());
			this.circlingOrderedEnabledCategories= new ArrayList<>(categories.size());
		}
		
	}
	
	
	private final String contentTypeId;
	
	private final String settingsGroupId;
	private final StringSetPref prefDisabledCategoryIds;
	private final StringArrayPref prefOrderedCategoryIds;
	
	private List<ContentAssistCategory> categories;
	private String specificModeId;
	
	
	public ContentAssistComputerRegistry(final String contentTypeId, final String prefQualifier, 
			final String settingsGroupId) {
		this.contentTypeId= contentTypeId;
		this.settingsGroupId= settingsGroupId;
		this.prefDisabledCategoryIds= new StringSetPref(prefQualifier, DEFAULT_DISABLED);
		this.prefOrderedCategoryIds= new StringArrayPref(prefQualifier, CIRCLING_ORDERED);
		
		PreferencesUtil.getSettingsChangeNotifier().addManageListener(this);
	}
	
	
	@Override
	public void dispose() {
		final SettingsChangeNotifier notifier= PreferencesUtil.getSettingsChangeNotifier();
		if (notifier != null) {
			notifier.removeManageListener(this);
		}
	}
	
	
	String getSettingsGroupId() {
		return this.settingsGroupId;
	}
	
	StringSetPref getPrefDefaultDisabledCategoryIds() {
		return this.prefDisabledCategoryIds;
	}
	
	StringArrayPref getPrefCirclingOrderedCategoryIds() {
		return this.prefOrderedCategoryIds;
	}
	
	@Override
	public void beforeSettingsChangeNotification(final Set<String> groupIds) {
		if (this.settingsGroupId != null && groupIds.contains(this.settingsGroupId)) {
			synchronized (this) {
				if (this.categories != null) {
					this.categories= Collections.unmodifiableList(applyPreferences(
							PreferencesUtil.getInstancePrefs(), this.categories));
				}
			}
		}
	}
	
	@Override
	public void afterSettingsChangeNotification(final Set<String> groupIds) {
	}
	
	private List<ContentAssistCategory> loadExtensions() {
		final ArrayList<IConfigurationElement> categoryConfigs= new ArrayList<>(); // categories of all content types!
		final Map<String, List<ComputerDescriptor>> computersByCategoryId= new HashMap<>();
		
		final IExtensionRegistry extensionRegistry= Platform.getExtensionRegistry();
		
		final IConfigurationElement[] elements= extensionRegistry.getConfigurationElementsFor(
				AdvancedExtensionsInternal.CONTENTASSIST_EXTENSIONPOINT_ID);
		for (final IConfigurationElement element : elements) {
			if (element.getName().equals(AdvancedExtensionsInternal.CONFIG_CATEGORY_ELEMENT_NAME)) {
				categoryConfigs.add(element);
				continue;
			}
			if (element.getName().equals(AdvancedExtensionsInternal.CONFIG_COMPUTER_ELEMENT_NAME)) {
				// Create computer descriptor
				String id= null;
				try {
					final String contentTypeId= AdvancedExtensionsInternal.getCheckedString(element, AdvancedExtensionsInternal.CONFIG_CONTENT_TYPE_ID_ATTRIBUTE_NAME);
					if (!this.contentTypeId.equals(contentTypeId)) {
						continue;
					}
					id= AdvancedExtensionsInternal.getCheckedString(element, AdvancedExtensionsInternal.CONFIG_ID_ATTRIBUTE_NAME).intern();
					final String categoryId= AdvancedExtensionsInternal.getCheckedString(element, AdvancedExtensionsInternal.CONFIG_CATEGORY_ID_ATTRIBUTE_NAME);
					final Set<String> partitions= new HashSet<>();
					final IConfigurationElement[] partitionConfigs= element.getChildren(AdvancedExtensionsInternal.CONFIG_PARTITION_ELEMENT_NAME);
					for (final IConfigurationElement partitionConfig : partitionConfigs) {
						partitions.add(AdvancedExtensionsInternal.getCheckedString(partitionConfig, AdvancedExtensionsInternal.CONFIG_CONTENTTYPE_ID_ELEMENT_NAME).intern());
					}
					checkPartitions(partitions);
					
					final ComputerDescriptor comp= new ComputerDescriptor(id, partitions, element);
					
					List<ComputerDescriptor> list= computersByCategoryId.get(categoryId);
					if (list == null) {
						list= new ArrayList<>(4);
						computersByCategoryId.put(categoryId, list);
					}
					list.add(comp);
				}
				catch (final CoreException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
							NLS.bind("Loading Completion Proposal Computer ''{0}'' failed (contributed by= ''{1}'')", //$NON-NLS-1$
									(id != null) ? id : "", element.getDeclaringExtension().getContributor().getName() ), //$NON-NLS-1$
							e ));
				}
			}
		}
		
		final ArrayList<ContentAssistCategory> categories= new ArrayList<>(8);
		for (final IConfigurationElement catConfig : categoryConfigs) {
			// Create category descriptor
			String id= null;
			try {
				id= AdvancedExtensionsInternal.getCheckedString(catConfig, AdvancedExtensionsInternal.CONFIG_ID_ATTRIBUTE_NAME);
				final List<ComputerDescriptor> descriptors= computersByCategoryId.get(id);
				if (descriptors != null) {
					final ImageDescriptor icon= AdvancedExtensionsInternal.getImageDescriptor(catConfig, AdvancedExtensionsInternal.CONFIG_ICON_ATTRIBUTE_NAME);
					final String name= AdvancedExtensionsInternal.getCheckedString(catConfig, AdvancedExtensionsInternal.CONFIG_NAME_ATTRIBUTE_NAME);
					final ContentAssistCategory cat= new ContentAssistCategory(id, name, icon, descriptors);
					categories.add(cat);
				}
			}
			catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
						NLS.bind("Loading Completion Proposal Category ''{0}'' failed (contributed by= ''{1}'')", //$NON-NLS-1$
								(id != null) ? id : "", catConfig.getDeclaringExtension().getContributor().getName() ), //$NON-NLS-1$
						e ));
			}
		}
		return (this.categories= Collections.unmodifiableList(applyPreferences(
				PreferencesUtil.getInstancePrefs(), categories)));
	}
	
	List<ContentAssistCategory> applyPreferences(final IPreferenceAccess prefAccess, final List<ContentAssistCategory> categories) {
		final Set<String> disabledIds= prefAccess.getPreferenceValue(getPrefDefaultDisabledCategoryIds());
		for (final ContentAssistCategory category : categories) {
			final boolean enabled= disabledIds == null || !disabledIds.contains(category.getId());
			category.isIncludedInDefault= enabled;
		}
		
		final Map<String, ContentAssistCategory> map= createCategoryByIdMap(categories);
		final String[] orderPref= prefAccess.getPreferenceValue(getPrefCirclingOrderedCategoryIds());
		final List<ContentAssistCategory> ordered= new ArrayList<>(categories.size());
		for (final String value : orderPref) {
			final String id;
			final boolean enabled;
			final int idx= value.lastIndexOf(':');
			if (idx > 0) {
				id= value.substring(0, idx);
				enabled= Boolean.parseBoolean(value.substring(idx+1));
			}
			else { // fallback
				id= value;
				enabled= false;
			}
			final ContentAssistCategory category= map.remove(id);
			if (category != null) {
				ordered.add(category);
				category.isEnabledAsSeparate= enabled;
			}
		}
		for (final ContentAssistCategory category : map.values()) {
			ordered.add(category);
			category.isEnabledAsSeparate= false;
		}
		
		return ordered;
	}
	
	public synchronized List<ContentAssistCategory> getCopyOfCategories() {
		List<ContentAssistCategory> categories= this.categories;
		if (categories == null) {
			categories= loadExtensions();
		}
		final List<ContentAssistCategory> copies= new ArrayList<>(categories.size());
		for (final ContentAssistCategory category : categories) {
			copies.add(new ContentAssistCategory(category));
		}
		return copies;
	}
	
	Map<Preference<?>, Object> createPreferences(final List<ContentAssistCategory> orderedCategories) {
		final Set<String> disabledIds= new HashSet<>();
		final String[] orderedPref= new String[orderedCategories.size()];
		
		for (int i= 0; i < orderedCategories.size(); i++) {
			final ContentAssistCategory category= orderedCategories.get(i);
			if (!category.isIncludedInDefault) {
				disabledIds.add(category.getId());
			}
			orderedPref[i]= category.getId() +
					(category.isEnabledAsSeparate ? ":true" : ":false"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		final Map<Preference<?>, Object> prefMap= new HashMap<>();
		prefMap.put(getPrefDefaultDisabledCategoryIds(), disabledIds);
		prefMap.put(getPrefCirclingOrderedCategoryIds(), orderedPref);
		return prefMap;
	}
	
	
	protected void checkPartitions(final Set<String> partitions) {
	}
	
	
	public synchronized List<ContentAssistCategory> getCategories() {
		List<ContentAssistCategory> categories= this.categories;
		if (categories == null) {
			categories= loadExtensions();
		}
		if (this.specificModeId != null) {
			for (final ContentAssistCategory category : categories) {
				if (category.getId().equals(this.specificModeId)) {
					final ContentAssistCategory copy= new ContentAssistCategory(category);
					copy.isIncludedInDefault= true;
					return Collections.singletonList(copy);
				}
			}
			return Collections.emptyList();
		}
		return categories;
	}
	
	public synchronized void startSpecificMode(final String categoryId) {
		this.specificModeId= categoryId;
	}
	
	public synchronized void stopSpecificMode() {
		this.specificModeId= null;
	}
	
	public synchronized boolean isInSpecificMode() {
		return (this.specificModeId != null);
	}
	
}
