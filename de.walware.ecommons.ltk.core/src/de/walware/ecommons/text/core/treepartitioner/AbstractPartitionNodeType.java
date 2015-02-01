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

package de.walware.ecommons.text.core.treepartitioner;

import org.eclipse.jface.text.IDocument;


public abstract class AbstractPartitionNodeType implements ITreePartitionNodeType {
	
	
	public AbstractPartitionNodeType() {
	}
	
	
	@Override
	public abstract String getPartitionType();
	
	@Override
	public boolean prefereAtBegin(final ITreePartitionNode node,
			final IDocument document) {
		return false;
	}
	
	@Override
	public boolean prefereAtEnd(final ITreePartitionNode node,
			final IDocument document) {
		return false;
	}
	
	
	@Override
	public String toString() {
		return getPartitionType();
	}
	
}
