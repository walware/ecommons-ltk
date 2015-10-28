/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.model;

import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.core.ISourceModelStamp;


/**
 * Container for model information of an source unit
 */
public interface ISourceUnitModelInfo {
	
	
	/**
	 * Returns the stamp of the model.
	 * 
	 * @return the stamp
	 */
	ISourceModelStamp getStamp();
	
	/**
	 * The AST used to create this info.
	 * 
	 * @return the AST information
	 */
	AstInfo getAst();
	
	/**
	 * The source unit as source structure model element.
	 * 
	 * @return the element
	 */
	ISourceStructElement getSourceElement();
	
	void addAttachment(Object data);
	void removeAttachment(Object data);
	ImList<Object> getAttachments();
	
}
