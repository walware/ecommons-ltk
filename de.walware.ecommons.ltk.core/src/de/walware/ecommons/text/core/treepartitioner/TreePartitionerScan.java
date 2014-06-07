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

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;


/**
 * Scan implementation for {@link TreePartitioner}.
 */
final class TreePartitionerScan implements ITreePartitionNodeScan {
	
	
	private final TreePartitioner partitioner;
	
	private int beginOffset;
	private int endOffset;
	
	private NodePosition beginPosition;
	
	private NodePosition lastParentPosition;
	private int lastAddedIndex;
	private NodePosition lastAddedPosition;
	
	private int currentOffset;
	
	private int dirtyBeginOffset;
	private int dirtyEndOffset;
	
	private int stamp;
	private int equalTypeEndOffset;
	
	private boolean autoBreakEnabled;
	
	
	public TreePartitionerScan(final TreePartitioner partitioner) {
		this.partitioner= partitioner;
	}
	
	
	void init(final int beginOffset, final int endOffset, final NodePosition beginPosition) {
		this.beginOffset= beginOffset;
		this.endOffset= endOffset;
		this.beginPosition= beginPosition;
		
		this.lastParentPosition= beginPosition;
		{	int childIdx= beginPosition.indexOfChild(beginOffset);
			if (childIdx < 0) {
				childIdx= -(childIdx + 1);
			}
			this.lastAddedIndex= childIdx - 1;
		}
		this.currentOffset= beginOffset;
		
		this.dirtyBeginOffset= Integer.MAX_VALUE;
		this.dirtyEndOffset= -1;
		
		this.stamp++;
		this.equalTypeEndOffset= Integer.MIN_VALUE;
		
		this.autoBreakEnabled= true;
	}
	
	@Override
	public IDocument getDocument() {
		return this.partitioner.document;
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
		return this.beginPosition;
	}
	
	
	@Override
	public void setAutoBreakEnabled(final boolean enable) {
		this.autoBreakEnabled= enable;
	}
	
	@Override
	public boolean isAutoBreakEnabled() {
		return this.autoBreakEnabled;
	}
	
	@Override
	public NodePosition add(final ITreePartitionNodeType type,
			final ITreePartitionNode parent, final int offset) {
		final NodePosition position= doAdd(type, (NodePosition) parent, offset, 0);
		
		if (this.autoBreakEnabled) {
			checkBreak();
		}
		
		return position;
	}
	
	@Override
	public ITreePartitionNode add(final ITreePartitionNodeType type,
			final ITreePartitionNode parent, final int offset, final int length) {
		final NodePosition position= doAdd(type, (NodePosition) parent, offset, length);
		
		if (position.getLength() != length) {
			doUpdateEnd(position, offset + length);
		}
		
		if (this.autoBreakEnabled) {
			checkBreak();
		}
		
		return position;
	}
	
	private NodePosition doAdd(final ITreePartitionNodeType type,
			final NodePosition parentPosition, final int offset, final int length) {
		if (type == null) {
			throw new NullPointerException("type"); //$NON-NLS-1$
		}
		if (parentPosition == null) {
			throw new NullPointerException("parent"); //$NON-NLS-1$
		}
		if (offset < parentPosition.getOffset()) {
			throw new IllegalArgumentException("offset: offset < parent.offset"); //$NON-NLS-1$
		}
		if (length < 0) {
			throw new IllegalArgumentException("length: length < 0"); //$NON-NLS-1$
		}
		
		NodePosition position= null;
		
		final int endOffset= offset + length;
		int childIdx;
		if (offset >= this.currentOffset) {
			childIdx= cleanUpTo(parentPosition);
			
			position= findReuse(parentPosition.children, childIdx, type, offset);
			
			if (position != null && length <= position.getLength()) {
				int end= endOffset;
				if (!position.children.isEmpty()) {
					final int firstOffset= position.children.get(0).getOffset();
					if (firstOffset < end) {
						end= firstOffset;
					}
				}
				this.equalTypeEndOffset= end;
			}
			
			doDeleteChildren(parentPosition, childIdx, endOffset, position);
			if (parentPosition.getEndOffset() < endOffset) {
				doUpdateEnd(parentPosition, endOffset);
			}
			
			if (position == null) {
				position= createPosition(parentPosition, offset, length, childIdx, type);
			}
			
			this.lastParentPosition= parentPosition;
			this.lastAddedIndex= childIdx;
			this.lastAddedPosition= position;
			this.currentOffset= offset;
		}
		else {
			if (offset < parentPosition.offset || endOffset >= parentPosition.getEndOffset()) {
				throw new IllegalArgumentException("node not inside parent");
			}
			childIdx= parentPosition.indexOfChild(offset);
			if (childIdx < 0) {
				childIdx= -(childIdx + 1);
			}
			while (childIdx < parentPosition.children.size()) {
				final NodePosition child= parentPosition.children.get(childIdx);
				if (child.getOffset() == offset && child.getLength() == 0) {
					childIdx++;
					continue;
				}
				else if (child.getOffset() > offset) {
					break;
				}
				throw new IllegalArgumentException("node overlaps with existing node");
			}
			
			position= createPosition(parentPosition, offset, length, childIdx, type);
			position.setLength(length);
		}
		
		return position;
	}
	
	private int cleanUpTo(final NodePosition parentPosition) {
		int childIdx;
		if (parentPosition == this.lastParentPosition) {
			childIdx= this.lastAddedIndex + 1;
		}
		else if (parentPosition == this.lastAddedPosition) {
			childIdx= 0;
		}
		else {
			// step down
			childIdx= this.lastAddedIndex + 1;
			NodePosition p= this.lastParentPosition;
			do {
				doDeleteChildren(p, childIdx, Integer.MAX_VALUE, null);
				childIdx= p.parent.indexOfChild(p) + 1;
				p= p.parent;
			}
			while (parentPosition != p);
		}
		return childIdx;
	}
	
	private NodePosition createPosition(final NodePosition parentPosition,
			final int offset, final int length, final int childIdx, final ITreePartitionNodeType type) {
		markDirtyRegion(offset, length);
		
		final NodePosition position= new NodePosition(parentPosition, offset, length,
				type, this.stamp );
		parentPosition.insertChild(childIdx, position);
		try {
			this.partitioner.addPosition(position);
		}
		catch (final BadPositionCategoryException e) {}
		catch (final BadLocationException e) {
			throw new RuntimeException(e);
		}
		return position;
	}
	
	private NodePosition findReuse(final List<NodePosition> children, int childIdx,
			final ITreePartitionNodeType type, final int offset) {
		for (; childIdx < children.size(); childIdx++) {
			final NodePosition child= children.get(childIdx);
			if (child.getOffset() == offset && type.equals(child.type)) {
				child.type= type;
				return child;
			}
			if (child.getOffset() > offset) {
				break;
			}
		}
		return null;
	}
	
	@Override
	public void expand(final ITreePartitionNode node, final int offset, final boolean close) {
		final NodePosition position= (NodePosition) node;
		if (position == null) {
			throw new IllegalArgumentException("node"); //$NON-NLS-1$
		}
		final int length= offset - position.getOffset();
		if (length < 0) {
			throw new IllegalArgumentException("offset: offset < node.offset"); //$NON-NLS-1$
		}
		
		if (offset >= this.currentOffset) {
			int childIdx;
			childIdx= cleanUpTo(position);
			
			doDeleteChildren(position, childIdx, (close) ? Integer.MAX_VALUE : offset, null);
			
			this.lastParentPosition= position;
			this.lastAddedIndex= childIdx - 1;
			this.lastAddedPosition= (childIdx > 0) ? position.children.get(childIdx - 1) : null;
			this.currentOffset= offset;
		}
		else {
			final List<NodePosition> children= position.children;
			if (!children.isEmpty() && children.get(children.size() - 1).getEndOffset() > offset) {
				throw new IllegalArgumentException("offset: offset < endOffset of children");
			}
		}
		
		if ((close) ? (position.getLength() == length) : (position.getLength() >= length)) {
			if (position.stamp != this.stamp && offset > this.equalTypeEndOffset) {
				this.equalTypeEndOffset= offset;
			}
		}
		else {
			doUpdateEnd(position, offset);
		}
		
		this.partitioner.check();
		
		if (this.autoBreakEnabled) {
			checkBreak();
		}
	}
	
	/** Sets the end/length and updates all parents */
	private void doUpdateEnd(NodePosition position, final int endOffset) {
		markDirtyEnd(Math.max(endOffset, position.getEndOffset()));
		position.setLength(endOffset - position.getOffset());
		position.stamp= this.stamp;
		
		// update parents
		while (position.parent != null) {
			final int parentLength= position.parent.getLength();
			final int parentOffset= position.parent.getOffset();
			
			// remove other children in the changed range
			doDeleteChildren(position.parent, position.parent.indexOfChild(position),
					endOffset, position );
			
			if (parentOffset + parentLength >= endOffset) {
				return;
			}
			else { // expand and continue with parent
				position.parent.setLength(endOffset - parentOffset);
				position.parent.stamp= this.stamp;
			}
			position= position.parent;
		}
	}
	
	private void doDeleteChildren(final NodePosition parentPosition,
			int childIdx, final int endOffset, final NodePosition keepPosition) {
		final List<NodePosition> children= parentPosition.children;
		for (; childIdx < children.size(); ) {
			final NodePosition child= children.get(childIdx);
			if (child == keepPosition) {
				childIdx++;
				continue;
			}
			if (child.getOffset() < endOffset) {
				markDirtyRegion(child.getOffset(), child.getLength());
				children.remove(childIdx);
				try {
					doDelete(child);
				}
				catch (final BadPositionCategoryException e) {}
				continue;
			}
			break;
		}
	}
	
	private void doDelete(final NodePosition position) throws BadPositionCategoryException {
		if (position.parent != null) {
			position.parent= null;
			position.isDeleted= true;
			this.partitioner.removePosition(position);
			
			final List<NodePosition> children= position.children;
			for (int childIdx= 0; childIdx < children.size(); childIdx++) {
				doDelete(children.get(childIdx));
			}
		}
	}
	
	
	void markDirtyRegion(final int offset, final int length) {
		markDirtyBegin(offset);
		markDirtyEnd(offset + length);
	}
	
	public void markDirtyBegin(final int offset) {
		if (offset < this.dirtyBeginOffset) {
			this.dirtyBeginOffset= offset;
		}
	}
	
	@Override
	public void markDirtyEnd(final int offset) {
		if (offset > this.dirtyEndOffset) {
			this.dirtyEndOffset= offset;
		}
	}
	
	/**
	 * Creates the minimal region containing all partition changes using the
	 * remembered offset, end offset, and deletion offset.
	 *
	 * @return the minimal region containing all the partition changes
	 */
	IRegion createDirtyRegion() {
		if (this.dirtyEndOffset < 0 || this.dirtyBeginOffset == Integer.MAX_VALUE) {
			return null;
		}
		return new Region(this.dirtyBeginOffset,
				Math.min(getDocument().getLength(), this.dirtyEndOffset) - this.dirtyBeginOffset );
	}
	
	public boolean canBreak() {
		return (this.equalTypeEndOffset >= this.dirtyEndOffset);
	}
	
	@Override
	public void checkBreak() {
		if (canBreak()) {
			throw new BreakException();
		}
	}
	
}
