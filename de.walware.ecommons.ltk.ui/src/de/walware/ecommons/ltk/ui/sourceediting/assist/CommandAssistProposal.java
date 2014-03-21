/*=============================================================================#
 # Copyright (c) 2011-2014 Stephan Wahlbrink (WalWare.de) and others.
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
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import de.walware.ecommons.text.ui.DefaultBrowserInformationInput;
import de.walware.ecommons.workbench.ui.WorkbenchUIUtil;

import de.walware.ecommons.ltk.ui.sourceediting.ICommandAccess;


public abstract class CommandAssistProposal implements IAssistCompletionProposal, ICommandAccess,
		ICompletionProposalExtension5, ICompletionProposalExtension6 {
	
	
	public static StyledString addAcceleratorStyled(final String message, final KeySequence binding) {
		final StyledString styledString= new StyledString(message);
		if (binding != null) {
			styledString.append(" (", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			styledString.append(binding.format(), StyledString.QUALIFIER_STYLER);
			styledString.append(')', StyledString.QUALIFIER_STYLER);
		}
		return styledString;
	}
	
	
	private final AssistInvocationContext context;
	
	private final String commandId;
	
	private String label;
	private String description;
	
	private int relevance;
	
	
	public CommandAssistProposal(final AssistInvocationContext invocationContext,
			final String commandId) {
		this.context= invocationContext;
		this.commandId= commandId;
	}
	
	public CommandAssistProposal(final AssistInvocationContext invocationContext,
			final String commandId,
			final String label, final String description) {
		this(invocationContext, commandId);
		
		this.label= label;
		this.description= description;
	}
	
	
	@Override
	public final String getCommandId() {
		return this.commandId;
	}
	
	protected AssistInvocationContext getInvocationContext() {
		return this.context;
	}
	
	
	protected void setLabel(final String label) {
		this.label= label;
	}
	
	protected void setDescription(final String description) {
		this.description= description;
	}
	
	protected void setRelevance(final int relevance) {
		this.relevance= relevance;
	}
	
	
	@Override
	public void selected(final ITextViewer textViewer, final boolean smartToggle) {
	}
	
	@Override
	public void unselected(final ITextViewer textViewer) {
	}
	
	@Override
	public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
		return false;
	}
	
	
	@Override
	public void apply(final IDocument document) {
		throw new UnsupportedOperationException();
	}
	
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
	
	@Override
	public String getDisplayString() {
		return this.label;
	}
	
	@Override
	public StyledString getStyledDisplayString() {
		return addAcceleratorStyled(getDisplayString(), WorkbenchUIUtil.getBestKeyBinding(this.commandId));
	}
	
	@Override
	public Image getImage() {
		return null;
	}
	
	@Override
	public String getAdditionalProposalInfo() {
		return this.description;
	}
	
	@Override
	public Object getAdditionalProposalInfo(final IProgressMonitor monitor) {
		return new DefaultBrowserInformationInput(null, getDisplayString(), this.description, 
				DefaultBrowserInformationInput.FORMAT_TEXT_INPUT);
	}
	
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
	
}
