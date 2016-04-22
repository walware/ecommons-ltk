/*=============================================================================#
 # Copyright (c) 2005-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #     Stephan Wahlbrink - adapted API and improvements
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ibm.icu.text.Collator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.FastList;
import de.walware.ecommons.ui.SharedMessages;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


/**
 * A content assist processor that aggregates the proposals of the {@link IContentAssistComputer}s
 * contributed via the extension point <code>de.walware.ecommons.ltk.advancedContentAssist</code>.
 * <p>
 * Subclasses may extend:
 * <ul>
 *   <li><code>createContext</code> to provide the context object passed to the computers</li>
 *   <li><code>createProgressMonitor</code> to change the way progress is reported</li>
 *   <li><code>filterAndSort</code> to add sorting and filtering</li>
 *   <li><code>getContextInformationValidator</code> to add context validation (needed if any
 *       contexts are provided)</li>
 *   <li><code>getErrorMessage</code> to change error reporting</li>
 * </ul></p>
 */
public class ContentAssistProcessor extends ContentAssist.Processor implements IContentAssistProcessor {
	
	
	private static final boolean DEBUG_LOG= Boolean.parseBoolean(
			Platform.getDebugOption("de.walware.ecommons.ltk/debug/ContentAssist/log") ); //$NON-NLS-1$
	
	private static final Collator NAME_COLLATOR= Collator.getInstance();
	
	static final Comparator<IAssistCompletionProposal> PROPOSAL_COMPARATOR= new Comparator<IAssistCompletionProposal>() {
		
		@Override
		public int compare(final IAssistCompletionProposal proposal1, final IAssistCompletionProposal proposal2) {
			final int diff= proposal2.getRelevance() - proposal1.getRelevance();
			if (diff != 0) {
				return diff; // reverse
			}
			return NAME_COLLATOR.compare(proposal1.getSortingString(), proposal2.getSortingString());
		}
		
	};
	
	private static final long INFO_ITER_SPAN= 3L * 1000000000L;
	
	
	/**
	 * The completion listener class for this processor.
	 */
	private final class CompletionListener implements ICompletionListener, ICompletionListenerExtension {
		
		@Override
		public void assistSessionStarted(final ContentAssistEvent event) {
			if (event.processor == ContentAssistProcessor.this) {
				startCompletionSession();
			}
		}
		
		@Override
		public void assistSessionRestarted(final ContentAssistEvent event) {
			if (isInCompletionProposalSession()) {
				if (event.processor == ContentAssistProcessor.this) {
					restartCompletionSession();
				}
				else {
					endCompletionSession();
				}
			}
		}
		
		@Override
		public void assistSessionEnded(final ContentAssistEvent event) {
			if (isInCompletionProposalSession()) {
				endCompletionSession();
			}
		}
		
		@Override
		public void selectionChanged(final ICompletionProposal proposal, final boolean smartToggle) {
		}
		
	}
	
	
	/**
	 * The completion proposal registry.
	 */
	private final ContentAssistComputerRegistry computerRegistry;
	
	private final ISourceEditor editor;
	
	private char[] completionProposalsAutoActivationCharacters;
	private char[] contextInformationAutoActivationCharacters;
	
	private boolean isCompletionProposalSession;
	private int sessionCounter;
	
	/* cycling stuff */
	private int iterationPos= -1;
	private boolean noIteration;
	private final FastList<ContentAssistCategory> expliciteCategories= new FastList<>(ContentAssistCategory.class);
	private List<ContentAssistCategory> availableCategories;
	private List<List<ContentAssistCategory>> categoryIteration;
	private IStatus status;
	
	private AssistInvocationContext completionProposalContext;
	
	private IContextInformationValidator contextInformationValidator;
	
	/* for detection if information mode is valid */
	private long informationModeTimestamp;
	private long informationModeModificationStamp;
	private int informationModeOffset;
	
	private ICompletionProposal[] reloadCompletionProposals;
	
	
	public ContentAssistProcessor(final ContentAssist assistant, final String contentType,
			final ContentAssistComputerRegistry registry, final ISourceEditor editor) {
		super(assistant, contentType);
		assert(registry != null);
		assert(editor != null);
		
		this.computerRegistry= registry;
		this.editor= editor;
		
		getAssistant().enableColoredLabels(true);
		getAssistant().addCompletionListener(new CompletionListener());
	}
	
	
	protected ISourceEditor getEditor() {
		return this.editor;
	}
	
	
	protected final boolean isInCompletionProposalSession() {
		return this.isCompletionProposalSession;
	}
	
	protected final int getSessionCounter() {
		return this.sessionCounter;
	}
	
	public void addCategory(final ContentAssistCategory category) {
		this.expliciteCategories.add(category);
	}
	
	@Override
	protected ICompletionProposal[] doComputeCompletionProposals(final ITextViewer viewer, final int offset) {
		if (!isInCompletionProposalSession()) {
			startCompletionSession();
		}
		
		clearState();
		
		if (this.reloadCompletionProposals != null) {
			return this.reloadCompletionProposals;
		}
		
		return computeCompletionProposals(offset);
	}
	
	private final ICompletionProposal[] computeCompletionProposals(final int offset) {
		final ContentAssist assistant= getAssistant();
		
		final long startTime= System.nanoTime();
		iterate();
		
		final SubMonitor m= SubMonitor.convert(createProgressMonitor());
		m.beginTask(EditingMessages.ContentAssistProcessor_ComputingProposals_task, 10);
		try {
			final AssistInvocationContext context= getCompletionProposalContext(offset, m.newChild(3));
			
			final long setup= (DEBUG_LOG) ? System.nanoTime() : 0;
			
			final long modificationStamp= ((AbstractDocument) context.getSourceViewer().getDocument()).getModificationStamp();
			final int mode;
			if (assistant.isCompletionProposalSpecificSession()) {
				mode= IContentAssistComputer.SPECIFIC_MODE;
			}
			else if (!assistant.isProposalPopupActive1()
					&& (   startTime - this.informationModeTimestamp > INFO_ITER_SPAN
						|| offset != this.informationModeOffset
						|| !assistant.isContextInfoPopupActive1()
						|| modificationStamp != this.informationModeModificationStamp)
					&& forceContextInformation(context)) {
				assistant.setRepeatedInvocationMode(true);
				assistant.setShowEmptyList(true);
				assistant.enableAutoInsertTemporarily();
				
				assistant.setStatusLineVisible(true);
				assistant.setStatusMessage(EditingMessages.ContentAssistProcessor_ContextSelection_label);
				
				return computeContextInformationProposals(context, modificationStamp, m);
			}
			else if (this.iterationPos > 0) {
				mode= IContentAssistComputer.SPECIFIC_MODE;
				
				assistant.enableAutoInsertSetting();
			}
			else {
				mode= IContentAssistComputer.COMBINED_MODE;
				
				assistant.enableAutoInsertSetting();
			}
			
			final List<ContentAssistCategory> categories= getCurrentCategories();
			
			m.setWorkRemaining(categories.size() + 1);
			m.subTask(EditingMessages.ContentAssistProcessor_ComputingProposals_Collecting_task);
			
			final AssistProposalCollector proposals= createProposalCollector();
			collectCompletionProposals(context, mode, categories, proposals, m);
			final long collect= (DEBUG_LOG) ? System.nanoTime() : 0;
			
			m.subTask(EditingMessages.ContentAssistProcessor_ComputingProposals_Sorting_task);
			final IAssistCompletionProposal[] result= filterAndSortCompletionProposals(proposals,
					context, m.newChild(1) );
			final long filter= (DEBUG_LOG) ? System.nanoTime() : 0;
			
			if (DEBUG_LOG) {
				final StringBuilder sb= new StringBuilder("Code Assist Stats"); //$NON-NLS-1$
				sb.append(" (").append(result.length).append(" proposals)"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("\n\t" + "setup=   ").append((setup - startTime)); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("\n\t" + "collect= ").append((collect - setup)); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("\n\t" + "sort=    ").append((filter - collect)); //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println(sb);
			}
			
			return result;
		}
		finally {
			m.done();
		}
	}
	
	private void startCompletionSession() {
		this.isCompletionProposalSession= true;
		this.sessionCounter++;
		
		final ContentAssist assistant= getAssistant();
		
		this.availableCategories= assistant.isCompletionProposalSpecificSession() ?
				this.computerRegistry.getCategory(assistant.getSpecificMode()) :
				this.computerRegistry.getCategories();
		this.categoryIteration= createCategoryIteration();
		
		notifySessionStarted();
		
		this.iterationPos= -1;
		
		if (this.categoryIteration.size() == 1) {
			assistant.setRepeatedInvocationMode(false);
			assistant.setShowEmptyList(false);
		}
		else {
			assistant.setRepeatedInvocationMode(true);
			assistant.setShowEmptyList(true);
			
			assistant.setStatusLineVisible(true);
			assistant.setStatusMessage(createIterationMessage(0));
		}
	}
	
	private void restartCompletionSession() {
		assert(this.isCompletionProposalSession);
		
		if (this.iterationPos >= 0) {
			this.iterationPos--;
		}
		
		this.completionProposalContext= null;
	}
	
	private void endCompletionSession() {
		assert(this.isCompletionProposalSession);
		
		this.isCompletionProposalSession= false;
		
		final ContentAssist assistant= getAssistant();
		
		notifySessionEnded();
		
		this.availableCategories= null;
		this.categoryIteration= null;
		this.iterationPos= -1;
		
		assistant.setRepeatedInvocationTrigger(null);
		assistant.setRepeatedInvocationMode(false);
		assistant.setShowEmptyList(false);
		assistant.enableAutoInsertSetting();
		assistant.setStatusLineVisible(false);
		
		this.completionProposalContext= null;
	}
	
	private void clearState() {
		this.status= null;
	}
	
	/**
	 * Collects the proposals.
	 * 
	 * @param context the code assist invocation context
	 * @param mode
	 * @param categories list of categories to use
	 * @param proposals collector for the proposals
	 * @param monitor the progress monitor
	 * @return the list of proposals
	 */
	private boolean collectCompletionProposals(
			final AssistInvocationContext context, final int mode,
			final List<ContentAssistCategory> categories,
			final AssistProposalCollector proposals, final SubMonitor progress) {
		for (final ContentAssistCategory category : categories) {
			final List<IContentAssistComputer> computers= category.getComputers(getContentType());
			final SubMonitor computorsProgress= progress.newChild(1);
			for (final IContentAssistComputer computer : computers) {
				try {
					final IStatus status= computer.computeCompletionProposals(context, mode,
							proposals, computorsProgress );
					if (status != null && status.getSeverity() >= IStatus.INFO
							&& (this.status == null || status.getSeverity() > this.status.getSeverity()) ) {
						this.status= status;
					}
				}
				catch (final Exception e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, LTK.PLUGIN_ID,
							"An error occurred when computing content assistant completion proposals.",
							e ));
				}
			}
		}
		return true;
	}
	
	
	@Override
	protected IContextInformation[] doComputeContextInformation(final ITextViewer viewer, final int offset) {
		clearState();
		
		final SubMonitor m= SubMonitor.convert(createProgressMonitor());
		m.beginTask(EditingMessages.ContentAssistProcessor_ComputingContexts_task, 10);
		
		try {
			if (!this.isCompletionProposalSession) {
				this.sessionCounter++;
				this.availableCategories= this.computerRegistry.getCategories();
				
				notifySessionStarted();
			}
			
			final AssistInvocationContext context= getContextInformationContext(offset, m.newChild(3));
			
			final long modificationStamp= (!getAssistant().isContextInformationAutoRequest()) ?
					((AbstractDocument) context.getSourceViewer().getDocument()).getModificationStamp() :
					0;
			final IAssistCompletionProposal[] completionProposals= computeContextInformationProposals(
					context, modificationStamp, m.newChild(6) );
			
			return toInformationProposals(completionProposals, context);
		}
		finally {
			if (!this.isCompletionProposalSession) {
				notifySessionEnded();
				this.availableCategories= null;
			}
			
			m.done();
		}
	}
	
	private IAssistCompletionProposal[] computeContextInformationProposals(
			final AssistInvocationContext context,
			final long modificationStamp,
			final SubMonitor m) {
		final ContentAssist assistant= getAssistant();
		
		final List<ContentAssistCategory> defaultGroup= new ArrayList<>();
		final List<ContentAssistCategory> otherGroup= new ArrayList<>();
		for (final ContentAssistCategory category : this.availableCategories) {
			if (category.isEnabledInDefault()) {
				defaultGroup.add(category);
			}
			else if (category.isEnabledInCircling()) {
				otherGroup.add(category);
			}
		}
		
		m.setWorkRemaining(defaultGroup.size() + otherGroup.size() + 1);
		m.subTask(EditingMessages.ContentAssistProcessor_ComputingContexts_Collecting_task);
		
		boolean ok;
		final AssistProposalCollector proposals= createProposalCollector();
		if (modificationStamp == 0) {
			ok= false;
			if (collectInformationProposals(context, defaultGroup, true, proposals,
					m.newChild(defaultGroup.size()) )) {
				if (proposals.getCount() == 1
						|| (collectInformationProposals(context, otherGroup, true, proposals,
									m.newChild(otherGroup.size()) )
								&& proposals.getCount() == 1) ) {
					ok= true;
				}
			}
		}
		else {
			ok= true;
			collectInformationProposals(context, defaultGroup, false, proposals,
					m.newChild(defaultGroup.size()) );
			if (proposals.getCount() == 0) {
				collectInformationProposals(context, otherGroup, false, proposals,
						m.newChild(otherGroup.size()) );
			}
		}
		
		if (!ok) {
			return null;
		}
		
		m.subTask(EditingMessages.ContentAssistProcessor_ComputingContexts_Sorting_task);
		m.setWorkRemaining(1);
		final IAssistCompletionProposal[] result= filterAndSortCompletionProposals(proposals,
				context, m.newChild(1) );
		
		if (result.length > 1
				&& assistant.isContextInfoPopupActive1()
				&& !assistant.isProposalPopupActive1()) {
			assistant.hidePopups();
		}
		if (modificationStamp != 0) {
			this.informationModeOffset= context.getInvocationOffset();
			this.informationModeTimestamp= System.nanoTime();
			this.informationModeModificationStamp= modificationStamp;
		}
		
		return result;
	}
	
	/**
	 * @return <code>false</code> if cancelled
	 */
	private boolean collectInformationProposals(final AssistInvocationContext context,
			final List<ContentAssistCategory> categories, final boolean single,
			final AssistProposalCollector proposals, final SubMonitor progress) {
		for (final ContentAssistCategory category : categories) {
			final List<IContentAssistComputer> computers= category.getComputers(getContentType());
			final SubMonitor computersProgress= progress.newChild(1);
			for (final IContentAssistComputer computer : computers) {
				computer.sessionStarted(context.getEditor(), getAssistant());
				IStatus status= null;
				try {
					status= computer.computeInformationProposals(context, proposals, computersProgress);
				}
				catch (final Exception e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, LTK.PLUGIN_ID,
							"An error occurred when computing content assistant context information.",
							e ));
				}
				finally {
					computer.sessionEnded();
				}
				if ((status != null && status.getCode() == IStatus.CANCEL)
						|| (single && proposals.getCount() > 1) ) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	protected AssistProposalCollector createProposalCollector() {
		return new AssistProposalCollector();
	}
	
	/**
	 * Filters and sorts the proposals. The passed list may be modified
	 * and returned, or a new list may be created and returned.
	 * 
	 * @param proposals the list of collected proposals
	 * @param context 
	 * @param monitor a progress monitor
	 * @return the list of filtered and sorted proposals, ready for display
	 */
	protected IAssistCompletionProposal[] filterAndSortCompletionProposals(
			final AssistProposalCollector proposals,
			final AssistInvocationContext context, final SubMonitor m) {
		final IAssistCompletionProposal[] array= proposals.toArray();
		if (array.length > 1) {
			Arrays.sort(array, PROPOSAL_COMPARATOR);
		}
		
		return array;
	}
	
	protected IAssistInformationProposal[] toInformationProposals(
			final IAssistCompletionProposal[] completionProposals,
			final AssistInvocationContext context) {
		if (completionProposals == null) {
			return null;
		}
		
		final IAssistInformationProposal[] infoProposals=
				new IAssistInformationProposal[completionProposals.length];
		for (int i= 0; i < completionProposals.length; i++) {
			final IAssistCompletionProposal proposal= completionProposals[i];
			infoProposals[i]= (proposal instanceof IAssistInformationProposal) ?
					(IAssistInformationProposal) proposal :
					new AssistCompletionInformationProposalWrapper(proposal, context);
		}
		return infoProposals;
	}
	
	
	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 *
	 * @param activationSet the activation set
	 */
	public final void setCompletionProposalAutoActivationCharacters(final char[] activationSet) {
		this.completionProposalsAutoActivationCharacters= activationSet;
	}
	
	@Override
	public final char[] getCompletionProposalAutoActivationCharacters() {
		return this.completionProposalsAutoActivationCharacters;
	}
	
	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 *
	 * @param activationSet the activation set
	 */
	public final void setContextInformationAutoActivationCharacters(final char[] activationSet) {
		this.contextInformationAutoActivationCharacters= activationSet;
	}
	
	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return this.contextInformationAutoActivationCharacters;
	}
	
	
	@Override
	public String getErrorMessage() {
		final IStatus status= this.status;
		return (status != null && status.getSeverity() == IStatus.ERROR) ? status.getMessage() : null;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * This implementation returns the validator created by
	 * {@link #createContextInformationValidator()}
	 */
	@Override
	public final IContextInformationValidator getContextInformationValidator() {
		if (this.contextInformationValidator == null) {
			this.contextInformationValidator= createContextInformationValidator();
		}
		return this.contextInformationValidator;
	}
	
	protected IContextInformationValidator createContextInformationValidator() {
		return null;
	}
	
	/**
	 * Creates a progress monitor.
	 * <p>
	 * The default implementation creates a
	 * <code>NullProgressMonitor</code>.
	 * </p>
	 * 
	 * @return a progress monitor
	 */
	protected IProgressMonitor createProgressMonitor() {
		return new NullProgressMonitor();
	}
	
	private AssistInvocationContext getCompletionProposalContext(final int offset,
			final IProgressMonitor monitor) {
		final AssistInvocationContext context;
		if (this.completionProposalContext != null
				&& this.completionProposalContext.reuse(getEditor(), offset) ) {
			context= this.completionProposalContext;
		}
		else {
			context= this.completionProposalContext= createCompletionProposalContext(offset, monitor);
		}
		context.session= this.sessionCounter;
		return context;
	}
	
	/**
	 * Creates the context that is passed to the completion proposal
	 * computers.
	 * 
	 * @param offset the content assist offset
	 * @return the context to be passed to the computers
	 */
	protected AssistInvocationContext createCompletionProposalContext(final int offset,
			final IProgressMonitor monitor) {
		return new AssistInvocationContext(getEditor(), offset, getContentType(), 0, monitor);
	}
	
	private AssistInvocationContext getContextInformationContext(final int offset,
			final IProgressMonitor monitor) {
		final AssistInvocationContext context;
		if (this.completionProposalContext != null
				&& this.completionProposalContext.reuse(getEditor(), offset) ) {
			context= this.completionProposalContext;
		}
		else {
			context= createContextInformationContext(offset, monitor);
		}
		return context;
	}
	
	/**
	 * Creates the context that is passed to the completion proposal
	 * computers.
	 * 
	 * @param offset the content assist offset
	 * @return the context to be passed to the computers
	 */
	protected AssistInvocationContext createContextInformationContext(final int offset,
			final IProgressMonitor monitor) {
		return new AssistInvocationContext(getEditor(), offset, getContentType(), 0, monitor);
	}
	
	protected boolean forceContextInformation(final AssistInvocationContext context) {
		return false;
	}
	
	private void iterate() {
		if (this.categoryIteration == null) {
			this.noIteration= false;
			return;
		}
		if (this.noIteration) {
			if (this.iterationPos < 0) {
				this.iterationPos= 0;
			}
			this.noIteration= false;
			return;
		}
		
		this.iterationPos++;
		if (this.iterationPos >= this.categoryIteration.size()) {
			this.iterationPos= 0;
		}
	}
	
	private List<ContentAssistCategory> getCurrentCategories() {
		if (this.categoryIteration == null) {
			return this.availableCategories;
		}
		final int iterationPos= this.iterationPos % this.categoryIteration.size();
		
		final ContentAssist assistant= getAssistant();
		assistant.setStatusMessage(createIterationMessage(iterationPos));
		assistant.setEmptyMessage(createEmptyMessage(iterationPos));
		
		return this.categoryIteration.get(iterationPos);
	}
	
	private List<List<ContentAssistCategory>> createCategoryIteration() {
		final List<List<ContentAssistCategory>> sequence= new ArrayList<>(this.availableCategories.size());
		sequence.add(createDefaultCategories());
		for (final ContentAssistCategory category : createSeparateCategories()) {
			sequence.add(Collections.singletonList(category));
		}
		return sequence;
	}
	
	private List<ContentAssistCategory> createDefaultCategories() {
		final List<ContentAssistCategory> included= new ArrayList<>(this.availableCategories.size());
		for (final ContentAssistCategory category : this.availableCategories) {
			if (category.isEnabledInDefault() && category.hasComputers(getContentType())) {
				included.add(category);
			}
		}
		final ContentAssistCategory[] exclicite= this.expliciteCategories.toArray();
		for (int i= 0; i < exclicite.length; i++) {
			final ContentAssistCategory category= exclicite[i];
			if (category.isEnabledInDefault() && category.hasComputers(getContentType())) {
				included.add(category);
			}
		}
		return included;
	}
	
	private List<ContentAssistCategory> createSeparateCategories() {
		final ArrayList<ContentAssistCategory> sorted= new ArrayList<>(this.availableCategories.size());
		for (final ContentAssistCategory category : this.availableCategories) {
			if (category.isEnabledInCircling() && category.hasComputers(getContentType())) {
				sorted.add(category);
			}
		}
		return sorted;
	}
	
	private void notifySessionStarted() {
		for (final ContentAssistCategory category : this.availableCategories) {
			final List<IContentAssistComputer> computers= category.getComputers(getContentType());
			for (final IContentAssistComputer computer : computers) {
				computer.sessionStarted(ContentAssistProcessor.this.editor, getAssistant());
			}
		}
	}
	
	private void notifySessionEnded() {
		if (this.availableCategories == null) {
			return;
		}
		for (final ContentAssistCategory category : this.availableCategories) {
			final List<IContentAssistComputer> computers= category.getComputers(getContentType());
			for (final IContentAssistComputer computer : computers) {
				try {
					computer.sessionEnded();
				}
				catch (final Exception e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, LTK.PLUGIN_ID, 0,
							"Error by contributed content assist computer.", e ));
				}
			}
		}
	}
	
	
	protected String createEmptyMessage(final int iterationPosition) {
		return NLS.bind(EditingMessages.ContentAssistProcessor_Empty_message, new String[] { 
				getCategoryName(iterationPosition)});
	}
	
	protected String createIterationMessage(final int iterationPosition) {
//		if (iterationPosition >= 0 && iterationPosition == iterationPosition % fCategoryIteration.size()) {
//			return getCategoryName(iterationPosition);
//		}
		final StringBuilder sb= new StringBuilder(
				getCategoryName(iterationPosition) );
		if (this.categoryIteration.size() > 0) {
			sb.append("\u2004\u2004"); //$NON-NLS-1$
			sb.append(NLS.bind(SharedMessages.DoToShow_message,
					getAssistant().getCompletionProposalIterationGesture(),
					getCategoryName(iterationPosition + 1) ));
		}
		return sb.toString();
	}
	
	protected String getCategoryName(final int repetition) {
		if (repetition < 0) {
			return EditingMessages.ContentAssistProcessor_ContextSelection_label;
		}
		final int iterationPosition= repetition % this.categoryIteration.size();
		if (iterationPosition == 0) {
			return EditingMessages.ContentAssistProcessor_DefaultProposalCategory;
		}
		return this.categoryIteration.get(iterationPosition).get(0).getDisplayName();
	}
	
	
	protected void reloadPossibleCompletions(final AssistInvocationContext context) {
		if (isInCompletionProposalSession()
				&& context.session == getSessionCounter()) {
			UIAccess.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					final SourceViewer viewer= context.getSourceViewer();
					if (isInCompletionProposalSession()
							&& context.session == getSessionCounter()
							&& UIAccess.isOkToUse(viewer)
							&& viewer.getTextWidget().isFocusControl()
							&& context.isInitialState()
							&& getAssistant().getCompletionProposalSelectionCounter() <= 1 ) {
						ContentAssistProcessor.this.noIteration= true;
						try {
							ContentAssistProcessor.this.reloadCompletionProposals=
									computeCompletionProposals(context.getInvocationOffset());
							if (ContentAssistProcessor.this.reloadCompletionProposals != null
									&& ContentAssistProcessor.this.reloadCompletionProposals.length > 0 ) {
								getAssistant().reloadPossibleCompletions();
							}
						}
						finally {
							ContentAssistProcessor.this.noIteration= false;
							ContentAssistProcessor.this.reloadCompletionProposals= null;
						}
					}
				}
			});
		}
	}
	
}
