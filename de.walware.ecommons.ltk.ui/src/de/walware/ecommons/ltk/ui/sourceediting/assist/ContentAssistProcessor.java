/*=============================================================================#
 # Copyright (c) 2005-2015 Stephan Wahlbrink (WalWare.de) and others.
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import de.walware.ecommons.FastList;
import de.walware.ecommons.ui.SharedMessages;
import de.walware.ecommons.workbench.ui.WorkbenchUIUtil;

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
public class ContentAssistProcessor implements IContentAssistProcessor {
	
	
	private static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("de.walware.ecommons.ui/debug/ResultCollector")); //$NON-NLS-1$ //$NON-NLS-2$
	
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
			if (event.processor != ContentAssistProcessor.this || event.assistant != ContentAssistProcessor.this.assistant) {
				return;
			}
			
			final KeySequence binding= WorkbenchUIUtil.getBestKeyBinding(
					ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS );
			ContentAssistProcessor.this.availableCategories= ContentAssistProcessor.this.computerRegistry.getCategories();
			ContentAssistProcessor.this.iterationGesture= createIterationGesture(binding);
			ContentAssistProcessor.this.categoryIteration= createCategoryIteration();
			
			for (final ContentAssistCategory category : ContentAssistProcessor.this.availableCategories) {
				final List<IContentAssistComputer> computers= category.getComputers(ContentAssistProcessor.this.partition);
				for (final IContentAssistComputer computer : computers) {
					computer.sessionStarted(ContentAssistProcessor.this.editor, ContentAssistProcessor.this.assistant);
				}
			}
			
			ContentAssistProcessor.this.repetition= 0;
			
			if (ContentAssistProcessor.this.assistant instanceof IContentAssistantExtension3) {
				final IContentAssistantExtension3 ext3= ContentAssistProcessor.this.assistant;
				((ContentAssistant) ext3).setRepeatedInvocationTrigger(binding);
			}
			if (ContentAssistProcessor.this.categoryIteration.size() == 1) {
				ContentAssistProcessor.this.assistant.setRepeatedInvocationMode(false);
				ContentAssistProcessor.this.assistant.setShowEmptyList(false);
			}
			else {
				ContentAssistProcessor.this.assistant.setRepeatedInvocationMode(true);
				ContentAssistProcessor.this.assistant.setShowEmptyList(true);
				
				ContentAssistProcessor.this.assistant.setStatusLineVisible(true);
				ContentAssistProcessor.this.assistant.setStatusMessage(createIterationMessage(0));
			}
		}
		
		@Override
		public void assistSessionEnded(final ContentAssistEvent event) {
			if (event.processor != ContentAssistProcessor.this || event.assistant != ContentAssistProcessor.this.assistant) {
				return;
			}
			
			if (ContentAssistProcessor.this.availableCategories != null) {
				for (final ContentAssistCategory category : ContentAssistProcessor.this.availableCategories) {
					final List<IContentAssistComputer> computers= category.getComputers(ContentAssistProcessor.this.partition);
					for (final IContentAssistComputer computer : computers) {
						computer.sessionEnded();
					}
				}
			}
			ContentAssistProcessor.this.availableCategories= null;
			ContentAssistProcessor.this.categoryIteration= null;
			ContentAssistProcessor.this.repetition= -1;
			ContentAssistProcessor.this.iterationGesture= null;
			
			ContentAssistProcessor.this.assistant.setRepeatedInvocationMode(false);
			ContentAssistProcessor.this.assistant.setShowEmptyList(false);
			ContentAssistProcessor.this.assistant.enableAutoInsertSetting();
			
			ContentAssistProcessor.this.assistant.setStatusLineVisible(false);
			if (ContentAssistProcessor.this.assistant instanceof IContentAssistantExtension3) {
				final IContentAssistantExtension3 ext3= ContentAssistProcessor.this.assistant;
				((ContentAssistant) ext3).setRepeatedInvocationTrigger(null);
			}
		}
		
		@Override
		public void selectionChanged(final ICompletionProposal proposal, final boolean smartToggle) {
		}
		
		@Override
		public void assistSessionRestarted(final ContentAssistEvent event) {
			ContentAssistProcessor.this.repetition= 0;
		}
		
	}
	
	
	/**
	 * The completion proposal registry.
	 */
	private final ContentAssistComputerRegistry computerRegistry;
	
	private final String partition;
	private final ContentAssist assistant;
	private final ISourceEditor editor;
	
	private char[] completionAutoActivationCharacters;
	
	/* cycling stuff */
	private int repetition= -1;
	private final FastList<ContentAssistCategory> expliciteCategories= new FastList<>(ContentAssistCategory.class);
	private List<ContentAssistCategory> availableCategories;
	private List<List<ContentAssistCategory>> categoryIteration;
	private String iterationGesture= null;
	private int numberOfComputedResults= 0;
	private IStatus status;
	
	private IContextInformationValidator contextInformationValidator;
	
	/* for detection if information mode is valid */
	private long informationModeTimestamp;
	private long informationModeModificationStamp;
	private int informationModeOffset;
	
	
	public ContentAssistProcessor(final ContentAssist assistant, final String partition, final ContentAssistComputerRegistry registry, final ISourceEditor editor) {
		assert(assistant != null);
		assert(partition != null);
		assert(registry != null);
		assert(editor != null);
		
		this.partition= partition;
		this.computerRegistry= registry;
		this.editor= editor;
		this.assistant= assistant;
		this.assistant.enableColoredLabels(true);
		this.assistant.addCompletionListener(new CompletionListener());
	}
	
	
	public String getPartition() {
		return this.partition;
	}
	
	protected ISourceEditor getEditor() {
		return this.editor;
	}
	
	public void addCategory(final ContentAssistCategory category) {
		this.expliciteCategories.add(category);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ICompletionProposal[] computeCompletionProposals(final ITextViewer viewer, final int offset) {
		final long start= System.nanoTime();
		
		clearState();
		
		final SubMonitor progress= SubMonitor.convert(createProgressMonitor());
		progress.beginTask(EditingMessages.ContentAssistProcessor_ComputingProposals_task, 10);
		
		final AssistInvocationContext context= createCompletionProposalContext(offset, progress.newChild(3));
		
		final long setup= DEBUG ? System.nanoTime() : 0L;
		
		final long modificationStamp= ((AbstractDocument) context.getSourceViewer().getDocument()).getModificationStamp();
		final int mode;
		if (this.computerRegistry.isInSpecificMode()) {
			mode= IContentAssistComputer.SPECIFIC_MODE;
		}
		else if (!this.assistant.isProposalPopupActive1()
				&& (   start-this.informationModeTimestamp > INFO_ITER_SPAN
					|| offset != this.informationModeOffset
					|| !this.assistant.isContextInfoPopupActive1()
					|| modificationStamp != this.informationModeModificationStamp)
				&& forceContextInformation(context)) {
			mode= IContentAssistComputer.INFORMATION_MODE;
			
			this.assistant.setRepeatedInvocationMode(true);
			this.assistant.setShowEmptyList(true);
			this.assistant.enableAutoInsertTemporarily();
			
			this.assistant.setStatusLineVisible(true);
//			fAssistant.setStatusMessage(createIterationMessage(-1));
			this.assistant.setStatusMessage(EditingMessages.ContentAssistProcessor_ContextSelection_label);
		}
		else if (this.repetition > 0) {
			mode= IContentAssistComputer.SPECIFIC_MODE;
			
			this.assistant.enableAutoInsertSetting();
		}
		else {
			mode= IContentAssistComputer.COMBINED_MODE;
			
			this.assistant.enableAutoInsertSetting();
		}
		
		final List<ContentAssistCategory> categories= (mode != IContentAssistComputer.INFORMATION_MODE) ?
				getCurrentCategories() : getInformationCategories();
		
		progress.setWorkRemaining(categories.size() + 1);
		progress.subTask((mode != IContentAssistComputer.INFORMATION_MODE) ?
				EditingMessages.ContentAssistProcessor_ComputingProposals_Collecting_task :
				EditingMessages.ContentAssistProcessor_ComputingContexts_Collecting_task);
		final AssistProposalCollector<IAssistCompletionProposal> proposals =
				createCompletionProposalCollector();
		collectCompletionProposals(context, mode, categories, proposals, progress);
		final long collect= DEBUG ? System.nanoTime() : 0L;
		
		progress.subTask((mode != IContentAssistComputer.INFORMATION_MODE) ?
				EditingMessages.ContentAssistProcessor_ComputingProposals_Sorting_task :
				EditingMessages.ContentAssistProcessor_ComputingContexts_Sorting_task);
		final IAssistCompletionProposal[] result= filterAndSortCompletionProposals(proposals, context, progress);
		final long filter= DEBUG ? System.nanoTime() : 0L;
		
		this.numberOfComputedResults= result.length;
		progress.done();
		
		if (mode == IContentAssistComputer.INFORMATION_MODE) {
			if (result.length > 1 && this.assistant.isContextInfoPopupActive1()
					&& !this.assistant.isProposalPopupActive1()) {
				this.assistant.hidePopups();
			}
			this.informationModeOffset= offset;
			this.informationModeTimestamp= System.nanoTime();
			this.informationModeModificationStamp= modificationStamp;
		}
		
		if (DEBUG) {
			final StringBuilder sb= new StringBuilder("Code Assist Stats"); //$NON-NLS-1$
			sb.append(" (").append(result.length).append(" proposals)"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\n\t" + "setup=   ").append((setup - start)); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\n\t" + "collect= ").append((collect - setup)); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\n\t" + "sort=    ").append((filter - collect)); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sb);
		}
		
		return result;
	}
	
	private void clearState() {
		this.status= null;
		this.numberOfComputedResults= 0;
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
			final AssistProposalCollector<IAssistCompletionProposal> proposals, final SubMonitor progress) {
		for (final ContentAssistCategory category : categories) {
			final List<IContentAssistComputer> computers= category.getComputers(this.partition);
			final SubMonitor computorsProgress= progress.newChild(1);
			for (final IContentAssistComputer computer : computers) {
				final IStatus status= computer.computeCompletionProposals(context, mode, proposals, computorsProgress);
				if (status != null && status.getSeverity() >= IStatus.INFO
						&& (this.status == null || status.getSeverity() > this.status.getSeverity()) ) {
					this.status= status;
				}
			}
		}
		return true;
	}
	
	protected AssistProposalCollector<IAssistCompletionProposal> createCompletionProposalCollector() {
		return new AssistProposalCollector<IAssistCompletionProposal>(IAssistCompletionProposal.class) {
			@Override
			public void add(final IAssistCompletionProposal proposal) {
				final IAssistCompletionProposal existing= this.proposals.put(proposal, proposal);
				if (existing != null && existing.getRelevance() > proposal.getRelevance()) {
					this.proposals.put(existing, existing);
				}
			}
		};
	}
	
	/**
	 * Filters and sorts the proposals. The passed list may be modified
	 * and returned, or a new list may be created and returned.
	 * 
	 * @param proposals the list of collected proposals
	 * @param monitor a progress monitor
	 * @param context 
	 * @return the list of filtered and sorted proposals, ready for display
	 */
	protected IAssistCompletionProposal[] filterAndSortCompletionProposals(
			final AssistProposalCollector<IAssistCompletionProposal> proposals,
			final AssistInvocationContext context, final IProgressMonitor monitor) {
		final IAssistCompletionProposal[] array= proposals.toArray();
		if (array.length > 1) {
			Arrays.sort(array, PROPOSAL_COMPARATOR);
		}
		return array;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IContextInformation[] computeContextInformation(final ITextViewer viewer, final int offset) {
		this.numberOfComputedResults= 0;
		IContextInformation[] result= null;
		
		clearState();
		
		final SubMonitor progress= SubMonitor.convert(createProgressMonitor());
		progress.beginTask(EditingMessages.ContentAssistProcessor_ComputingContexts_task, 10);
		
		final AssistInvocationContext context= createContextInformationContext(offset, progress.newChild(3));
		
		final List<ContentAssistCategory> available= this.computerRegistry.getCategories();
		final List<ContentAssistCategory> defaultGroup= new ArrayList<>();
		final List<ContentAssistCategory> otherGroup= new ArrayList<>();
		for (final ContentAssistCategory category : otherGroup) {
			if (category.isEnabledInDefault()) {
				defaultGroup.add(category);
			}
			else if (category.isEnabledInCircling()) {
				otherGroup.add(category);
			}
		}
		
		progress.setWorkRemaining(available.size());
		progress.subTask(EditingMessages.ContentAssistProcessor_ComputingContexts_Collecting_task);
		final AssistProposalCollector<IAssistInformationProposal> proposals =
				createInformationProposalCollector();
		if (collectSingleInformationProposals(context, defaultGroup, proposals, progress)) {
			if (proposals.getCount() <= 0) {
				if (collectSingleInformationProposals(context, otherGroup, proposals, progress)) {
					if (proposals.getCount() == 1) {
						this.numberOfComputedResults= 1;
						result= proposals.toArray();
					}
				}
			}
		}
		
		progress.done();
		
		return result;
	}
	
	/**
	 * @return <code>false</code> if cancelled
	 */
	private boolean collectSingleInformationProposals(final AssistInvocationContext context,
			final List<ContentAssistCategory> categories,
			final AssistProposalCollector<IAssistInformationProposal> proposals, final SubMonitor progress) {
		for (final ContentAssistCategory category : categories) {
			final List<IContentAssistComputer> computers= category.getComputers(this.partition);
			final SubMonitor computersProgress= progress.newChild(1);
			for (final IContentAssistComputer computer : computers) {
				computer.sessionStarted(context.getEditor(), this.assistant);
				final IStatus status;
				try {
					status= computer.computeContextInformation(context, proposals, computersProgress);
				}
				finally {
					computer.sessionEnded();
				}
				if ((status != null && status.getSeverity() > IStatus.WARNING)
						|| proposals.getCount() > 1) {
					return false;
				}
			}
		}
		return true;
	}
	
	protected AssistProposalCollector<IAssistInformationProposal> createInformationProposalCollector() {
		return new AssistProposalCollector<>(IAssistInformationProposal.class);
	}
	
	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 *
	 * @param activationSet the activation set
	 */
	public final void setCompletionProposalAutoActivationCharacters(final char[] activationSet) {
		this.completionAutoActivationCharacters= activationSet;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final char[] getCompletionProposalAutoActivationCharacters() {
		return this.completionAutoActivationCharacters;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
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
	
	/**
	 * Creates the context that is passed to the completion proposal
	 * computers.
	 * 
	 * @param offset the content assist offset
	 * @return the context to be passed to the computers
	 */
	protected AssistInvocationContext createCompletionProposalContext(final int offset,
			final IProgressMonitor monitor) {
		return new AssistInvocationContext(getEditor(), offset, 0, monitor);
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
		return new AssistInvocationContext(getEditor(), offset, 0, monitor);
	}
	
	protected boolean forceContextInformation(final AssistInvocationContext context) {
		return false;
	}
	
	private List<ContentAssistCategory> getCurrentCategories() {
		if (this.categoryIteration == null) {
			return this.availableCategories;
		}
		final int iteration= this.repetition % this.categoryIteration.size();
		this.assistant.setStatusMessage(createIterationMessage(iteration));
		this.assistant.setEmptyMessage(createEmptyMessage());
		this.repetition++;
		if (this.repetition >= this.categoryIteration.size()) {
			this.repetition= 0;
		}
		
		return this.categoryIteration.get(iteration);
	}
	
	private List<ContentAssistCategory> getInformationCategories() {
		final List<ContentAssistCategory> categories= new ArrayList<>(this.availableCategories.size());
		for (final ContentAssistCategory category : this.availableCategories) {
			if (category.isEnabledInDefault() || category.isEnabledInCircling()) {
				categories.add(category);
			}
		}
		return categories;
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
			if (category.isEnabledInDefault() && category.hasComputers(this.partition)) {
				included.add(category);
			}
		}
		final ContentAssistCategory[] exclicite= this.expliciteCategories.toArray();
		for (int i= 0; i < exclicite.length; i++) {
			final ContentAssistCategory category= exclicite[i];
			if (category.isEnabledInDefault() && category.hasComputers(this.partition)) {
				included.add(category);
			}
		}
		return included;
	}
	
	private List<ContentAssistCategory> createSeparateCategories() {
		final ArrayList<ContentAssistCategory> sorted= new ArrayList<>(this.availableCategories.size());
		for (final ContentAssistCategory category : this.availableCategories) {
			if (category.isEnabledInCircling() && category.hasComputers(this.partition)) {
				sorted.add(category);
			}
		}
		return sorted;
	}
	
	protected String createEmptyMessage() {
		return NLS.bind(EditingMessages.ContentAssistProcessor_Empty_message, new String[] { 
				getCategoryName(this.repetition)});
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
					this.iterationGesture,
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
	
	private String createIterationGesture(final KeySequence binding) {
		return (binding != null) ?
				NLS.bind(SharedMessages.Affordance_Press_message, binding.format()) :
				SharedMessages.Affordance_Click_message;
	}
	
}
