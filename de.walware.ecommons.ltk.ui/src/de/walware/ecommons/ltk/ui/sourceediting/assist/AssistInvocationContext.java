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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Point;

import de.walware.ecommons.text.core.ITextRegion;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.ast.AstSelection;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class AssistInvocationContext implements IQuickAssistInvocationContext, ITextRegion {
	
	
	private final ISourceEditor editor;
	private final SourceViewer sourceViewer;
	
	private final int invocationOffset;
	private final int selectionOffset;
	private final int selectionLength;
	
	private final String invocationContentType;
	
	private String invocationPrefix;
	
	private final ISourceUnit sourceUnit;
	private AstInfo astInfo;
	private ISourceUnitModelInfo modelInfo;
	
	private AstSelection invocationAstSelection;
	private AstSelection astSelection;
	
	int session;
	
	
	public AssistInvocationContext(final ISourceEditor editor,
			final int offset, final String contentType,
			final int synch, final IProgressMonitor monitor) {
		this.editor= editor;
		
		this.sourceViewer= editor.getViewer();
		
		this.invocationOffset= offset;
		final Point selectedRange= this.sourceViewer.getSelectedRange();
		this.selectionOffset= selectedRange.x;
		this.selectionLength= selectedRange.y;
		
		this.invocationContentType= contentType;
		
		this.sourceUnit= editor.getSourceUnit();
		
		init(synch, monitor);
	}
	
	public AssistInvocationContext(final ISourceEditor editor,
			final IRegion region, final String contentType,
			final int synch, final IProgressMonitor monitor) {
		if (region.getOffset() < 0 || region.getLength() < 0) {
			throw new IllegalArgumentException("region"); //$NON-NLS-1$
		}
		this.editor= editor;
		
		this.sourceViewer= editor.getViewer();
		
		this.invocationOffset= region.getOffset();
		this.selectionOffset= region.getOffset();
		this.selectionLength= region.getLength();
		
		this.invocationContentType= contentType;
		
		this.sourceUnit= editor.getSourceUnit();
		
		init(synch, monitor);
	}
	
	private void init(final int synch, final IProgressMonitor monitor) {
		if (this.sourceUnit != null) {
			final String type= getModelTypeId();
			// TODO check if/how we can reduce model requirement in content assistant
			this.modelInfo= this.sourceUnit.getModelInfo(type, synch, monitor);
			this.astInfo= this.modelInfo != null ? this.modelInfo.getAst() : this.sourceUnit.getAstInfo(type, true, monitor);
		}
	}
	
	
	boolean isInitialState() {
		final Point selectedRange= this.sourceViewer.getSelectedRange();
		return (selectedRange.x == getOffset() && selectedRange.y == getLength());
	}
	
	protected boolean reuse(final ISourceEditor editor, final int offset) {
		return (this.editor == editor
				&& this.invocationOffset == offset
				&& isInitialState() );
	}
	
	
	protected String getModelTypeId() {
		return null;
	}
	
	
	public ISourceEditor getEditor() {
		return this.editor;
	}
	
	@Override
	public SourceViewer getSourceViewer() {
		return this.sourceViewer;
	}
	
	public IDocument getDocument() {
		return getSourceViewer().getDocument();
	}
	
	
	/**
	 * Returns the invocation (cursor) offset.
	 * 
	 * @return the invocation offset
	 */
	public final int getInvocationOffset() {
		return this.invocationOffset;
	}
	
	/**
	 * Returns the text selection offset.
	 * 
	 * @return offset of selection
	 */
	@Override
	public int getOffset() {
		return this.selectionOffset;
	}
	
	@Override
	public int getEndOffset() {
		return this.selectionOffset +  this.selectionLength;
	}
	
	/**
	 * Returns the text selection length
	 * 
	 * @return length of selection (>= 0)
	 */
	@Override
	public int getLength() {
		return this.selectionLength;
	}
	
	
	public final String getInvocationContentType() {
		return this.invocationContentType;
	}
	
	
	public String getIdentifierPrefix() {
		if (this.invocationPrefix == null) {
			try {
				this.invocationPrefix= computeIdentifierPrefix(getInvocationOffset());
				if (this.invocationPrefix == null) {
					this.invocationPrefix= ""; // prevent recomputing //$NON-NLS-1$
				}
			}
			catch (final BadPartitioningException | BadLocationException e) {
				this.invocationPrefix= ""; //$NON-NLS-1$
				throw new RuntimeException(e);
			}
		}
		return this.invocationPrefix;
	}
	
	public int getIdentifierOffset() {
		return getInvocationOffset() - getIdentifierPrefix().length();
	}
	
	/**
	 * Computes the prefix separated by a white space ( {@link Character#isWhitespace(char)}
	 * immediately precedes the invocation offset.
	 * 
	 * @return the prefix preceding the content assist invocation offset, <code>null</code> if
	 *     there is no document
	 */
	protected String computeIdentifierPrefix(final int offset)
			throws BadPartitioningException, BadLocationException {
		final IDocument document= getDocument();
		
		final ITypedRegion partition= TextUtilities.getPartition(document,
				getEditor().getDocumentContentInfo().getPartitioning(), offset, true );
		final int bound= partition.getOffset();
		int prefixOffset= offset;
		for (; prefixOffset > bound; prefixOffset--) {
			if (Character.isWhitespace(document.getChar(prefixOffset - 1))) {
				break;
			}
		}
		
		return document.get(prefixOffset, offset - prefixOffset);
	}
	
	
	public ISourceUnit getSourceUnit() {
		return this.sourceUnit;
	}
	
	public AstInfo getAstInfo() {
		return this.astInfo;
	}
	
	public ISourceUnitModelInfo getModelInfo() {
		return this.modelInfo;
	}
	
	public AstSelection getInvocationAstSelection() {
		if (this.invocationAstSelection == null && this.astInfo != null && this.astInfo.root != null) {
			this.invocationAstSelection= AstSelection.search(this.astInfo.root,
					getInvocationOffset(), getInvocationOffset(), AstSelection.MODE_COVERING_SAME_LAST );
		}
		return this.invocationAstSelection;
	}
	
	public AstSelection getAstSelection() {
		if (this.astSelection == null && this.astInfo != null && this.astInfo.root != null) {
			this.astSelection= AstSelection.search(this.astInfo.root,
					getOffset(), getOffset() + getLength(), AstSelection.MODE_COVERING_SAME_LAST );
		}
		return this.astSelection;
	}
	
	
}
