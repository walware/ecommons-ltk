/*=============================================================================#
 # Copyright (c) 2013-2014 Stephan Wahlbrink (WalWare.de) and others.
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


public class GotoLineEndHandler extends SourceEditorTextHandler {
	
	
	public GotoLineEndHandler(final ISourceEditor editor) {
		super(editor);
	}
	
	
	@Override
	protected int getTextActionId() {
		return ST.LINE_END;
	}
	
	@Override
	protected void exec(final ExecData data) throws BadLocationException {
		final int beginOffset = getCaretSmartLineEndOffset(data);
		
		selectAndReveal(data, beginOffset, 0);
	}
	
}
