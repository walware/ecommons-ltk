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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import de.walware.ecommons.text.core.treepartitioner.ITreePartitionNodeScan.BreakException;


public interface ITreePartitionNodeScanner {
	
	
	int getRestartOffset(ITreePartitionNode node, IDocument document, int offset)
			throws BadLocationException;
	
	AbstractPartitionNodeType getRootType();
	
	void execute(ITreePartitionNodeScan scan) throws BreakException;
	
}
