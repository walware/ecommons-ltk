/*******************************************************************************
 * Copyright (c) 2010-2011 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ICommonStatusConstants;
import de.walware.ecommons.IDisposable;
import de.walware.ecommons.preferences.IPreferenceAccess;
import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.preferences.Preference.StringArrayPref;
import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.SettingsChangeNotifier;
import de.walware.ecommons.preferences.SettingsChangeNotifier.ManageListener;
import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.internal.ui.AdvancedExtensionsInternal;


/**
 * Registry for information hover types of a content type.
 */
public class InfoHoverRegistry implements ManageListener, IDisposable {
	
	
	public static final String TYPE_SETTINGS = "hover.type_settings.ids:setting"; //$NON-NLS-1$
	
	
	public static final class EffectiveHovers {
		
		private final int[] fStateMasks;
		private final List<InfoHoverDescriptor> fStateMaskDescriptors;
		private final List<InfoHoverDescriptor> fCombinedDescriptors;
		
		
		public EffectiveHovers(final int[] stateMasks, final List<InfoHoverDescriptor> effectiveDescriptors,
				final List<InfoHoverDescriptor> combinedDescriptors) {
			fStateMasks = stateMasks;
			fStateMaskDescriptors = effectiveDescriptors;
			fCombinedDescriptors = combinedDescriptors;
		}
		
		
		public int[] getStateMasks() {
			return fStateMasks;
		}
		
		public InfoHoverDescriptor getDescriptor(final int stateMask) {
			for (final InfoHoverDescriptor descriptor : fStateMaskDescriptors) {
				if (descriptor.getStateMask() == stateMask) {
					return descriptor;
				}
			}
			return null;
		}
		
		public List<InfoHoverDescriptor> getDescriptorsForCombined() {
			return fCombinedDescriptors;
		}
		
	}
	
	
	private final String fContentTypeId;
	
	private final String fSettingsGroupId;
	private final StringArrayPref fPrefTypeSettings;
	
	private List<InfoHoverDescriptor> fDescriptors;
	private EffectiveHovers fEffectiveHovers;
	
	
	public InfoHoverRegistry(final String contentTypeId, final String prefQualifier,
			final String settingsGroupId) {
		fContentTypeId = contentTypeId;
		fSettingsGroupId = settingsGroupId;
		
		fPrefTypeSettings = new StringArrayPref(prefQualifier, TYPE_SETTINGS);
		PreferencesUtil.getSettingsChangeNotifier().addManageListener(this);
	}
	
	
	@Override
	public void dispose() {
		final SettingsChangeNotifier notifier = PreferencesUtil.getSettingsChangeNotifier();
		if (notifier != null) {
			notifier.removeManageListener(this);
		}
	}
	
	
	@Override
	public void beforeSettingsChangeNotification(final Set<String> groupIds) {
		if (fSettingsGroupId != null && groupIds.contains(fSettingsGroupId)) {
			synchronized (this) {
				fDescriptors = applyPreferences(PreferencesUtil.getInstancePrefs(),
						new ArrayList<InfoHoverDescriptor>(fDescriptors));
				fEffectiveHovers = null;
			}
		}
	}
	
	@Override
	public void afterSettingsChangeNotification(final Set<String> groupIds) {
	}
	
	String getSettingsGroupId() {
		return fSettingsGroupId;
	}
	
	StringArrayPref getPrefSeparateSettings() {
		return fPrefTypeSettings;
	}
	
	List<InfoHoverDescriptor> loadCurrent() {
		final IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		final IConfigurationElement[] elements = extensionRegistry.getConfigurationElementsFor(
				AdvancedExtensionsInternal.INFOHOVER_EXTENSIONPOINT_ID);
		final List<InfoHoverDescriptor> descriptors = new ArrayList<InfoHoverDescriptor>();
		
		for (final IConfigurationElement element : elements) {
			String id = null;
			try {
				final String contentTypeId = AdvancedExtensionsInternal.getCheckedString(element,
						AdvancedExtensionsInternal.CONFIG_CONTENT_TYPE_ID_ATTRIBUTE_NAME);
				if (!fContentTypeId.equals(contentTypeId)) {
					continue;
				}
				id = AdvancedExtensionsInternal.getCheckedString(element,
						AdvancedExtensionsInternal.CONFIG_ID_ATTRIBUTE_NAME).intern();
				final String name = AdvancedExtensionsInternal.getCheckedString(element,
						AdvancedExtensionsInternal.CONFIG_NAME_ATTRIBUTE_NAME);
				
				final InfoHoverDescriptor descriptor = new InfoHoverDescriptor(id,
						name, element);
				descriptors.add(descriptor);
			}
			catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
						NLS.bind("Loading Text Hover failed (id = ''{0}'', contributed by = ''{1}'')",
								(id != null) ? id : "", element.getDeclaringExtension().getContributor().getName()), e));
			}
		}
		
		return applyPreferences(PreferencesUtil.getInstancePrefs(), descriptors);
	}
	
	List<InfoHoverDescriptor> applyPreferences(final IPreferenceAccess prefAccess, final List<InfoHoverDescriptor> descriptors) {
		final String[] settings = prefAccess.getPreferenceValue(getPrefSeparateSettings());
		
		final List<InfoHoverDescriptor> sortedDescriptors = new ArrayList<InfoHoverDescriptor>(descriptors.size());
		for (final String setting : settings) {
			final int idx1 = setting.indexOf(':');
			if (idx1 >= 0) {
				final int idx2 = setting.indexOf(';', idx1+1);
				if (idx2 >= 0) {
					final String id = setting.substring(0, idx1);
					for (final InfoHoverDescriptor descriptor : descriptors) {
						if (descriptor.getId().equals(id)) {
							descriptors.remove(descriptor);
							descriptor.fIsEnabled = Boolean.parseBoolean(setting.substring(idx1+1, idx2));
							descriptor.fStateMask = AdvancedExtensionsInternal.computeStateMask(setting.substring(idx2+1));
							sortedDescriptors.add(descriptor);
							break;
						}
					}
				}
			}
		}
		for (final InfoHoverDescriptor descriptor : descriptors) {
			descriptor.fIsEnabled = false;
			descriptor.fStateMask = 0;
			sortedDescriptors.add(descriptor);
		}
		return sortedDescriptors;
	}
	
	Map<Preference, Object> toPreferencesMap(final List<InfoHoverDescriptor> descriptors) {
		final Map<Preference, Object> map = new HashMap<Preference, Object>();
		final String[] settings = new String[descriptors.size()];
		for (int i = 0; i < settings.length; i++) {
			final InfoHoverDescriptor descriptor = descriptors.get(i);
			settings[i] = descriptor.getId() + ':' + descriptor.isEnabled() + ';' + 
					AdvancedExtensionsInternal.createUnifiedStateMaskString(descriptor.getStateMask());
		}
		map.put(fPrefTypeSettings, settings);
		return map;
	}
	
	
	public String getContentTypeId() {
		return fContentTypeId;
	}
	
	public synchronized InfoHoverDescriptor getHoverDescriptor(final int stateMask) {
		List<InfoHoverDescriptor> descriptors = fDescriptors;
		if (descriptors == null) {
			fDescriptors = descriptors = loadCurrent();
		}
		for (final InfoHoverDescriptor descriptor : descriptors) {
			if (descriptor.isEnabled() && descriptor.getStateMask() == stateMask) {
				return descriptor;
			}
		}
		return null;
	}
	
	public synchronized EffectiveHovers getEffectiveHoverDescriptors() {
		if (fEffectiveHovers == null) {
			updateEffectiveHovers();
		}
		return fEffectiveHovers;
	}
	
	private void updateEffectiveHovers() {
		List<InfoHoverDescriptor> descriptors = fDescriptors;
		if (descriptors == null) {
			fDescriptors = descriptors = loadCurrent();
		}
		int[] stateMasks = new int[descriptors.size()];
		final List<InfoHoverDescriptor> effectiveDescriptors = new ArrayList<InfoHoverDescriptor>(descriptors.size());
		final List<InfoHoverDescriptor> combinedDescriptors = new ArrayList<InfoHoverDescriptor>(descriptors.size());
		for (final InfoHoverDescriptor descriptor : descriptors) {
			if (!descriptor.getId().endsWith("CombinedHover")) {
				combinedDescriptors.add(descriptor);
			}
			if (descriptor.isEnabled()) {
				final int stateMask = descriptor.getStateMask();
				if (stateMask == -1) {
					continue;
				}
				for (int i = 0; i < effectiveDescriptors.size(); i++) {
					if (stateMasks[i] == stateMask) {
						continue;
					}
				}
				stateMasks[effectiveDescriptors.size()] = stateMask;
				effectiveDescriptors.add(descriptor);
			}
		}
		if (stateMasks.length != effectiveDescriptors.size()) {
			final int[] fittedMasks = new int[effectiveDescriptors.size()];
			System.arraycopy(stateMasks, 0, fittedMasks, 0, effectiveDescriptors.size());
			stateMasks = fittedMasks;
		}
		fEffectiveHovers = new EffectiveHovers(stateMasks, effectiveDescriptors, combinedDescriptors);
	}
	
}
