/*******************************************************************************
 * Copyright (c) 2008-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ui.IElementLabelProvider;


/**
 * Proposal completing a given {@link IElementName} of a element.
 */
public abstract class ElementNameCompletionProposal extends CompletionProposalWithOverwrite
		implements ICompletionProposalExtension6 {
	
	
	protected final IElementName fReplacementName;
	
	protected final IModelElement fElement;
	
	private final int fRelevance;
	
	private final IElementLabelProvider fLabelProvider;
	
	/** The additional info of this proposal. */
	private String fAdditionalProposalInfo;
	
	/** The cursor position after this proposal has been applied. */
	private int fCursorPosition = -1;
	
	
	public ElementNameCompletionProposal(final AssistInvocationContext context, 
			final IElementName replacementName, final int replacementOffset,
			final IModelElement element, final int relevance, 
			final IElementLabelProvider labelProvider) {
		super(context, replacementOffset);
		fReplacementName = replacementName;
		fElement = element;
		fLabelProvider = labelProvider;
		fRelevance = relevance;
	}
	
	
	protected IElementLabelProvider getLabelProvider() {
		return fLabelProvider;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Image getImage() {
		return fLabelProvider.getImage(fElement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayString() {
		return fLabelProvider.getText(fElement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public StyledString getStyledDisplayString() {
		return fLabelProvider.getStyledText(fElement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAdditionalProposalInfo() {
		return fAdditionalProposalInfo;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRelevance() {
		return fRelevance;
	}
	
	@Override
	public String getSortingString() {
		return fReplacementName.getSegmentName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
		try {
			final String content = document.get(getReplacementOffset(), offset - getReplacementOffset());
			if (fReplacementName.getSegmentName().regionMatches(true, 0, content, 0, content.length())) {
				return true;
			}
		}
		catch (final BadLocationException e) {
			// ignore concurrently modified document
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutoInsertable() {
		return false;
	}
	
	@Override
	protected void doApply(final char trigger, final int stateMask, final int caretOffset, final int replacementOffset, final int replacementLength) throws BadLocationException {
		final SourceViewer viewer = fContext.getSourceViewer();
		final IDocument document = viewer.getDocument();
		final StringBuilder replacement = new StringBuilder(fReplacementName.getDisplayName());
		document.replace(replacementOffset, replacementLength, replacement.toString());
		setCursorPosition(replacementOffset + replacement.length());
	}
	
	
	protected void setCursorPosition(final int offset) {
		fCursorPosition = offset;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * This implementation returns <code>null</code>
	 */
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Point getSelection(final IDocument document) {
		if (fCursorPosition >= 0) {
			return new Point(fCursorPosition, 0);
		}
		return null;
	}
	
	
	@Override
	public int hashCode() {
		return getClass().hashCode() * ((fReplacementName != null) ? fReplacementName.hashCode() : 564);
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ElementNameCompletionProposal other = (ElementNameCompletionProposal) obj;
		return (   ((fReplacementName != null) ? fReplacementName.equals(other.fReplacementName) : null == other.fReplacementName)
				&& ((fElement != null) ? fElement.equals(other.fElement) : null == other.fElement) );
	}
	
}
