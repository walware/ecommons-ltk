/*******************************************************************************
 * Copyright (c) 2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting.actions;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.ISourceElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.IWorkspaceSourceUnit;


public class OpenDeclaration {
	
	
	public OpenDeclaration() {
	}
	
	
	public <T> T selectElement(final List<? extends T> list, final IWorkbenchPart part)
			throws CoreException {
		if (list.isEmpty()) {
			return null;
		}
		else if (list.size() == 1) {
			return list.get(0);
		}
		else {
			final ListDialog dialog = new ListDialog(part != null ? part.getSite().getShell() : UIAccess.getActiveWorkbenchShell(true));
			dialog.setTitle("Open Declaration");
			dialog.setMessage("Select the appropriate declaration:");
			dialog.setHelpAvailable(false);
			dialog.setContentProvider(new ArrayContentProvider());
			dialog.setLabelProvider(createLabelProvider());
			dialog.setInput(list);
			dialog.setInitialSelections(new Object[] { list.get(0) });
			
			if (dialog.open() == Dialog.OK) {
				return (T) dialog.getResult()[0];
			}
			else {
				throw new CoreException(Status.CANCEL_STATUS);
			}
		}
	}
	
	public ILabelProvider createLabelProvider() {
		return new LabelProvider();
	}
	
	public void open(final ISourceElement element, final boolean activate) throws PartInitException  {
		final ISourceUnit su = element.getSourceUnit();
		if (su instanceof IWorkspaceSourceUnit) {
			final IResource resource = ((IWorkspaceSourceUnit) su).getResource();
			if (resource.getType() == IResource.FILE) {
				open((IFile) resource, activate, element.getNameSourceRange());
				return;
			}
		}
	}
	
	public void open(final IFile file, final boolean activate, final IRegion region) throws PartInitException  {
		final IWorkbenchPage page = UIAccess.getActiveWorkbenchPage(true);
		final IEditorDescriptor editorDescriptor = IDE.getEditorDescriptor(file, true);
		final FileEditorInput input = new FileEditorInput(file);
		IEditorPart editorPart = page.findEditor(input);
		if (editorPart == null || !(editorPart instanceof ITextEditor)) {
			editorPart = page.openEditor(input, editorDescriptor.getId(), activate);
		}
		else if (activate) {
			page.activate(editorPart);
		}
		if (editorPart instanceof ITextEditor) {
			((ITextEditor) editorPart).selectAndReveal(region.getOffset(), region.getLength());
		}
	}
	
}
