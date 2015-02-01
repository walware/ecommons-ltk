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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ICommonStatusConstants;

import de.walware.ecommons.ltk.internal.ui.AdvancedExtensionsInternal;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


/**
 * Describes an configured information hover type.
 */
public class InfoHoverDescriptor {
	
	
	private final String id;
	
	private final String name;
	
	int stateMask;
	
	boolean isEnabled;
	
	private final IConfigurationElement configurationElement;
	
	
	/**
	 * Creates a new editor text hover descriptor from the given configuration element.
	 * @param name 
	 *
	 * @param element the configuration element
	 */
	InfoHoverDescriptor(final String id, final String name,
			final IConfigurationElement configurationElement) {
		this.id= id;
		this.name= name;
		this.configurationElement= configurationElement;
	}
	
	
	public String getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	
	/**
	 * Creates the editor text hover.
	 *
	 * @return the text hover
	 */
	public IInfoHover createHover() {
		try {
			return (IInfoHover) this.configurationElement.createExecutableExtension(
					AdvancedExtensionsInternal.CONFIG_CLASS_ATTRIBUTE_NAME);
		}
		catch (final CoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
					"Could not create text hover '"+this.name+"'.", e));
		}
		return null;
	}
	
	/**
	 * Returns the configured modifier getStateMask for this hover.
	 *
	 * @return the hover modifier stateMask or -1 if no hover is configured
	 */
	public int getStateMask() {
		return this.stateMask;
	}
	
	/**
	 * Returns whether this hover is enabled or not.
	 *
	 * @return <code>true</code> if enabled
	 */
	public boolean isEnabled() {
		return this.isEnabled;
	}
	
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		return (obj != null && getClass().equals(obj.getClass())
				&& this.id.equals(((InfoHoverDescriptor) obj).getId()) );
	}
	
}
