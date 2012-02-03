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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Point;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.ast.AstSelection;


public class AssistInvocationContext implements IQuickAssistInvocationContext, IRegion {
	
	
	private final ISourceEditor fEditor;
	private final SourceViewer fSourceViewer;
	
	private final ISourceUnit fSourceUnit;
	private AstInfo fAstInfo;
	private ISourceUnitModelInfo fModelInfo;
	private AstSelection fAstSelection;
	
	private final int fInvocationOffset;
	private final int fSelectionOffset;
	private final int fSelectionLength;
	
	private String fPrefix;
	
	
	public AssistInvocationContext(final ISourceEditor editor, final int offset, final int synch) {
		fEditor = editor;
		
		fSourceViewer = editor.getViewer();
		fSourceUnit = editor.getSourceUnit();
		
		fInvocationOffset = offset;
		final Point selectedRange = fSourceViewer.getSelectedRange();
		fSelectionOffset = selectedRange.x;
		fSelectionLength = selectedRange.y;
		
		init(synch);
	}
	
	public AssistInvocationContext(final ISourceEditor editor, final IRegion region, final int synch) {
		fEditor = editor;
		
		fSourceViewer = editor.getViewer();
		fSourceUnit = editor.getSourceUnit();
		
		fInvocationOffset = region.getOffset();
		fSelectionOffset = region.getOffset();
		fSelectionLength = region.getLength();
		
		init(synch);
	}
	
	private void init(final int synch) {
		if (fSourceUnit != null) {
			final NullProgressMonitor monitor = new NullProgressMonitor();
			final String type = null;
			// TODO check if/how we can reduce model requirement in content assistant
			fModelInfo = fSourceUnit.getModelInfo(type, synch, monitor);
			fAstInfo = fModelInfo != null ? fModelInfo.getAst() : fSourceUnit.getAstInfo(type, true, monitor);
			if (fAstInfo != null && fAstInfo.getRootNode() != null) {
				fAstSelection = AstSelection.search(fAstInfo.getRootNode(),
						getOffset(), getOffset(), AstSelection.MODE_COVERING_SAME_LAST );
			}
		}
	}
	
	
	/**
	 * Returns the invocation (cursor) offset.
	 * 
	 * @return the invocation offset
	 */
	public final int getInvocationOffset() {
		return fInvocationOffset;
	}
	
	public ISourceEditor getEditor() {
		return fEditor;
	}
	
	@Override
	public SourceViewer getSourceViewer() {
		return fSourceViewer;
	}
	
	/**
	 * Returns the text selection offset.
	 * 
	 * @return offset of selection
	 */
	@Override
	public int getOffset() {
		return fSelectionOffset;
	}
	
	/**
	 * Returns the text selection length
	 * 
	 * @return length of selection (>= 0)
	 */
	@Override
	public int getLength() {
		return fSelectionLength;
	}
	
	
	public ISourceUnit getSourceUnit() {
		return fSourceUnit;
	}
	
	public AstInfo getAstInfo() {
		return fAstInfo;
	}
	
	public ISourceUnitModelInfo getModelInfo() {
		return fModelInfo;
	}
	
	public AstSelection getAstSelection() {
		return fAstSelection;
	}
	
	public String getIdentifierPrefix() {
		if (fPrefix == null) {
			fPrefix = computeIdentifierPrefix(getInvocationOffset());
			if (fPrefix == null) {
				fPrefix = ""; // prevent recomputing //$NON-NLS-1$
			}
		}
		return fPrefix;
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
