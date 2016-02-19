/*=============================================================================#
 # Copyright (c) 2007-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk;

import de.walware.ecommons.ltk.core.IModelTypeDescriptor;


/**
 * Allows multiple content types in addition to the primary content type.
 * <p>
 * E.g.: Sweave documents (LaTeX/R) inherit primary of LaTeX and have the secondary type of R.</p>
 */
public interface IExtContentTypeManager {
	
	
	String[] getSecondaryContentTypes(String primaryContentType);
	String[] getPrimaryContentTypes(String secondaryContentType);
	
	boolean matchesActivatedContentType(String primaryContentTypeId, String secondaryContentTypeId, boolean self);
	
	
	IModelTypeDescriptor getModelType(String modelTypeId);
	
	IModelTypeDescriptor getModelTypeForContentType(String contentTypeId);
	
}
