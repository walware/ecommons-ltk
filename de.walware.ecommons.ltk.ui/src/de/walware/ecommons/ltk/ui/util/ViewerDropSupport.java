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

package de.walware.ecommons.ltk.ui.util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.DelegatingDropAdapter;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;

import de.walware.ecommons.ui.util.PluginTransferDropAdapter;

import de.walware.ecommons.ltk.core.refactoring.CommonRefactoringFactory;


public class ViewerDropSupport {
	
	
	private final StructuredViewer fViewer;
	private final DelegatingDropAdapter fDelegatingAdapter;
	
	private final ViewerSelectionTransferDropAdapter fReorgDropListener;
	
	private boolean fInitialized;
	
	
	public ViewerDropSupport(final StructuredViewer viewer, final IAdaptable part,
			final CommonRefactoringFactory refactoring) {
		fViewer = viewer;
		
		fDelegatingAdapter = new DelegatingDropAdapter();
		fReorgDropListener = new ViewerSelectionTransferDropAdapter(fViewer, part, refactoring);
		fReorgDropListener.setFeedbackEnabled(true);
		fDelegatingAdapter.addDropTargetListener(fReorgDropListener);
//		fDelegatingAdapter.addDropTargetListener(new FileTransferDropAdapter(fViewer));
		fDelegatingAdapter.addDropTargetListener(new PluginTransferDropAdapter(fViewer));
		
		fInitialized = false;
	}
	
	
	public void addDropTargetListener(final TransferDropTargetListener listener) {
		assert (!fInitialized);
		
		fDelegatingAdapter.addDropTargetListener(listener);
	}
	
	public void init() {
		Assert.isLegal(!fInitialized);
		
		fViewer.addDropSupport((DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_DEFAULT),
				fDelegatingAdapter.getTransfers(), fDelegatingAdapter);
		
		fInitialized = true;
	}
	
	public void setFeedbackEnabled(final boolean enabled) {
		fReorgDropListener.setFeedbackEnabled(enabled);
	}
	
}
