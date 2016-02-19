/*=============================================================================#
 # Copyright (c) 2007-2016 Stephan Wahlbrink (WalWare.de) and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.text.core.DocumentEnhancer;
import de.walware.ecommons.text.core.IDocumentEnhancement;
import de.walware.ecommons.text.core.sections.IDocContentSections;
import de.walware.ecommons.text.core.util.ExclusivePositionUpdater;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


/**
 * Linked mode exit policy for auto inserted pairs like brackets or quotes.
 */
public abstract class BracketLevel implements IExitPolicy, ILinkedModeListener {
	
	
	public static final int CONSOLE_MODE=                   0x00000001;
	public static final int AUTODELETE=                     1 << 24;
	
	
	private static final String POSITION_CATEGORY= "de.walware.ecommons.text.ui.BracketLevel"; //$NON-NLS-1$
	private static final IPositionUpdater POSITION_UPDATER= new ExclusivePositionUpdater(POSITION_CATEGORY);
	
	private static class DocumentData {
		
		private int counter;
		
	}
	
	
	public static abstract class InBracketPosition extends LinkedPosition {
		
		
		private Position openPos;
		private Position closePos;
		
		
		public InBracketPosition(
				final IDocument document, final int offset, final int length, final int sequence) {
			super(document, offset, length, sequence);
		}
		
		
		public abstract char getOpenChar();
		
		public abstract char getCloseChar();
		
		
		void createOpenClosePositions(final IDocument document)
				throws BadPositionCategoryException, BadLocationException {
			{	final Position pos= new Position(getOffset() - 1, 1);
				assert (document.getChar(pos.getOffset()) == getOpenChar());
				document.addPosition(POSITION_CATEGORY, pos);
				this.openPos= pos;
			}
			{	final Position pos= new Position(getOffset() + getLength(), 1);
				assert (document.getChar(pos.getOffset()) == getCloseChar());
				document.addPosition(POSITION_CATEGORY, pos);
				this.closePos= pos;
			}
		}
		
		boolean hasOpenClosePositions() {
			return (this.openPos != null && this.closePos != null);
		}
		
		Position getOpenPosition() {
			return this.openPos;
		}
		
		Position getClosePosition() {
			return this.closePos;
		}
		
		
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
	
	
	private final IDocument document;
	private final IDocContentSections documentContentInfo;
	
	private final List<LinkedPosition> positions;
	private final int mode;
	
	private boolean hasOwnPositions;
	private DocumentData documentData;
	
	
	public BracketLevel(final LinkedModeModel model,
			final IDocument document, final IDocContentSections documentContentInfo,
			final List<LinkedPosition> positions, final int mode) {
		this.document= document;
		this.documentContentInfo= documentContentInfo;
		
		this.positions= positions;
		this.mode= mode;
		
		init(model);
	}
	
	protected void init(final LinkedModeModel model) {
		if ((this.mode & AUTODELETE) != 0 && this.document instanceof IDocumentExtension) {
			this.documentData= fetchDocumentData();
			if (this.documentData != null) {
				try {
					model.addLinkingListener(this);
					
					setupPositionCategory();
					
					for (final LinkedPosition position : this.positions) {
						if (position instanceof InBracketPosition) {
							final InBracketPosition inPos= (InBracketPosition) position;
							inPos.createOpenClosePositions(this.document);
						}
					}
				}
				catch (final Exception e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
							"An error occurred when preparing auto-undo of autoedit insertions.",
							e ));
				}
			}
		}
	}
	
	
	private void setupPositionCategory() {
		this.hasOwnPositions= true;
		this.documentData.counter++;
		
		if (!this.document.containsPositionCategory(POSITION_CATEGORY)) {
			this.document.addPositionCategory(POSITION_CATEGORY);
			this.document.addPositionUpdater(POSITION_UPDATER);
		}
	}
	
	private void disposePositionCategory() {
		if (this.document.containsPositionCategory(POSITION_CATEGORY)) {
			try {
				this.document.removePositionCategory(POSITION_CATEGORY);
				this.document.removePositionUpdater(POSITION_UPDATER);
			}
			catch (final BadPositionCategoryException e) {}
		}
	}
	
	protected final DocumentData fetchDocumentData() {
		final IDocumentEnhancement documentEnhancement= DocumentEnhancer.get(this.document);
		if (documentEnhancement != null) {
			DocumentData data= (DocumentData) documentEnhancement.getData(POSITION_CATEGORY);
			if (data == null) {
				data= new DocumentData();
				documentEnhancement.setData(POSITION_CATEGORY, data);
			}
			return data;
		}
		return null;
	}
	
	
	protected final int getPositionIdx(final int offset) {
		for (int i= 0; i < this.positions.size(); i++) {
			if (this.positions.get(i).includes(offset)) {
				return i;
			}
		}
		return -1;
	}
	
	
	@Override
	public ExitFlags doExit(final LinkedModeModel model, final VerifyEvent event,
			final int offset, final int length) {
		try {
			final int posIdx= getPositionIdx(offset);
			final InBracketPosition inPos= (posIdx >= 0 && this.positions.get(posIdx) instanceof InBracketPosition) ?
					(InBracketPosition) this.positions.get(posIdx) : null;
			switch (event.character) {
			case 0x0A: // cr
			case 0x0D:
				if ((this.mode & CONSOLE_MODE) != 0) {
					return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
				}
				if (length > 0 || (inPos != null && inPos.insertCR(offset))) {
					return new ExitFlags(ILinkedModeListener.NONE, true);
				}
				return null;
			case SWT.BS: // backspace
				if ((this.mode & AUTODELETE) != 0
						&& !this.hasOwnPositions
						&& length == 0
						&& inPos != null && inPos.getOffset() == offset && inPos.getLength() == 0) {
					int count= 2;
					for (int i= posIdx + 1; i < this.positions.size(); i++) {
						final LinkedPosition position= this.positions.get(i);
						if (position instanceof InBracketPosition) {
							if (position.getOffset() == offset + count
									&& position.getLength() == 0) {
								count += 2;
							}
							else {
								break;
							}
						}
					}
					this.document.replace(offset - 1, count, ""); //$NON-NLS-1$
					return new ExitFlags(ILinkedModeListener.NONE, false);
				}
				return null;
			}
			// don't enter the character if if its the closing peer
			if (length == 0 && inPos != null && inPos.matchesClose(this, offset, event.character) ) {
				skipChars(event, 1);
				if (posIdx == this.positions.size() - 1) {
					return new ExitFlags(ILinkedModeListener.NONE, false);
				}
				else {
					return null;
				}
			}
			// don't enter the character if if its the opening peer
			if (length == 0
					&& (posIdx < 0 || !this.positions.get(posIdx).includes(offset + 1)) ) {
				final int nextIdx= getPositionIdx(offset + 1);
				if (nextIdx > 0 && this.positions.get(nextIdx) instanceof InBracketPosition
						&& ((InBracketPosition) this.positions.get(nextIdx)).matchesOpen(this, offset, event.character) ) {
					skipChars(event, 1);
					return null;
				}
			}
		}
		catch (final BadLocationException e) {
		}
		return null;
	}
	
	@Override
	public void resume(final LinkedModeModel model, final int flags) {
	}
	
	@Override
	public void suspend(final LinkedModeModel model) {
	}
	
	@Override
	public void left(final LinkedModeModel model, final int flags) {
		if ((this.mode & AUTODELETE) != 0
				&& this.hasOwnPositions
				&& flags == ILinkedModeListener.EXTERNAL_MODIFICATION) {
			((IDocumentExtension) this.document).registerPostNotificationReplace(null, new IDocumentExtension.IReplace() {
				@Override
				public void perform(final IDocument document, final IDocumentListener owner) {
					checkOwnPositionCleanup();
					
					try {
						int beginOffset= -1;
						int endOffset= -1;
						for (final LinkedPosition position : BracketLevel.this.positions) {
							if (position instanceof InBracketPosition) {
								final InBracketPosition inPos= (InBracketPosition) position;
								if (!inPos.hasOpenClosePositions()) {
									break;
								}
								if (beginOffset < 0) {
									if ((inPos.getOpenPosition().isDeleted() || inPos.getOpenPosition().getLength() == 0)
											&& (inPos.isDeleted() || inPos.getLength() == 0)
											&& !inPos.getClosePosition().isDeleted() ) {
										beginOffset= inPos.getClosePosition().getOffset();
										endOffset= inPos.getClosePosition().getOffset() + 1;
									}
								}
								else {
									if (inPos.getOpenPosition().getOffset() == endOffset
											&& !inPos.getOpenPosition().isDeleted()
											&& (inPos.isDeleted() || inPos.getLength() == 0)
											&& !inPos.getClosePosition().isDeleted() ) {
										endOffset= inPos.getClosePosition().getOffset() + 1;
									}
									else {
										break;
									}
								}
							}
						}
						if (beginOffset >= 0 && endOffset > beginOffset) {
								document.replace(beginOffset, endOffset - beginOffset, ""); //$NON-NLS-1$
						}
					}
					catch (final Exception e) {
						StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
								"An error occurred when performing auto-undo of autoedit insertions.",
								e ));
					}
				}
			});
			return;
		}
		
		checkOwnPositionCleanup();
	}
	
	private void checkOwnPositionCleanup() {
		if (this.hasOwnPositions) {
			this.documentData.counter--;
			
			if (this.documentData.counter == 0) {
				disposePositionCategory();
			}
		}
	}
	
	private void skipChars(final VerifyEvent event, final int n) {
		event.doit= false;
		final StyledText styledText= (StyledText) event.widget;
		styledText.setSelection(styledText.getCaretOffset() + n);
	}
	
	
	/**
	 * Utility method returning the partition type at the given offset
	 * 
	 * @param offset
	 * @return the partition type
	 * @throws BadLocationException
	 */
	public String getPartitionType(final int offset) throws BadLocationException {
		return TextUtilities.getPartition(this.document, this.documentContentInfo.getPartitioning(),
				offset, true).getType();
	}
	
}
