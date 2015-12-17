/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import de.walware.ecommons.ui.SharedUIResources;


/**
 * Proposal completing a given replacement string.
 */
public abstract class SimpleCompletionProposal extends CompletionProposalWithOverwrite {
	
	
	/** The replacement string. */
	private final String replacementString;
	
	/** The cursor position after this proposal has been applied. */
	private int cursorPosition= -1;
	
	
	public SimpleCompletionProposal(final AssistInvocationContext context, final String replacementString, final int replacementOffset) {
		super(context, replacementOffset);
		this.replacementString= replacementString;
	}
	
	
	protected final String getReplacementString() {
		return this.replacementString;
	}
	
	protected final void setCursorPosition(final int offset) {
		this.cursorPosition= offset;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Image getImage() {
		return SharedUIResources.getImages().get(SharedUIResources.PLACEHOLDER_IMAGE_ID);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayString() {
		return getReplacementString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 * {@value}
	 */
	@Override
	public int getRelevance() {
		return 0;
	}
	
	@Override
	public String getSortingString() {
		return this.replacementString;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
		try {
			final int replacementOffset= getReplacementOffset();
			final String content= document.get(replacementOffset, offset - replacementOffset);
			if (this.replacementString.regionMatches(true, 0, content, 0, content.length())) {
				return true;
			}
		}
		catch (final BadLocationException e) {
			// ignore concurrently modified document
		}
		return false;
	}
	
	/**
	 * not supported, use {@link #apply(ITextViewer, char, int, int)}
	 */
	@Override
	public void apply(final IDocument document) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected void doApply(final char trigger, final int stateMask,
			final int caretOffset, final int replacementOffset, final int replacementLength) throws BadLocationException {
		final AssistInvocationContext context= getInvocationContext();
		final SourceViewer viewer= context.getSourceViewer();
		final IDocument document= viewer.getDocument();
		try {
			final String replacementString= getReplacementString();
			document.replace(replacementOffset, replacementLength, replacementString);
			setCursorPosition(replacementOffset + replacementString.length());
		}
		catch (final BadLocationException x) {
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Point getSelection(final IDocument document) {
		if (this.cursorPosition >= 0) {
			return new Point(this.cursorPosition, 0);
		}
		return null;
	}
	
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
	
}
