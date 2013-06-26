/*******************************************************************************
 * Copyright (c) 2013 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class ReshowCompletionsRunnable implements Runnable, Listener, ICompletionListener {
	
	
	// Snapshot of current state
	private final ISourceEditor editor;
	private final ISourceViewer viewer;
	private final ISourceUnit su;
	private final AbstractDocument document;
	private final long documentStamp;
	private final Point selection;
	
	private final Display display;
	private final ContentAssist assist;
	
	private boolean assistSelection;
	
	
	public ReshowCompletionsRunnable(final ISourceEditor editor, final ContentAssist assist) {
		this.editor = editor;
		this.viewer = editor.getViewer();
		this.su = editor.getSourceUnit();
		this.document = (AbstractDocument) this.viewer.getDocument();
		this.documentStamp = this.document.getModificationStamp();
		this.selection = this.viewer.getSelectedRange();
		
		this.assist = assist;
		this.display = this.viewer.getTextWidget().getDisplay();
		
		this.display.addFilter(SWT.Verify, this);
		this.display.addFilter(SWT.FocusOut, this);
		this.assist.addCompletionListener(this);
	}
	
	
	protected void dispose() {
		if (this.display.isDisposed()) {
			return;
		}
		
		this.display.removeFilter(SWT.Verify, this);
		this.display.removeFilter(SWT.FocusOut, this);
		this.assist.removeCompletionListener(this);
	}
	
	private boolean isValid() {
		return (UIAccess.isOkToUse(ReshowCompletionsRunnable.this.viewer.getTextWidget())
				&& (this.viewer.getTextWidget().isFocusControl()
						|| this.assist.hasProposalPopupFocus())
				&& this.editor.getViewer() == this.viewer
				&& this.editor.getSourceUnit() == this.su
				&& this.viewer.getDocument() == this.document
				&& this.document.getModificationStamp() == this.documentStamp
				&& this.viewer.getSelectedRange().equals(this.selection) );
	}
	
	protected void cancel() {
		dispose();
	}
	
	@Override
	public void handleEvent(final Event event) {
		switch (event.type) {
		case SWT.FocusOut:
			this.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isValid()) {
						cancel();
					}
				}
			});
			break;
		default:
			cancel();
			break;
		}
	}
	
	@Override
	public void assistSessionStarted(final ContentAssistEvent event) {
		cancel();
	}
	
	@Override
	public void assistSessionEnded(final ContentAssistEvent event) {
		if (!this.assistSelection) {
			return;
		}
		cancel();
	}
	
	@Override
	public void selectionChanged(final ICompletionProposal proposal, final boolean smartToggle) {
		if (!this.assistSelection) {
			this.assistSelection = true;
			return;
		}
		cancel();
	}
	
	protected boolean showCompletionsNow() {
		return true;
	}
	
	@Override
	public void run() {
		if (this.display.isDisposed()) {
			return;
		}
		
		this.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				dispose();
				if (isValid() && showCompletionsNow()) {
					ReshowCompletionsRunnable.this.assist.showPossibleCompletions(true, true);
				}
			}
		});
	}
	
}
