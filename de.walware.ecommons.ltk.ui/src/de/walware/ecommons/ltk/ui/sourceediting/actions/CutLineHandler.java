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
import org.eclipse.jface.text.IRegion;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class CutLineHandler extends SourceEditorTextHandler {
	
	
	public CutLineHandler(final ISourceEditor editor) {
		super(editor);
	}
	
	
	@Override
	protected void exec(final ExecData data) throws BadLocationException {
		final IRegion region = getWholeLinesRegion(data);
		copyToClipboard(data, region);
		delete(data, region);
	}
	
}
