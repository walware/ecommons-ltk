/*******************************************************************************
 * Copyright (c) 2011 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
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
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import de.walware.ecommons.text.DocumentCodepointIterator;
import de.walware.ecommons.text.ICodepointIterator;
import de.walware.ecommons.text.TextUtil;
import de.walware.ecommons.ui.util.DNDUtil;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class SourceEditorTextHandler extends AbstractHandler {
	// TODO Add CamelCase support
	
	
	private static int W_INIT = -1;
	private static int W_WORD = 0;
	private static int W_SEP = 1;
	private static int W_WS = 2;
	
	
	protected static class ExecData {
		
		private final ISourceEditor fEditor;
		private final SourceViewer fViewer;
		private final StyledText fWidget;
		
		private final AbstractDocument fDocument;
		
		private final int fCaretWidgetOffset;
		private final int fCaretDocOffset;
		
		private int fCaretDocLine = Integer.MIN_VALUE;
		private IRegion fCaretDocLineInfo;
		
		private LinkedModeModel fLinkedModel;
		
		
		public ExecData(final ISourceEditor editor) throws BadLocationException {
			fEditor = editor;
			fViewer = editor.getViewer();
			fWidget = getViewer().getTextWidget();
			fDocument = (AbstractDocument) getViewer().getDocument();
			fCaretWidgetOffset = this.getWidget().getCaretOffset();
			fCaretDocOffset = getViewer().widgetOffset2ModelOffset(getCaretWidgetOffset());
			if (fCaretDocOffset < 0) {
				throw new BadLocationException();
			}
		}
		
		
		public ISourceEditor getEditor() {
			return fEditor;
		}
		
		public SourceViewer getViewer() {
			return fViewer;
		}
		
		public StyledText getWidget() {
			return fWidget;
		}
		
		
		public AbstractDocument getDocument() {
			return fDocument;
		}
		
		public int toWidgetOffset(final int docOffset) {
			return fViewer.modelOffset2WidgetOffset(docOffset);
		}
		
		public int getCaretWidgetOffset() {
			return fCaretWidgetOffset;
		}
		
		public int getCaretDocOffset() {
			return fCaretDocOffset;
		}
		
		public int getCaretDocLine() throws BadLocationException {
			if (fCaretDocLine == Integer.MIN_VALUE) {
				fCaretDocLine = getDocument().getLineOfOffset(getCaretDocOffset());
			}
			return fCaretDocLine;
		}
		
		public IRegion getCaretDocLineInformation() throws BadLocationException {
			if (fCaretDocLineInfo == null) {
				fCaretDocLineInfo = getDocument().getLineInformation(getCaretDocLine());
			}
			return fCaretDocLineInfo;
		}
		
		public int getCaretDocLineBeginOffset() throws BadLocationException {
			return getCaretDocLineInformation().getOffset();
		}
		
		public int getCaretDocLineEndOffset() throws BadLocationException {
			final IRegion region = getCaretDocLineInformation();
			return region.getOffset() + region.getLength();
		}
		
		public int getCaretColumn() throws BadLocationException {
			return getCaretDocOffset() - getCaretDocLineBeginOffset();
		}
		
		public LinkedModeModel getLinkedModel() {
			if (fLinkedModel == null) {
				fLinkedModel = LinkedModeModel.getModel(getDocument(), getCaretDocOffset());
			}
			return fLinkedModel;
		}
		
	}
	
	
	private final ISourceEditor fEditor;
	
	
	public SourceEditorTextHandler(final ISourceEditor editor) {
		fEditor = editor;
	}
	
	
	private ISourceEditor getEditor(final Object context) {
		return fEditor;
	}
	
	protected int getTextActionId() {
		return 0;
	}
	
	protected boolean isEditAction() {
		switch (getTextActionId()) {
		case ST.DELETE_PREVIOUS:
		case ST.DELETE_NEXT:
		case ST.DELETE_WORD_PREVIOUS:
		case ST.DELETE_WORD_NEXT:
			return true;
		default:
			return false;
		}
	}
	
	@Override
	public void setEnabled(final Object evaluationContext) {
		final ISourceEditor editor = getEditor(evaluationContext);
		setBaseEnabled(editor != null
				&& (!isEditAction() || editor.isEditable(false)) );
	}
	
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISourceEditor editor = getEditor(event.getApplicationContext());
		if (editor == null) {
			return null;
		}
		if (isEditAction() && !editor.isEditable(true)) {
			return null;
		}
		try {
			final ExecData data = new ExecData(editor);
			final Point oldSelection = data.getWidget().getSelection();
			
			DocumentRewriteSession session = null;
			if (isEditAction() && data.getLinkedModel() != null) {
				session = data.getDocument().startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
			}
			try {
				exec(data);
			}
			finally {
				if (session != null) {
					data.getDocument().stopRewriteSession(session);
				}
				
				data.getWidget().showSelection();
				final Point newSelection = data.getWidget().getSelection();
				if (!newSelection.equals(oldSelection)) {
					fireSelectionChanged(data, newSelection);
				}
			}
		}
		catch (final BadLocationException e) {
			throw new ExecutionException("An error occurred when executing the text viewer command.", e);
		}
		return null;
	}
	
	private void fireSelectionChanged(final ExecData data, final Point newSelection) {
		final Event event = new Event();
		event.x = newSelection.x;
		event.y = newSelection.y;
		data.getWidget().notifyListeners(SWT.Selection, event);
	}
	
	protected void exec(final ExecData data) throws BadLocationException {
		final int textActionId = getTextActionId();
		if (textActionId != 0) {
			data.getWidget().invokeAction(textActionId);
		}
	}
	
	protected int findPreviousWordOffset(final ExecData data, final int offset,
			final boolean sameLine) throws BadLocationException {
		int bound = 0;
		if (data.getLinkedModel() != null) {
			final LinkedPosition linkedPosition = data.getLinkedModel().findPosition(
					new LinkedPosition(data.getDocument(), offset, 0) );
			if (linkedPosition != null) {
				final int begin = linkedPosition.getOffset();
				if (begin < offset) {
					bound = begin;
				}
			}
		}
		
		int previousOffset = offset;
		if (offset == data.getCaretDocLineBeginOffset()) {
			if (!sameLine && data.getCaretDocLine() > 0) {
				final IRegion nextLine = data.getDocument().getLineInformation(data.getCaretDocLine()-1);
				previousOffset = nextLine.getOffset() + nextLine.getLength();
			}
			else {
				previousOffset = offset;
			}
		}
		else {
			if (bound < data.getCaretDocLineBeginOffset()) {
				bound = data.getCaretDocLineBeginOffset();
			}
			if (offset <= bound) {
				return offset;
			}
			final ICodepointIterator iterator = DocumentCodepointIterator.create(data.getDocument(),
					bound, offset );
			iterator.setIndex(offset, ICodepointIterator.PREPARE_BACKWARD);
			int mode = W_INIT;
			int cp = iterator.previous();
			while (cp != -1) {
				final int newMode = getMode(cp);
				if (mode != W_INIT && mode != W_WS && newMode != mode) {
					break;
				}
				
				mode = newMode;
				previousOffset = iterator.getCurrentIndex();
				cp = iterator.previous();
			}
		}
		if (previousOffset < bound) {
			previousOffset = bound;
		}
		return previousOffset;
	}
	
	
	
	protected int findNextWordOffset(final ExecData data, final int offset,
			final boolean sameLine) throws BadLocationException {
		int bound = data.getDocument().getLength();
		if (data.getLinkedModel() != null) {
			final LinkedPosition linkedPosition = data.getLinkedModel().findPosition(
					new LinkedPosition(data.getDocument(), offset, 0) );
			if (linkedPosition != null) {
				final int end = linkedPosition.getOffset() + linkedPosition.getLength();
				if (end > offset) {
					bound = end;
				}
			}
		}
		
		int nextOffset = offset;
		if (data.getCaretDocLineEndOffset() <= offset) {
			if (!sameLine) {
				nextOffset = data.getCaretDocLineBeginOffset() + data.getDocument().getLineLength(data.getCaretDocLine());
			}
			else {
				nextOffset = offset;
			}
		}
		else {
			if (bound > data.getCaretDocLineEndOffset()) {
				bound = data.getCaretDocLineEndOffset();
			}
			
			final ICodepointIterator iterator = DocumentCodepointIterator.create(data.getDocument(),
					offset, bound );
			iterator.setIndex(offset, ICodepointIterator.PREPARE_FORWARD);
			int mode = W_INIT;
			int cp = iterator.current();
			while (cp != -1) {
				final int newMode = getMode(cp);
				if (mode != W_INIT && newMode != W_WS && newMode != mode) {
					break;
				}
				
				mode = newMode;
				nextOffset = iterator.getCurrentIndex() + iterator.getCurrentLength();
				cp = iterator.next();
			}
		}
		if (nextOffset > bound) {
			nextOffset = bound;
		}
		return nextOffset;
	}
	
	private int getMode(final int cp) {
		if (Character.isLetterOrDigit(cp)) {
			return W_WORD;
		}
		else if (cp == ' ' || cp == '\t') {
			return W_WS;
		}
		else {
			return W_SEP;
		}
	}
	
	protected IRegion getWholeLinesRegion(final ExecData data) throws BadLocationException {
		final Point selectedRange = data.getViewer().getSelectedRange();
		return TextUtil.getBlock(data.getDocument(), selectedRange.x, selectedRange.y);
	}
	
	protected IRegion getToLineBeginRegion(final ExecData data) throws BadLocationException {
		return new Region(data.getCaretDocLineBeginOffset(),
				data.getCaretDocOffset() - data.getCaretDocLineBeginOffset() );
	}
	
	protected IRegion getToLineEndRegion(final ExecData data) throws BadLocationException {
		return new Region(data.getCaretDocOffset(),
				data.getCaretDocLineEndOffset() - data.getCaretDocOffset() );
	}
	
	protected void expandBlockSelection(final ExecData data, final int newWidgetOffset) {
		if (newWidgetOffset > data.getCaretWidgetOffset()) {
			while (data.getWidget().getCaretOffset() < newWidgetOffset) {
				data.getWidget().invokeAction(ST.SELECT_COLUMN_NEXT);
			}
		}
		else {
			while (data.getWidget().getCaretOffset() > newWidgetOffset) {
				data.getWidget().invokeAction(ST.SELECT_COLUMN_PREVIOUS);
			}
		}
	}
	
	protected void expandWidgetSelection(final ExecData data, final int newWidgetOffset) {
		if (data.getCaretWidgetOffset() == newWidgetOffset) {
			return;
		}
		int otherOffset;
		{	final Point widgetSelection = data.getWidget().getSelection();
			otherOffset = (data.getCaretWidgetOffset() == widgetSelection.x) ?
				widgetSelection.y : widgetSelection.x;
		}
		if (newWidgetOffset >= 0 && otherOffset >= 0) {
			data.getWidget().setSelectionRange(otherOffset, newWidgetOffset - otherOffset);
		}
	}
	
	protected void expandDocSelection(final ExecData data, final int newDocOffset) {
		int otherOffset, newWidgetOffset;
		{	final Point widgetSelection = data.getWidget().getSelection();
			otherOffset = (data.getCaretWidgetOffset() == widgetSelection.x) ?
					widgetSelection.y : widgetSelection.x;
			newWidgetOffset = data.toWidgetOffset(newDocOffset);
		}
		if (newWidgetOffset < 0 && data.getViewer() instanceof ProjectionViewer) {
			otherOffset = data.getViewer().widgetOffset2ModelOffset(otherOffset);
			if (((ProjectionViewer) data.getViewer()).exposeModelRange(new Region(newDocOffset, 0))) {
				otherOffset = data.getViewer().modelOffset2WidgetOffset(otherOffset);
				newWidgetOffset = data.toWidgetOffset(newDocOffset);
			}
		}
		if (newWidgetOffset >= 0 && otherOffset >= 0) {
			data.getWidget().setSelectionRange(otherOffset, newWidgetOffset - otherOffset);
		}
	}
	
	protected void copyToClipboard(final ExecData data, final IRegion docRegion)
			throws BadLocationException {
		final String text = data.getDocument().get(docRegion.getOffset(), docRegion.getLength());
		final Clipboard clipboard = new Clipboard(data.getWidget().getDisplay());
		try {
			DNDUtil.setContent(clipboard,
					new String[] { text },
					new Transfer[] { TextTransfer.getInstance() } );
		}
		finally {
			clipboard.dispose();
		}
	}
	
	protected void delete(final ExecData data, final IRegion docRegion)
			throws BadLocationException {
		if (data.getViewer() instanceof ProjectionViewer) {
			((ProjectionViewer) data.getViewer()).exposeModelRange(docRegion);
		}
		if (docRegion.getLength() > 0) {
			data.getDocument().replace(docRegion.getOffset(), docRegion.getLength(), ""); //$NON-NLS-1$
		}
		final int widgetOffset = data.getViewer().modelOffset2WidgetOffset(docRegion.getOffset());
		if (widgetOffset >= 0) {
			data.getWidget().setSelectionRange(widgetOffset, 0);
		}
	}
	
}
