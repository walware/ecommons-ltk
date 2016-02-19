/*=============================================================================#
 # Copyright (c) 2015-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;


public class AssistCompletionInformationProposalWrapper implements IAssistInformationProposal,
		IContextInformationExtension {
	
	
	private final IAssistCompletionProposal completionProposal;
	
	private AssistInvocationContext context;
	
	
	public AssistCompletionInformationProposalWrapper(
			final IAssistCompletionProposal completionProposal,
			final AssistInvocationContext context) {
		if (completionProposal == null) {
			throw new NullPointerException("completionProposal"); //$NON-NLS-1$
		}
		this.completionProposal= completionProposal;
		this.context= context;
	}
	
	
	@Override
	public Image getImage() {
		return this.completionProposal.getImage();
	}
	
	@Override
	public String getContextDisplayString() {
		return this.completionProposal.getDisplayString();
	}
	
	
	public IContextInformation getContextInformation() {
		if (this.context != null) {
			try {
				this.completionProposal.apply(this.context.getSourceViewer(), SWT.CR, 0,
						this.context.getInvocationOffset() );
			}
			finally {
				this.context= null;
			}
		}
		return this.completionProposal.getContextInformation();
	}
	
	@Override
	public String getInformationDisplayString() {
		final IContextInformation contextInformation= getContextInformation();
		return contextInformation.getInformationDisplayString();
	}
	
	@Override
	public int getContextInformationPosition() {
		final IContextInformation contextInformation= getContextInformation();
		return (contextInformation instanceof IContextInformationExtension) ?
				((IContextInformationExtension) contextInformation).getContextInformationPosition() :
				-1;
	}
	
	
	@Override
	public int hashCode() {
		return this.completionProposal.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		return (this == obj
				|| (obj instanceof AssistCompletionInformationProposalWrapper
						&& this.completionProposal.equals(((AssistCompletionInformationProposalWrapper) obj).completionProposal) )
				);
	}
	
}
