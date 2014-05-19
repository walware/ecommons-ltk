/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Point;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.ast.AstSelection;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class AssistInvocationContext implements IQuickAssistInvocationContext, IRegion {
	
	
	private final ISourceEditor editor;
	private final SourceViewer sourceViewer;
	
	private final ISourceUnit sourceUnit;
	private AstInfo astInfo;
	private ISourceUnitModelInfo modelInfo;
	
	private AstSelection invocationAstSelection;
	private AstSelection astSelection;
	
	private final int invocationOffset;
	private final int selectionOffset;
	private final int selectionLength;
	
	private String prefix;
	
	
	public AssistInvocationContext(final ISourceEditor editor, final int offset,
			final int synch, final IProgressMonitor monitor) {
		this.editor= editor;
		
		this.sourceViewer= editor.getViewer();
		this.sourceUnit= editor.getSourceUnit();
		
		this.invocationOffset= offset;
		final Point selectedRange= this.sourceViewer.getSelectedRange();
		this.selectionOffset= selectedRange.x;
		this.selectionLength= selectedRange.y;
		
		init(synch, monitor);
	}
	
	public AssistInvocationContext(final ISourceEditor editor, final IRegion region,
			final int synch, final IProgressMonitor monitor) {
		if (region.getOffset() < 0 || region.getLength() < 0) {
			throw new IllegalArgumentException("region"); //$NON-NLS-1$
		}
		this.editor= editor;
		
		this.sourceViewer= editor.getViewer();
		this.sourceUnit= editor.getSourceUnit();
		
		this.invocationOffset= region.getOffset();
		this.selectionOffset= region.getOffset();
		this.selectionLength= region.getLength();
		
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
	
	protected String getModelTypeId() {
		return null;
	}
	
	
	/**
	 * Returns the invocation (cursor) offset.
	 * 
	 * @return the invocation offset
	 */
	public final int getInvocationOffset() {
		return this.invocationOffset;
	}
	
	public ISourceEditor getEditor() {
		return this.editor;
	}
	
	@Override
	public SourceViewer getSourceViewer() {
		return this.sourceViewer;
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
	
	/**
	 * Returns the text selection length
	 * 
	 * @return length of selection (>= 0)
	 */
	@Override
	public int getLength() {
		return this.selectionLength;
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
	
	public String getIdentifierPrefix() {
		if (this.prefix == null) {
			this.prefix= computeIdentifierPrefix(getInvocationOffset());
			if (this.prefix == null) {
				this.prefix= ""; // prevent recomputing //$NON-NLS-1$
			}
		}
		return this.prefix;
	}
	
	/**
	 * Computes the identifier (as specified by {@link Character#isJavaIdentifierPart(char)}) that
	 * immediately precedes the invocation offset.
	 * 
	 * @return the prefix preceding the content assist invocation offset, <code>null</code> if
	 *     there is no document
	 */
	protected String computeIdentifierPrefix(final int offset) {
		return null;
	}
	
}
