/*******************************************************************************
 * Copyright (c) 2000-2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Wahlbrink - removed editor dependencies
 *******************************************************************************/

package de.walware.ecommons.text.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.source.ISourceViewer;


/**
 * Command handler for source viewers showing the information popup.
 * 
 * This action behaves in two different ways: If there is no current text hover, the information
 * is displayed using information presenter. If there is a current text hover, it is converted 
 * into a information presenter in order to make it sticky.
 */
public final class InformationDispatchHandler extends AbstractHandler {
	
	
	private final ISourceViewer fSourceViewer;
	
	/** The wrapped text operation action. */
	private final TextViewerAction fTextOperationAction;
	
	
	public InformationDispatchHandler(final ISourceViewer sourceViewer) {
		if (sourceViewer == null) {
			throw new NullPointerException("viewer"); //$NON-NLS-1$
		}
		fSourceViewer = sourceViewer;
		fTextOperationAction = new TextViewerAction(sourceViewer, ISourceViewer.INFORMATION);
	}
	
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		if (fSourceViewer instanceof ITextViewerExtension4) {
			final ITextViewerExtension4 extension4 = (ITextViewerExtension4) fSourceViewer;
			if (extension4.moveFocusToWidgetToken()) {
				return null;
			}
		}
		// otherwise, just run the action
		if (fTextOperationAction.isEnabled()) {
			fTextOperationAction.run();
		}
		return null;
	}
	
}
