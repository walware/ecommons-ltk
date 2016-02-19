/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.util;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


/**
 * Util methods for Eclipse IDE workbench
 */
public class LTKWorkbenchUIUtil {
	
	
	public static AbstractDocument getDocument(final IWorkbenchPart part) {
		{	final ISourceEditor editor = (ISourceEditor) part.getAdapter(ISourceEditor.class);
			if (editor != null) {
				return (AbstractDocument) editor.getViewer().getDocument();
			}
		}
		if (part instanceof AbstractTextEditor) {
			final AbstractTextEditor textEditor = (AbstractTextEditor) part;
			final IDocumentProvider documentProvider = textEditor.getDocumentProvider();
			if (documentProvider != null) {
				return (AbstractDocument) documentProvider.getDocument(textEditor.getEditorInput());
			}
		}
		return null;
	}
	
	public static String getContentTypeId(final IWorkbenchPart part) {
		{	final IContentType contentType= (IContentType) part.getAdapter(IContentType.class);
			if (contentType != null) {
				return contentType.getId();
			}
		}
		if (part instanceof IEditorPart) {
			final IEditorInput input = ((IEditorPart) part).getEditorInput();
			{	final IFile file = ResourceUtil.getFile(input);
				if (file != null) {
					final IContentType contentType = IDE.guessContentType(file);
					return (contentType != null) ? contentType.getId() : null;
				}
			}
			{	String fileName = null;
				if (input instanceof IPathEditorInput) {
					fileName = ((IPathEditorInput) input).getPath().lastSegment();
				}
				else if (input instanceof IURIEditorInput) {
					fileName = URIUtil.lastSegment(((IURIEditorInput) input).getURI());
				}
				if (fileName != null) {
					final IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(fileName);
					return (contentType != null) ? contentType.getId() : null;
				}
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
	
	
	private LTKWorkbenchUIUtil() {}
	
}
