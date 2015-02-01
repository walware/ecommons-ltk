/*=============================================================================#
 # Copyright (c) 2000-2015 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;


/**
 * An enhanced implementation of the <code>ICompletionProposal</code> interface implementing
 * the extension interfaces 1-2.
 * <p>
 * It uses a position to track its replacement offset and length. The position must be set up
 * externally.</p>
 */
public final class PositionBasedCompletionProposal implements ICompletionProposal, ICompletionProposalExtension, ICompletionProposalExtension2 {
	
	
	/** The string to be displayed in the completion proposal popup */
	private final String fDisplayString;
	/** The replacement string */
	private final String fReplacementString;
	/** The replacement position. */
	private final Position fReplacementPosition;
	/** The cursor position after this proposal has been applied */
	private final int fCursorPosition;
	/** The image to be displayed in the completion proposal popup */
	private final Image fImage;
	/** The context information of this proposal */
	private final IContextInformation fContextInformation;
	/** The additional info of this proposal */
	private final String fAdditionalProposalInfo;
	
	
	/**
	 * Creates a new completion proposal based on the provided information.  The replacement string is
	 * considered being the display string too. All remaining fields are set to <code>null</code>.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementPosition the position of the text to be replaced
	 * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
	 */
	public PositionBasedCompletionProposal(final String replacementString, final Position replacementPosition, final int cursorPosition) {
		this(replacementString, replacementPosition, cursorPosition, null, null, null, null);
	}
	
	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementPosition the position of the text to be replaced
	 * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal
	 * @param contextInformation the context information associated with this proposal
	 * @param additionalProposalInfo the additional information associated with this proposal
	 */
	public PositionBasedCompletionProposal(final String replacementString, final Position replacementPosition, final int cursorPosition, final Image image, final String displayString, final IContextInformation contextInformation, final String additionalProposalInfo) {
		Assert.isNotNull(replacementString);
		Assert.isTrue(replacementPosition != null);
		
		fReplacementString = replacementString;
		fReplacementPosition = replacementPosition;
		fCursorPosition = cursorPosition;
		fImage = image;
		fDisplayString = displayString;
		fContextInformation = contextInformation;
		fAdditionalProposalInfo = additionalProposalInfo;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void selected(final ITextViewer viewer, final boolean smartToggle) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unselected(final ITextViewer viewer) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidFor(final IDocument document, final int offset) {
		// not called anymore
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
		try {
			final String content = document.get(fReplacementPosition.getOffset(), offset - fReplacementPosition.getOffset());
			if (fReplacementString.startsWith(content)) {
				return true;
			}
		} catch (final BadLocationException e) {
			// ignore concurrently modified document
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public char[] getTriggerCharacters() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Image getImage() {
		return fImage;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayString() {
		if (fDisplayString != null) {
			return fDisplayString;
		}
		return fReplacementString;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAdditionalProposalInfo() {
		return fAdditionalProposalInfo;
	}
	
	
	@Override
	public void apply(final IDocument document, final char trigger, final int offset) {
	}
	
	@Override
	public void apply(final IDocument document) {
		try {
			document.replace(fReplacementPosition.getOffset(), fReplacementPosition.getLength(), fReplacementString);
		} catch (final BadLocationException x) {
			// ignore
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void apply(final ITextViewer viewer, final char trigger, final int stateMask, final int offset) {
		apply(viewer.getDocument());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Point getSelection(final IDocument document) {
		return new Point(fReplacementPosition.getOffset() + fCursorPosition, 0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IContextInformation getContextInformation() {
		return fContextInformation;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getContextInformationPosition() {
		return fReplacementPosition.getOffset();
	}
	
}
