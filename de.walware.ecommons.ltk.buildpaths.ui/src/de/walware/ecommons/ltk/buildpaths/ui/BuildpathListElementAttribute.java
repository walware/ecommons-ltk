/*=============================================================================#
 # Copyright (c) 2010-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.ui;

import org.eclipse.core.runtime.IStatus;

import de.walware.ecommons.ltk.buildpaths.core.BuildpathAttribute;
import de.walware.ecommons.ltk.buildpaths.core.IBuildpathAttribute;


public class BuildpathListElementAttribute {
	
	
	private final BuildpathListElement element;
	
	private final String name;
	private Object value;
	
	private final boolean builtin;
	private final IStatus status;
	
	
	public BuildpathListElementAttribute(final BuildpathListElement parent,
			final String name, final Object value, final boolean builtin) {
		this.name= name;
		this.value= value;
		this.element= parent;
		
		this.builtin= builtin;
		this.status= parent.getContainerChildStatus(this);
	}
	
	
	public BuildpathListElement getParent() {
		return this.element;
	}
	
	
	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the value.
	 * @return Object
	 */
	public Object getValue() {
		return this.value;
	}
	
	/**
	 * Sets the value.
	 * @param value value to set
	 */
	public void setValue(final Object value) {
		this.value= value;
		getParent().attributeChanged(this.name);
	}
	
	public boolean isBuiltin() {
		return this.builtin;
	}
	
	public IStatus getStatus() {
		return this.status;
	}
	
	public IBuildpathAttribute getCoreAttribute() {
		return new BuildpathAttribute(this.name, (this.value != null) ? this.value.toString() : null);
	}
	
	
	@Override
	public int hashCode() {
		return this.element.getPath().hashCode() + this.name.hashCode() * 89;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof BuildpathListElementAttribute) {
			final BuildpathListElementAttribute other= (BuildpathListElementAttribute) obj;
			return (this.element.getPath().equals(other.element.getPath())
					&& this.name == other.name );
		}
		return false;
	}
	
}
