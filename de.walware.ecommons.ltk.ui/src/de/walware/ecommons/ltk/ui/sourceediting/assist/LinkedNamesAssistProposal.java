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

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import de.walware.ecommons.text.ui.DefaultBrowserInformationInput;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.LTKUI;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public abstract class LinkedNamesAssistProposal implements IAssistCompletionProposal,
		ICompletionProposalExtension5 {
	
	
	/**
	 * An exit policy that skips Backspace and Delete at the beginning and at the end
	 * of a linked position, respectively.
	 */
	public static class DeleteBlockingExitPolicy implements IExitPolicy {
		
		private final IDocument document;
		
		public DeleteBlockingExitPolicy(final IDocument document) {
			this.document= document;
		}
		
		@Override
		public ExitFlags doExit(final LinkedModeModel model, final VerifyEvent event,
				final int offset, final int length) {
			switch (event.character) {
			case SWT.BS:
				{	//skip backspace at beginning of linked position
					final LinkedPosition position= model.findPosition(new LinkedPosition(
							this.document, offset, 0, LinkedPositionGroup.NO_STOP));
					if (position != null && offset <= position.getOffset() && length == 0) {
						event.doit= false;
					}
					return null;
				}
			case SWT.DEL:
				{	//skip delete at end of linked position
					final LinkedPosition position= model.findPosition(new LinkedPosition(
							this.document, offset, 0, LinkedPositionGroup.NO_STOP));
					if (position != null && offset >= position.getOffset()+position.getLength() && length == 0) {
						event.doit= false;
					}
					return null;
				}
			}
			return null;
		}
	}
	
	
	private final AssistInvocationContext context;
	
	private String label;
	private String description;
	private int relevance;
	
	private String valueSuggestion;
	
	
	public LinkedNamesAssistProposal(final AssistInvocationContext invocationContext) {
		this.context= invocationContext;
	}
	
	
	protected void init(final String label, final String description, final int relevance) {
		this.label= label;
		this.description= description;
		this.relevance= relevance;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void selected(final ITextViewer textViewer, final boolean smartToggle) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unselected(final ITextViewer textViewer) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void apply(final ITextViewer viewer, final char trigger, final int stateMask, final int offset) {
		try {
			Point seletion= viewer.getSelectedRange();
			final IDocument document= viewer.getDocument();
			
			final LinkedModeModel model= new LinkedModeModel();
			
			final LinkedPositionGroup group= new LinkedPositionGroup();
			collectPositions(document, group);
			if (group.isEmpty()) {
				return;
			}
			model.addGroup(group);
			
			model.forceInstall();
			{	final ISourceEditor editor= this.context.getEditor();
				if (editor != null && editor.getTextEditToolSynchronizer() != null) {
					editor.getTextEditToolSynchronizer().install(model);
				}
			}
			
			final LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
			ui.setExitPolicy(new DeleteBlockingExitPolicy(document));
			ui.setExitPosition(viewer, offset, 0, LinkedPositionGroup.NO_STOP);
			ui.enter();
			
			if (this.valueSuggestion != null) {
				final Position position= group.getPositions()[0];
				document.replace(position.getOffset(), position.getLength(), this.valueSuggestion);
				seletion= new Point(position.getOffset(), this.valueSuggestion.length());
			}
			
			viewer.setSelectedRange(seletion.x, seletion.y); // by default full word is selected, restore original selection
		}
		catch (final BadLocationException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, -1, "Error initializing linked rename.", e)); //$NON-NLS-1$
		}
	}
	
	protected abstract void collectPositions(final IDocument document, final LinkedPositionGroup group)
			throws BadLocationException;
	
	protected int addPosition(final LinkedPositionGroup group, final IDocument document,
			final Position position, final int idx) throws BadLocationException {
		if (position != null) {
			group.addPosition(new LinkedPosition(document, position.getOffset(), position.getLength(), idx));
			return idx+1;
		}
		return idx;
	}
	
	@Override
	public void apply(final IDocument document) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Point getSelection(final IDocument document) {
		return null;
	}
	
	
	@Override
	public int getRelevance() {
		return this.relevance;
	}
	
	@Override
	public String getSortingString() {
		return this.label;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayString() {
		return this.label;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Image getImage() {
		return LTKUI.getImages().get(LTKUI.OBJ_TEXT_LINKEDRENAME_IMAGE_ID);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAdditionalProposalInfo() {
		return this.description;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAdditionalProposalInfo(final IProgressMonitor monitor) {
		return new DefaultBrowserInformationInput(null, getDisplayString(), this.description, 
				DefaultBrowserInformationInput.FORMAT_TEXT_INPUT);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
	
	
}
