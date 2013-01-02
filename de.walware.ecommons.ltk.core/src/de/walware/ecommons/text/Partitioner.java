/*******************************************************************************
 * Copyright (c) 2007-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text;

import java.util.Arrays;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;


/**
 * Extended {@link FastPartitioner}.
 */
public class Partitioner extends FastPartitioner {
	
	
	public static boolean equalPartitioner(final IDocumentPartitioner o1, final IDocumentPartitioner o2) {
		if (!((o1 instanceof Partitioner) && (o2 instanceof Partitioner))) {
			return false;
		}
		final Partitioner p1 = (Partitioner) o1;
		final Partitioner p2 = (Partitioner) o2;
		
		return ( ( (p1.fScanner == null && p2.fScanner == null)
					|| (p1.fScanner.getClass() == p2.fScanner.getClass()) )
				&& (p1.fDocument == p2.fDocument)
				&& Arrays.equals(p1.fLegalContentTypes, p2.fLegalContentTypes));
	}
	
	
	public Partitioner(final IPartitionTokenScanner scanner, final String[] legalContentTypes) {
		super(scanner, legalContentTypes);
		if (scanner instanceof IPartitionScannerCallbackExt) {
			((IPartitionScannerCallbackExt) scanner).setPartitionerCallback(this);
		}
	}
	
	
	public void setStartPartitionType(final String partitionType) {
		if (fScanner instanceof IPartitionScannerConfigExt) {
			((IPartitionScannerConfigExt) fScanner).setStartPartitionType(partitionType);
		}
		else {
			throw new UnsupportedOperationException();
		}
	}
	
	
	public void resetCache() {
		clearPositionCache();
	}
	
	@Override
	public ITypedRegion getPartition(final int offset, final boolean preferOpenPartitions) {
		final ITypedRegion region = getPartition(offset);
		if (preferOpenPartitions) {
			if (offset > 0) {
				if (offset == fDocument.getLength()) {
					return getPartition(offset - 1);
				}
				try {
					char c = fDocument.getChar(offset);
					if ((c == '\n' || c == '\r') &&
							((c = fDocument.getChar(offset - 1)) != '\n' && c != '\r') ) {
						return getPartition(offset - 1);
					}
				} catch (final BadLocationException e) {
				}
			}
			
			final String contentType = region.getType();
			if (region.getOffset() == offset && !(
					contentType.equals(IDocument.DEFAULT_CONTENT_TYPE) || contentType.endsWith("_default"))) { //$NON-NLS-1$
				String type;
				if (offset > 0) {
					final ITypedRegion open = getPartition(offset - 1);
					type = getPrefereOpenType(open.getType(), contentType);
					if (type == open.getType()) {
						return open;
					}
					if (type == contentType) {
						return region;
					}
				}
				else {
					type = IDocument.DEFAULT_CONTENT_TYPE;
				}
				return new TypedRegion(offset, 0, type);
			}
		}
		return region;
	}
	
	protected String getPrefereOpenType(final String open, final String opening) {
		if (open.equals(IDocument.DEFAULT_CONTENT_TYPE) || open.endsWith("_default")) {
			return open;
		}
		return IDocument.DEFAULT_CONTENT_TYPE;
	}
	
}
