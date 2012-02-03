/*******************************************************************************
 * Copyright (c) 2007-2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.util;

import org.eclipse.jface.util.DelegatingDragAdapter;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;

import de.walware.ecommons.ui.util.SelectionTransferDragAdapter;


public class ViewerDragSupport {
	
	
	private final StructuredViewer fViewer;
	private final DelegatingDragAdapter fDelegatingAdapter;
	
	private boolean fInitialized;
	
	
	public ViewerDragSupport(final StructuredViewer viewer) {
		fViewer = viewer;
		
		fDelegatingAdapter = new DelegatingDragAdapter();
		fDelegatingAdapter.addDragSourceListener(new SelectionTransferDragAdapter(fViewer));
//		fDelegatingAdapter.addDragTargetListener(new FileTransferDropAdapter(fViewer));
		
		fInitialized = false;
	}
	
	
	public void addDragSourceListener(final TransferDragSourceListener listener) {
		assert (!fInitialized);
		
		fDelegatingAdapter.addDragSourceListener(listener);
	}
	
	public void init() {
		assert (!fInitialized);
		
		fViewer.addDragSupport((DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK),
				fDelegatingAdapter.getTransfers(), fDelegatingAdapter );
		
		fInitialized = true;
	}
	
}
