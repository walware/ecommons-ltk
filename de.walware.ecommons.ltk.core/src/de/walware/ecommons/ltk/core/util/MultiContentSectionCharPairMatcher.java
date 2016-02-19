/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.util;

import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import de.walware.ecommons.text.ICharPairMatcher;
import de.walware.ecommons.text.core.sections.IDocContentSections;


/**
 * Character pair matcher supporting separate implementation for each document content section
 * type.
 * 
 * Implement {@link #createHandler(String)} for lazy initialization of the matchers of the section
 * types.
 * 
 * @see IDocContentSections
 */
public class MultiContentSectionCharPairMatcher implements ICharPairMatcher {
	
	
	private static final Object NULL= new Object();
	
	
	private final IDocContentSections sections;
	
	private final Map<String, Object> handlers= new IdentityHashMap<>(8);
	
	private ICharPairMatcher activeMatcher;
	
	
	public MultiContentSectionCharPairMatcher(final IDocContentSections sections) {
		if (sections == null) {
			throw new NullPointerException("sections"); //$NON-NLS-1$
		}
		this.sections= sections;
	}
	
	public MultiContentSectionCharPairMatcher(final IDocContentSections sections,
			final String sectionType1, final ICharPairMatcher matcher1) {
		this(sections, sectionType1, matcher1, null, null);
	}
	
	public MultiContentSectionCharPairMatcher(final IDocContentSections sections,
			final String sectionType1, final ICharPairMatcher matcher1,
			final String sectionType2, final ICharPairMatcher matcher2) {
		this(sections);
		
		if (sectionType1 != null) {
			registerHandler(sectionType1, matcher1);
		}
		if (sectionType2 != null) {
			registerHandler(sectionType2, matcher2);
		}
	}
	
	
	protected IDocContentSections getSections() {
		return this.sections;
	}
	
	@Override
	public void dispose() {
		for (final Object handler : this.handlers.values()) {
			if (handler != NULL) {
				((ICharPairMatcher) handler).dispose();
			}
		}
		this.handlers.clear();
	}
	
	
	public void registerHandler(final String sectionType, final ICharPairMatcher matcher) {
		if (sectionType == null) {
			throw new NullPointerException("sectionType");
		}
		this.handlers.put(sectionType, matcher);
	}
	
	protected final ICharPairMatcher getHandler(final String sectionType) {
		if (sectionType == IDocContentSections.ERROR) {
			return null;
		}
		Object handler= this.handlers.get(sectionType);
		if (handler == null) {
			handler= NULL;
			try {
				final ICharPairMatcher newHandler= createHandler(sectionType);
				if (newHandler != null) {
					handler= newHandler;
				}
			}
			finally {
				this.handlers.put(sectionType, handler);
			}
		}
		return (handler != NULL) ? (ICharPairMatcher) handler : null;
	}
	
	protected ICharPairMatcher createHandler(final String sectionType) {
		return null;
	}
	
	
	@Override
	public void clear() {
		if (this.activeMatcher != null) {
			this.activeMatcher.clear();
			this.activeMatcher= null;
		}
	}
	
	@Override
	public IRegion match(final IDocument document, final int offset) {
		final ICharPairMatcher previousMatcher= this.activeMatcher;
		this.activeMatcher= getHandler(
				this.sections.getType(document, offset) );
		
		if (previousMatcher != null && previousMatcher != this.activeMatcher) {
			previousMatcher.clear();
		}
		
		if (this.activeMatcher != null) {
			return this.activeMatcher.match(document, offset);
		}
		return null;
	}
	
	@Override
	public IRegion match(final IDocument document, final int offset, final boolean auto) {
		final ICharPairMatcher previousMatcher= this.activeMatcher;
		this.activeMatcher= getHandler(
				this.sections.getType(document, offset) );
		
		if (previousMatcher != null && previousMatcher != this.activeMatcher) {
			previousMatcher.clear();
		}
		
		if (this.activeMatcher != null) {
			return this.activeMatcher.match(document, offset, auto);
		}
		return null;
	}
	
	@Override
	public int getAnchor() {
		if (this.activeMatcher != null) {
			return this.activeMatcher.getAnchor();
		}
		return -1;
	}
	
}
