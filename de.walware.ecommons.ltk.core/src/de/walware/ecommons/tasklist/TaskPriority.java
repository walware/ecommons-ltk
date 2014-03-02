/*=============================================================================#
 # Copyright (c) 2006-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.tasklist;

import org.eclipse.core.resources.IMarker;


public enum TaskPriority {
	
	
	HIGH (IMarker.PRIORITY_HIGH), 
	NORMAL (IMarker.PRIORITY_NORMAL), 
	LOW (IMarker.PRIORITY_LOW);
	
	
	private int priority;
	
	
	TaskPriority(final int priority) {
		this.priority= priority;
	}
	
	
	public int getMarkerPriority() {
		return this.priority;
	}
	
}
