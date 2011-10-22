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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BlockTextSelection;
import org.eclipse.swt.custom.ST;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class SelectNextWordHandler extends SourceEditorTextHandler {
	
	
	public SelectNextWordHandler(final ISourceEditor editor) {
		super(editor);
	}
	
	
	@Override
	protected int getTextActionId() {
		return ST.SELECT_WORD_NEXT;
	}
	
	@Override
	protected void exec(final ExecData data) throws BadLocationException {
		if (data.getWidget().getBlockSelection()) {
			final BlockTextSelection blockSelection = (BlockTextSelection) data.getViewer().getSelection();
			if ((blockSelection.getStartColumn() != data.getCaretColumn()
					&& blockSelection.getEndColumn() != data.getCaretColumn())
					|| (data.getCaretDocOffset() == data.getCaretDocLineEndOffset()) ) {
				super.exec(data);
				return;
			}
			
			final int newDocOffset;
			if (blockSelection.getStartColumn() > data.getCaretDocLineInformation().getLength()) {
				newDocOffset = data.getCaretDocLineEndOffset();
			}
			else {
				newDocOffset = findNextWordOffset(data, data.getCaretDocOffset(), true);
			}
			final int newWidgetOffset = data.toWidgetOffset(newDocOffset);
			expandBlockSelection(data, newWidgetOffset);
		}
		else {
			final int newDocOffset = findNextWordOffset(data, data.getCaretDocOffset(), false);
			final int newWidgetOffset = data.toWidgetOffset(newDocOffset);
			if (newWidgetOffset >= 0) {
				expandWidgetSelection(data, newWidgetOffset);
			}
			else {
				data.getWidget().invokeAction(ST.SELECT_COLUMN_NEXT);
			}
		}
	}
	
}