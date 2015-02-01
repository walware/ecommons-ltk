/*=============================================================================#
 # Copyright (c) 2013-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;


public class FixDocumentPartitioner implements IDocumentPartitioner {
	
	
	private final String[] contentTypes;
	
	private IDocument document;
	
	private final List<ITypedRegion> partitions= new ArrayList<>();
	
	
	public FixDocumentPartitioner(final String[] contentTypes) {
		this.contentTypes= contentTypes;
	}
	
	
	public void append(final String contentType, final int length) {
		if (this.partitions.isEmpty()) {
			this.partitions.add(new TypedRegion(0, length, contentType));
		}
		else {
			final ITypedRegion previous= this.partitions.get(this.partitions.size() - 1);
			if (previous.getType() == contentType) {
				this.partitions.set(this.partitions.size() - 1, new TypedRegion(
						previous.getOffset(), previous.getLength() + length, contentType ));
			}
			else {
				this.partitions.add(new TypedRegion(
						previous.getOffset() + previous.getLength(), length, contentType ));
			}
		}
	}
	
	@Override
	public void connect(final IDocument document) {
		this.document= document;
	}
	
	@Override
	public void disconnect() {
		this.document= null;
	}
	
	@Override
	public void documentAboutToBeChanged(final DocumentEvent event) {
	}
	
	@Override
	public boolean documentChanged(final DocumentEvent event) {
		return true;
	}
	
	@Override
	public String[] getLegalContentTypes() {
		return this.contentTypes;
	}
	
	private int indexOf(final int offset, final boolean prefereOpen) {
		final int last= this.partitions.size() - 1;
		int i= 0;
		if (prefereOpen) {
			for (; i < last; i++) {
				final ITypedRegion partition= this.partitions.get(i);
				if (offset < partition.getOffset() + partition.getLength()) {
					return i;
				}
			}
		}
		// last or prefereOpen
		for (; i <= last; i++) {
			final ITypedRegion partition= this.partitions.get(i);
			if (offset <= partition.getOffset() + partition.getLength()) {
				return i;
			}
		}
		throw new IndexOutOfBoundsException("offset: " + Integer.toString(offset)); //$NON-NLS-1$
	}
	
	@Override
	public String getContentType(final int offset) {
		return this.partitions.get(indexOf(offset, false)).getType();
	}
	
	@Override
	public ITypedRegion[] computePartitioning(final int offset, final int length) {
		final int startIdx= indexOf(offset, false);
		final int endIdx= indexOf(offset + length, true);
		final List<ITypedRegion> list= this.partitions.subList(startIdx, endIdx);
		return list.toArray(new ITypedRegion[list.size()]);
	}
	
	@Override
	public ITypedRegion getPartition(final int offset) {
		return this.partitions.get(indexOf(offset, false));
	}
	
}
