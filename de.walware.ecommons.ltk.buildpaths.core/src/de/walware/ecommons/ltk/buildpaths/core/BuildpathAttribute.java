/*=============================================================================#
 # Copyright (c) 2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.core;

import java.util.Objects;


public class BuildpathAttribute implements IBuildpathAttribute {
	
	
	private final String name;
	
	private final String value;
	
	
	public BuildpathAttribute(final String name, final String value) {
		this.name= name;
		this.value= value;
	}
	
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public String getValue() {
		return this.value;
	}
	
	
	@Override
	public int hashCode() {
		return this.name.hashCode() * 17 + Objects.hashCode(this.value.hashCode());
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof BuildpathAttribute) {
			final BuildpathAttribute other= (BuildpathAttribute) obj;
			return (this.name.equals(other.name)
					&& Objects.equals(this.value, other.value) );
		}
		return false;
	}
	
	@Override
	public String toString() {
		return this.name + "=" + this.value; //$NON-NLS-1$
	}
	
}
