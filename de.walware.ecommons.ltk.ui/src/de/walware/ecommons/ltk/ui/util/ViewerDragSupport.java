/*=============================================================================#
 # Copyright (c) 2007-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.util;

import java.util.Iterator;

import org.eclipse.jface.util.DelegatingDragAdapter;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import de.walware.ecommons.ui.util.SelectionTransferDragAdapter;


public class ViewerDragSupport {
	
	
	public static class TextDragSourceListener implements TransferDragSourceListener {
		
		
		private final StructuredViewer fViewer;
		
		private String fText;
		
		
		public TextDragSourceListener(final StructuredViewer viewer) {
			fViewer = viewer;
		}
		
		
		@Override
		public Transfer getTransfer() {
			return TextTransfer.getInstance();
		}
		
		@Override
		public void dragStart(final DragSourceEvent event) {
			final IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
			final ILabelProvider labelProvider = (ILabelProvider) fViewer.getLabelProvider();
			if (selection.isEmpty()) {
				event.doit = false;
				return;
			}
			final Iterator iterator = selection.iterator();
//			final String sep = System.getProperty("line.separator"); //$NON-NLS-1$
			final String sep = ", "; //$NON-NLS-1$
			final StringBuilder sb = new StringBuilder();
			while (iterator.hasNext()) {
				sb.append(labelProvider.getText(iterator.next()));
				sb.append(sep);
			}
			fText = sb.substring(0, sb.length() - sep.length());
		}
		
		@Override
		public void dragSetData(final DragSourceEvent event) {
			event.data = fText;
		}
		
		@Override
		public void dragFinished(final DragSourceEvent event) {
			fText = ""; //$NON-NLS-1$
		}
		
	}
	
	
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
		
		fViewer.addDragSupport((DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK),
				fDelegatingAdapter.getTransfers(), fDelegatingAdapter );
		
		fInitialized = true;
	}
	
}
