/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.core.sections;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;


public abstract class DocContentSections {
	
	
	public static final String ERROR= ""; //$NON-NLS-1$
	
	
	private final String partitioning;
	
	private final String primaryType;
	
	
	protected DocContentSections(final String partitioning, final String primaryType) {
		this.partitioning= partitioning;
		this.primaryType= primaryType;
	}
	
	
	public final String getPartitioning() {
		return this.partitioning;
	}
	
	public String getPrimaryType() {
		return this.primaryType;
	}
	
	
	public String getType(final IDocument document, final int offset) {
		try {
			return getTypeByPartition(
					TextUtilities.getPartition(document, getPartitioning(), offset, true).getType()
					);
		}
		catch (final BadLocationException e) {
			return ERROR;
		}
	}
	
	public abstract String getTypeByPartition(String contentType);
	
}
