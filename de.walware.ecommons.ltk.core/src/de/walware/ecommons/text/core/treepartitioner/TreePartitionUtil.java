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

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import de.walware.ecommons.text.IPartitionConstraint;


public class TreePartitionUtil {
	
	
	public static class PartitionPrinter {
		
		
		private final Writer writer;
		
		private IDocument document;
		
		private int maxFragmentSize= 25;
		
		
		public PartitionPrinter(final Writer writer) {
			if (writer == null) {
				throw new NullPointerException("writer"); //$NON-NLS-1$
			}
			this.writer= writer;
		}
		
		
		public void setMaxFragmentSize(final int size) {
			this.maxFragmentSize= size;
		}
		
		public void print(final ITreePartitionNode node, final IDocument document) throws IOException {
			this.document= document;
			try {
				print(node, 0);
			}
			finally {
				this.document= null;
			}
		}
		
		public void print(final List<TreePartition> partitions, final IDocument document) throws IOException {
			this.document= document;
			try {
				print(partitions);
			}
			finally {
				this.document= null;
			}
		}
		
		protected void print(final ITreePartitionNode node, final int nodeDepth) throws IOException {
			printIdent(nodeDepth);
			
			final int beginOffset= node.getOffset();
			final int endOffset= node.getOffset() + node.getLength();
			
			this.writer.write('['); 
			this.writer.write(Integer.toString(beginOffset));
			this.writer.write(", "); //$NON-NLS-1$
			this.writer.write(Integer.toString(endOffset));
			this.writer.write(") "); //$NON-NLS-1$
			this.writer.write(node.getType().toString());
			printFragment(beginOffset, endOffset);
			this.writer.write('\n');
			
			final int childCount= node.getChildCount();
			for (int childIdx= 0; childIdx < childCount; childIdx++) {
				print(node.getChild(childIdx), nodeDepth + 1);
			}
		}
		
		protected void print(final List<TreePartition> partitions) throws IOException {
			for (int i= 0; i < partitions.size(); i++) {
				final TreePartition partition= partitions.get(i);
				final int beginOffset= partition.getOffset();
				final int endOffset= partition.getOffset() + partition.getLength();
				
				this.writer.write('['); 
				this.writer.write(Integer.toString(beginOffset));
				this.writer.write(", "); //$NON-NLS-1$
				this.writer.write(Integer.toString(endOffset));
				this.writer.write(") "); //$NON-NLS-1$
				this.writer.write(partition.getType());
				printFragment(beginOffset, endOffset);
				this.writer.append('\n');
			}
		}
		
		protected void printIdent(final int depth) throws IOException {
			for (int i= 0; i < depth; i++) {
				this.writer.write("    "); //$NON-NLS-1$
			}
		}
		
		protected void printFragment(final int beginOffset, final int endOffset) throws IOException {
			if (this.document != null && this.maxFragmentSize > 0) {
				try {
					this.writer.write(": "); //$NON-NLS-1$
					int l= endOffset - beginOffset;
					if (l <= this.maxFragmentSize) {
						writeEncoded(this.document.get(beginOffset, l));
					}
					else if (this.maxFragmentSize < 13) {
						writeEncoded(this.document.get(beginOffset, this.maxFragmentSize - 3));
						this.writer.write(" ... "); //$NON-NLS-1$
					}
					else {
						l= (this.maxFragmentSize - 3) / 2;
						writeEncoded(this.document.get(beginOffset, l));
						this.writer.write(" ... "); //$NON-NLS-1$
						writeEncoded(this.document.get(endOffset - l, l));
					}
				}
				catch (final BadLocationException e) {
					this.writer.write("!!!ERROR!!!"); //$NON-NLS-1$
				}
			}
		}
		
		private void writeEncoded(final String s) throws IOException {
			for (int i= 0; i < s.length(); i++) {
				final int c= s.charAt(i);
				if (c < 0x10) {
					this.writer.write("<0x0"); //$NON-NLS-1$
					this.writer.write(Integer.toHexString(c));
					this.writer.write('>');
				}
				else if (c < 0x20) {
					this.writer.write("<0x"); //$NON-NLS-1$
					this.writer.write(Integer.toHexString(c));
					this.writer.write('>');
				}
				else {
					this.writer.write(c);
				}
			}
		}
		
	}
	
	
	public final static ITreePartitionNode getRootNode(final IDocument document, final String partitioning) {
		try {
			final TreePartition partition= (TreePartition) TextUtilities.getPartition(document, partitioning,
					0, false );
			ITreePartitionNode node= partition.getTreeNode();
			ITreePartitionNode parent;
			while ((parent= node.getParent()) != null) {
				node= parent;
			}
			return node;
		}
		catch (final BadLocationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public final static ITreePartitionNode getNode(final IDocument document, final String partitioning,
			final int offset, final boolean prefereOpen) throws BadLocationException {
		final TreePartition partition= (TreePartition) TextUtilities.getPartition(document,
				partitioning, offset, prefereOpen );
		if (partition instanceof TreePartition) {
			return partition.getTreeNode();
		}
		return null;
	}
	
	public final static ITreePartitionNode searchNodeUp(ITreePartitionNode node,
			final ITreePartitionNodeType type) {
		while (node != null && type != node.getType()) {
			node= node.getParent();
		}
		return node;
	}
	
	public final static ITreePartitionNode searchNodeUp(ITreePartitionNode node,
			final String partitionType) {
		while (node != null && partitionType != node.getType().getPartitionType()) {
			node= node.getParent();
		}
		return node;
	}
	
	public final static ITreePartitionNode searchNodeUp(ITreePartitionNode node,
			final IPartitionConstraint partitionConstraint) {
		while (node != null && !partitionConstraint.matches(node.getType().getPartitionType())) {
			node= node.getParent();
		}
		return node;
	}
	
	public final static ITreePartitionNode searchNode(final IDocument document, final String partitioning,
			final int offset, final boolean prefereOpen, final ITreePartitionNodeType type)
			throws BadLocationException {
		final TreePartition partition= (TreePartition) TextUtilities.getPartition(document, partitioning,
				offset, prefereOpen );
		return searchNodeUp(partition.getTreeNode(), type);
	}
	
	public final static ITreePartitionNode searchNode(final IDocument document, final String partitioning,
			final int offset, final boolean prefereOpen, final String partitionType)
			throws BadLocationException {
		final TreePartition partition= (TreePartition) TextUtilities.getPartition(document, partitioning,
				offset, prefereOpen );
		return searchNodeUp(partition.getTreeNode(), partitionType);
	}
	
	public final static ITreePartitionNode searchNode(final IDocument document, final String partitioning,
			final int offset, final boolean prefereOpen, final IPartitionConstraint partitionConstraint)
			throws BadLocationException {
		final TreePartition partition= (TreePartition) TextUtilities.getPartition(document, partitioning,
				offset, prefereOpen );
		return searchNodeUp(partition.getTreeNode(), partitionConstraint);
	}
	
	public final static IRegion searchPartitionRegion(final IDocument document, final String partitioning,
			final int offset, final boolean prefereOpen, final IPartitionConstraint partitionConstraint)
			throws BadLocationException {
		return searchPartitionRegion((TreePartition) TextUtilities.getPartition(document, partitioning,
				offset, prefereOpen ), partitionConstraint );
	}
	
	public final static IRegion searchPartitionRegion(final TreePartition partition,
			final IPartitionConstraint partitionConstraint) {
		if (partition == null) {
			throw new NullPointerException("partition");
		}
		if (!partitionConstraint.matches(partition.getType())) {
			return null;
		}
		final int begin= searchBegin(partition.getTreeNode(), partition.getOffset(), partitionConstraint);
		final int end= searchEnd(partition.getTreeNode(), partition.getOffset() + partition.getLength(), partitionConstraint);
		return new Region(begin, end - begin);
	}
	
	private static int searchBegin(final ITreePartitionNode node, final int offset,
			final IPartitionConstraint partitionConstraint) {
		final int childCount= node.getChildCount();
		int childIdx= node.indexOfChild(offset);
		if (childIdx < 0) {
			childIdx= -(childIdx + 1);
		}
		if (childIdx == childCount) {
			childIdx--;
		}
		for (; childIdx >= 0; childIdx--) {
			final ITreePartitionNode child= node.getChild(childIdx);
			if (!partitionConstraint.matches(child.getType().getPartitionType())) {
				return child.getOffset() + child.getLength();
			}
			final int stop= searchBeginChild(node, partitionConstraint);
			if (stop >= 0) {
				return stop;
			}
		}
		if (node.getParent() == null 
				|| !partitionConstraint.matches(node.getParent().getType().getPartitionType())) {
			return node.getOffset();
		}
		return searchBegin(node.getParent(), offset, partitionConstraint);
	}
	
	private static int searchBeginChild(final ITreePartitionNode node,
			final IPartitionConstraint partitionConstraint) {
		final int childCount= node.getChildCount();
		for (int childIdx= childCount - 1; childIdx >= 0; childIdx--) {
			final ITreePartitionNode child= node.getChild(childIdx);
			if (!partitionConstraint.matches(child.getType().getPartitionType())) {
				return child.getOffset() + child.getLength();
			}
			final int stop= searchBeginChild(child, partitionConstraint);
			if (stop >= 0) {
				return stop;
			}
		}
		return -1;
	}
	
	private static int searchEnd(final ITreePartitionNode node, final int offset,
			final IPartitionConstraint partitionConstraint) {
		final int childCount= node.getChildCount();
		int childIdx= (offset == node.getOffset()) ? 0 : node.indexOfChild(offset);
		if (childIdx < 0) {
			childIdx= -(childIdx + 1);
		}
		for (; childIdx < childCount; childIdx++) {
			final ITreePartitionNode child= node.getChild(childIdx);
			if (!partitionConstraint.matches(child.getType().getPartitionType())) {
				return child.getOffset();
			}
			final int stop= searchEndChild(node, partitionConstraint);
			if (stop >= 0) {
				return stop;
			}
		}
		if (node.getParent() == null 
				|| !partitionConstraint.matches(node.getParent().getType().getPartitionType())) {
			return node.getOffset() + node.getLength();
		}
		return searchEnd(node.getParent(), offset, partitionConstraint);
	}
	
	private static int searchEndChild(final ITreePartitionNode node,
			final IPartitionConstraint partitionConstraint) {
		final int childCount= node.getChildCount();
		for (int childIdx= 0; childIdx < childCount; childIdx++) {
			final ITreePartitionNode child= node.getChild(childIdx);
			if (!partitionConstraint.matches(child.getType().getPartitionType())) {
				return child.getOffset();
			}
			final int stop= searchEndChild(child, partitionConstraint);
			if (stop >= 0) {
				return stop;
			}
		}
		return -1;
	}
	
}
