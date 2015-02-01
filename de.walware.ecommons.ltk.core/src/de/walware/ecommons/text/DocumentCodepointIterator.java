/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;


public class DocumentCodepointIterator extends CharCodepointIterator {
	
	
	private static int FRAGMENT_LENGTH = 2048;
	private static int FRAGMENT_ADDITION = 64;
	
	
	/**
	 * Creates a new iterator for a jface document.
	 * 
	 * @param document the document
	 * @param beginIndex the begin index of the iterator in the document
	 * @param endIndex the end index of the iterator in the document
	 * @throws BadLocationException if an index is not valid
	 */
	public static CharCodepointIterator create(final IDocument document,
			final int beginIndex, final int endIndex)
			throws BadLocationException {
		if (endIndex-beginIndex <= FRAGMENT_LENGTH + FRAGMENT_ADDITION) {
			return create(document.get(beginIndex, endIndex-beginIndex),
					beginIndex, beginIndex, endIndex );
		}
		else {
			return new DocumentCodepointIterator(document, beginIndex, endIndex);
		}
	}
	
	/**
	 * Creates a new iterator for a whole jface document.
	 * 
	 * @param document the document
	 * @throws BadLocationException if an index is not valid
	 */
	public static CharCodepointIterator create(final IDocument document)
			throws BadLocationException {
		return create(document, 0, document.getLength());
	}
	
	
	private final IDocument fDocument;
	
	private int fFragmentOffset;
	private String fFragment;
	
	
	protected DocumentCodepointIterator(final IDocument doc,
			final int beginIndex, final int endIndex) throws BadLocationException {
		super(beginIndex, endIndex);
		fDocument = doc;
		
		fFragmentOffset = Integer.MIN_VALUE;
	}
	
	
	@Override
	protected char getChar(final int offset, final byte prepare) {
		int offsetInFragment = offset - fFragmentOffset;
		if (offsetInFragment < 0 || offsetInFragment >= FRAGMENT_LENGTH) {
			if ((prepare & PREPARE_FORWARD) != 0 ||
					(prepare & PREPARE_FORWARD) == 0 && offset < fFragmentOffset) {
				fFragmentOffset = Math.max(offset - FRAGMENT_LENGTH + FRAGMENT_ADDITION,
						getBeginIndex() );
			}
			else {
				fFragmentOffset = Math.max(offset - FRAGMENT_ADDITION,
						getBeginIndex() );
			}
			try {
				fFragment = fDocument.get(fFragmentOffset, Math.min(FRAGMENT_LENGTH, 
						getEndIndex() - fFragmentOffset ));
				offsetInFragment = offset - fFragmentOffset;
			}
			catch (final BadLocationException e) {
				fFragmentOffset = Integer.MIN_VALUE;
				fFragment = null;
				return (char) EOF;
			}
		}
		return fFragment.charAt(offsetInFragment);
	}
	
	
	@Override
	public String toString() {
		try {
			return fDocument.get(getBeginIndex(), getEndIndex() - getBeginIndex());
		}
		catch (final BadLocationException e) {
			return e.getMessage();
		}
	}
	
}
