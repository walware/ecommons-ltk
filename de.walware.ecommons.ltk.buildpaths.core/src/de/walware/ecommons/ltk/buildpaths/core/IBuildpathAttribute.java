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


public interface IBuildpathAttribute {
	
	
	String SOURCE_ATTACHMENT= "Source.path"; //$NON-NLS-1$
	
	String FILTER_INCLUSIONS= "Filter.inclusions"; //$NON-NLS-1$
	String FILTER_EXCLUSIONS= "Filter.exclusions"; //$NON-NLS-1$
	
	String OUTPUT= "Output.path"; //$NON-NLS-1$
	
	String EXPORTED= "Exported"; //$NON-NLS-1$
	
	String OPTIONAL= "Optional"; //$NON-NLS-1$
	
	
	String getName();
	
	String getValue();
	
}
