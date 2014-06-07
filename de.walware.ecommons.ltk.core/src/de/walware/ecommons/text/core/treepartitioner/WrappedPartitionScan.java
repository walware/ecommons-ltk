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


public class WrappedPartitionScan implements ITreePartitionNodeScan {
	
	
	private final ITreePartitionNodeScan parent;
	
	private int beginOffset;
	private int endOffset;
	
	private ITreePartitionNode beginNode;
	
	private boolean wasAutoBreakEnabled;
	
	
	public WrappedPartitionScan(final ITreePartitionNodeScan parent) {
		if (parent == null) {
			throw new NullPointerException("parent"); //$NON-NLS-1$
		}
		this.parent= parent;
	}
	
	
	public void init(final int beginOffset, final int endOffset, final ITreePartitionNode beginNode) {
		if (beginNode == null) {
			throw new NullPointerException("beginNode"); //$NON-NLS-1$
		}
		this.beginOffset= beginOffset;
		this.endOffset= endOffset;
		this.beginNode= beginNode;
		
		this.wasAutoBreakEnabled= this.parent.isAutoBreakEnabled();
		if (!this.wasAutoBreakEnabled) {
			this.parent.setAutoBreakEnabled(true);
		}
	}
	
	public void exit() {
		if (this.wasAutoBreakEnabled != this.parent.isAutoBreakEnabled()) {
			this.parent.setAutoBreakEnabled(this.wasAutoBreakEnabled);
			if (this.wasAutoBreakEnabled) {
				this.parent.checkBreak();
			}
		}
		
		this.beginNode= null;
	}
	
	
	@Override
	public IDocument getDocument() {
		return this.parent.getDocument();
	}
	
	@Override
	public int getBeginOffset() {
		return this.beginOffset;
	}
	
	@Override
	public int getEndOffset() {
		return this.endOffset;
	}
	
	@Override
	public ITreePartitionNode getBeginNode() {
		return this.beginNode;
	}
	
	
	@Override
	public void setAutoBreakEnabled(final boolean enable) {
		this.parent.setAutoBreakEnabled(enable);
	}
	
	@Override
	public boolean isAutoBreakEnabled() {
		return this.parent.isAutoBreakEnabled();
	}
	
	@Override
	public void checkBreak() throws BreakException {
		this.parent.checkBreak();
	}
	
	@Override
	public ITreePartitionNode add(final ITreePartitionNodeType type, final ITreePartitionNode parent, final int offset) {
		return this.parent.add(type, parent, offset);
	}
	
	@Override
	public ITreePartitionNode add(final ITreePartitionNodeType type, final ITreePartitionNode parent,
			final int offset, final int length) {
		return this.parent.add(type, parent, offset, length);
	}
	
	@Override
	public void expand(final ITreePartitionNode node, final int endOffset, final boolean close) {
		this.parent.expand(node, endOffset, close);
	}
	
	@Override
	public void markDirtyEnd(final int offset) {
		this.parent.markDirtyEnd(offset);
	}
	
}
