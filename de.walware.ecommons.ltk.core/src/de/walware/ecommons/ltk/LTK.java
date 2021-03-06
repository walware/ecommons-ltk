/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk;

import de.walware.ecommons.ltk.internal.core.LTKCorePlugin;


public class LTK {
	
	
	public static final String PLUGIN_ID = "de.walware.ecommons.ltk.core"; //$NON-NLS-1$
	
	
	public static final WorkingContext PERSISTENCE_CONTEXT = new WorkingContext("persistence.default"); //$NON-NLS-1$
	
	public static final WorkingContext EDITOR_CONTEXT = new WorkingContext("editor.default"); //$NON-NLS-1$
	
	
	public static ISourceUnitManager getSourceUnitManager() {
		return LTKCorePlugin.getSafe().getSourceUnitManager();
	}
	
	public static IExtContentTypeManager getExtContentTypeManager() {
		return LTKCorePlugin.getSafe().getContentTypeServices();
	}
	
	public static Object getModelAdapter(final String type, final Class<?> required) {
		return LTKCorePlugin.getSafe().getModelAdapterFactory().get(type, required);
	}
	
}
