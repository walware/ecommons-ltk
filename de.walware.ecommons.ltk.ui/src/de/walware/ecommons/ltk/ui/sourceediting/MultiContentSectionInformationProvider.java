/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;

import de.walware.ecommons.text.core.sections.IDocContentSections;


/**
 * Source viewer information provider supporting separate implementations for each document content
 * section type.
 * 
 * Implement {@link #createHandler(String)} for lazy initialization of the information provider
 * of the section types.
 * 
 * @see IDocContentSections
 */
public class MultiContentSectionInformationProvider implements
		IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2 {
	
	
	private static final Object NULL= new Object();
	
	
	private final IDocContentSections sections;
	
	private final Map<String, Object> handlers= new IdentityHashMap<>(8);
	
	private EditorInformationProvider activeProvider;
	
	
	public MultiContentSectionInformationProvider(final IDocContentSections sections) {
		if (sections == null) {
			throw new NullPointerException("sections"); //$NON-NLS-1$
		}
		this.sections= sections;
	}
	
	public MultiContentSectionInformationProvider(final IDocContentSections sections,
			final String key1, final EditorInformationProvider provider1) {
		this(sections, key1, provider1, null, null);
	}
	
	public MultiContentSectionInformationProvider(final IDocContentSections sections,
			final String sectionType1, final EditorInformationProvider provider1,
			final String sectionType2, final EditorInformationProvider provider2) {
		this(sections);
		
		if (sectionType1 != null) {
			registerProvider(sectionType1, provider1);
		}
		if (sectionType2 != null) {
			registerProvider(sectionType2, provider2);
		}
	}
	
	
	protected final IDocContentSections getSections() {
		return this.sections;
	}
	
	
	public void registerProvider(final String sectionType, final EditorInformationProvider provider) {
		if (sectionType == null) {
			throw new NullPointerException("sectionType"); //$NON-NLS-1$
		}
		this.handlers.put(sectionType, (provider != null) ? provider : NULL);
	}
	
	protected final EditorInformationProvider getProvider(final String sectionType) {
		if (sectionType == IDocContentSections.ERROR) {
			return null;
		}
		Object handler= this.handlers.get(sectionType);
		if (handler == null) {
			handler= NULL;
			try {
				final EditorInformationProvider newHandler= createHandler(sectionType);
				// newProvider.init
				if (newHandler != null) {
					handler= newHandler;
				}
			}
			finally {
				this.handlers.put(sectionType, handler);
			}
		}
		return (handler != NULL) ? (EditorInformationProvider) handler : null;
	}
	
	protected EditorInformationProvider createHandler(final String sectionType) {
		return null;
	}
	
	
	@Override
	public IRegion getSubject(final ITextViewer textViewer, final int offset) {
		this.activeProvider= null;
		
		final EditorInformationProvider provider= getProvider(
				this.sections.getType(textViewer.getDocument(), offset) );
		if (provider != null) {
			this.activeProvider= provider;
			return this.activeProvider.getSubject(textViewer, offset);
		}
		return null;
	}
	
	@Override
	public String getInformation(final ITextViewer textViewer, final IRegion subject) {
		return null;
	}
	
	@Override
	public Object getInformation2(final ITextViewer textViewer, final IRegion subject) {
		if (this.activeProvider == null) {
			return null;
		}
		return this.activeProvider.getInformation2(textViewer, subject);
	}
	
	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (this.activeProvider == null) {
			return null;
		}
		return this.activeProvider.getInformationPresenterControlCreator();
	}
	
}
