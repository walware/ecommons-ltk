/*=============================================================================#
 # Copyright (c) 2007-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.ui;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;


/**
 * Linked mode exit policy for auto inserted pairs like brackets or quotes.
 */
public abstract class BracketLevel implements IExitPolicy {
	
	
	public static final int CONSOLE_MODE =                  0x00000001;
	public static final int AUTODELETE =                    0x01000000;
	
	
	public static abstract class InBracketPosition extends LinkedPosition {
		
		
		public InBracketPosition(
				final IDocument document, final int offset, final int length, final int sequence) {
			super(document, offset, length, sequence);
		}
		
		
		public abstract char getOpenChar();
		
		public abstract char getCloseChar();
		
		
		/**
		 * Whether CR key event should insert new line
		 * 
		 * Default returns always <code>true</code>.
		 * 
		 * @param charOffset event offset
		 * @return 
		 * @throws BadLocationException
		 */
		protected boolean insertCR(final int charOffset) throws BadLocationException {
			return true;
		}
		
		protected boolean isEscaped(final int offset) throws BadLocationException {
			return false;
		}
		
		/**
		 * If the char is part of the existing end of the language element
		 * (closing bracket).
		 * If <code>true</code>, the input is ignored but the caret is updated.
		 * 
		 * @param level the bracket level
		 * @param charOffset event offset
		 * @return <code>true</code> if valid close offset otherwise <code>false</code>
		 * @throws BadLocationException
		 */
		public boolean matchesOpen(final BracketLevel level, final int offset, final char character)
				throws BadLocationException {
			return (getOffset() == offset + 1 && getOpenChar() == character
					&& !isEscaped(offset) );
		}
		
		/**
		 * If the char is part of the existing end of the language element
		 * (closing bracket).
		 * If <code>true</code>, the input is ignored but the caret is updated.
		 * 
		 * @param level the bracket level
		 * @param charOffset event offset
		 * @return <code>true</code> if valid close offset otherwise <code>false</code>
		 * @throws BadLocationException
		 */
		public boolean matchesClose(final BracketLevel level, final int offset, final char character)
				throws BadLocationException {
			return (getOffset() + getLength() == offset && getCloseChar() == character
					&& level.getPartitionType(getOffset()) == level.getPartitionType(offset)
					&& !isEscaped(offset) );
		}
		
	}
	
	
	private final List<LinkedPosition> fPositions;
	
	protected final IDocument fDocument;
	protected final String fPartitioning;
	protected final int fMode;
	
	
	public BracketLevel(final IDocument document, final String partitioning,
			final List<LinkedPosition> positions, final int mode) {
		fPositions = positions;
		fDocument = document;
		fPartitioning = partitioning;
		fMode = mode;
	}
	
	
	@Override
	public ExitFlags doExit(final LinkedModeModel model, final VerifyEvent event,
			final int offset, final int length) {
		try {
			final int posIdx = getPositionIdx(offset);
			final InBracketPosition inPos = (posIdx >= 0 && fPositions.get(posIdx) instanceof InBracketPosition) ?
					(InBracketPosition) fPositions.get(posIdx) : null;
			switch (event.character) {
			case 0x0A: // cr
			case 0x0D:
				if ((fMode & CONSOLE_MODE) != 0) {
					return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
				}
				if (length > 0 || (inPos != null && inPos.insertCR(offset))) {
					return new ExitFlags(ILinkedModeListener.NONE, true);
				}
				return null;
			case SWT.BS: // backspace
				if (length == 0 && (fMode & AUTODELETE) != 0 
						&& inPos != null && inPos.getOffset() == offset && inPos.getLength() == 0) {
					int count = 2;
					for (int i = posIdx + 1; i < fPositions.size(); i++) {
						if (fPositions.get(i) instanceof InBracketPosition) {
							if (fPositions.get(i).getOffset() == offset + count
									&& fPositions.get(i).getLength() == 0) {
								count += 2;
							}
							else {
								break;
							}
						}
					}
					fDocument.replace(offset-1, count, ""); //$NON-NLS-1$
					return new ExitFlags(ILinkedModeListener.NONE, false);
				}
				return null;
			}
			// don't enter the character if if its the closing peer
			if (length == 0 && inPos != null && inPos.matchesClose(this, offset, event.character) ) {
				skipChars(event, 1);
				if (posIdx == fPositions.size() - 1) {
					return new ExitFlags(ILinkedModeListener.NONE, false);
				}
				else {
					return null;
				}
			}
			// don't enter the character if if its the opening peer
			if (length == 0
					&& (posIdx < 0 || !fPositions.get(posIdx).includes(offset + 1)) ) {
				final int nextIdx = getPositionIdx(offset + 1);
				if (nextIdx > 0 && fPositions.get(nextIdx) instanceof InBracketPosition
						&& ((InBracketPosition) fPositions.get(nextIdx)).matchesOpen(this, offset, event.character) ) {
					skipChars(event, 1);
					return null;
				}
			}
		}
		catch (final BadLocationException e) {
		}
		return null;
	}
	
	private void skipChars(final VerifyEvent event, final int n) {
		event.doit = false;
		final StyledText styledText = (StyledText) event.widget;
		styledText.setSelection(styledText.getCaretOffset() + n);
	}
	
	protected int getPositionIdx(final int offset) {
		for (int i = 0; i < fPositions.size(); i++) {
			if (fPositions.get(i).includes(offset)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Utility method returning the partition type at the given offset
	 * 
	 * @param offset
	 * @return the partition type
	 * @throws BadLocationException
	 */
	public String getPartitionType(final int offset) throws BadLocationException {
		return TextUtilities.getPartition(fDocument, fPartitioning, offset, true).getType();
	}
	
}
