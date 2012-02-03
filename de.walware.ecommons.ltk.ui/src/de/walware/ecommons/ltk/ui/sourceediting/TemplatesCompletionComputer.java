/*******************************************************************************
 * Copyright (c) 2005-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.graphics.Image;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.LTKUI;
import de.walware.ecommons.ltk.ui.sourceediting.TemplateProposal.TemplateComparator;
import de.walware.ecommons.ltk.ui.templates.SourceEditorTemplateContext;


/**
 * Content assist computer for editor templates.
 */
public abstract class TemplatesCompletionComputer implements IContentAssistComputer {
	
	
	private static final TemplateComparator fgTemplateComparator = new TemplateProposal.TemplateComparator();
	
	
	protected final TemplateStore fTemplateStore;
	protected final ContextTypeRegistry fTypeRegistry;
	
	
	public TemplatesCompletionComputer(final TemplateStore templateStore, final ContextTypeRegistry contextTypes) {
		fTemplateStore = templateStore;
		fTypeRegistry = contextTypes;
	}
	
	
	@Override
	public void sessionStarted(final ISourceEditor editor) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionEnded() {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IStatus computeCompletionProposals(final AssistInvocationContext context,
			final int mode, final AssistProposalCollector<IAssistCompletionProposal> tenders, final IProgressMonitor monitor) {
		final ISourceViewer viewer = context.getSourceViewer();
		
		String prefix = extractPrefix(context);
		if (prefix == null
				|| (prefix.length() == 0 && mode != IContentAssistComputer.SPECIFIC_MODE) ) {
			return null;
		}
		IRegion region;
		if (context.getLength() == 0) {
			region = new Region(context.getInvocationOffset() - prefix.length(), prefix.length());
		}
		else {
			region = new Region(context.getOffset(), context.getLength());
		}
		final DocumentTemplateContext templateContext = createTemplateContext(context, region);
		if (templateContext == null) {
			return null;
		}
		
		int count = 0;
		if (prefix.length() > 0 && prefix.length() == context.getLength()) {
			count = doComputeProposals(tenders, templateContext, prefix, region);
			prefix = ""; // wenn erfolglos, dann ohne prefix //$NON-NLS-1$
		}
		
		if (count == 0) {
			try {
				final String text = viewer.getDocument().get(context.getOffset(), context.getLength());
				templateContext.setVariable("selection", text); // name of the selection variables {line, word}_selection //$NON-NLS-1$
				doComputeProposals(tenders, templateContext, prefix, region);
			}
			catch (final BadLocationException e) {
			}
		}
		return null;
	}
	
	private int doComputeProposals(final AssistProposalCollector<IAssistCompletionProposal> proposals,
			final DocumentTemplateContext context, final String prefix, final IRegion replacementRegion) {
		// Add Templates
		final int count = 0;
		final Template[] templates = getTemplates(context.getContextType().getId());
		for (int i = 0; i < templates.length; i++) {
			final Template template = templates[i];
			try {
				context.getContextType().validate(template.getPattern());
			}
			catch (final TemplateException e) {
				continue;
			}
			if (include(template, prefix)) { // Change <-> super
				proposals.add(createProposal(template, context, replacementRegion,
						getRelevance(template, prefix) ));
			}
		}
		
		return count;
	}
	
	protected boolean include(final Template template, final String prefix) {
		return template.getName().regionMatches(true, 0, prefix, 0, prefix.length());
	}
	
	@Override
	public IStatus computeContextInformation(final AssistInvocationContext context,
			final AssistProposalCollector<IAssistInformationProposal> tenders, final IProgressMonitor monitor) {
		return null;
	}
	
	
	protected String extractPrefix(final AssistInvocationContext context) {
		return context.getIdentifierPrefix();
	}
	
	protected Template[] getTemplates(final String contextTypeId) {
		return fTemplateStore.getTemplates(contextTypeId);
	}
	
	protected abstract TemplateContextType getContextType(final AssistInvocationContext context, final IRegion region);
	
	protected DocumentTemplateContext createTemplateContext(final AssistInvocationContext context, final IRegion region) {
		final ISourceViewer viewer = context.getSourceViewer();
		final TemplateContextType contextType = getContextType(context, region);
		if (contextType != null) {
			final IDocument document = viewer.getDocument();
			return new SourceEditorTemplateContext(contextType, document, region.getOffset(), region.getLength(), context.getEditor());
		}
		return null;
	}
	
	protected TemplateProposal createProposal(final Template template, final TemplateContext context, final IRegion region, final int relevance) {
		return new TemplateProposal(template, context, region, getImage(template), relevance);
	}
	
	protected Image getImage(final Template template) {
		return LTKUIPlugin.getDefault().getImageRegistry().get(LTKUI.OBJ_TEXT_TEMPLATE);
	}
	
	/**
	 * Returns the relevance of a template given a prefix. The default
	 * implementation returns a number greater than zero if the template name
	 * starts with the prefix, and zero otherwise.
	 *
	 * @param template the template to compute the relevance for
	 * @param prefix the prefix after which content assist was requested
	 * @return the relevance of <code>template</code>
	 * @see #extractPrefix(ITextViewer, int)
	 */
	protected int getRelevance(final Template template, final String prefix) {
		if (template.getName().regionMatches(true, 0, prefix, 0, prefix.length())) {
			return 90;
		}
		return 0;
	}
	
}
