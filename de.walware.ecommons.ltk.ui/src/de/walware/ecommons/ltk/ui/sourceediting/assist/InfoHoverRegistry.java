/*=============================================================================#
 # Copyright (c) 2010-2015 Stephan Wahlbrink (WalWare.de) and others.
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
	
	
	public static final String TYPE_SETTINGS= "hover.type_settings.ids:setting"; //$NON-NLS-1$
	
	
	public static final class EffectiveHovers {
		
		private final int[] stateMasks;
		private final List<InfoHoverDescriptor> stateMaskDescriptors;
		private final List<InfoHoverDescriptor> combinedDescriptors;
		
		
		public EffectiveHovers(final int[] stateMasks, final List<InfoHoverDescriptor> effectiveDescriptors,
				final List<InfoHoverDescriptor> combinedDescriptors) {
			this.stateMasks= stateMasks;
			this.stateMaskDescriptors= effectiveDescriptors;
			this.combinedDescriptors= combinedDescriptors;
		}
		
		
		public int[] getStateMasks() {
			return this.stateMasks;
		}
		
		public InfoHoverDescriptor getDescriptor(final int stateMask) {
			for (final InfoHoverDescriptor descriptor : this.stateMaskDescriptors) {
				if (descriptor.getStateMask() == stateMask) {
					return descriptor;
				}
			}
			return null;
		}
		
		public List<InfoHoverDescriptor> getDescriptorsForCombined() {
			return this.combinedDescriptors;
		}
		
	}
	
	
	private final String contentTypeId;
	
	private final String settingsGroupId;
	private final StringArrayPref prefTypeSettings;
	
	private List<InfoHoverDescriptor> descriptors;
	private EffectiveHovers effectiveHovers;
	
	
	public InfoHoverRegistry(final String contentTypeId, final String prefQualifier,
			final String settingsGroupId) {
		this.contentTypeId= contentTypeId;
		this.settingsGroupId= settingsGroupId;
		
		this.prefTypeSettings= new StringArrayPref(prefQualifier, TYPE_SETTINGS);
		PreferencesUtil.getSettingsChangeNotifier().addManageListener(this);
	}
	
	
	@Override
	public void dispose() {
		final SettingsChangeNotifier notifier= PreferencesUtil.getSettingsChangeNotifier();
		if (notifier != null) {
			notifier.removeManageListener(this);
		}
	}
	
	
	@Override
	public void beforeSettingsChangeNotification(final Set<String> groupIds) {
		if (this.settingsGroupId != null && groupIds.contains(this.settingsGroupId)) {
			synchronized (this) {
				this.descriptors= applyPreferences(PreferencesUtil.getInstancePrefs(),
						new ArrayList<>(this.descriptors));
				this.effectiveHovers= null;
			}
		}
	}
	
	@Override
	public void afterSettingsChangeNotification(final Set<String> groupIds) {
	}
	
	String getSettingsGroupId() {
		return this.settingsGroupId;
	}
	
	StringArrayPref getPrefSeparateSettings() {
		return this.prefTypeSettings;
	}
	
	List<InfoHoverDescriptor> loadCurrent() {
		final IExtensionRegistry extensionRegistry= Platform.getExtensionRegistry();
		final IConfigurationElement[] elements= extensionRegistry.getConfigurationElementsFor(
				AdvancedExtensionsInternal.INFOHOVER_EXTENSIONPOINT_ID);
		final List<InfoHoverDescriptor> descriptors= new ArrayList<>();
		
		for (final IConfigurationElement element : elements) {
			String id= null;
			try {
				final String contentTypeId= AdvancedExtensionsInternal.getCheckedString(element,
						AdvancedExtensionsInternal.CONFIG_CONTENT_TYPE_ID_ATTRIBUTE_NAME);
				if (!this.contentTypeId.equals(contentTypeId)) {
					continue;
				}
				id= AdvancedExtensionsInternal.getCheckedString(element,
						AdvancedExtensionsInternal.CONFIG_ID_ATTRIBUTE_NAME).intern();
				final String name= AdvancedExtensionsInternal.getCheckedString(element,
						AdvancedExtensionsInternal.CONFIG_NAME_ATTRIBUTE_NAME);
				
				final InfoHoverDescriptor descriptor= new InfoHoverDescriptor(id,
						name, element);
				descriptors.add(descriptor);
			}
			catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
						NLS.bind("Loading Text Hover failed (id= ''{0}'', contributed by= ''{1}'')", //$NON-NLS-1$
								(id != null) ? id : "", element.getDeclaringExtension().getContributor().getName()), e)); //$NON-NLS-1$
			}
		}
		
		return applyPreferences(PreferencesUtil.getInstancePrefs(), descriptors);
	}
	
	List<InfoHoverDescriptor> applyPreferences(final IPreferenceAccess prefAccess, final List<InfoHoverDescriptor> descriptors) {
		final String[] settings= prefAccess.getPreferenceValue(getPrefSeparateSettings());
		
		final List<InfoHoverDescriptor> sortedDescriptors= new ArrayList<>(descriptors.size());
		for (final String setting : settings) {
			final int idx1= setting.indexOf(':');
			if (idx1 >= 0) {
				final int idx2= setting.indexOf(';', idx1+1);
				if (idx2 >= 0) {
					final String id= setting.substring(0, idx1);
					for (final InfoHoverDescriptor descriptor : descriptors) {
						if (descriptor.getId().equals(id)) {
							descriptors.remove(descriptor);
							descriptor.isEnabled= Boolean.parseBoolean(setting.substring(idx1+1, idx2));
							descriptor.stateMask= AdvancedExtensionsInternal.computeStateMask(setting.substring(idx2+1));
							sortedDescriptors.add(descriptor);
							break;
						}
					}
				}
			}
		}
		for (final InfoHoverDescriptor descriptor : descriptors) {
			descriptor.isEnabled= false;
			descriptor.stateMask= 0;
			sortedDescriptors.add(descriptor);
		}
		return sortedDescriptors;
	}
	
	Map<Preference<?>, Object> toPreferencesMap(final List<InfoHoverDescriptor> descriptors) {
		final Map<Preference<?>, Object> map= new HashMap<>();
		final String[] settings= new String[descriptors.size()];
		for (int i= 0; i < settings.length; i++) {
			final InfoHoverDescriptor descriptor= descriptors.get(i);
			settings[i]= descriptor.getId() + ':' + descriptor.isEnabled() + ';' + 
					AdvancedExtensionsInternal.createUnifiedStateMaskString(descriptor.getStateMask());
		}
		map.put(this.prefTypeSettings, settings);
		return map;
	}
	
	
	public String getContentTypeId() {
		return this.contentTypeId;
	}
	
	public synchronized InfoHoverDescriptor getHoverDescriptor(final int stateMask) {
		List<InfoHoverDescriptor> descriptors= this.descriptors;
		if (descriptors == null) {
			this.descriptors= descriptors= loadCurrent();
		}
		for (final InfoHoverDescriptor descriptor : descriptors) {
			if (descriptor.isEnabled() && descriptor.getStateMask() == stateMask) {
				return descriptor;
			}
		}
		return null;
	}
	
	public synchronized EffectiveHovers getEffectiveHoverDescriptors() {
		if (this.effectiveHovers == null) {
			updateEffectiveHovers();
		}
		return this.effectiveHovers;
	}
	
	private void updateEffectiveHovers() {
		List<InfoHoverDescriptor> descriptors= this.descriptors;
		if (descriptors == null) {
			this.descriptors= descriptors= loadCurrent();
		}
		int[] stateMasks= new int[descriptors.size()];
		final List<InfoHoverDescriptor> effectiveDescriptors= new ArrayList<>(descriptors.size());
		final List<InfoHoverDescriptor> combinedDescriptors= new ArrayList<>(descriptors.size());
		for (final InfoHoverDescriptor descriptor : descriptors) {
			if (!descriptor.getId().endsWith("CombinedHover")) { //$NON-NLS-1$
				combinedDescriptors.add(descriptor);
			}
			if (descriptor.isEnabled()) {
				final int stateMask= descriptor.getStateMask();
				if (stateMask == -1) {
					continue;
				}
				for (int i= 0; i < effectiveDescriptors.size(); i++) {
					if (stateMasks[i] == stateMask) {
						continue;
					}
				}
				stateMasks[effectiveDescriptors.size()]= stateMask;
				effectiveDescriptors.add(descriptor);
			}
		}
		if (stateMasks.length != effectiveDescriptors.size()) {
			final int[] fittedMasks= new int[effectiveDescriptors.size()];
			System.arraycopy(stateMasks, 0, fittedMasks, 0, effectiveDescriptors.size());
			stateMasks= fittedMasks;
		}
		this.effectiveHovers= new EffectiveHovers(stateMasks, effectiveDescriptors, combinedDescriptors);
	}
	
}
