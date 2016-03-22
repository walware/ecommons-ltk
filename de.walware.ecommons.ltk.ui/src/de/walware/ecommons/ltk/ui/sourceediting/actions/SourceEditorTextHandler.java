/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
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
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import de.walware.ecommons.text.DocumentCodepointIterator;
import de.walware.ecommons.text.ICodepointIterator;
import de.walware.ecommons.text.TextUtil;
import de.walware.ecommons.ui.util.DNDUtil;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public abstract class SourceEditorTextHandler extends AbstractHandler {
	// TODO Add CamelCase support
	
	
	private static int W_INIT= -1;
	private static int W_WORD= 0;
	private static int W_SEP= 1;
	private static int W_WS= 2;
	
	
	protected static class ExecData {
		
		private final ISourceEditor editor;
		private final SourceViewer viewer;
		private final StyledText widget;
		
		private final AbstractDocument document;
		
		private final int caretWidgetOffset;
		private final int caretDocOffset;
		
		private int caretDocLine= Integer.MIN_VALUE;
		private IRegion caretDocLineInfo;
		
		private LinkedModeModel linkedModel;
		
		
		public ExecData(final ISourceEditor editor) throws BadLocationException {
			this.editor= editor;
			this.viewer= editor.getViewer();
			this.widget= getViewer().getTextWidget();
			this.document= (AbstractDocument) getViewer().getDocument();
			this.caretWidgetOffset= getWidget().getCaretOffset();
			this.caretDocOffset= getViewer().widgetOffset2ModelOffset(getCaretWidgetOffset());
			if (this.caretDocOffset < 0) {
				throw new BadLocationException();
			}
		}
		
		
		public boolean isSmartHomeBeginEndEnabled() {
			final IPreferenceStore store= EditorsUI.getPreferenceStore();
			return (store != null
					&& store.getBoolean(AbstractTextEditor.PREFERENCE_NAVIGATION_SMART_HOME_END) );
		}
		
		public ISourceEditor getEditor() {
			return this.editor;
		}
		
		public SourceViewer getViewer() {
			return this.viewer;
		}
		
		public StyledText getWidget() {
			return this.widget;
		}
		
		
		public AbstractDocument getDocument() {
			return this.document;
		}
		
		public int toWidgetOffset(final int docOffset) {
			return this.viewer.modelOffset2WidgetOffset(docOffset);
		}
		
		public int toDocOffset(final int widgetOffset) {
			return this.viewer.widgetOffset2ModelOffset(widgetOffset);
		}
		
		public int getCaretWidgetOffset() {
			return this.caretWidgetOffset;
		}
		
		public int getCaretDocOffset() {
			return this.caretDocOffset;
		}
		
		public int getCaretDocLine() throws BadLocationException {
			if (this.caretDocLine == Integer.MIN_VALUE) {
				this.caretDocLine= getDocument().getLineOfOffset(getCaretDocOffset());
			}
			return this.caretDocLine;
		}
		
		public IRegion getCaretDocLineInformation() throws BadLocationException {
			if (this.caretDocLineInfo == null) {
				this.caretDocLineInfo= getDocument().getLineInformation(getCaretDocLine());
			}
			return this.caretDocLineInfo;
		}
		
		public int getCaretDocLineBeginOffset() throws BadLocationException {
			return getCaretDocLineInformation().getOffset();
		}
		
		public int getCaretDocLineEndOffset() throws BadLocationException {
			final IRegion region= getCaretDocLineInformation();
			return region.getOffset() + region.getLength();
		}
		
		public int getCaretColumn() throws BadLocationException {
			return getCaretDocOffset() - getCaretDocLineBeginOffset();
		}
		
		public LinkedModeModel getLinkedModel() {
			if (this.linkedModel == null) {
				this.linkedModel= LinkedModeModel.getModel(getDocument(), getCaretDocOffset());
			}
			return this.linkedModel;
		}
		
	}
	
	
	private final ISourceEditor editor;
	
	
	public SourceEditorTextHandler(final ISourceEditor editor) {
		this.editor= editor;
	}
	
	
	private ISourceEditor getEditor(final Object context) {
		return this.editor;
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
		final ISourceEditor editor= getEditor(evaluationContext);
		setBaseEnabled(editor != null
				&& (!isEditAction() || editor.isEditable(false)) );
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISourceEditor editor= getEditor(event.getApplicationContext());
		if (editor == null) {
			return null;
		}
		if (isEditAction() && !editor.isEditable(true)) {
			return null;
		}
		try {
			final ExecData data= new ExecData(editor);
			final Point oldSelection= data.getWidget().getSelection();
			
			try {
				exec(data);
			}
			finally {
				data.getWidget().showSelection();
				final Point newSelection= data.getWidget().getSelection();
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
		final Event event= new Event();
		event.x= newSelection.x;
		event.y= newSelection.y;
		data.getWidget().notifyListeners(SWT.Selection, event);
	}
	
	protected void exec(final ExecData data) throws BadLocationException {
		final int textActionId= getTextActionId();
		if (textActionId != 0) {
			data.getWidget().invokeAction(textActionId);
		}
	}
	
	protected int findPreviousWordOffset(final ExecData data, final int offset,
			final boolean sameLine) throws BadLocationException {
		int bound= 0;
		if (data.getLinkedModel() != null) {
			final LinkedPosition linkedPosition= data.getLinkedModel().findPosition(
					new LinkedPosition(data.getDocument(), offset, 0) );
			if (linkedPosition != null) {
				final int begin= linkedPosition.getOffset();
				if (begin < offset) {
					bound= begin;
				}
			}
		}
		
		int previousOffset= offset;
		if (offset == data.getCaretDocLineBeginOffset()) {
			if (!sameLine && data.getCaretDocLine() > 0) {
				final IRegion nextLine= data.getDocument().getLineInformation(data.getCaretDocLine()-1);
				previousOffset= nextLine.getOffset() + nextLine.getLength();
			}
			else {
				previousOffset= offset;
			}
		}
		else {
			if (bound < data.getCaretDocLineBeginOffset()) {
				bound= data.getCaretDocLineBeginOffset();
			}
			if (offset <= bound) {
				return offset;
			}
			final ICodepointIterator iterator= DocumentCodepointIterator.create(data.getDocument(),
					bound, offset );
			iterator.setIndex(offset, ICodepointIterator.PREPARE_BACKWARD);
			int mode= W_INIT;
			int cp= iterator.previous();
			while (cp != -1) {
				final int newMode= getMode(cp);
				if (mode != W_INIT && mode != W_WS && newMode != mode) {
					break;
				}
				
				mode= newMode;
				previousOffset= iterator.getCurrentIndex();
				cp= iterator.previous();
			}
		}
		if (previousOffset < bound) {
			previousOffset= bound;
		}
		return previousOffset;
	}
	
	
	
	protected int findNextWordOffset(final ExecData data, final int offset,
			final boolean sameLine) throws BadLocationException {
		int bound= data.getDocument().getLength();
		if (data.getLinkedModel() != null) {
			final LinkedPosition linkedPosition= data.getLinkedModel().findPosition(
					new LinkedPosition(data.getDocument(), offset, 0) );
			if (linkedPosition != null) {
				final int end= linkedPosition.getOffset() + linkedPosition.getLength();
				if (end > offset) {
					bound= end;
				}
			}
		}
		
		int nextOffset= offset;
		if (data.getCaretDocLineEndOffset() <= offset) {
			if (!sameLine) {
				nextOffset= data.getCaretDocLineBeginOffset() + data.getDocument().getLineLength(data.getCaretDocLine());
			}
			else {
				nextOffset= offset;
			}
		}
		else {
			if (bound > data.getCaretDocLineEndOffset()) {
				bound= data.getCaretDocLineEndOffset();
			}
			
			final ICodepointIterator iterator= DocumentCodepointIterator.create(data.getDocument(),
					offset, bound );
			iterator.setIndex(offset, ICodepointIterator.PREPARE_FORWARD);
			int mode= W_INIT;
			int cp= iterator.current();
			while (cp != -1) {
				final int newMode= getMode(cp);
				if (mode != W_INIT && newMode != W_WS && newMode != mode) {
					break;
				}
				
				mode= newMode;
				nextOffset= iterator.getCurrentIndex() + iterator.getCurrentLength();
				cp= iterator.next();
			}
		}
		if (nextOffset > bound) {
			nextOffset= bound;
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
	
	public int getCaretSmartLineBeginOffset(final ExecData data) throws BadLocationException {
		if (data.isSmartHomeBeginEndEnabled()) {
			final LinkedModeModel linkedModel= data.getLinkedModel();
			if (linkedModel != null) {
				final LinkedPosition position= linkedModel.findPosition(
						new LinkedPosition(data.getDocument(), data.getCaretDocOffset(), 0) );
				if (position != null) {
					if (data.getCaretDocOffset() > position.getOffset()) {
						return position.getOffset();
					}
				}
			}
		}
		return data.getCaretDocLineBeginOffset();
	}
	
	public int getCaretSmartLineEndOffset(final ExecData data) throws BadLocationException {
		if (data.isSmartHomeBeginEndEnabled()) {
			final LinkedModeModel linkedModel= data.getLinkedModel();
			if (linkedModel != null) {
				final LinkedPosition position= linkedModel.findPosition(
						new LinkedPosition(data.getDocument(), data.getCaretDocOffset(), 0) );
				if (position != null) {
					if (data.getCaretDocOffset() < position.getOffset() + position.getLength()) {
						return position.getOffset() + position.getLength();
					}
				}
			}
		}
		return data.getCaretDocLineEndOffset();
	}
	
	protected IRegion getWholeLinesRegion(final ExecData data) throws BadLocationException {
		final Point selectedRange= data.getViewer().getSelectedRange();
		return TextUtil.getBlock(data.getDocument(), selectedRange.x, selectedRange.y);
	}
	
	protected IRegion getToLineBeginRegion(final ExecData data) throws BadLocationException {
		final int beginOffset= getCaretSmartLineBeginOffset(data);
		return new Region(beginOffset, data.getCaretDocOffset() - beginOffset);
	}
	
	protected IRegion getToLineEndRegion(final ExecData data) throws BadLocationException {
		final int endOffset= getCaretSmartLineEndOffset(data);
		return new Region(data.getCaretDocOffset(), endOffset - data.getCaretDocOffset());
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
	
	protected void expandDocSelection(final ExecData data, final int newDocOffset) {
		int otherDocOffset;
		{	final Point widgetSelection= data.getWidget().getSelection();
			otherDocOffset= data.toDocOffset((data.getCaretWidgetOffset() == widgetSelection.x) ?
					widgetSelection.y : widgetSelection.x);
		}
		if (data.toWidgetOffset(newDocOffset) < 0 && data.getViewer() instanceof ProjectionViewer) {
			((ProjectionViewer) data.getViewer()).exposeModelRange(new Region(newDocOffset, 0));
		}
		selectAndReveal(data, otherDocOffset, newDocOffset - otherDocOffset);
	}
	
	protected void copyToClipboard(final ExecData data, final IRegion docRegion)
			throws BadLocationException {
		final String text= data.getDocument().get(docRegion.getOffset(), docRegion.getLength());
		final Clipboard clipboard= new Clipboard(data.getWidget().getDisplay());
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
		data.getViewer().setSelectedRange(docRegion.getOffset(), 0);
		if (docRegion.getLength() > 0) {
			data.getDocument().replace(docRegion.getOffset(), docRegion.getLength(), ""); //$NON-NLS-1$
		}
		data.getViewer().revealRange(data.getViewer().getSelectedRange().x, 0);
	}
	
	protected void selectAndReveal(final ExecData data, final int docOffset, final int length) {
		data.getViewer().setSelectedRange(docOffset, length);
		data.getViewer().revealRange(docOffset, length);
	}
	
}
