/*******************************************************************************
 * Copyright (c) 2011 WalWare/StatET-Project (www.walware.de/goto/statet).
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
import org.eclipse.core.runtime.IStatus;


/**
 * Computes quick fix or assist information displayed by the editor quick assistant.
 * Contributions to the extension point <code>de.walware.ecommons.ltk.advancedQuickAssist</code>
 * must implement this interface.
 */
public interface IQuickAssistComputer {
	
	
	/**
	 * Returns a list of assist proposals valid at the given invocation context.
	 * 
	 * @param context the context of the quick assist invocation
	 * @param proposals a set collecting the completion proposals
	 * @param monitor a progress monitor to report progress. The monitor is private to this
	 *     invocation, i.e. there is no need for the receiver to spawn a sub monitor.
	 */
	IStatus computeAssistProposals(AssistInvocationContext context,
			AssistProposalCollector<IAssistCompletionProposal> proposals,
			IProgressMonitor monitor);
	
}
