/*=============================================================================#
 # Copyright (c) 2005-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.GlobalTemplateVariables.LineSelection;
import org.eclipse.jface.text.templates.GlobalTemplateVariables.WordSelection;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.graphics.Image;

import de.walware.ecommons.collections.ImCollections;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.LTKUI;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.sourceediting.assist.TemplateProposal.TemplateComparator;
import de.walware.ecommons.ltk.ui.templates.SourceEditorTemplateContext;


/**
 * Content assist computer for editor templates.
 */
public abstract class TemplatesCompletionComputer implements IContentAssistComputer {
	
	
	private static final TemplateComparator fgTemplateComparator= new TemplateProposal.TemplateComparator();
	
	private static final byte SELECTION_NONE= 0;
	private static final byte SELECTION_INLINE= 1;
	private static final byte SELECTION_MULTILINE= 2;
	
	private static final Pattern SELECTION_INLINE_PATTERN= Pattern.compile(
			"\\$\\{" + WordSelection.NAME + "\\}"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final Pattern SELECTION_ANY_PATTERN= Pattern.compile(
			"\\$\\{(?:" + WordSelection.NAME + "|" + LineSelection.NAME + ")\\}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	
	protected final TemplateStore templateStore;
	
	protected final ContextTypeRegistry typeRegistry;
	
	
	public TemplatesCompletionComputer(final TemplateStore templateStore, final ContextTypeRegistry contextTypes) {
		if (templateStore == null) {
			throw new NullPointerException("templateStore"); //$NON-NLS-1$
		}
		if (contextTypes == null) {
			throw new NullPointerException("contextTypes"); //$NON-NLS-1$
		}
		this.templateStore= templateStore;
		this.typeRegistry= contextTypes;
	}
	
	
	protected final TemplateStore getTemplateStore() {
		return this.templateStore;
	}
	
	protected final ContextTypeRegistry getTypeRegistry() {
		return this.typeRegistry;
	}
	
	
	@Override
	public void sessionStarted(final ISourceEditor editor, final ContentAssist assist) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionEnded() {
	}
	
	protected boolean handleRequest(final int mode, final String prefix) {
		return (prefix != null
				&& (prefix.length() > 0 || mode == IContentAssistComputer.SPECIFIC_MODE) );
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IStatus computeCompletionProposals(final AssistInvocationContext context,
			final int mode, final AssistProposalCollector<IAssistCompletionProposal> tenders, final IProgressMonitor monitor) {
		final ISourceViewer viewer= context.getSourceViewer();
		
		String prefix= extractPrefix(context);
		if (!handleRequest(mode, prefix)) {
			return null;
		}
		IRegion region;
		if (context.getLength() == 0) {
			region= new Region(context.getInvocationOffset() - prefix.length(), prefix.length());
		}
		else {
			region= new Region(context.getOffset(), context.getLength());
		}
		DocumentTemplateContext templateContext= createTemplateContext(context, region);
		if (templateContext == null) {
			return null;
		}
		
		int count= 0;
		if (context.getLength() > 0) {
			if (prefix.length() == context.getLength()) {
				count= doComputeProposals(tenders, templateContext, prefix, 0, region);
			}
			prefix= ""; // wenn erfolglos, dann ohne prefix //$NON-NLS-1$
			if (count != 0) {
				templateContext= createTemplateContext(context, region);
			}
		}
		try {
			final IDocument document= viewer.getDocument();
			final String text= document.get(context.getOffset(), context.getLength());
			final int selectionType;
			if (text.isEmpty()) {
				selectionType= SELECTION_NONE;
			}
			else {
				selectionType= (text.indexOf('\n') >= 0) ? SELECTION_MULTILINE : SELECTION_INLINE;
				templateContext.setVariable("text", text); //$NON-NLS-1$
			}
			templateContext.setVariable(GlobalTemplateVariables.SELECTION, text);
			
			doComputeProposals(tenders, templateContext, prefix, selectionType, region);
		}
		catch (final BadLocationException e) {
		}
		return null;
	}
	
	private int doComputeProposals(final AssistProposalCollector<IAssistCompletionProposal> proposals,
			final DocumentTemplateContext context, final String prefix, final int selectionType,
			final IRegion replacementRegion) {
		// Add Templates
		final int count= 0;
		final List<Template> templates= getTemplates(context.getContextType().getId());
		for (final Template template : templates) {
			if (include(template, prefix) || isSelectionTemplate(selectionType, template) ) {
				try {
					context.getContextType().validate(template.getPattern());
				}
				catch (final TemplateException e) {
					continue;
				}
				
				proposals.add(createProposal(template, context, prefix, replacementRegion,
						(template.getName().regionMatches(true, 0, prefix, 0, prefix.length())) ? 90 : 0
						));
			}
		}
		
		return count;
	}
	
	protected boolean include(final Template template, final String prefix) {
		return template.getName().regionMatches(true, 0, prefix, 0, prefix.length());
	}
	
	private boolean isSelectionTemplate(final int selectionType, final Template template) {
		switch (selectionType) {
		case SELECTION_INLINE:
			return SELECTION_INLINE_PATTERN.matcher(template.getPattern()).matches();
		case SELECTION_MULTILINE:
			return SELECTION_ANY_PATTERN.matcher(template.getPattern()).matches();
		default:
			return false;
		}
	}
	
	@Override
	public IStatus computeContextInformation(final AssistInvocationContext context,
			final AssistProposalCollector<IAssistInformationProposal> tenders, final IProgressMonitor monitor) {
		return null;
	}
	
	
	protected String extractPrefix(final AssistInvocationContext context) {
		return context.getIdentifierPrefix();
	}
	
	protected List<Template> getTemplates(final String contextTypeId) {
		return ImCollections.newList(this.templateStore.getTemplates(contextTypeId));
	}
	
	protected abstract TemplateContextType getContextType(AssistInvocationContext context,
			IRegion region);
	
	protected DocumentTemplateContext createTemplateContext(final AssistInvocationContext context,
			final IRegion region) {
		final ISourceViewer viewer= context.getSourceViewer();
		final TemplateContextType contextType= getContextType(context, region);
		if (contextType != null) {
			final IDocument document= viewer.getDocument();
			return new SourceEditorTemplateContext(contextType, document, region.getOffset(), region.getLength(), context.getEditor());
		}
		return null;
	}
	
	protected TemplateProposal createProposal(final Template template,
			final DocumentTemplateContext context, final String prefix, final IRegion region,
			final int relevance) {
		return new TemplateProposal(template, context, region, getImage(template), relevance);
	}
	
	protected Image getImage(final Template template) {
		return LTKUIPlugin.getDefault().getImageRegistry().get(LTKUI.OBJ_TEXT_TEMPLATE_IMAGE_ID);
	}
	
}
