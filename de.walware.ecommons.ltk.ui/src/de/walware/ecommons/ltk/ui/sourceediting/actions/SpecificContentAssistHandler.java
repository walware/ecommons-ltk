/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;

import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditorCommandIds;
import de.walware.ecommons.ltk.ui.sourceediting.assist.ContentAssist;


/**
 * A content assist executor can invoke content assist for a specific proposal category on an editor.
 */
public final class SpecificContentAssistHandler extends AbstractHandler {
	
	
	private final ISourceEditor editor;
	
	private final ContentAssist ContentAssist;
	
	
	/**
	 * Creates a new handler.
	 *
	 * @param registry the computer registry to use for the enablement of proposal categories
	 */
	public SpecificContentAssistHandler(final ISourceEditor editor, final ContentAssist ContentAssist) {
		this.editor= editor;
		this.ContentAssist= ContentAssist;
	}
	
	
	/**
	 * Invokes content assist on <code>editor</code>, showing only proposals computed by the
	 * <code>CompletionProposalCategory</code> with the given <code>categoryId</code>.
	 *
	 * @param editor the editor to invoke code assist on
	 * @param categoryId the id of the proposal category to show proposals for
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final String par= event.getParameter(ISourceEditorCommandIds.SPECIFIC_CONTENT_ASSIST_CATEGORY_PARAMETER_ID);
		if (par == null || !UIAccess.isOkToUse(this.editor.getViewer())) {
			return null;
		}
		
		final ITextOperationTarget target= (ITextOperationTarget) this.editor.getAdapter(ITextOperationTarget.class);
		if (target != null && target.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS)) {
			this.ContentAssist.showPossibleCompletions(par);
		}
		
		return null;
	}
	
}
