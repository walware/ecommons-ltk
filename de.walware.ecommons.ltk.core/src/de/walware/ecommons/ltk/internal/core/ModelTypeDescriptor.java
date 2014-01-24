/*=============================================================================#
 # Copyright (c) 2013-2014 Stephan Wahlbrink (WalWare.de) and others.
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
import java.util.Collection;
import java.util.List;

import de.walware.ecommons.ltk.core.IModelTypeDescriptor;


public class ModelTypeDescriptor implements IModelTypeDescriptor {
	
	
	private final String id;
	
	List<String> secondaryTypeIds = new ArrayList<String>(1);
	List<String> checkedSecondaryTypeIds;
	
	
	public ModelTypeDescriptor(final String id) {
		this.id = id;
	}
	
	
	@Override
	public String getId() {
		return this.id;
	}
	
	@Override
	public Collection<String> getSecondaryTypeIds() {
		return this.checkedSecondaryTypeIds;
	}
	
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		return (obj instanceof ModelTypeDescriptor
				&& this.id == ((ModelTypeDescriptor) obj).id);
	}
	
	
	@Override
	public String toString() {
		return this.id;
	}
	
}
