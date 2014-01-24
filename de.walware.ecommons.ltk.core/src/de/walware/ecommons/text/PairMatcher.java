/*=============================================================================#
 # Copyright (c) 2005-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text;

import static de.walware.ecommons.text.ITokenScanner.CLOSING_PEER;
import static de.walware.ecommons.text.ITokenScanner.OPENING_PEER;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;


/**
 * Helper class for match pairs of characters.
 */
public class PairMatcher implements ICharPairMatcher {
	
	
	private static final char IGNORE = '\n';
	
	private static final byte NOTHING_FOUND = 0;
	private static final byte OPENING_NOT_FOUND = 1;
	private static final byte CLOSING_NOT_FOUND = 2;
	private static final byte PAIR_FOUND = 3;
	
	
	protected final char[][] fPairs;
	protected final String fPartitioning;
	protected final String[] fApplicablePartitions;
	protected final char fEscapeChar;
	protected final ITokenScanner fScanner;
	
	protected int fOffset;
	
	protected int fBeginPos;
	protected int fEndPos;
	protected int fAnchor;
	protected String fPartition;
	
	
	public PairMatcher(final char[][] pairs, final String partitioning, final String[] partitions,
			final ITokenScanner scanner, final char escapeChar) {
		fPairs = pairs;
		fScanner = scanner;
		fPartitioning = partitioning;
		fApplicablePartitions = partitions;
		fEscapeChar = escapeChar;
	}
	
	/**
	 * Constructor using <code>BasicHeuristicTokenScanner</code>.
	 */
	public PairMatcher(final char[][] pairs, final PartitioningConfiguration partitioning, final String[] partitions, final char escapeChar) {
		this(pairs, partitioning.getPartitioning(), partitions, new BasicHeuristicTokenScanner(partitioning), escapeChar);
	}
	
	
	/**
	 * @return Returns the fPairs.
	 */
	public char[][] getPairs(final IDocument document, final int offset) {
		return fPairs;
	}
	
	@Override
	public IRegion match(final IDocument document, final int offset) {
		if (document == null || offset < 0) {
			return null;
		}
		fOffset = offset;
		if (matchPairsAt(document, true) == PAIR_FOUND) {
			return new Region(fBeginPos, fEndPos - fBeginPos + 1);
		}
		else {
			return null;
		}
	}
	
	@Override
	public IRegion match(final IDocument document, final int offset, final boolean auto) {
		if (document == null || offset < 0) {
			return null;
		}
		fOffset = offset;
		switch (matchPairsAt(document, auto)) {
		case OPENING_NOT_FOUND:
			return new Region(fEndPos, -1);
		case CLOSING_NOT_FOUND:
			return new Region(fBeginPos, -1);
		case PAIR_FOUND:
			return new Region(fBeginPos, fEndPos - fBeginPos + 1);
		default:
			return null;
		}
	}
	
	@Override
	public int getAnchor() {
		return fAnchor;
	}
	
	@Override
	public void dispose() {
		clear();
	}
	
	@Override
	public void clear() {
	}
	
	/**
	 * Search Pairs
	 * @param document 
	 * @param auto 
	 */
	protected byte matchPairsAt(final IDocument document, final boolean auto) {
		fBeginPos = -1;
		fEndPos = -1;
		
		// get the chars preceding and following the start position
		try {
			final ITypedRegion thisPartition = TextUtilities.getPartition(document, fPartitioning, fOffset, false);
			final ITypedRegion prevPartition = (fOffset > 0) ? TextUtilities.getPartition(document, fPartitioning, fOffset-1, false) : null;
			
			char thisChar = IGNORE;
			char prevChar = IGNORE;
			final int thisPart = checkPartition(thisPartition.getType());
			if (thisPart >= 0 && fOffset < document.getLength()) {
				thisChar = document.getChar(fOffset);
			}
			
			// check, if escaped
			int prevPart = -1;
			if (prevPartition != null) {
				prevPart = checkPartition(prevPartition.getType());
				if (auto && prevPart >= 0) {
					prevChar = document.getChar(fOffset-1);
					final int partitionOffset = prevPartition.getOffset();
					int checkOffset = fOffset-2;
					final char escapeChar = getEscapeChar(prevPartition.getType());
					while (checkOffset >= partitionOffset) {
						if (document.getChar(checkOffset) == escapeChar) {
							checkOffset--;
						}
						else {
							break;
						}
					}
					if ( (fOffset - checkOffset) % 2 == 1) {
						// prev char is escaped
						prevChar = IGNORE;
					}
					else if (prevPart == thisPart && prevChar == escapeChar) {
						// this char is escaped
						thisChar = IGNORE;
					}
				}
			}
			
			final int pairIdx = findChar(prevChar, prevPart, thisChar, thisPart);
			
			if (fBeginPos > -1) {		// closing peer
				fAnchor = LEFT;
				fScanner.configure(document, fPartition);
				fEndPos = fScanner.findClosingPeer(fBeginPos + 1, fPairs[pairIdx], getEscapeChar(fPartition));
				return (fEndPos > -1 && fBeginPos != fEndPos) ? PAIR_FOUND : CLOSING_NOT_FOUND;
			}
			else if (fEndPos > -1) {	// opening peer
				fAnchor = RIGHT;
				fScanner.configure(document, fPartition);
				fBeginPos = fScanner.findOpeningPeer(fEndPos - 1, fPairs[pairIdx], getEscapeChar(fPartition));
				return (fBeginPos > -1 && fBeginPos != fEndPos) ? PAIR_FOUND : OPENING_NOT_FOUND;
			}
			
		} catch (final BadLocationException x) {
		} // ignore
		
		return NOTHING_FOUND;
	}
	
	private int checkPartition(final String id) {
		for (int i = 0; i < fApplicablePartitions.length; i++) {
			if (fApplicablePartitions[i].equals(id)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * @param prevChar
	 * @param thisChar
	 * @return
	 */
	private int findChar(final char prevChar, final int prevPart, final char thisChar, final int thisPart) {
		// search order 3{2 1}4
		for (int i = 0; i < fPairs.length; i++) {
			if (thisChar == fPairs[i][CLOSING_PEER]) {
				fEndPos = fOffset;
				fPartition = fApplicablePartitions[thisPart];
				return i;
			}
		}
		for (int i = 0; i < fPairs.length; i++) {
			if (prevChar == fPairs[i][OPENING_PEER]) {
				fBeginPos = fOffset-1;
				fPartition = fApplicablePartitions[prevPart];
				return i;
			}
		}
		for (int i = 0; i < fPairs.length; i++) {
			if (thisChar == fPairs[i][OPENING_PEER]) {
				fBeginPos = fOffset;
				fPartition = fApplicablePartitions[thisPart];
				return i;
			}
		}
		for (int i = 0; i < fPairs.length; i++) {
			if (prevChar == fPairs[i][CLOSING_PEER]) {
				fEndPos = fOffset-1;
				fPartition = fApplicablePartitions[prevPart];
				return i;
			}
		}
		fPartition = null;
		return -1;
	}
	
	protected char getEscapeChar(final String contentType) {
		return fEscapeChar;
	}
	
}
