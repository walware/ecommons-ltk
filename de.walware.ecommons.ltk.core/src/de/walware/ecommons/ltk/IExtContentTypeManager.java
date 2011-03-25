/*******************************************************************************
 * Copyright (c) 2007-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;


/**
 * Allows multiple content types in addition to the primary content type.
 * <p>
 * E.g.: Sweave documents (LaTeX/R) inherit primary of LaTeX and have the secondary type of R.</p>
 */
public interface IExtContentTypeManager {
	
	
	public String[] getSecondaryContentTypes(String primaryContentType);
	public String[] getPrimaryContentTypes(String secondaryContentType);
	
	public boolean matchesActivatedContentType(String primaryContentTypeId, String secondaryContentTypeId, boolean self);
	
	public String getContentTypeForModelType(String modelTypeId);
	public String getModelTypeForContentType(String contentTypeId);
	
}
