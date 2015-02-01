/*=============================================================================#
 # Copyright (c) 2014-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.tasklist;


public class TaskTag {
	
	
	private final String keyword;
	
	private final TaskPriority priority;
	
	
	public TaskTag(final String tag, final TaskPriority priority) {
		this.keyword= tag;
		this.priority= priority;
	}
	
	
	public String getKeyword() {
		return this.keyword;
	}
	
	public TaskPriority getPriority() {
		return this.priority;
	}
	
	
	@Override
	public int hashCode() {
		return this.keyword.hashCode();
	}
	
	@Override
	public String toString() {
		return this.keyword;
	}
	
}
