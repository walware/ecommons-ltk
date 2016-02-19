/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import de.walware.ecommons.ui.SharedMessages;
import de.walware.ecommons.workbench.ui.WorkbenchUIUtil;


/**
 * LTK content assistant.
 */
public class ContentAssist extends ContentAssistant {
	
	
	private static final int ON=                            1 << 0;
	
	private static final int AUTO_REQUEST=                  1 << 3;
	
	private static final int SPECIFIC_SESSION=              1 << 5;
	
	private static final int RELOAD_REQUEST=                1 << 6;
	
	
	
	abstract static class Processor implements IContentAssistProcessor {
		
		
		private final ContentAssist assist;
		
		private final String contentType;
		
		
		public Processor(final ContentAssist assist, final String contentType) {
			assert (assist != null);
			assert (contentType != null);
			
			this.assist= assist;
			
			this.contentType= contentType;
		}
		
		
		public final ContentAssist getAssistant() {
			return this.assist;
		}
		
		public final String getContentType() {
			return this.contentType;
		}
		
		@Override
		public final ICompletionProposal[] computeCompletionProposals(
				final ITextViewer viewer, final int offset) {
			this.assist.onCompletionProposalComputeBegin(getContentType());
			try {
				return doComputeCompletionProposals(viewer, offset);
			}
			finally {
				this.assist.onCompletionProposalComputeEnd(getContentType());
			}
		}
		
		protected abstract ICompletionProposal[] doComputeCompletionProposals(
				final ITextViewer viewer, final int offset);
		
		@Override
		public final IContextInformation[] computeContextInformation(
				final ITextViewer viewer, final int offset) {
			this.assist.onContextInformationComputeBegin(getContentType());
			try {
				return doComputeContextInformation(viewer, offset);
			}
			finally {
				this.assist.onContextInformationComputeEnd(getContentType());
			}
		}
		
		protected abstract IContextInformation[] doComputeContextInformation(
				final ITextViewer viewer, final int offset);
		
	}
	
	private static class WrappedProcessor extends Processor {
		
		
		private final IContentAssistProcessor processor;
		
		
		public WrappedProcessor(final ContentAssist assist, final String contentType,
				final IContentAssistProcessor processor) {
			super(assist, contentType);
			this.processor= processor;
		}
		
		
		@Override
		public char[] getCompletionProposalAutoActivationCharacters() {
			return this.processor.getCompletionProposalAutoActivationCharacters();
		}
		
		@Override
		public char[] getContextInformationAutoActivationCharacters() {
			return this.processor.getContextInformationAutoActivationCharacters();
		}
		
		@Override
		protected ICompletionProposal[] doComputeCompletionProposals(
				final ITextViewer viewer, final int offset) {
			return this.processor.computeCompletionProposals(viewer, offset);
		}
		
		@Override
		protected IContextInformation[] doComputeContextInformation(final ITextViewer viewer,
				final int offset) {
			return this.processor.computeContextInformation(viewer, offset);
		}
		
		@Override
		public String getErrorMessage() {
			return this.processor.getErrorMessage();
		}
		
		@Override
		public IContextInformationValidator getContextInformationValidator() {
			return this.processor.getContextInformationValidator();
		}
		
	}
	
	
	private class SelfListener implements ICompletionListener, ICompletionListenerExtension {
		
		
		@Override
		public void assistSessionStarted(final ContentAssistEvent event) {
			onCompletionProposalSessionBegin(event.isAutoActivated);
		}
		
		@Override
		public void assistSessionRestarted(final ContentAssistEvent event) {
		}
		
		@Override
		public void assistSessionEnded(final ContentAssistEvent event) {
			onCompletionProposalSessionEnd();
		}
		
		@Override
		public void selectionChanged(final ICompletionProposal proposal, final boolean smartToggle) {
			if (proposal != ContentAssist.this.completionProposalSelection) {
				ContentAssist.this.completionProposalSelection= proposal;
				ContentAssist.this.completionProposalSelectionCounter++;
			}
		}
		
	}
	
	
	private boolean isAutoInsertEnabled;
	private boolean isAutoInsertOverwritten;
	
	private boolean isRepeatedInvocationModeEnabled;
	
	private int completionProposalSessionRequest;
	private int completionProposalSession;
	private int completionProposalSessionCounter;
	
	private String specificCategoryId;
	
	private ICompletionProposal completionProposalSelection;
	private int completionProposalSelectionCounter;
	
	private KeySequence completionProposalKeyBinding;
	private String completionProposalIterationGesture;
	
	private int contextInformationSession;
	
	
	public ContentAssist() {
		addCompletionListener(new SelfListener());
	}
	
	
	boolean isProposalPopupActive1() {
		return super.isProposalPopupActive();
	}
	
	boolean isContextInfoPopupActive1() {
		return super.isContextInfoPopupActive();
	}
	
	void hidePopups() {
		super.hide();
	}
	
	
	private void onCompletionProposalSessionBegin(final boolean isAutoActivated) {
		this.completionProposalSession= (this.completionProposalSessionRequest != 0) ?
				ContentAssist.this.completionProposalSessionRequest :
				(isAutoActivated) ? (ON | AUTO_REQUEST) : (ON);
		this.completionProposalSessionRequest= 0;
		
		this.completionProposalSessionCounter++;
		
		this.completionProposalSelectionCounter= 0;
		
		this.completionProposalKeyBinding= WorkbenchUIUtil.getBestKeyBinding(
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS );
		setRepeatedInvocationTrigger(this.completionProposalKeyBinding);
		this.completionProposalIterationGesture= (this.completionProposalKeyBinding != null) ?
				NLS.bind(SharedMessages.Affordance_Press_message, this.completionProposalKeyBinding.format()) :
				SharedMessages.Affordance_Click_message;
	}
	
	private void onCompletionProposalComputeBegin(final String contentType) {
		this.completionProposalSessionCounter= 0;
		this.completionProposalSelection= null;
	}
	
	private void onCompletionProposalComputeEnd(final String contentType) {
		this.completionProposalSession&= ~AUTO_REQUEST;
	}
	
	private void onCompletionProposalSessionEnd() {
		this.completionProposalSession= 0;
		
		this.completionProposalSelection= null;
		
		this.completionProposalKeyBinding= null;
		this.completionProposalIterationGesture= null;
	}
	
	private void onContextInformationComputeBegin(final String contentType) {
		if (this.contextInformationSession == 0) {
			this.contextInformationSession= (ON | AUTO_REQUEST);
		}
	}
	
	private void onContextInformationComputeEnd(final String contentType) {
		this.contextInformationSession= 0;
	}
	
	
	@Override
	public void setContentAssistProcessor(IContentAssistProcessor processor,
			final String contentType) {
		if (contentType == null) {
			throw new NullPointerException("contentType"); //$NON-NLS-1$
		}
		if (processor != null && !(processor instanceof Processor)) {
			processor= new WrappedProcessor(this, contentType, processor);
		}
		super.setContentAssistProcessor(processor, contentType);
	}
	
	@Override
	public void enableAutoInsert(final boolean enabled) {
		this.isAutoInsertEnabled= enabled;
		if (!this.isAutoInsertOverwritten) {
			super.enableAutoInsert(enabled);
		}
	}
	
	@Override
	public void setRepeatedInvocationMode(final boolean cycling) {
		this.isRepeatedInvocationModeEnabled= cycling;
		super.setRepeatedInvocationMode(cycling);
	}
	
	/**
	 * Overwrites the current (user) setting temporarily and enables auto insert until it is reset
	 * by calling {@link #enableAutoInsertSetting()}.
	 * 
	 * @see #enableAutoInsert(boolean)
	 */
	void enableAutoInsertTemporarily() {
		this.isAutoInsertOverwritten= true;
		super.enableAutoInsert(true);
	}
	
	/**
	 * Disables the overwriting of auto insert enabled by {@link #enableAutoInsertTemporarily()}
	 * and resets it to the (user) setting.
	 * 
	 * @see #enableAutoInsert(boolean)
	 */
	void enableAutoInsertSetting() {
		if (this.isAutoInsertOverwritten) {
			this.isAutoInsertOverwritten= false;
			super.enableAutoInsert(this.isAutoInsertEnabled);
		}
	}
	
	
	public void showPossibleCompletions(final boolean restart, final boolean autostart) {
		class AutoAssist extends AutoAssistListener {
			
			public static final int SHOW_PROPOSALS= 1;
			
			@Override
			public void start(final int showStyle) {
				showAssist(showStyle);
			}
			
		}
		
		if (restart) {
			super.hide();
		}
		if (autostart) {
			new AutoAssist().start(AutoAssist.SHOW_PROPOSALS);
		}
		else {
			super.showPossibleCompletions();
		}
	}
	
	@Override
	public String showPossibleCompletions() {
		this.completionProposalSessionRequest= (ON);
		try {
			return super.showPossibleCompletions();
		}
		finally {
			this.completionProposalSessionRequest= 0;
		}
	}
	
	public String showPossibleCompletions(final String categoryId) {
		if (categoryId == null) {
			throw new NullPointerException("categoryId"); //$NON-NLS-1$
		}
		this.completionProposalSessionRequest= (ON | SPECIFIC_SESSION);
		this.specificCategoryId= categoryId;
		try {
			return super.showPossibleCompletions();
		}
		finally {
			this.completionProposalSessionRequest= 0;
		}
	}
	
	void reloadPossibleCompletions() {
		if (this.completionProposalSession == 0
				|| (isCompletionProposalAutoRequest() && !isProposalPopupActive1())) {
			return;
		}
		
		this.completionProposalSession|= RELOAD_REQUEST;
		this.completionProposalSessionRequest= this.completionProposalSession;
		super.setRepeatedInvocationMode(true); // avoid start of new session
		try {
			super.showPossibleCompletions();
		}
		finally {
			this.completionProposalSession&= ~RELOAD_REQUEST;
			this.completionProposalSessionRequest= 0;
			super.setRepeatedInvocationMode(this.isRepeatedInvocationModeEnabled);
		}
	}
	
	public final boolean isCompletionProposalAutoRequest() {
		return ((this.completionProposalSession & AUTO_REQUEST) != 0);
	}
	
	final boolean isCompletionProposalReloadRequest() {
		return ((this.completionProposalSession & RELOAD_REQUEST) != 0);
	}
	
	public final boolean isCompletionProposalSpecificSession() {
		return ((this.completionProposalSession & SPECIFIC_SESSION) != 0);
	}
	
	public final String getSpecificMode() {
		return (isCompletionProposalSpecificSession()) ? this.specificCategoryId : null;
	}
	
	public final int getCompletionProposalSessionCounter() {
		return this.completionProposalSessionCounter;
	}
	
	public KeySequence getCompletionProposalKeyBinding() {
		return this.completionProposalKeyBinding;
	}
	
	public String getCompletionProposalIterationGesture() {
		return this.completionProposalIterationGesture;
	}
	
	public final int getCompletionProposalSelectionCounter() {
		return this.completionProposalSelectionCounter;
	}
	
	
	@Override
	public String showContextInformation() {
		if (isCompletionProposalSpecificSession() && isProposalPopupActive()) {
			hide();
		}
		
		this.contextInformationSession= (ON);
		return super.showContextInformation();
	}
	
	public final boolean isContextInformationAutoRequest() {
		return ((this.contextInformationSession & AUTO_REQUEST) != 0);
	}
	
}
