/*******************************************************************************
 * Copyright (c) 2000-2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.participants.CopyProcessor;
import org.eclipse.ltk.core.refactoring.participants.CopyRefactoring;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.ISourceElement;
import de.walware.ecommons.ltk.ISourceStructElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.core.refactoring.CommonRefactoringFactory;
import de.walware.ecommons.ltk.core.refactoring.RefactoringAdapter;
import de.walware.ecommons.ltk.core.refactoring.RefactoringDestination;
import de.walware.ecommons.ltk.core.refactoring.RefactoringElementSet;
import de.walware.ecommons.ltk.internal.ui.refactoring.Messages;
import de.walware.ecommons.ltk.ui.refactoring.RefactoringExecutionHelper;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditorAssociated;


public class ViewerSelectionTransferDropAdapter extends ViewerDropAdapter implements TransferDropTargetListener {
	
	
	private final IAdaptable fPart;
	
	private final CommonRefactoringFactory fRefactoring;
	private RefactoringAdapter fAdapter;
	
	private RefactoringElementSet fElements;
	
	private MoveProcessor fMoveProcessor;
	private int fCanMoveElements;
	private CopyProcessor fCopyProcessor;
	private int fCanCopyElements;
	private ISelection fSelection;
	
	
	public ViewerSelectionTransferDropAdapter(final StructuredViewer viewer,
			final CommonRefactoringFactory refactoring) {
		this(viewer, null, refactoring);
	}
	
	public ViewerSelectionTransferDropAdapter(final StructuredViewer viewer, final IAdaptable part,
			final CommonRefactoringFactory refactoring) {
		super(viewer);
		fPart = part;
		
		fRefactoring = refactoring;
		
		setScrollEnabled(true);
		setExpandEnabled(true);
		setSelectionFeedbackEnabled(false);
		setFeedbackEnabled(false);
	}
	
	//---- TransferDropTargetListener interface ---------------------------------------
	
	
	@Override
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getTransfer();
	}
	
	@Override
	public boolean isEnabled(final DropTargetEvent event) {
		final Object target = event.item != null ? event.item.getData() : null;
		if (target == null) {
			return false;
		}
		return (target instanceof ISourceStructElement);
	}
	
	
	//---- Actual DND -----------------------------------------------------------------
	
	@Override
	public void dragEnter(final DropTargetEvent event) {
		clear();
		super.dragEnter(event);
	}
	
	@Override
	public void dragLeave(final DropTargetEvent event) {
		clear();
		super.dragLeave(event);
	}
	
	private void clear() {
		setSelectionFeedbackEnabled(false);
		fElements = null;
		fSelection = null;
		fMoveProcessor = null;
		fCanMoveElements = 0;
		fCopyProcessor = null;
		fCanCopyElements = 0;
		fAdapter = null;
	}
	
	@Override
	public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {
		final int result = internalDetermineOperation(target, operation,
				DND.DROP_MOVE | DND.DROP_COPY);
		
		if (result == DND.DROP_NONE) {
			setSelectionFeedbackEnabled(false);
			return false;
		}
		else {
			setSelectionFeedbackEnabled(true);
			overrideOperation(result);
			return true;
		}
	}
	
	private int internalDetermineOperation(final Object target, final int operation, final int operations) {
		if (!(target instanceof ISourceElement)) {
			return DND.DROP_NONE;
		}
		
		if (!initializeSelection()) {
			return DND.DROP_NONE;
		}
		
		if (fElements.getResources().size() > 0) { // resources not yet supported
			return DND.DROP_NONE;
		}
		fElements.removeElementsWithAncestorsOnList();
		
		RefactoringDestination.Position pos;
		switch (getCurrentLocation()) {
		case LOCATION_BEFORE:
			pos = RefactoringDestination.Position.ABOVE;
			break;
		case LOCATION_AFTER:
			pos = RefactoringDestination.Position.BELOW;
			break;
		default:
			pos = RefactoringDestination.Position.INTO;
		}
		final RefactoringDestination destination = new RefactoringDestination(target, pos);
		fAdapter = fRefactoring.createAdapter(destination);
		if (fAdapter == null || !fAdapter.canInsert(fElements, destination)) {
			return DND.DROP_NONE;
		}
		
		try {
			switch (operation) {
				case DND.DROP_DEFAULT:
					return handleValidateDefault(destination, operations);
				case DND.DROP_COPY:
					return handleValidateCopy(destination);
				case DND.DROP_MOVE:
					return handleValidateMove(destination);
			}
		}
		catch (final CoreException e) {
			if (e.getStatus().getSeverity() == IStatus.ERROR) { 
				StatusManager.getManager().handle(new Status(IStatus.ERROR,
						fAdapter.getPluginIdentifier(), -1,
						"An error occurred when validation the drop location.", e ));
			}
		}
		return DND.DROP_NONE;
	}
	
	protected boolean initializeSelection(){
		if (fElements != null) {
			return fElements.isOK();
		}
		final ISelection s = LocalSelectionTransfer.getTransfer().getSelection();
		if (!(s instanceof IStructuredSelection)) {
			return false;
		}
		fSelection = s;
		fElements = new RefactoringElementSet(((IStructuredSelection) s).toArray());
		if (!fElements.isOK()) {
			return false;
		}
		return true;
	}
	
	protected ISelection getSelection(){
		return fSelection;
	}
	
	@Override
	public boolean performDrop(final Object data) {
		switch(getCurrentOperation()) {
		case DND.DROP_MOVE:
			return handleDropMove();
		case DND.DROP_COPY:
			return handleDropCopy();
		}
		return false;
	}
	
	private int handleValidateDefault(final RefactoringDestination destination,
			final int operations) throws CoreException {
		if ((operations & DND.DROP_MOVE) != 0) {
			final int result = handleValidateMove(destination);
			if (result != DND.DROP_NONE) {
				return result;
			}
		}
		return handleValidateCopy(destination);
	}
	
	
	private int handleValidateMove(final RefactoringDestination destination) throws CoreException {
		if (fMoveProcessor == null) {
			final MoveProcessor processor = fRefactoring.createMoveProcessor(fElements, destination, fAdapter);
			if (processor != null && processor.isApplicable()) {
				fMoveProcessor = processor;
			}
		}
		return (canMoveElements()) ? DND.DROP_MOVE : DND.DROP_NONE;
	}
	
	private boolean canMoveElements() {
		if (fCanMoveElements == 0) {
			fCanMoveElements = (fMoveProcessor != null) ? 2 : 1;
		}
		return fCanMoveElements == 2;
	}
	
	protected boolean handleDropMove() {
		try {
			execute(new MoveRefactoring(fMoveProcessor));
			return true;
		}
		catch (final InvocationTargetException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR,
							fAdapter.getPluginIdentifier(), -1,
							Messages.MoveElements_error_message, e.getCause() ),
					StatusManager.LOG | StatusManager.SHOW );
			return false;
		}
		catch (final InterruptedException e) {
			return false;
		}
	}
	
	private int handleValidateCopy(final RefactoringDestination destination) throws CoreException {
		if (fCopyProcessor == null) {
			final CopyProcessor processor = fRefactoring.createCopyProcessor(fElements, destination, fAdapter);
			if (processor != null && processor.isApplicable()) {
				fCopyProcessor = processor;
			}
		}
		return (canCopyElements()) ? DND.DROP_COPY : DND.DROP_NONE;
	}
	
	private boolean canCopyElements() {
		if (fCanCopyElements == 0) {
			fCanCopyElements = (fCopyProcessor != null) ? 2 : 1;
		}
		return fCanCopyElements == 2;
	}
	
	protected boolean handleDropCopy() {
		try {
			execute(new CopyRefactoring(fCopyProcessor));
			return true;
		}
		catch (final InvocationTargetException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR,
							fAdapter.getPluginIdentifier(), -1,
							Messages.CopyElements_error_message, e.getCause() ),
					StatusManager.LOG | StatusManager.SHOW );
			return false;
		}
		catch (final InterruptedException e) {
			return false;
		}
	}
	
	protected void execute(final Refactoring refactoring) throws InterruptedException, InvocationTargetException {
		final IWorkbenchWindow window = UIAccess.getActiveWorkbenchWindow(true);
		final IProgressService context = (IProgressService) window.getService(IProgressService.class);
		final RefactoringExecutionHelper helper = new RefactoringExecutionHelper(refactoring, 
				RefactoringCore.getConditionCheckingFailedSeverity(), 
				getShell(), context );
		
		ISourceEditor editor = null;
		if (fPart != null) {
			editor = (ISourceEditor) fPart.getAdapter(ISourceEditor.class);
			if (editor == null) {
				final ISourceEditorAssociated associated = (ISourceEditorAssociated) fPart
						.getAdapter(ISourceEditorAssociated.class);
				if (associated != null) {
					editor = associated.getSourceEditor();
				}
			}
		}
		if (editor != null) {
			final ISourceUnit su = editor.getSourceUnit();
			if (su != null) {
				helper.enableInsertPosition(su);
			}
		}
		helper.perform(false, false);
		if (editor != null) {
			final Position position = helper.getInsertPosition();
			if (position != null) {
				editor.selectAndReveal(position.getOffset(), 0);
			}
		}
	}
	
	private Shell getShell() {
		return getViewer().getControl().getShell();
	}
	
	@Override
	protected int getCurrentLocation() {
		if (getFeedbackEnabled()) {
			return super.getCurrentLocation();
		}
		else {
			return LOCATION_ON;
		}
	}
	
}
