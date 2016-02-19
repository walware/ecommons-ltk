/*=============================================================================#
 # Copyright (c) 2013-2016 Stephan Wahlbrink (WalWare.de) and others.
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
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ui.IWorkbenchPart;

import de.walware.ecommons.ui.util.UIAccess;
import de.walware.ecommons.workbench.ui.WorkbenchUIUtil;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class SourceEditorOperationHandler extends AbstractHandler {
	
	
	private final int viewerOperation;
	
	
	public SourceEditorOperationHandler(final int viewerOperation) {
		this.viewerOperation = viewerOperation;
	}
	
	
	protected ISourceEditor getSourceEditor(final Object context) {
		final IWorkbenchPart part = WorkbenchUIUtil.getActivePart(context);
		if (part == null) {
			return null;
		}
		return (ISourceEditor) part.getAdapter(ISourceEditor.class);
	}
	
	@Override
	public void setEnabled(final Object evaluationContext) {
		final ISourceEditor editor = getSourceEditor(evaluationContext);
		if (editor == null) {
			setBaseEnabled(false);
			return;
		}
		final SourceViewer viewer = editor.getViewer();
		setBaseEnabled(UIAccess.isOkToUse(viewer)
				&& viewer.canDoOperation(this.viewerOperation) );
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISourceEditor editor = getSourceEditor(event.getApplicationContext());
		if (editor == null) {
			return null;
		}
		final SourceViewer viewer = editor.getViewer();
		if (UIAccess.isOkToUse(viewer)) {
			viewer.doOperation(this.viewerOperation);
		}
		return null;
	}
	
}
