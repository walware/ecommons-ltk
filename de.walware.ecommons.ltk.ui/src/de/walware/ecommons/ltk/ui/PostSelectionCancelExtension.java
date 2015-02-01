/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextInputListener;


/**
 * Extension for {@link PostSelectionWithElementInfoController} for source viewers / documents
 * enabling suppression of selection notification if the document or its content changed.
 */
public abstract class PostSelectionCancelExtension implements ITextInputListener, IDocumentListener {
	
	
	PostSelectionWithElementInfoController fController;
	
	
	public abstract void init();
	public abstract void dispose();
	
	
	@Override
	public void inputDocumentAboutToBeChanged(final IDocument oldInput, final IDocument newInput) {
		if (oldInput != null) {
			oldInput.removeDocumentListener(this);
		}
		fController.cancel();
	}
	
	@Override
	public void inputDocumentChanged(final IDocument oldInput, final IDocument newInput) {
		if (newInput != null) {
			newInput.addDocumentListener(this);
		}
	}
	
	@Override
	public void documentAboutToBeChanged(final DocumentEvent event) {
		fController.cancel();
	}
	
	@Override
	public void documentChanged(final DocumentEvent event) {
	}
	
}
