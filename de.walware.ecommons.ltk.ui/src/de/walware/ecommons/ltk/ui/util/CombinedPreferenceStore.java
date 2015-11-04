/*=============================================================================#
 # Copyright (c) 2006-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.preferences.core.IPreferenceAccess;
import de.walware.ecommons.preferences.ui.ScopedPreferenceStore;


/**
 * Util to create combination of preference stores.
 */
public class CombinedPreferenceStore {
	
	public static IPreferenceStore createStore(
			final IPreferenceStore[] preferenceStores, final IPreferenceAccess corePrefs, final String[] coreQualifier) {
		
		ImList<IScopeContext> contexts= corePrefs.getPreferenceContexts();
		// default scope must not be included (will be automatically added)
		if (contexts.size() > 0 && contexts.get(contexts.size() - 1) instanceof DefaultScope) {
			contexts= contexts.subList(0, contexts.size() - 1);
		}
		final IScopeContext mainScope = (!contexts.isEmpty()) ? contexts.get(0) : InstanceScope.INSTANCE;
		
		if (preferenceStores.length == 0 && contexts.size() <= 1 && coreQualifier.length == 1) {
			return new ScopedPreferenceStore(mainScope, coreQualifier[0]);
		}
		
		final List<IPreferenceStore> stores = new ArrayList<>();
		for (final String qualifier : coreQualifier) {
			final ScopedPreferenceStore store = new ScopedPreferenceStore(mainScope, qualifier);
			store.setSearchContexts(contexts);
			stores.add(store);
		}
		stores.addAll(Arrays.asList(preferenceStores));
		return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
	}
	
	public static IPreferenceStore createStore(final IPreferenceAccess corePrefs, final String coreQualifier) {
		return createStore(new IPreferenceStore[0], corePrefs, new String[] { coreQualifier });
	}
	
	public static IPreferenceStore createStore(final IPreferenceStore... preferenceStores) {
		return new ChainedPreferenceStore(preferenceStores);
	}
	
	
	private CombinedPreferenceStore() {}
	
}
