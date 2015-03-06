/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package de.walware.ecommons.text.core.treepartitioner;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitionerExtension;
import org.eclipse.jface.text.IDocumentPartitionerExtension2;
import org.eclipse.jface.text.IDocumentPartitionerExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;

import de.walware.ecommons.text.core.treepartitioner.ITreePartitionNodeScan.BreakException;

import de.walware.ecommons.ltk.internal.core.LTKCorePlugin;


/**
 * A implementation of a document partitioner using a tree as data structure.
 * <p>It uses an {@link ITreePartitionNodeScanner} to scan the document and to determine the 
 * document's partitioning.
 * The partitioner maintains the document's partitions in a tree of ITreePartitionNode backed by
 * positions in the document itself.
 * </p>
 * @see ITreePartitionNodeScanner
 */
public class TreePartitioner implements IDocumentPartitioner,
		IDocumentPartitionerExtension, IDocumentPartitionerExtension2, IDocumentPartitionerExtension3 {
	
	
	static final boolean DEBUG_CHECK= "true".equalsIgnoreCase( //$NON-NLS-1$
			Platform.getDebugOption("de.walware.ecommons.text/debug/TreePartitioner/validate")); //$NON-NLS-1$
	
	
	/**
	 * The position category this partitioner uses to store the document's partitioning information.
	 */
	private static final String CONTENT_TYPES_CATEGORY= "__content_types_category"; //$NON-NLS-1$
	
	/** The partitioner's scanner */
	protected final ITreePartitionNodeScanner scanner;
	
	/** The legal content types of this partitioner */
	protected final String[] legalContentTypes;
	
	/**
	 * Flag indicating whether this partitioner has been initialized.
	 */
	private boolean isInitialized= false;
	
	/** The partitioner's document */
	protected IDocument document;
	
	/** The document length before a document change occurred */
	protected int previousDocumentLength;
	
	/** The position updater used to for the default updating of partitions */
	protected final DefaultPositionUpdater positionUpdater;
	
	/**
	 * The position category this partitioner uses to store the document's partitioning information.
	 */
	private final String positionCategory;
	
	private final NodePosition rootPosition;
	
	private ITreePartitionNodeType startType;
	
	/**
	 * The active document rewrite session.
	 */
	private DocumentRewriteSession activeRewriteSession;
	
	private TreePartitionerScan scan;
	
	
	/**
	 * Creates a new partitioner that uses the given scanner and may return
	 * partitions of the given legal content types.
	 *
	 * @param scanner the scanner this partitioner is supposed to use
	 * @param legalContentTypes the legal content types of this partitioner
	 */
	public TreePartitioner(final ITreePartitionNodeScanner scanner,
			final String[] legalContentTypes) {
		this.scanner= scanner;
		this.legalContentTypes= Arrays.copyOf(legalContentTypes, legalContentTypes.length, String[].class);
		this.positionCategory= CONTENT_TYPES_CATEGORY + hashCode();
		this.positionUpdater= new DefaultPositionUpdater(this.positionCategory);
		this.rootPosition= new NodePosition(null, 0, Integer.MAX_VALUE, scanner.getRootType(), 0) {
			@Override
			public void setLength(final int length) {
			}
			@Override
			public int getLength() {
				return TreePartitioner.this.document.getLength();
			}
		};
	}
	
	
	@Override
	public String[] getManagingPositionCategories() {
		return new String[] { this.positionCategory };
	}
	
	public void setStartType(final ITreePartitionNodeType type) {
		this.startType= type;
	}
	
	@Override
	public final void connect(final IDocument document) {
		connect(document, false);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * May be extended by subclasses.
	 * </p>
	 */
	@Override
	public void connect(final IDocument document, final boolean delayInitialization) {
		Assert.isNotNull(document);
		Assert.isTrue(!document.containsPositionCategory(this.positionCategory));
		
		this.document= document;
		this.document.addPositionCategory(this.positionCategory);
		
		this.isInitialized= false;
		if (!delayInitialization) {
			checkInitialization();
		}
	}
	
	/**
	 * Calls {@link #initialize()} if the receiver is not yet initialized.
	 */
	protected final void checkInitialization() {
		if (!this.isInitialized) {
			initialize();
		}
	}
	
	protected void clear() {
		// remove all position belonging to the partitioner position category
		try {
			this.rootPosition.children.clear();
			this.document.removePositionCategory(this.positionCategory);
		} catch (final BadPositionCategoryException x) {
		}
		this.document.addPositionCategory(this.positionCategory);
		
		this.isInitialized= false;
	}
	
	/**
	 * Performs the initial partitioning of the partitioner's document.
	 * <p>
	 * May be extended by subclasses.
	 * </p>
	 */
	protected void initialize() {
		this.isInitialized= true;
		
		if (this.scan == null) {
			this.scan= new TreePartitionerScan(this);
		}
		
		NodePosition beginPosition= this.rootPosition;
		if (this.startType != null) {
			beginPosition= this.scan.add(this.startType, beginPosition, 0);
		}
		
		this.scan.init(0, this.document.getLength(), beginPosition);
		this.scan.markDirtyRegion(0, Integer.MAX_VALUE);
		try {
			this.scanner.execute(this.scan);
		}
		catch (final BreakException b) {
		}
		finally {
			if (DEBUG_CHECK) {
				check();
//				System.out.println(toString());
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * May be extended by subclasses.
	 * </p>
	 */
	@Override
	public void disconnect() {
		Assert.isTrue(this.document.containsPositionCategory(this.positionCategory));
		
		try {
			this.document.removePositionCategory(this.positionCategory);
		} catch (final BadPositionCategoryException x) {
			// can not happen because of Assert
		}
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * May be extended by subclasses.
	 * </p>
	 */
	@Override
	public void documentAboutToBeChanged(final DocumentEvent e) {
		if (this.isInitialized) {
			Assert.isTrue(e.getDocument() == this.document);
			
			this.previousDocumentLength= e.getDocument().getLength();
		}
	}
	
	@Override
	public final boolean documentChanged(final DocumentEvent e) {
		if (this.isInitialized) {
			final IRegion region= documentChanged2(e);
			return (region != null);
		}
		return false;
	}
	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * May be extended by subclasses.
	 * </p>
	 */
	@Override
	public IRegion documentChanged2(final DocumentEvent event) {
		if (!this.isInitialized) {
			return null;
		}
		try {
			Assert.isTrue(event.getDocument() == this.document);
			
			int reparseStart= event.getOffset();
			final int lineOfOffset= this.document.getLineOfOffset(reparseStart);
			final int newLength= event.getText() == null ? 0 : event.getText().length();
			
			this.positionUpdater.update(event);
			
			// line start of previous line
			/* This adds support of two-line partition separators.
			 * An alternative would be to implement it (depend on the position) in
			 * scanner.getRestartOffset(...) */
			reparseStart= this.document.getLineOffset((lineOfOffset > 0) ? lineOfOffset - 1 : 0);
			
			NodePosition beginPosition= null;
			if (reparseStart > 0) {
				beginPosition= findPositionPreferOpen(reparseStart);
				final int offset= this.scanner.getRestartOffset(beginPosition, this.document, reparseStart);
				if (reparseStart != offset) {
					reparseStart= offset;
					if (reparseStart != 0) {
						beginPosition= findPositionPreferOpen(reparseStart);
					}
				}
			}
			if (reparseStart == 0) {
				beginPosition= this.rootPosition;
				
				if (this.startType != null) {
					beginPosition= this.scan.add(this.startType, beginPosition, 0);
				}
			}
			
			this.scan.init(reparseStart, this.document.getLength(), beginPosition);
			this.scan.markDirtyBegin(reparseStart);
			this.scan.markDirtyEnd(event.getOffset() + newLength);
			try {
				this.scanner.execute(this.scan);
			}
			catch (final BreakException b) {
			}
			finally {
				if (DEBUG_CHECK) {
					check();
//					System.out.println(toString());
				}
			}
			
			return this.scan.createDirtyRegion();
		}
		catch (final BadLocationException e) {
			e.printStackTrace();
			clear();
			return new Region(0, this.document.getLength());
		}
	}
	
	
	final void addPosition(final NodePosition position) throws BadLocationException, BadPositionCategoryException {
		this.document.addPosition(this.positionCategory, position);
	}
	
	final void removePosition(final NodePosition position) throws BadPositionCategoryException {
		this.document.removePosition(this.positionCategory, position);
	}
	
	final NodePosition findPosition(final int offset) {
		NodePosition p= this.rootPosition;
		
		while (true) {
			assert (p.includes(offset));
			
			final List<NodePosition> children= p.children;
			final int childCount= children.size();
			if (childCount > 0) {
				final int idx= NodePosition.indexOf(children, offset);
				if (idx >= 0) {
					p= children.get(idx);
					if (p.getLength() == 0) {
						if (idx + 1 < children.size()) {
							p= children.get(idx + 1);
							if (p.includes(offset)) {
								continue;
							}
						}
						return p.parent;
					}
					continue;
				}
			}
			return p;
		}
	}
	
	@Override
	public String getContentType(final int offset) {
		checkInitialization();
		
		final NodePosition position= findPosition(offset);
		return position.type.getPartitionType();
	}
	
	private TreePartition createPartition(final NodePosition p, final int childIdx) {
		final int offset;
		final int length;
		if (childIdx > 0) {
			offset= p.children.get(childIdx - 1).getEndOffset();
		}
		else {
			offset= p.getOffset();
		}
		if (childIdx >= 0 && childIdx < p.children.size()) {
			length= p.children.get(childIdx).getOffset() - offset;
		}
		else {
			length= p.getEndOffset() - offset;
		}
		return new TreePartition(offset, length, p);
	}
	
	@Override
	public TreePartition getPartition(final int offset) {
		checkInitialization();
		
		NodePosition p= this.rootPosition;
		
		while (true) {
			assert (p.includes(offset));
			
			final List<NodePosition> children= p.children;
			final int childCount= children.size();
			if (childCount > 0) {
				final int idx= NodePosition.indexOf(children, offset);
				if (idx >= 0) {
					p= p.children.get(idx);
					if (p.getLength() == 0) {
						if (idx + 1 < children.size()) {
							p= children.get(idx + 1);
							if (p.includes(offset)) {
								continue;
							}
						}
						return createPartition(p.parent, idx);
					}
					continue;
				}
				
				return createPartition(p, -(idx + 1));
			}
			
			return createPartition(p, -1);
		}
	}
	
	/**
	 * Recursively adds partitions to the given list.
	 * 
	 * @param partitions the list of partitions
	 * @param p the position to add
	 * @param offset the min offset
	 * @param endOffset the max offset
	 * @return the end offset of the last added partition
	 */
	private int addPartition(final List<TreePartition> partitions, final NodePosition p,
			int offset, int endOffset) {
		offset= (Math.max(offset, p.getOffset()));
		endOffset= (Math.min(endOffset, p.getEndOffset()));
		final List<NodePosition> children= p.children;
		int childIdx= 0;
		final int childCount= children.size();
		if (childCount > 0) {
			if (p.getOffset() < offset) {
				childIdx= NodePosition.indexOf(children, offset);
				if (childIdx < 0) {
					childIdx= -(childIdx + 1);
				}
			}
			for (; childIdx < childCount; childIdx++) {
				final NodePosition child= children.get(childIdx);
				if (child.getOffset() > endOffset) {
					break;
				}
				if (offset < child.getOffset()) {
					partitions.add(new TreePartition(offset, child.getOffset() - offset, p));
				}
				offset= addPartition(partitions, child, offset, endOffset);
			}
		}
		if (offset < endOffset) {
			partitions.add(new TreePartition(offset, endOffset - offset, p));
		}
		return endOffset;
	}
	
	@Override
	public final ITypedRegion[] computePartitioning(final int offset, final int length) {
		checkInitialization();
		
		final List<TreePartition> partitions= new ArrayList<>();
		try {
			addPartition(partitions, this.rootPosition, offset, offset + length);
		}
		catch (final RuntimeException ex) {
			clear();
			throw ex;
		}
		
		return partitions.toArray(new ITypedRegion[partitions.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * May be replaced or extended by subclasses.
	 * </p>
	 */
	@Override
	public String[] getLegalContentTypes() {
		return Arrays.copyOf(this.legalContentTypes, this.legalContentTypes.length, String[].class);
	}
	
	/**
	 * Returns whether the given type is one of the legal content types.
	 * <p>
	 * May be extended by subclasses.
	 * </p>
	 *
	 * @param contentType the content type to check
	 * @return <code>true</code> if the content type is a legal content type
	 */
	protected boolean isSupportedPartitionType(final String contentType) {
		if (contentType != null) {
			for (int i= 0; i < this.legalContentTypes.length; i++) {
				if (this.legalContentTypes[i].equals(contentType)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/* zero-length partition support */
	
	final NodePosition findPositionPreferOpen(final int offset) {
		NodePosition p= this.rootPosition;
		
		while (true) {
			assert (p.includes(offset) || p.getEndOffset() == offset);
			
			final List<NodePosition> children= p.children;
			int idx= -1;
			final int childCount= children.size();
			if (childCount > 0) {
				idx= NodePosition.indexOf(children, offset);
				if (idx >= 0) {
					final NodePosition child= children.get(idx);
					if (child.getOffset() < offset
							|| (/*child.getOffset() == offset &&*/ child.type.prefereAtBegin(child, this.document)) ) {
						p= child;
						continue;
					}
				}
				else {
					idx= -(idx + 1);
				}
				if (idx > 0) {
					final NodePosition child= children.get(idx - 1);
					if (child.getEndOffset() == offset && child.type.prefereAtEnd(child, this.document)) {
						p= child;
						continue;
					}
				}
			}
			return p;
		}
	}
	
	private TreePartition getPartitionPreferOpen(final int offset) {
		checkInitialization();
		
		NodePosition p= this.rootPosition;
		
		while (true) {
			assert (p.includes(offset) || p.getEndOffset() == offset);
			
			final List<NodePosition> children= p.children;
			int idx= -1;
			final int childCount= children.size();
			if (childCount > 0) {
				idx= NodePosition.indexOf(children, offset);
				if (idx >= 0) {
					final NodePosition child= p.children.get(idx);
					if (child.getOffset() < offset
							|| (/*child.getOffset() == offset &&*/ child.type.prefereAtBegin(child, this.document)) ) {
						p= child;
						continue;
					}
				}
				else {
					idx= -(idx + 1);
				}
				if (idx > 0) {
					final NodePosition child= p.children.get(idx - 1);
					if (child.getEndOffset() == offset && child.type.prefereAtEnd(child, this.document)) {
						p= child;
						continue;
					}
				}
			}
			return createPartition(p, idx);
		}
	}
	
	@Override
	public String getContentType(final int offset, final boolean preferOpenPartitions) {
		checkInitialization();
		
		final NodePosition position= (preferOpenPartitions) ?
				findPositionPreferOpen(offset) :
				findPosition(offset);
		return position.type.getPartitionType();
	}
	
	public ITreePartitionNodeType getTreeNode(final int offset, final boolean preferOpenPartitions) {
		checkInitialization();
		
		final NodePosition position= (preferOpenPartitions) ?
				findPositionPreferOpen(offset) :
				findPosition(offset);
		return position.type;
	}
	
	@Override
	public TreePartition getPartition(final int offset, final boolean preferOpenPartitions) {
		return (preferOpenPartitions) ?
				getPartitionPreferOpen(offset) :
				getPartition(offset);
	}
	
	/**
	 * Recursively adds partitions to the given list.
	 * 
	 * @param partitions the list of partitions
	 * @param p the position to add
	 * @param offset the min offset
	 * @param endOffset the max offset
	 * @return the end offset of the last added partition
	 */
	private int addPartitionIncludeZeroLength(final List<TreePartition> partitions,
			final NodePosition p, int offset, int endOffset) {
		offset= (Math.max(offset, p.getOffset()));
		endOffset= (Math.min(endOffset, p.getEndOffset()));
		final List<NodePosition> children= p.children;
		int childIdx= 0;
		final int childCount= children.size();
		if (childCount > 0) {
			if (p.getOffset() < offset) {
				childIdx= NodePosition.indexOf(children, offset);
				if (childIdx < 0) {
					childIdx= -(childIdx + 1);
				}
			}
			for (; childIdx < childCount; childIdx++) {
				final NodePosition child= children.get(childIdx);
				if (child.getOffset() > endOffset) {
					break;
				}
//				if (offset <= child.getOffset()) {
					partitions.add(new TreePartition(offset, child.getOffset() - offset, p));
//				}
				offset= addPartitionIncludeZeroLength(partitions, child, offset, endOffset);
			}
		}
		if (offset < endOffset || (offset == endOffset
				&& (--childIdx < 0 || offset == children.get(childIdx).getEndOffset()) )) {
			partitions.add(new TreePartition(offset, endOffset - offset, p));
		}
		return endOffset;
	}
	
	private ITypedRegion[] computePartitioningIncludeZeroLength(final int offset, final int length) {
		checkInitialization();
		
		final List<TreePartition> partitions= new ArrayList<>();
		try {
			addPartitionIncludeZeroLength(partitions, this.rootPosition, offset, offset + length);
		}
		catch (final RuntimeException ex) {
			// Make sure we clear the cache
			clear();
			throw ex;
		}
		
		return partitions.toArray(new ITypedRegion[partitions.size()]);
	}
	
	@Override
	public ITypedRegion[] computePartitioning(final int offset, final int length,
			final boolean includeZeroLengthPartitions) {
		return (includeZeroLengthPartitions) ?
				computePartitioningIncludeZeroLength(offset, length) :
				computePartitioning(offset, length);
	}
	
	@Override
	public void startRewriteSession(final DocumentRewriteSession session) throws IllegalStateException {
		if (this.activeRewriteSession != null) {
			throw new IllegalStateException();
		}
		this.activeRewriteSession= session;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * May be extended by subclasses.
	 * </p>
	 */
	@Override
	public void stopRewriteSession(final DocumentRewriteSession session) {
		if (this.activeRewriteSession == session) {
			flushRewriteSession();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * May be extended by subclasses.
	 * </p>
	 */
	@Override
	public DocumentRewriteSession getActiveRewriteSession() {
		return this.activeRewriteSession;
	}
	
	/**
	 * Flushes the active rewrite session.
	 */
	protected final void flushRewriteSession() {
		this.activeRewriteSession= null;
		clear();
	}
	
	
	void check() {
		try {
			validateChildren(this.rootPosition);
		}
		catch (final Error e) {
			LTKCorePlugin.log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID,
					"Error in document partition - " + toString() +
					"\n==== document content:\n" + this.document.get() + "\n====",
					e ));
		}
	}
	
	private void validateChildren(final NodePosition parent) {
		int offset= parent.getOffset();
		final int childCount= parent.children.size();
		for (int childIdx= 0; childIdx < childCount; childIdx++) {
			final NodePosition child= parent.children.get(childIdx);
			if (child.parent != parent) {
				throw new AssertionError("position.parent");
			}
			if (child.isDeleted()) {
				throw new AssertionError("position.isDeleted");
			}
			if (child.getOffset() < offset || child.getOffset() > parent.getEndOffset()) {
				throw new AssertionError("position.offset");
			}
			if (child.getLength() < 0 || child.getEndOffset() > parent.getEndOffset()) {
				throw new AssertionError("position.length");
			}
			offset= child.getEndOffset();
			
			validateChildren(child);
		}
	}
	
	@Override
	public String toString() {
		final StringWriter writer= new StringWriter();
		writer.append("TreePartitioner (scanner= " + this.scanner.getClass().getCanonicalName() + "):\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!this.isInitialized) {
			writer.append("<no initialized>"); //$NON-NLS-1$
		}
		final TreePartitionUtil.PartitionPrinter printer= new TreePartitionUtil.PartitionPrinter(writer);
		try {
			printer.print(this.rootPosition, this.document);
			writer.append("====");
		}
		catch (final IOException e) {}
		return writer.toString();
	}
	
}
