/*=============================================================================#
 # Copyright (c) 2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.core.sections;

import org.eclipse.jface.text.IDocument;

import de.walware.jcommons.collections.ImList;


public interface IDocContentSections {
	
	
	String ERROR= ""; //$NON-NLS-1$
	
	
	String getPartitioning();
	
	String getPrimaryType();
	
	ImList<String> getSecondaryTypes();
	
	String getType(IDocument document, int offset);
	
	String getTypeByPartition(String contentType);
	
}
