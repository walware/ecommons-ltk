/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;


public interface IAssistCompletionProposal extends ICompletionProposal, ICompletionProposalExtension2 {
	
	/**
	 * Returns relevance of the proposal
	 * <p>
	 * Higher values indicates that it is likely more relevant.</p>
	 * 
	 * @return the relevance
	 */
	int getRelevance();
	
	/**
	 * Returns the string to use when sorting proposals
	 * 
	 * @return the sorting string
	 */
	String getSortingString();
	
}
