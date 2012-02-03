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

package de.walware.ecommons.ltk.ui.util;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import de.walware.ecommons.ui.SharedUIResources;


/**
 * Util methods for Eclipse IDE workbench
 */
public class WorkbenchUIUtil {
	
	
	public static ISelection getCurrentSelection(final Object context) {
		if (context instanceof IEvaluationContext) {
			final IEvaluationContext evaluationContext = (IEvaluationContext) context;
			Object object = evaluationContext.getVariable(ISources.ACTIVE_SITE_NAME);
			if (object instanceof IWorkbenchSite) {
				final IWorkbenchSite site = (IWorkbenchSite) object;
				final ISelectionProvider selectionProvider = site.getSelectionProvider();
				if (selectionProvider != null) {
					return selectionProvider.getSelection();
				}
				return null;
			}
			else {
				object = evaluationContext.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
				if (object instanceof ISelection) {
					return (ISelection) object;
				}
			}
		}
		return null;
	}
	
	public static IWorkbenchPart getActivePart(final Object context) {
		if (context instanceof IEvaluationContext) {
			final Object object = ((IEvaluationContext) context).getVariable(ISources.ACTIVE_PART_NAME);
			if (object instanceof IWorkbenchPart) {
				return (IWorkbenchPart) object;
			}
		}
		return null;
	}
	
	public static void openEditor(final IWorkbenchPage page, final IFile file, final IRegion initialSelection) {
		final Display display = page.getWorkbenchWindow().getShell().getDisplay();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				IMarker marker;
				try { 
					marker = file.createMarker("de.walware.ecommons.resourceMarkers.InitialSelection"); //$NON-NLS-1$
					if (initialSelection != null) {
						marker.setAttribute(IMarker.CHAR_START, initialSelection.getOffset());
						marker.setAttribute(IMarker.CHAR_END, initialSelection.getOffset() + initialSelection.getLength());
					}
				}
				catch (final CoreException e) {
					marker = null;
				}
				try {
					if (marker != null) {
						IDE.openEditor(page, marker, true);
					}
					else {
						IDE.openEditor(page, file, true);
					}
				}
				catch (final PartInitException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, -1,
							NLS.bind("Could not open editor for ''{0}''", file.getName()), e));
				}
				if (marker != null) {
					try {
						marker.delete();
					}
					catch (final CoreException e) {
					}
				}
			}
		});
	}
	
	public static void indicateStatus(final IStatus status, final ExecutionEvent executionEvent) {
		if (status.isOK()) {
			return;
		}
		if (status.getMessage() != null && executionEvent != null) {
			final IWorkbenchPart workbenchPart = HandlerUtil.getActivePart(executionEvent);
			if (workbenchPart != null) {
				final IEditorStatusLine statusLine = (IEditorStatusLine) workbenchPart.getAdapter(IEditorStatusLine.class);
				if (statusLine != null) {
					statusLine.setMessage(status.getSeverity() == IStatus.ERROR, status.getMessage(), null);
				}
			}
		}
		if (status.getSeverity() == IStatus.ERROR) {
			Display.getCurrent().beep();
		}
	}
	
	public static KeySequence getBestKeyBinding(final String commandId) {
		final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		if (bindingSvc == null) {
			return null;
		}
		{	final TriggerSequence binding = bindingSvc.getBestActiveBindingFor(commandId);
			if (binding instanceof KeySequence) {
				return (KeySequence) binding;
			}
		}
		{	final TriggerSequence[] bindings = bindingSvc.getActiveBindingsFor(commandId);
			for (int i = 0; i < bindings.length; i++) {
				if (bindings[i] instanceof KeySequence) {
					return (KeySequence) bindings[i];
				}
			}
		}
		return null;
	}
	
	
	private WorkbenchUIUtil() {}
	
}
