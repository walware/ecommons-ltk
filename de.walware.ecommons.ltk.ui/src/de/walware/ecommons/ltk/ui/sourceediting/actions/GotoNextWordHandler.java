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
import org.eclipse.swt.custom.ST;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class GotoNextWordHandler extends SourceEditorTextHandler {
	
	
	public GotoNextWordHandler(final ISourceEditor editor) {
		super(editor);
	}
	
	
	@Override
	protected int getTextActionId() {
		return ST.WORD_NEXT;
	}
	
	@Override
	protected void exec(final ExecData data) throws BadLocationException {
		final int newDocOffset = findNextWordOffset(data, data.getCaretDocOffset(), false);
		final int newWidgetOffset = data.toWidgetOffset(newDocOffset);
		if (newWidgetOffset >= 0) {
			if (data.getCaretWidgetOffset() != newWidgetOffset) {
				data.getWidget().setCaretOffset(newWidgetOffset);
			}
		}
		else {
			data.getWidget().invokeAction(ST.COLUMN_NEXT);
		}
	}
	
}
