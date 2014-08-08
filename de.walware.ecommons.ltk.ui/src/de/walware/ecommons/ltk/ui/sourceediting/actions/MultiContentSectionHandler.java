/*=============================================================================#
 # Copyright (c) 2011-2014 Stephan Wahlbrink (WalWare.de) and others.
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
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ui.IWorkbenchPart;

import de.walware.ecommons.text.core.sections.DocContentSections;
import de.walware.ecommons.workbench.ui.WorkbenchUIUtil;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


/**
 * Command handler supporting separate implementation for each document content section type.
 * 
 * Implement {@link #createHandler(String)} for lazy initialization of the command handlers
 * of the section types.
 * 
 * @see DocContentSections
 */
public class MultiContentSectionHandler extends AbstractHandler {
	
	
	private static final Object NULL= new Object();
	
	
	private final DocContentSections sections;
	
	private final Map<String, Object> handlers= new IdentityHashMap<>(8);
	
	private ISourceEditor expliciteEditor;
	
	
	public MultiContentSectionHandler(final DocContentSections sections) {
		if (sections == null) {
			throw new NullPointerException("sections"); //$NON-NLS-1$
		}
		this.sections= sections;
	}
	
	public MultiContentSectionHandler(final DocContentSections sections,
			final String sectionType1, final IHandler2 handler1) {
		this(sections, sectionType1, handler1, null, null);
	}
	
	public MultiContentSectionHandler(final DocContentSections sections,
			final String sectionType1, final IHandler2 handler1,
			final String sectionType2, final IHandler2 handler2) {
		this(sections);
		
		if (sectionType1 != null) {
			registerHandler(sectionType1, handler1);
		}
		if (sectionType2 != null) {
			registerHandler(sectionType2, handler2);
		}
	}
	
	
	protected void setEditor(final ISourceEditor editor) {
		this.expliciteEditor= editor;
	}
	
	protected final DocContentSections getSections() {
		return this.sections;
	}
	
	
	@Override
	public void dispose() {
		super.dispose();
		
		for (final Object handler : this.handlers.values()) {
			if (handler != NULL) {
				((IHandler2) handler).dispose();
			}
		}
		this.handlers.clear();
	}
	
	
	protected ISourceEditor getEditor(final Object applicationContext) {
		if (this.expliciteEditor != null) {
			return this.expliciteEditor;
		}
		final IWorkbenchPart activePart= WorkbenchUIUtil.getActivePart(applicationContext);
		if (activePart instanceof ISourceEditor) {
			return (ISourceEditor) activePart;
		}
		return (ISourceEditor) activePart.getAdapter(ISourceEditor.class);
	}
	
	
	public void registerHandler(final String sectionType, final IHandler2 handler) {
		if (sectionType == null) {
			throw new NullPointerException("sectionType");
		}
		this.handlers.put(sectionType, handler);
	}
	
	protected final IHandler2 getHandler(final String sectionType) {
		if (sectionType == DocContentSections.ERROR) {
			return null;
		}
		Object handler= this.handlers.get(sectionType);
		if (handler == null) {
			handler= NULL;
			try {
				final IHandler2 newHandler= createHandler(sectionType);
				if (newHandler != null) {
					handler= newHandler;
				}
			}
			finally {
				this.handlers.put(sectionType, handler);
			}
		}
		return (handler != NULL) ? (IHandler2) handler : null;
	}
	
	protected IHandler2 createHandler(final String sectionType) {
		return null;
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISourceEditor editor= getEditor(event.getApplicationContext());
		if (editor == null) {
			return null;
		}
		final SourceViewer viewer= editor.getViewer();
		if (viewer == null) {
			return null;
		}
		final IHandler2 handler= getHandler(
				this.sections.getType(viewer.getDocument(), viewer.getSelectedRange().x) );
		if (handler != null) {
			return handler.execute(event);
		}
		return null;
	}
	
}
