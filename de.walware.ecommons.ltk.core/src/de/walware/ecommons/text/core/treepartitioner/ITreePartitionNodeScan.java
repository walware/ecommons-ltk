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


public interface ITreePartitionNodeScan {
	
	
	class BreakException extends RuntimeException {
		
		private static final long serialVersionUID= 1L;
		
		
		public BreakException() {
			super("BreakScan", null, true, false);
		}
		
	}
	
	
	IDocument getDocument();
	
	int getBeginOffset();
	
	int getEndOffset();
	
	ITreePartitionNode getBeginNode();
	
	
	boolean isAutoBreakEnabled();
	
	void setAutoBreakEnabled(boolean enable);
	
	void checkBreak() throws BreakException;
	
	
	ITreePartitionNode add(ITreePartitionNodeType type,
			ITreePartitionNode parent, int offset);
	
	ITreePartitionNode add(ITreePartitionNodeType type,
			ITreePartitionNode parent, int offset, int length);
	
	void expand(ITreePartitionNode node, int endOffset, boolean close);
	
	void markDirtyEnd(int offset);
	
}
