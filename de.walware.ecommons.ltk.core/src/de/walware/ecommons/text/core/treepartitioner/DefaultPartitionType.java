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

package de.walware.ecommons.text.core.treepartitioner;

import org.eclipse.jface.text.IDocument;


public class DefaultPartitionType extends AbstractPartitionNodeType {
	
	
	public DefaultPartitionType() {
	}
	
	
	@Override
	public String getPartitionType() {
		return IDocument.DEFAULT_CONTENT_TYPE;
	}
	
	@Override
	public boolean prefereAtBegin(final ITreePartitionNode node,
			final IDocument document) {
		return true;
	}
	
	@Override
	public boolean prefereAtEnd(final ITreePartitionNode node,
			final IDocument document) {
		return true;
	}
	
}
