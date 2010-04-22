/*******************************************************************************
 * Copyright (c) 2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.epatches.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import de.walware.ecommons.ui.util.UIAccess;


/**
 * Opens the Search dialog supporting parameters.
 */
public class OpenSearchDialogHandler extends AbstractHandler {
	
	
	public OpenSearchDialogHandler() {
	}
	
	
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final String pageId = event.getParameter("pageId");
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window == null) {
			window = UIAccess.getActiveWorkbenchWindow(false);
		}
		if (pageId != null && pageId.length() > 0) {
			NewSearchUI.openSearchDialog(window, pageId);
		}
		else {
			NewSearchUI.openSearchDialog(window, null);
		}
		return null;
	}
	
}
