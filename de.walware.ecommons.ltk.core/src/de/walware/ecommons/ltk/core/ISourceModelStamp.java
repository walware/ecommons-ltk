/*=============================================================================#
 # Copyright (c) 2015-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core;

import org.eclipse.jface.text.IDocumentExtension4;


/**
 * The version of a source model like an AST.
 * 
 * Beside the {@link #getSourceStamp() source content stamp} it can e.g. consider the language
 * configuration used when parsing the source.
 */
public interface ISourceModelStamp {
	
	
	/**
	 * Returns the source content stamp.
	 * 
	 * @see SourceContent#getStamp()
	 * @see IDocumentExtension4#getModificationStamp()
	 * 
	 * @return the stamp
	 */
	public long getSourceStamp();
	
}
