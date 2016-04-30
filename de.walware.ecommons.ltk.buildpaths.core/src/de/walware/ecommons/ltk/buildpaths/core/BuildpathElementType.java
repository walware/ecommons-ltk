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

import de.walware.jcommons.collections.ImList;


public class BuildpathElementType {
	
	
	private final String modelTypeId;
	
	private final String name;
	
	private final ImList<String> attributeBuiltinKeys;
	
	
	public BuildpathElementType(final String modelTypeId,
			final String name, final ImList<String> attributeBuiltinKeys) {
		this.modelTypeId= modelTypeId;
		this.name= name;
		this.attributeBuiltinKeys= attributeBuiltinKeys;
	}
	
	
	public String getModelTypeId() {
		return this.modelTypeId;
	}
	
	public String getName() {
		return this.name;
	}
	
	public ImList<String> getAttributeBuiltinKeys() {
		return this.attributeBuiltinKeys;
	}
	
	public boolean isAttributeBuiltin(final String key) {
		return this.attributeBuiltinKeys.contains(key);
	}
	
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof BuildpathElementType) {
			final BuildpathElementType other= (BuildpathElementType) obj;
			return (this.name == other.name);
		}
		return false;
	}
	
}
