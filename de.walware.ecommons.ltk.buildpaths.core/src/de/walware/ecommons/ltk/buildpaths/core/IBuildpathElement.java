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

import org.eclipse.core.runtime.IPath;

import de.walware.jcommons.collections.ImList;

public interface IBuildpathElement {
	
	
	String SOURCE= "Source"; //$NON-NLS-1$
	String PROJECT= "Project"; //$NON-NLS-1$
	String LIBRARY= "Library"; //$NON-NLS-1$
	String VARIABLE= "Variable"; //$NON-NLS-1$
	
	
	BuildpathElementType getType();
	String getTypeName();
	
	IPath getPath();
	
	ImList<IPath> getInclusionPatterns();
	ImList<IPath> getExclusionPatterns();
	
	Object getSourceAttachmentPath();
	
	IPath getOutputPath();
	
	boolean isExported();
	
	ImList<IBuildpathAttribute> getExtraAttributes();
	
	
}
