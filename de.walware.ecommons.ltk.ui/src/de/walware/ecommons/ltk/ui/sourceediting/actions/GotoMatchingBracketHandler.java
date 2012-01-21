/*******************************************************************************
 * Copyright (c) 2006-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import de.walware.ecommons.text.ICharPairMatcher;

import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class GotoMatchingBracketHandler extends AbstractHandler {
	
	
	private final ISourceEditor fSourceEditor;
	
	private final ICharPairMatcher fPairMatcher;
	
	
	public GotoMatchingBracketHandler(final ICharPairMatcher pairMatcher, final ISourceEditor editor) {
		assert (pairMatcher != null);
		assert (editor != null);
		fSourceEditor = editor;
		fPairMatcher = pairMatcher;
		
//		setBaseEnabled(true);
	}
	
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		gotoMatchingBracket();
		
		return null;
	}
	
	/**
	 * Jumps to the matching bracket.
	 */
	public void gotoMatchingBracket() {
		final ISourceViewer sourceViewer = fSourceEditor.getViewer();
		if (sourceViewer == null) {
			return;
		}
		final IDocument document = sourceViewer.getDocument();
		
		final ITextSelection selection = (ITextSelection) sourceViewer.getSelectionProvider().getSelection();
		final int offset = selection.getOffset();
		final int selectionLength = selection.getLength();
		
		final IRegion region = (selectionLength <= 1) ?
				fPairMatcher.match(document, offset, (selection.getLength() == 0)): null;
		{	String message = null;
			if (region == null) {
				message = EditingMessages.GotoMatchingBracketAction_error_InvalidSelection;
			}
			else if (region.getLength() < 2) {
				if (region.getLength() >= 0) {
					return; // invalid
				}
				message = EditingMessages.GotoMatchingBracketAction_error_NoMatchingBracket;
			}
			if (message != null) {
				final IEditorStatusLine statusLine = (IEditorStatusLine) fSourceEditor.getAdapter(IEditorStatusLine.class);
				if (statusLine != null) {
					statusLine.setMessage(true, message, null);
				}
				Display.getCurrent().beep();
				return;
			}
		}
		
		{	final int targetOffset = (fPairMatcher.getAnchor() == ICharacterPairMatcher.RIGHT) ?
					region.getOffset() + 1 : region.getOffset() + region.getLength() - 1;
			
			boolean visible = false;
			if (sourceViewer instanceof ITextViewerExtension5) {
				final ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
				visible = (extension.modelOffset2WidgetOffset(targetOffset) > -1);
			} else {
				final IRegion visibleRegion = sourceViewer.getVisibleRegion();
				visible = (targetOffset >= visibleRegion.getOffset() && targetOffset <= visibleRegion.getOffset() + visibleRegion.getLength());
			}
			
			if (!visible) {
				final IEditorStatusLine statusLine = (IEditorStatusLine) fSourceEditor.getAdapter(IEditorStatusLine.class);
				if (statusLine != null) {
					statusLine.setMessage(true, EditingMessages.GotoMatchingBracketAction_error_BracketOutsideSelectedElement, null);
				}
				Display.getCurrent().beep();
				return;
			}
			
			sourceViewer.setSelectedRange(targetOffset, 0);
			sourceViewer.revealRange(targetOffset, 0);
		}
	}
	
}
