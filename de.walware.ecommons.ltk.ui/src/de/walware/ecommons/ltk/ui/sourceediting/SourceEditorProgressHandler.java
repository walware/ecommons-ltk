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

package de.walware.ecommons.ltk.ui.sourceediting;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


public abstract class SourceEditorProgressHandler extends AbstractHandler {
	
	
	private final ISourceEditor fEditor;
	
	
	public SourceEditorProgressHandler(final ISourceEditor editor) {
		fEditor = editor;
	}
	
	
	protected abstract String getTaskLabel();
	
	protected abstract boolean isEditTask();
	
	
	protected ISourceEditor getEditor(final Object context) {
		return fEditor;
	}
	
	@Override
	public void setEnabled(final Object evaluationContext) {
		final ISourceEditor editor = getEditor(evaluationContext);
		setBaseEnabled(editor != null
				&& (!isEditTask() || editor.isEditable(false)) );
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISourceEditor editor = getEditor(event.getApplicationContext());
		if (editor == null) {
			return null;
		}
		if (!editor.isEditable(true)) {
			return null;
		}
		final ISourceUnit su = editor.getSourceUnit();
		final ITextSelection selection = (ITextSelection) editor.getViewer().getSelection();
		if (su == null || selection == null) {
			return null;
		}
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						doExecute(editor, su, selection, monitor);
					}
					catch (final Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		catch (final InvocationTargetException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, -1,
					NLS.bind(EditingMessages.GenericAction_error_message, getTaskLabel()),
					e.getTargetException() ));
		}
		catch (final InterruptedException e) {}
		return null;
	}
	
	
	protected abstract void doExecute(ISourceEditor editor, ISourceUnit su,
			ITextSelection selection, IProgressMonitor monitor) throws Exception;
	
}
