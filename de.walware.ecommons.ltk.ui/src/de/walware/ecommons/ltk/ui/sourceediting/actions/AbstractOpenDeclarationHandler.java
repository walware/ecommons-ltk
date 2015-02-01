/*=============================================================================#
 # Copyright (c) 2012-2015 Stephan Wahlbrink (WalWare.de) and others.
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.workbench.ui.WorkbenchUIUtil;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public abstract class AbstractOpenDeclarationHandler extends AbstractHandler {
	
	
	public AbstractOpenDeclarationHandler() {
	}
	
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbenchPart activePart = WorkbenchUIUtil.getActivePart(event.getApplicationContext());
		final ISourceEditor editor = (ISourceEditor) activePart.getAdapter(ISourceEditor.class);
		if (editor != null) {
			final ITextSelection selection = (ITextSelection) editor.getViewer().getSelection();
			if (execute(editor, new Region(selection.getOffset(), selection.getLength()))) {
				return null;
			}
		}
		Display.getCurrent().beep();
		return null;
	}
	
	public abstract boolean execute(final ISourceEditor editor, final IRegion selection);
	
	
	protected void logError(final Exception e, final String name) {
		StatusManager.getManager().handle(new Status(IStatus.INFO, SharedUIResources.PLUGIN_ID, -1,
				NLS.bind("An error occurred when opening editor for the declaration of ''{0}''", name), e));
	}
	
}
