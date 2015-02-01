/*=============================================================================#
 # Copyright (c) 2013-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.actions;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.text.core.sections.DocContentSections;
import de.walware.ecommons.ui.actions.ListContributionItem;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


/**
 * Contribution item for element search (menu) supporting separate implementations for each
 * document content section type.
 * 
 * Implement {@link #createItem(String)} for lazy initialization of the contribution items of the
 * section types.
 * 
 * @see DocContentSections
 */
public class MultiContentSectionElementSearchContributionItem extends ListContributionItem
		implements IWorkbenchContribution, IExecutableExtension {
	
	
	private static final Object NULL= new Object();
	
	
	private final DocContentSections sections;
	
	private final Map<String, Object> items= new IdentityHashMap<>(8);
	
	private String commandId;
	
	private IServiceLocator serviceLocator;
	
	
	public MultiContentSectionElementSearchContributionItem(final DocContentSections sections) {
		super();
		if (sections == null) {
			throw new NullPointerException("sections"); //$NON-NLS-1$
		}
		this.sections= sections;
	}
	
	public MultiContentSectionElementSearchContributionItem(final DocContentSections sections,
			final String sectionType1, final ListContributionItem item1) {
		this(sections, sectionType1, item1, null, null);
	}
	
	public MultiContentSectionElementSearchContributionItem(final DocContentSections sections,
			final String sectionType1, final ListContributionItem item1,
			final String sectionType2, final ListContributionItem item2) {
		this(sections);
		
		if (sectionType1 != null) {
			registerItem(sectionType1, item1);
		}
		if (sectionType2 != null) {
			registerItem(sectionType2, item2);
		}
	}
	
	
	@Override
	public void setInitializationData(final IConfigurationElement config,
			final String propertyName, final Object data) throws CoreException {
		if (this.commandId != null) {
			return;
		}
		final String s= config.getAttribute("id"); //$NON-NLS-1$
		if (s != null) {
			this.commandId= s.intern();
		}
	}
	
	public String getCommandId() {
		return this.commandId;
	}
	
	@Override
	public void initialize(final IServiceLocator serviceLocator) {
		this.serviceLocator= serviceLocator;
		
		for (final Object item : this.items.values()) {
			if (item != NULL && item instanceof IWorkbenchContribution) {
				((IWorkbenchContribution) item).initialize(serviceLocator);
			}
		}
	}
	
	
	protected final DocContentSections getSections() {
		return this.sections;
	}
	
	
	@Override
	public void dispose() {
		super.dispose();
		
		for (final Object item : this.items.values()) {
			if (item != NULL) {
				((ListContributionItem) item).dispose();
			}
		}
		this.items.clear();
	}
	
	
	protected void registerItem(final String sectionType, final ListContributionItem item) {
		if (sectionType == null) {
			throw new NullPointerException("sectionType"); //$NON-NLS-1$
		}
		this.items.put(sectionType, (item != null) ? item : NULL);
	}
	
	protected final ListContributionItem getItem(final String sectionType) {
		if (sectionType == DocContentSections.ERROR) {
			return null;
		}
		Object item= this.items.get(sectionType);
		if (item == null) {
			item= NULL;
			try {
				final ListContributionItem newItem= createItem(sectionType);
				if (newItem != null) {
					if (newItem instanceof IWorkbenchContribution) {
						((IWorkbenchContribution) newItem).initialize(this.serviceLocator);
					}
					item= newItem;
				}
			}
			finally {
				this.items.put(sectionType, item);
			}
		}
		return (item != NULL) ? (ListContributionItem) item : null;
	}
	
	protected ListContributionItem createItem(final String sectionType) {
		return null;
	}
	
	
	@Override
	public void createContributionItems(List<IContributionItem> items) {
		final IWorkbenchPart part= UIAccess.getActiveWorkbenchPart(true);
		if (part instanceof ISourceEditor) {
			final ISourceEditor editor= (ISourceEditor) part;
			final SourceViewer viewer= editor.getViewer();
			final ListContributionItem item= getItem(
					this.sections.getType(viewer.getDocument(), viewer.getSelectedRange().x) );
			if (item != null) {
				createContributionItems(items, item);
			}
		}
	}
	
}
