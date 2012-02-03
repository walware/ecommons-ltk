/*******************************************************************************
 * Copyright (c) 2011-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

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

import de.walware.ecommons.ltk.ui.util.WorkbenchUIUtil;


public abstract class CommandAssistProposal implements IAssistCompletionProposal, ICommandAccess,
		ICompletionProposalExtension5, ICompletionProposalExtension6 {
	
	
	public static StyledString addAcceleratorStyled(final String message, final KeySequence binding) {
		final StyledString styledString = new StyledString(message);
		if (binding != null) {
			styledString.append(" (", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			styledString.append(binding.format(), StyledString.QUALIFIER_STYLER);
			styledString.append(')', StyledString.QUALIFIER_STYLER);
		}
		return styledString;
	}
	
	
	protected final AssistInvocationContext fContext;
	
	protected final String fCommandId;
	
	protected String fLabel;
	protected String fDescription;
	
	protected int fRelevance;
	
	
	public CommandAssistProposal(final AssistInvocationContext invocationContext,
			final String commandId) {
		fContext = invocationContext;
		fCommandId = commandId;
	}
	
	
	@Override
	public final String getCommandId() {
		return fCommandId;
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
		return fRelevance;
	}
	
	@Override
	public String getSortingString() {
		return fLabel;
	}
	
	@Override
	public String getDisplayString() {
		return fLabel;
	}
	
	@Override
	public StyledString getStyledDisplayString() {
		return addAcceleratorStyled(getDisplayString(), WorkbenchUIUtil.getBestKeyBinding(fCommandId));
	}
	
	@Override
	public Image getImage() {
		return null;
	}
	
	@Override
	public String getAdditionalProposalInfo() {
		return fDescription;
	}
	
	@Override
	public Object getAdditionalProposalInfo(final IProgressMonitor monitor) {
		return new DefaultBrowserInformationInput(null, getDisplayString(), fDescription, 
				DefaultBrowserInformationInput.FORMAT_TEXT_INPUT);
	}
	
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
	
}
