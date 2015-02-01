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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.Position;

import de.walware.ecommons.collections.ImCollections;
import de.walware.ecommons.collections.ImList;


class NodePosition extends Position implements ITreePartitionNode {
	
	
	static final int indexOf(final List<NodePosition> children, final int offset) {
		int begin= 0;
		int end= children.size() - 1;
		while (begin <= end) {
			int i= (begin + end) >>> 1;
			NodePosition child= children.get(i);
			if (child.offset > offset) {
				end= i - 1;
			}
			// !(child.offset == offset || child.offset + child.length > offset)
			else if (child.offset != offset && child.offset + child.length <= offset) {
				begin= i + 1;
			}
			else {
				for (; i > 0; i--) {
					child= children.get(i - 1);
					if (child.offset != offset && child.offset + child.length <= offset) {
						break;
					}
				}
				return i;
			}
		}
		return -(begin + 1);
	}
	
	
	private static final List<NodePosition> NO_CHILDREN= Collections.emptyList();
	
	private static final ImList<Object> NO_ATTACHMENTS= ImCollections.emptyList();
	
	
	NodePosition parent;
	
	/**
	 * Sorted list of children
	 */
	List<NodePosition> children= NodePosition.NO_CHILDREN;
	
	ITreePartitionNodeType type;
	
	int stamp;
	
	private volatile ImList<Object> attachments= NO_ATTACHMENTS;
	
	
	public NodePosition(final NodePosition parent, final int offset, final int length,
			final ITreePartitionNodeType type, final int stamp) {
		super(offset, length);
		
		this.parent= parent;
		this.type= type;
		this.stamp= stamp;
	}
	
	
	public final int getEndOffset() {
		return getOffset() + getLength();
	}
	
	
	final void insertChild(final int childIdx, final NodePosition child) {
		assert (child.parent == this);
		
		if (this.children == NO_CHILDREN) {
			this.children= new ArrayList<>(8);
		}
		this.children.add(childIdx, child);
	}
	
	private final void removeChild(final NodePosition child) {
		assert (child.parent == this);
		
		child.parent= null;
		if (this.isDeleted) {
			return;
		}
		
		// note: if child is deleted, the position may be out-of-sync
		for (int idx= 0; idx < this.children.size(); idx++) {
			if (this.children.get(idx) == child) {
				this.children.remove(idx);
				return;
			}
		}
		assert (false);
	}
	
	@Override
	public final void delete() {
		super.delete();
		
		if (this.parent != null) {
			this.parent.removeChild(this);
		}
	}
	
	
	
	@Override
	public final void undelete() {
		throw new UnsupportedOperationException();
	}
	
	
	@Override
	public final ITreePartitionNodeType getType() {
		return this.type;
	}
	
	@Override
	public ITreePartitionNode getParent() {
		return this.parent;
	}
	
	@Override
	public final int getChildCount() {
		return this.children.size();
	}
	
	@Override
	public final ITreePartitionNode getChild(final int idx) {
		return this.children.get(idx);
	}
	
	@Override
	public final int indexOfChild(final int offset) {
		return indexOf(this.children, offset);
	}
	
	@Override
	public final int indexOfChild(final ITreePartitionNode child) {
		int idx= indexOf(this.children, child.getOffset());
		assert (idx >= 0);
		do {
			if (this.children.get(idx) == child) {
				return idx;
			}
		}
		while (++idx < this.children.size());
		
//		for (idx= 0; idx < this.children.size(); idx++) {
//			if (this.children.get(idx) == child) {
//				return idx;
//			}
//		}
		
		throw new IllegalArgumentException("child"); //$NON-NLS-1$
	}
	
	
	@Override
	public synchronized void addAttachment(final Object data) {
		this.attachments= ImCollections.addElement(this.attachments, data);
	}
	
	@Override
	public synchronized void removeAttachment(final Object data) {
		this.attachments= ImCollections.removeElement(this.attachments, data);
	}
	
	@Override
	public ImList<Object> getAttachments() {
		return this.attachments;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb= new StringBuilder("NodePosition ["); //$NON-NLS-1$
		sb.append(getOffset());
		sb.append(", "); //$NON-NLS-1$
		sb.append(getOffset() + getLength());
		sb.append(") "); //$NON-NLS-1$
		if (isDeleted()) {
			sb.append("<deleted> "); //$NON-NLS-1$
		}
		sb.append(this.type);
		return sb.toString();
	}
	
}
