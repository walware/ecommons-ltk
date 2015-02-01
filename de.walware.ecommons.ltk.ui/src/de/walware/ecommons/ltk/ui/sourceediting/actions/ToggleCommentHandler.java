/*=============================================================================#
 # Copyright (c) 2014-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.actions;

import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.ui.EditorUtil;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditor1;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewer;


public class ToggleCommentHandler extends AbstractHandler {
	
	
	private final SourceEditor1 editor;
	
	/** The text operation target */
	private ITextOperationTarget operationTarget;
	
	private final Map<String, String[]> contentTypePrefixes= new IdentityHashMap<>();
	
	
	public ToggleCommentHandler(final SourceEditor1 editor) {
		this.editor= editor;
		
		setBaseEnabled(false);
	}
	
	
	protected SourceEditor1 getEditor() {
		return this.editor;
	}
	
	@Override
	public void setEnabled(final Object evaluationContext) {
		if (this.editor.getViewer() instanceof SourceEditorViewer
				&& !this.editor.isEditorInputModifiable()) {
			setBaseEnabled(false);
			return;
		}
		
		if (this.operationTarget == null) {
			this.operationTarget= (ITextOperationTarget) this.editor.getAdapter(ITextOperationTarget.class);
		}
		setBaseEnabled(this.operationTarget != null
				&& this.operationTarget.canDoOperation(ITextOperationTarget.PREFIX)
				&& this.operationTarget.canDoOperation(ITextOperationTarget.STRIP_PREFIX) );
	}
	
	private ITextSelection getSelection() {
		final ISelection selection= this.editor.getSelectionProvider().getSelection();
		return (selection instanceof ITextSelection) ? (ITextSelection) selection : null;
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		if (!this.editor.validateEditorInputState() || !isEnabled()) {
			return null;
		}
		
		final ITextSelection selection= getSelection();
		final IDocument document= this.editor.getViewer().getDocument();
		final int operationCode= (isSelectionCommented(document, selection)) ?
				ITextOperationTarget.STRIP_PREFIX : ITextOperationTarget.PREFIX;
		
		final Shell shell= this.editor.getSite().getShell();
		if (!this.operationTarget.canDoOperation(operationCode)) {
			signalError();
			return null;
		}
		
		Display display= null;
		if (shell != null && !shell.isDisposed()) {
			display= shell.getDisplay();
		}
		
		BusyIndicator.showWhile(display, new Runnable() {
			@Override
			public void run() {
				ToggleCommentHandler.this.run(document, selection, operationCode);
			}
		});
		return null;
	}
	
	protected void run(final IDocument document, final ITextSelection selection,
			final int operationCode) {
		this.operationTarget.doOperation(operationCode);
	}
	
	
	protected String[] getPrefixes(final String contentType) {
		String[] prefixes= this.contentTypePrefixes.get(contentType);
		if (prefixes == null) {
			prefixes= ((SourceEditorViewer) this.editor.getViewer()).getDefaultPrefixes(contentType);
			if (prefixes == null) {
				prefixes= new String[0];
			}
			else {
				int numEmpty= 0;
				for (int i= 0; i < prefixes.length; i++) {
					if (prefixes[i].isEmpty()) {
						numEmpty++;
					}
				}
				if (numEmpty > 0) {
					final String[] nonemptyPrefixes= new String[prefixes.length - numEmpty];
					for (int i= 0, j= 0; i < prefixes.length; i++) {
						final String prefix= prefixes[i];
						if (!prefix.isEmpty()) {
							nonemptyPrefixes[j++]= prefix;
						}
					}
					prefixes= nonemptyPrefixes;
				}
			}
			this.contentTypePrefixes.put(contentType, prefixes);
		}
		return prefixes;
	}
	
	/**
	 * Is the given selection single-line commented?
	 * 
	 * @param selection Selection to check
	 * @return <code>true</code> iff all selected lines are commented
	 */
	private boolean isSelectionCommented(final IDocument document, final ITextSelection selection) {
		if (selection.getStartLine() < 0 || selection.getEndLine() < 0) {
			return false;
		}
		
		try {
			final IRegion block= EditorUtil.getTextBlockFromSelection(document,
					selection.getOffset(), selection.getLength() );
			final ITypedRegion[] regions= TextUtilities.computePartitioning(document,
					this.editor.getDocumentContentInfo().getPartitioning(),
					block.getOffset(), block.getLength(), false );
			
			final int[] lines= new int[regions.length * 2]; // [startline, endline, startline, endline, ...]
			for (int i= 0, j= 0; i < regions.length; i++, j+= 2) {
				// start line of region
				lines[j]= getFirstCompleteLineOfRegion(document, regions[i]);
				// end line of region
				final int length= regions[i].getLength();
				int offset= regions[i].getOffset() + length;
				if (length > 0) {
					offset--;
				}
				lines[j + 1]= (lines[j] == -1) ? -1 : document.getLineOfOffset(offset);
			}
			
			// Perform the check
			for (int i= 0, j= 0; i < regions.length; i++, j+= 2) {
				final String[] prefixes= getPrefixes(regions[i].getType());
				if (prefixes != null && prefixes.length > 0 && lines[j] >= 0 && lines[j + 1] >= 0) {
					if (!isBlockCommented(lines[j], lines[j + 1], prefixes, document)) {
						return false;
					}
				}
			}
			return true;
		}
		catch (final BadLocationException e) {
			log(e);
		}
		return false;
	}
	
	/**
	 * Determines whether each line is prefixed by one of the prefixes.
	 * 
	 * @param startLine Start line in document
	 * @param endLine End line in document
	 * @param prefixes Possible comment prefixes
	 * @param document The document
	 * @return <code>true</code> iff each line from <code>startLine</code>
	 *     to and including <code>endLine</code> is prepended by one
	 *     of the <code>prefixes</code>, ignoring whitespace at the
	 *     begin of line
	 */
	private boolean isBlockCommented(final int startLine, final int endLine, final String[] prefixes,
			final IDocument document) {
		try {
			// check for occurrences of prefixes in the given lines
			for (int i= startLine; i <= endLine; i++) {
				
				final IRegion line= document.getLineInformation(i);
				final String text= document.get(line.getOffset(), line.getLength());
				
				final int[] found= TextUtilities.indexOf(prefixes, text, 0);
				
				if (found[0] == -1) {
					// found a line which is not commented
					return false;
				}
				
				final String beforePrefix= text.substring(0, found[0]);
				if (beforePrefix.trim().length() != 0) {
					// found a line which is not commented
					return false;
				}
			}
			return true;
		}
		catch (final BadLocationException e) {
			log(e);
		}
		return false;
	}
	
	/**
	 * Returns the index of the first line whose start offset is in the given text range.
	 * 
	 * @param document The document
	 * @param region the text range in characters where to find the line
	 * @return the first line whose start index is in the given range, -1 if there is no such line
	 * @throws BadLocationException 
	 */
	protected int getFirstCompleteLineOfRegion(final IDocument document, final IRegion region) throws BadLocationException {
		final int startLine= document.getLineOfOffset(region.getOffset());
		
		int offset= document.getLineOffset(startLine);
		if (offset >= region.getOffset()) {
			return startLine;
		}
		offset= document.getLineOffset(startLine + 1);
		return (offset > region.getOffset() + region.getLength() ? -1 : startLine + 1);
	}
	
	protected void addPrefix(final IDocument document, final IRegion region, final String prefix) throws BadLocationException {
		final int startLine= document.getLineOfOffset(region.getOffset());
		final int stopLine= (region.getLength() > 0) ?
				document.getLineOfOffset(region.getOffset()+region.getLength()-1) :
				startLine;
		final MultiTextEdit multi= new MultiTextEdit(region.getOffset(), region.getLength());
		for (int line= startLine; line <= stopLine; line++) {
			multi.addChild(new InsertEdit(document.getLineOffset(line), prefix));
		}
		if (document instanceof IDocumentExtension4) {
			final IDocumentExtension4 document4= (IDocumentExtension4) document;
			final DocumentRewriteSession rewriteSession= document4.startRewriteSession(
					DocumentRewriteSessionType.STRICTLY_SEQUENTIAL );
			try {
				multi.apply(document, TextEdit.NONE);
			}
			finally {
				document4.stopRewriteSession(rewriteSession);
			}
		}
		else {
			multi.apply(document, TextEdit.NONE);
		}
	}
	
	
	protected void signalError() {
		final IEditorStatusLine statusLine= (IEditorStatusLine) this.editor.getAdapter(IEditorStatusLine.class);
		statusLine.setMessage(true, EditingMessages.ToggleCommentAction_error, null);
		this.editor.getViewer().getTextWidget().getDisplay().beep();
	}
	
	protected void log(final Exception e) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, 0,
				"An error ocurred when executing toggle comment operation.", e ), StatusManager.LOG);
	}
	
}
