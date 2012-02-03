/*******************************************************************************
 * Copyright (c) 2008-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import de.walware.ecommons.ltk.IModelManager;


/**
 * LTK quick assistant processor.
 */
public class QuickAssistProcessor implements IQuickAssistProcessor {
	
	
	private static class SpellingProposal implements IAssistCompletionProposal {
		
		
		private final ICompletionProposal fProposal;
		
		
		public SpellingProposal(final ICompletionProposal proposal) {
			fProposal = proposal;
		}
		
		
		@Override
		public int getRelevance() {
			try {
				final Method method = fProposal.getClass().getMethod("getRelevance");
				final Object value = method.invoke(fProposal);
				if (value instanceof Integer) {
					return ((Integer) value).intValue();
				}
			}
			catch (final Exception e) {
			}
			return 0;
		}
		
		@Override
		public String getSortingString() {
			return "";
		}
		
		@Override
		public Image getImage() {
			return fProposal.getImage();
		}
		
		@Override
		public String getDisplayString() {
			return fProposal.getDisplayString();
		}
		
		@Override
		public void selected(final ITextViewer viewer, final boolean smartToggle) {
			if (fProposal instanceof ICompletionProposalExtension2) {
				((ICompletionProposalExtension2) fProposal).selected(viewer, smartToggle);
			}
		}
		
		@Override
		public void unselected(final ITextViewer viewer) {
			if (fProposal instanceof ICompletionProposalExtension2) {
				((ICompletionProposalExtension2) fProposal).unselected(viewer);
			}
		}
		
		@Override
		public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
			if (fProposal instanceof ICompletionProposalExtension2) {
				return ((ICompletionProposalExtension2) fProposal).validate(document, offset, event);
			}
			return false;
		}
		
		@Override
		public String getAdditionalProposalInfo() {
			return fProposal.getAdditionalProposalInfo();
		}
		
		@Override
		public void apply(final IDocument document) {
			fProposal.apply(document);
		}
		
		@Override
		public void apply(final ITextViewer viewer, final char trigger, final int stateMask, final int offset) {
			if (fProposal instanceof ICompletionProposalExtension2) {
				((ICompletionProposalExtension2) fProposal).apply(viewer, trigger, stateMask, offset);
			}
			else {
				fProposal.apply(viewer.getDocument());
			}
		}
		
		@Override
		public Point getSelection(final IDocument document) {
			return fProposal.getSelection(document);
		}
		
		@Override
		public IContextInformation getContextInformation() {
			return fProposal.getContextInformation();
		}
		
	}
	
	
	private final ISourceEditor fEditor;
	private String fErrorMessage;
	
	
	public QuickAssistProcessor() {
		this(null);
	}
	
	public QuickAssistProcessor(final ISourceEditor editor) {
		fEditor = editor;
	}
	
	
	/**
	 * @return the editor
	 */
	public ISourceEditor getEditor() {
		return fEditor;
	}
	
	
	@Override
	public boolean canAssist(final IQuickAssistInvocationContext invocationContext) {
		return false;
	}
	
	@Override
	public boolean canFix(final Annotation annotation) {
		if (annotation.isMarkedDeleted()) {
			return false;
		}
		final String type = annotation.getType();
		if (type.equals(SpellingAnnotation.TYPE)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Creates the context that is passed to the completion proposal
	 * computers.
	 * 
	 * @param invocationContext the original invocation context
	 * 
	 * @return the context to be passed to the computers
	 */
	protected AssistInvocationContext createContext(final IQuickAssistInvocationContext invocationContext,
			final IProgressMonitor monitor) {
		return new AssistInvocationContext(getEditor(), invocationContext.getOffset(),
				IModelManager.MODEL_FILE, monitor );
	}
	
	
	@Override
	public ICompletionProposal[] computeQuickAssistProposals(final IQuickAssistInvocationContext invocationContext) {
		fErrorMessage = null;
		final SubMonitor progress = SubMonitor.convert(null);
		progress.beginTask("", 10);
		
		final AssistInvocationContext context = createContext(invocationContext, progress.newChild(3));
		if (context == null) {
			return null;
		}
		final ISourceViewer viewer = context.getSourceViewer();
		if (viewer == null) {
			return null;
		}
		final AssistProposalCollector<IAssistCompletionProposal> proposals = new AssistProposalCollector<IAssistCompletionProposal>(IAssistCompletionProposal.class);
		
		final IAnnotationModel model = viewer.getAnnotationModel();
		if (model != null) {
			addAnnotationProposals(context, proposals, model);
		}
		if (context.getModelInfo() != null) {
			addModelAssistProposals(context, proposals, progress);
		}
		
		if (proposals.getCount() == 0) {
			return null;
		}
		return filterAndSortCompletionProposals(proposals, context, progress);
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
		final IAssistCompletionProposal[] array = proposals.toArray();
		if (array.length > 1) {
			Arrays.sort(array, ContentAssistProcessor.PROPOSAL_COMPARATOR);
		}
		return array;
	}
	
	
	protected boolean isMatchingPosition(final Position pos, final int offset) {
		return (pos != null)
				&& (offset >= pos.getOffset())
				&& (offset <= pos.getOffset()+pos.getLength());
	}
	
	private void addAnnotationProposals(final IQuickAssistInvocationContext invocationContext,
			final AssistProposalCollector<IAssistCompletionProposal> proposals,
			final IAnnotationModel model) {
		final int offset = invocationContext.getOffset();
		final Iterator<Annotation> iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			final Annotation annotation = iter.next();
			if (annotation.isMarkedDeleted()) {
				continue;
			}
			final String type = annotation.getType();
			if (type.equals(SpellingAnnotation.TYPE)) {
				if (!isMatchingPosition(model.getPosition(annotation), offset)) {
					continue;
				}
				if (annotation instanceof SpellingAnnotation) {
					final SpellingProblem problem = ((SpellingAnnotation) annotation).getSpellingProblem();
					final ICompletionProposal[] annotationProposals = problem.getProposals(invocationContext);
					if (annotationProposals != null && annotationProposals.length > 0) {
						for (int i = 0; i < annotationProposals.length; i++) {
							proposals.add(new SpellingProposal(annotationProposals[i]));
						}
					}
				}
			}
		}
	}
	
	protected void addModelAssistProposals(final AssistInvocationContext context,
			final AssistProposalCollector<IAssistCompletionProposal> proposals,
			final IProgressMonitor monitor) {
	}
	
	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}
	
}
