/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.HashMap;
import java.util.Map;


public class AssistProposalCollector {
	
	
	private final Map<IAssistCompletionProposal, IAssistCompletionProposal> proposals;
	
	
	public AssistProposalCollector() {
		this.proposals= new HashMap<>();
	}
	
	
	public void add(final IAssistCompletionProposal proposal) {
		final IAssistCompletionProposal existing= this.proposals.put(proposal, proposal);
		if (existing != null && existing.getRelevance() > proposal.getRelevance()) {
			this.proposals.put(existing, existing);
		}
	}
	
	public int getCount() {
		return this.proposals.size();
	}
	
	public IAssistCompletionProposal[] toArray() {
		return this.proposals.values().toArray(
				new IAssistCompletionProposal[this.proposals.size()] );
	}
	
	
}
