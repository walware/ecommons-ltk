/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.core.treepartitioner;

import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;


public class TreePartition extends Region implements ITypedRegion {
	
	
	private final ITreePartitionNode node;
	
	
	/**
	 * Creates a typed region based on the given specification.
	 * 
	 * @param offset the region's offset
	 * @param length the region's length
	 * @param type the region's type
	 */
	public TreePartition(final int offset, final int length, final ITreePartitionNode node) {
		super(offset, length);
		
		if (node == null) {
			throw new NullPointerException("node"); //$NON-NLS-1$
		}
		this.node= node;
	}
	
	
	@Override
	public String getType() {
		return this.node.getType().getPartitionType();
	}
	
	public ITreePartitionNode getTreeNode() {
		return this.node;
	}
	
	
	@Override
	public int hashCode() {
		return super.hashCode() | getType().hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TreePartition)) {
			return false;
		}
		return (super.equals(obj)
				&& getType().equals(((TreePartition) obj).getType()) );
	}
	
	@Override
	public String toString() {
		return getType() + ": offset= " + getOffset() + ", length= " + getLength(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}
