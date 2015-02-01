/*=============================================================================#
 # Copyright (c) 2014-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;

import de.walware.ecommons.text.core.sections.DocContentSections;


/**
 * Quick assist processor supporting separate implementations for each document content section
 * type.
 * 
 * Implement {@link #createProcessor(String)} for lazy initialization of the processors of the
 * section types.
 * 
 * @see DocContentSections
 */
public class MultiContentSectionQuickAssistProcessor implements IQuickAssistProcessor {
	
	
	private static final Object NULL= new Object();
	
	
	private final DocContentSections sections;
	
	private final Map<String, Object> processors= new IdentityHashMap<>(8);
	
	private String errorMessage;
	
	
	public MultiContentSectionQuickAssistProcessor(final DocContentSections sections) {
		if (sections == null) {
			throw new NullPointerException("sections"); //$NON-NLS-1$
		}
		this.sections= sections;
	}
	
	
	protected void registerProcessor(final String sectionType, final IQuickAssistProcessor processor) {
		if (sectionType == null) {
			throw new NullPointerException("sectionType"); //$NON-NLS-1$
		}
		this.processors.put(sectionType, (processor != null) ? processor : NULL);
	}
	
	protected final IQuickAssistProcessor getProcessor(final String sectionType) {
		if (sectionType == DocContentSections.ERROR) {
			return null;
		}
		Object processor= this.processors.get(sectionType);
		if (processor == null) {
			processor= NULL;
			try {
				final IQuickAssistProcessor newProcessor= createProcessor(sectionType);
				// newProcessor.init
				if (newProcessor != null) {
					processor= newProcessor;
				}
			}
			finally {
				this.processors.put(sectionType, processor);
			}
		}
		return (processor != NULL) ? (IQuickAssistProcessor) processor : null;
	}
	
	protected IQuickAssistProcessor createProcessor(final String sectionType) {
		return null;
	}
	
	
	@Override
	public boolean canFix(final Annotation annotation) {
		return false;
	}
	
	@Override
	public boolean canAssist(final IQuickAssistInvocationContext invocationContext) {
		return false;
	}
	
	@Override
	public ICompletionProposal[] computeQuickAssistProposals(final IQuickAssistInvocationContext invocationContext) {
		this.errorMessage= null;
		final IQuickAssistProcessor processor= getProcessor(
				this.sections.getType(invocationContext.getSourceViewer().getDocument(), invocationContext.getOffset() ));
		if (processor != null) {
			try {
				return processor.computeQuickAssistProposals(invocationContext);
			}
			finally {
				this.errorMessage= processor.getErrorMessage();
			}
		}
		return null;
	}
	
	@Override
	public String getErrorMessage() {
		return this.errorMessage;
	}
	
}
