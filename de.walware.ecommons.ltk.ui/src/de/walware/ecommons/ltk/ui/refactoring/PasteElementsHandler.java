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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.core.refactoring.CommonRefactoringFactory;
import de.walware.ecommons.ltk.core.refactoring.RefactoringAdapter;
import de.walware.ecommons.ltk.core.refactoring.RefactoringDestination;
import de.walware.ecommons.ltk.internal.ui.refactoring.Messages;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.util.WorkbenchUIUtil;


/**
 * Command handler pasting elements from clipboard at selected position.
 */
public class PasteElementsHandler extends AbstractElementsHandler {
	
	
	private final ISourceEditor fEditor;
	
	
	public PasteElementsHandler(final ISourceEditor editor,
			final CommonRefactoringFactory refactoring) {
		super(refactoring);
		fEditor = editor;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEnabled(final Object evaluationContext) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISelection selection = WorkbenchUIUtil.getCurrentSelection(event.getApplicationContext());
		if (selection == null) {
			return null;
		}
		final ISourceUnit su = fEditor.getSourceUnit();
		if (su == null) {
			return null;
		}
		RefactoringDestination destination = null;
		if (selection instanceof IStructuredSelection) {
			final IProgressMonitor monitor = new NullProgressMonitor();
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			final ISourceUnitModelInfo modelInfo = su.getModelInfo(fEditor.getModelTypeId(),
					IModelManager.MODEL_FILE, monitor );
			if (modelInfo == null) {
				return null;
			}
			if (structuredSelection.isEmpty()) {
				destination = new RefactoringDestination(modelInfo.getSourceElement());
			}
			else if (structuredSelection.size() == 1) {
				final Object object = structuredSelection.getFirstElement();
				destination = new RefactoringDestination(object);
			}
		}
		if (destination == null || !destination.isOK()) {
			return null;
		}
		final RefactoringAdapter adapter = fRefactoring.createAdapter(destination);
		if (adapter == null) {
			return null;
		}
		
		final String code = getCodeFromClipboard(event);
		if (code == null || code.length() == 0) {
			return null;
		}
		
		final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		final IWorkbenchPartSite site = activePart.getSite();
		final Shell shell = site.getShell();
		final IProgressService progressService = (IProgressService) site.getService(IProgressService.class);
		
		try {
			final Position position = startInsertRefactoring(code, destination, su, adapter, shell, progressService);
			if (position != null && !position.isDeleted()) {
				fEditor.selectAndReveal(position.getOffset(), 0);
			}
		}
		catch (final InvocationTargetException e) {
			StatusManager.getManager().handle(new Status(
					IStatus.ERROR, adapter.getPluginIdentifier(), -1,
					Messages.PastingElements_error_message,
					e.getCause() ),
					StatusManager.LOG | StatusManager.SHOW);
		}
		catch (final InterruptedException e) {
		}
		return null;
	}
	
	private Position startInsertRefactoring(final String code, final RefactoringDestination destination,
			final ISourceUnit su, final RefactoringAdapter adapter,
			final Shell shell, final IProgressService context)
			throws InvocationTargetException, InterruptedException {
		final RefactoringProcessor processor = fRefactoring.createPasteProcessor(code, destination, adapter);
		final Refactoring refactoring = new ProcessorBasedRefactoring(processor);
		final RefactoringExecutionHelper helper = new RefactoringExecutionHelper(refactoring, 
				RefactoringCore.getConditionCheckingFailedSeverity(), 
				shell, context );
		helper.enableInsertPosition(su);
		helper.perform(false, false);
		
		return helper.getInsertPosition();
	}
	
}
