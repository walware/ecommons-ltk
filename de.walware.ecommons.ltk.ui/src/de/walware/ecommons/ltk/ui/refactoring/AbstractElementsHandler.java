/*******************************************************************************
 * Copyright (c) 2008-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.refactoring;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import de.walware.ecommons.ui.util.DNDUtil;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.core.refactoring.CommonRefactoringFactory;


public abstract class AbstractElementsHandler extends AbstractHandler {
	
	
	protected final CommonRefactoringFactory fRefactoring;
	
	
	public AbstractElementsHandler(final CommonRefactoringFactory refactoring) {
		fRefactoring = refactoring;
	}
	
	
	protected boolean copyToClipboard(final ExecutionEvent event, final String sourceCode) {
		final Clipboard clipboard = new Clipboard(UIAccess.getDisplay());
		try {
			return DNDUtil.setContent(clipboard, 
					new Object[] { sourceCode }, 
					new Transfer[] { TextTransfer.getInstance() });
		}
		finally {
			if (clipboard != null && !clipboard.isDisposed()) {
				clipboard.dispose();
			}
		}
	}
	
	protected String getCodeFromClipboard(final ExecutionEvent event) {
		final Clipboard clipboard = new Clipboard(UIAccess.getDisplay());
		try {
			return (String) clipboard.getContents(TextTransfer.getInstance());
		}
		finally {
			if (clipboard != null && !clipboard.isDisposed()) {
				clipboard.dispose();
			}
		}
	}
	
}
