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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BlockTextSelection;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.custom.ST;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class DeleteNextWordHandler extends SourceEditorTextHandler {
	
	
	public DeleteNextWordHandler(final ISourceEditor editor) {
		super(editor);
	}
	
	
	@Override
	protected int getTextActionId() {
		return ST.DELETE_WORD_NEXT;
	}
	
	@Override
	protected void exec(final ExecData data) throws BadLocationException {
		if (data.getWidget().getBlockSelection()) {
			final BlockTextSelection blockSelection = (BlockTextSelection) data.getViewer().getSelection();
			if (blockSelection.getEndColumn() > blockSelection.getStartColumn()
					|| data.getCaretDocOffset() == data.getCaretDocLineEndOffset()) {
				data.getWidget().invokeAction(ST.DELETE_NEXT);
				return;
			}
			final int newDocOffset;
			if (blockSelection.getStartColumn() > data.getCaretDocLineInformation().getLength()) {
				if (blockSelection.getStartLine() == blockSelection.getEndLine()) {
					data.getWidget().invokeAction(ST.LINE_END);
					return;
				}
				newDocOffset = data.getCaretDocLineEndOffset();
			}
			else {
				newDocOffset = findNextWordOffset(data, data.getCaretDocOffset(), true);
			}
			final int newWidgetOffset = data.toWidgetOffset(newDocOffset);
			if (newWidgetOffset >= 0) {
				expandBlockSelection(data, newWidgetOffset);
				data.getWidget().invokeAction(ST.DELETE_NEXT);
			}
		}
		else if (data.getWidget().getSelectionCount() > 0) {
			data.getWidget().invokeAction(ST.DELETE_NEXT);
		}
		else {
			final int newDocOffset = findNextWordOffset(data, data.getCaretDocOffset(), false);
			final IRegion docRegion = new Region(data.getCaretDocOffset(),
					newDocOffset - data.getCaretDocOffset() );
			delete(data, docRegion);
		}
	}
	
}
