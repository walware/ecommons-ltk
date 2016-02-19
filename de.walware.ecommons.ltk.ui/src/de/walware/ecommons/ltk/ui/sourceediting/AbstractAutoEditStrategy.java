/*=============================================================================#
 # Copyright (c) 2012-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

import de.walware.ecommons.text.BasicHeuristicTokenScanner;
import de.walware.ecommons.text.IIndentSettings;
import de.walware.ecommons.text.ITokenScanner;
import de.walware.ecommons.text.IndentUtil;
import de.walware.ecommons.text.core.ITextRegion;
import de.walware.ecommons.text.core.TextRegion;
import de.walware.ecommons.text.core.sections.IDocContentSections;
import de.walware.ecommons.text.core.treepartitioner.TreePartition;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.core.SourceContent;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


/**
 * Auto edit strategy for Wikitext markup
 */
public abstract class AbstractAutoEditStrategy extends DefaultIndentLineAutoEditStrategy
		implements ISourceEditorAddon, VerifyKeyListener {
	
	
	private final ISourceEditor editor;
	private final ITextEditorExtension3 editor3;
	private final IDocContentSections documentContentInfo;
	private final SourceViewer viewer;
	
	private AbstractDocument document;
	private ITextRegion validRange;
	
	private boolean ignoreCommands= false;
	
	
	public AbstractAutoEditStrategy(final ISourceEditor editor) {
		assert (editor != null);
		
		this.editor= editor;
		this.documentContentInfo= this.editor.getDocumentContentInfo();
		
		this.viewer= this.editor.getViewer();
		this.editor3= (editor instanceof SourceEditor1) ? (SourceEditor1) editor : null;
	}
	
	
	public final ISourceEditor getEditor() {
		return this.editor;
	}
	
	protected final ITextEditorExtension3 getEditor3() {
		return this.editor3;
	}
	
	public final IDocContentSections getDocumentContentInfo() {
		return this.documentContentInfo;
	}
	
	protected final SourceViewer getViewer() {
		return this.viewer;
	}
	
	protected abstract ISmartInsertSettings getSettings();
	
	protected abstract IIndentSettings getCodeStyleSettings();
	
	
	@Override
	public void install(final ISourceEditor editor) {
		assert (editor.getViewer() == this.viewer);
		this.viewer.prependVerifyKeyListener(this);
	}
	
	@Override
	public void uninstall() {
		this.viewer.removeVerifyKeyListener(this);
	}
	
	
	protected TreePartition initCustomization(final int offset, final int ch)
			throws BadLocationException, BadPartitioningException {
		assert(this.document != null);
		
		final TreePartition partition= (TreePartition) this.document.getPartition(
				this.documentContentInfo.getPartitioning(), offset, true );
		this.validRange= computeValidRange(offset, partition, ch);
		return (this.validRange != null) ? partition : null;
	}
	
	protected ITextRegion computeValidRange(final int offset, final TreePartition partition, final int c) {
		return new TextRegion(0, this.document.getLength());
	}
	
	protected final AbstractDocument getDocument() {
		return this.document;
	}
	
	protected final ITextRegion getValidRange() {
		return this.validRange;
	}
	
	protected abstract BasicHeuristicTokenScanner getScanner();
	
	protected IndentUtil createIndentUtil(final AbstractDocument doc) {
		return new IndentUtil(doc, getCodeStyleSettings());
	}
	
	protected void quitCustomization() {
		this.document= null;
	}
	
	
	private final boolean isSmartInsertEnabled() {
		return ((this.editor3 != null) ?
				(this.editor3.getInsertMode() == ITextEditorExtension3.SMART_INSERT) :
				getSettings().isSmartInsertEnabledByDefault());
	}
	
	private final boolean isBlockSelection() {
		final StyledText textWidget= this.viewer.getTextWidget();
		return (textWidget.getBlockSelection() && textWidget.getSelectionRanges().length > 2);
	}
	
	
	/**
	 * Second main entry method for real single key presses.
	 */
	@Override
	public final void verifyKey(final VerifyEvent event) {
		final char ch;
		if (!event.doit || (ch= isCustomizeKey(event)) == 0
				|| !isSmartInsertEnabled()
				|| !UIAccess.isOkToUse(this.viewer) || isBlockSelection() ) {
			return;
		}
		try {
			this.document= (AbstractDocument) this.viewer.getDocument();
			final ITextSelection selection= (ITextSelection) this.viewer.getSelection();
			final TreePartition partition= initCustomization(selection.getOffset(), ch);
			if (partition == null) {
				return;
			}
			this.ignoreCommands= true;
			
			final DocumentCommand command= new DocumentCommand() {};
			command.offset= selection.getOffset();
			command.length= selection.getLength();
			command.doit= true;
			command.shiftsCaret= true;
			command.caretOffset= -1;
			
			doCustomizeKeyCommand(ch, command, partition);
			event.doit= command.doit;
		}
		catch (final Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, -1,
					"An error occurred when customizing action for pressed key in auto edit strategy.", //$NON-NLS-1$
					e ));
		}
		finally {
			this.ignoreCommands= false;
			quitCustomization();
		}
	}
	
	@Override
	public final void customizeDocumentCommand(final IDocument d, final DocumentCommand command) {
		if (this.ignoreCommands || command.doit == false || command.text == null) {
			return;
		}
		if (!isSmartInsertEnabled() || isBlockSelection()) {
			super.customizeDocumentCommand(d, command);
			return;
		}
		
		try {
			this.document= (AbstractDocument) d;
			final TreePartition partition= initCustomization(command.offset, -1);
			if (partition == null) {
				return;
			}
			
			doCustomizeOtherCommand(command, partition);
		}
		catch (final Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, -1,
					"An error occurred when customizing action for document command in auto edit strategy.", e )); //$NON-NLS-1$
		}
		finally {
			if (!command.doit) {
				command.text= null;
				command.length= 0;
			}
			
			quitCustomization();
		}
	}
	
	
	
	protected abstract char isCustomizeKey(KeyEvent event);
	
	protected abstract void doCustomizeKeyCommand(char ch, DocumentCommand command,
			TreePartition partition) throws Exception;
	
	protected abstract void doCustomizeOtherCommand(DocumentCommand command,
			TreePartition partition) throws Exception;
	
	
	
	protected final int getChar(final int offset) throws BadLocationException {
		final ITextRegion validRange= getValidRange();
		return (offset >= validRange.getOffset() && offset < validRange.getEndOffset()) ?
				getDocument().getChar(offset) : -1;
	}
	
	protected final boolean isRegularTabCommand(final DocumentCommand command) throws BadLocationException {
		return (command.length == 0 
				|| this.document.getLineOfOffset(command.offset) == this.document.getLineOfOffset(command.offset + command.length) );
	}
	
	protected final void customizeCommandDefault(final DocumentCommand command) {
		super.customizeDocumentCommand(getDocument(), command);
	}
	
	protected final void applyCommand(final DocumentCommand command) throws BadLocationException {
		this.document.replace(command.offset, command.length, command.text);
		command.doit= false;
	}
	
	protected final void updateSelection(final DocumentCommand command) {
		if (command.caretOffset == -1) {
			command.caretOffset= command.offset + command.text.length();
		}
		final TextSelection textSelection= new TextSelection(this.document, command.caretOffset, 0);
		this.viewer.setSelection(textSelection, true);
		command.shiftsCaret= false;
	}
	
	
	protected final boolean endsWithNewLine(final String text) {
		for (int idx= text.length() - 1; idx >= 0; idx--) {
			final char c= text.charAt(idx);
			switch (c) {
			case '\n':
				return true;
			case ' ':
			case '\t':
				continue;
			default:
				break;
			}
		}
		return false;
	}
	
	protected final int indexOfNewLine(final String text) {
		for (int idx= 0; idx < text.length(); idx++) {
			final char c= text.charAt(idx);
			switch (c) {
			case '\r':
				if (idx + 1 < text.length() && text.charAt(idx + 1) == '\n') {
					return idx;
				}
				continue;
			case '\n':
				return idx;
			default:
				continue;
			}
		}
		return -1;
	}
	
	protected final boolean containsControl(final String text) {
		for (int idx= 0; idx < text.length(); idx++) {
			if (text.charAt(idx) < 0x20) {
				return true;
			}
		}
		return false;
	}
	
	protected final SourceContent createSourceContent(final AbstractDocument document,
			final ITextRegion region, final DocumentCommand command) throws BadLocationException {
		final StringBuilder sb= new StringBuilder(region.getLength() - command.length + command.text.length());
		sb.append(document.get(region.getOffset(), command.offset - region.getOffset()));
		sb.append(command.text);
		sb.append(document.get(command.offset + command.length, region.getEndOffset() - (command.offset + command.length)));
		return new SourceContent(region.getOffset(), sb.toString());
	}
	
	
	protected void smartInsertOnTab(final DocumentCommand command, boolean indent)
			throws Exception {
		final AbstractDocument doc= getDocument();
		final IndentUtil indentUtil= createIndentUtil(doc);
		final int lineNum= doc.getLineOfOffset(command.offset);
		final int column= indentUtil.getColumn(lineNum, command.offset);
		
		if (indent) {
			final BasicHeuristicTokenScanner scanner= getScanner();
			scanner.configure(doc);
			if (scanner.findAnyNonBlankBackward(command.offset, doc.getLineOffset(lineNum) - 1, false) != ITokenScanner.NOT_FOUND) {
				indent= false;
			}
		}
		if (!indent) {
			if (getCodeStyleSettings().getReplaceOtherTabsWithSpaces()) {
				command.text= indentUtil.createTabSpacesCompletionString(column);
			}
			return;
		}
		
		switch (getSettings().getSmartInsertTabAction()) {
		case INSERT_TAB_CHAR:
			return;
		case INSERT_TAB_LEVEL:
			command.text= indentUtil.createTabCompletionString(column);
			return;
		case INSERT_INDENT_LEVEL:
			command.text= indentUtil.createIndentCompletionString(column);
			return;
		case CORRECT_INDENT:
			command.text= indentUtil.createIndentCompletionString(column);
			correctIndent(command, column + 1, indentUtil);
			return;
		default:
			throw new UnsupportedOperationException();
		}
	}
	
	protected void correctIndent(final DocumentCommand command, final int minColumn,
			final IndentUtil indentUtil) throws Exception {
		throw new UnsupportedOperationException();
	}
	
}
