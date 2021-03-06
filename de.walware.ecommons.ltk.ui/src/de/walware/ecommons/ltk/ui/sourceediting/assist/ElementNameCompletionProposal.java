/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.Objects;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.ui.IElementLabelProvider;


/**
 * Proposal completing a given {@link IElementName} of a element.
 */
public abstract class ElementNameCompletionProposal<E extends IModelElement>
		extends CompletionProposalWithOverwrite
		implements ICompletionProposalExtension3, ICompletionProposalExtension6 {
	
	
	private final IElementName replacementName;
	
	private final E element;
	
	private final int relevance;
	
	private final IElementLabelProvider labelProvider;
	
	/** The cursor position after this proposal has been applied. */
	private int cursorPosition= -1;
	
	
	public ElementNameCompletionProposal(final AssistInvocationContext context, 
			final IElementName replacementName, final int replacementOffset,
			final E element, final int relevance, 
			final IElementLabelProvider labelProvider) {
		super(context, replacementOffset);
		this.replacementName= replacementName;
		this.element= element;
		this.labelProvider= labelProvider;
		this.relevance= relevance;
	}
	
	
	public final E getElement() {
		return this.element;
	}
	
	protected IElementLabelProvider getLabelProvider() {
		return this.labelProvider;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Image getImage() {
		return this.labelProvider.getImage(getElement());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayString() {
		return this.labelProvider.getText(getElement());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public StyledString getStyledDisplayString() {
		return this.labelProvider.getStyledText(getElement());
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
	 */
	@Override
	public int getRelevance() {
		return this.relevance;
	}
	
	
	public IElementName getReplacementName() {
		return this.replacementName;
	}
	
	@Override
	public String getSortingString() {
		return getReplacementName().getSegmentName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
		try {
			final String content= document.get(getReplacementOffset(), offset - getReplacementOffset());
			if (this.getReplacementName().getSegmentName().regionMatches(true, 0, content, 0, content.length())) {
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
	protected void doApply(final char trigger, final int stateMask, final int caretOffset,
			final int replacementOffset, final int replacementLength) throws BadLocationException {
		final AssistInvocationContext context= getInvocationContext();
		final SourceViewer viewer= context.getSourceViewer();
		final IDocument document= viewer.getDocument();
		
		final StringBuilder replacement= new StringBuilder(this.getReplacementName().getDisplayName());
		document.replace(replacementOffset, replacementLength, replacement.toString());
		setCursorPosition(replacementOffset + replacement.length());
	}
	
	
	protected void setCursorPosition(final int offset) {
		this.cursorPosition= offset;
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
		if (this.cursorPosition >= 0) {
			return new Point(this.cursorPosition, 0);
		}
		return null;
	}
	
	
	@Override
	public int getPrefixCompletionStart(final IDocument document, final int offset) {
		return getReplacementOffset();
	}
	
	@Override
	public CharSequence getPrefixCompletionText(final IDocument document, final int offset) {
		return null;
	}
	
	@Override
	public IInformationControlCreator getInformationControlCreator() {
		return null;
	}
	
	
	@Override
	public int hashCode() {
		return getClass().hashCode() * Objects.hashCode(getReplacementName());
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() == obj.getClass()) {
			final ElementNameCompletionProposal<?> other= (ElementNameCompletionProposal<?>) obj;
			return (Objects.equals(getReplacementName(), other.getReplacementName())
					&& Objects.equals(getElement(), other.getElement()) );
		}
		return false;
	}
	
}
