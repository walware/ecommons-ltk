/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.Arrays;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.actions.HandlerCollection;

import de.walware.ecommons.ltk.core.model.ISourceStructElement;
import de.walware.ecommons.ltk.core.refactoring.CommonRefactoringFactory;
import de.walware.ecommons.ltk.core.refactoring.RefactoringAdapter;
import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.ui.refactoring.AbstractElementsHandler;
import de.walware.ecommons.ltk.ui.refactoring.CopyElementsHandler;
import de.walware.ecommons.ltk.ui.refactoring.CopyNamesHandler;
import de.walware.ecommons.ltk.ui.refactoring.CutElementsHandler;
import de.walware.ecommons.ltk.ui.refactoring.DeleteElementsHandler;
import de.walware.ecommons.ltk.ui.refactoring.PasteElementsHandler;
import de.walware.ecommons.ltk.ui.util.LTKSelectionUtil;


public abstract class SourceEditor2OutlinePage extends SourceEditor1OutlinePage {
	
	
	protected class SelectCodeRangeAction extends Action {
		
		
		public SelectCodeRangeAction(final CommonRefactoringFactory refactoring) {
			super();
			setText(EditingMessages.SelectSourceCode_label);
			
			setEnabled(!getSelection().isEmpty());
		}
		
		@Override
		public void run() {
			final ISourceStructElement[] elements = LTKSelectionUtil.getSelectedSourceStructElements(getSelection());
			if (elements != null) {
				RefactoringAdapter adapter = fRefactoring.createAdapter(elements);
				if (adapter == null) {
					adapter = fRefactoring.createAdapter(null);
				}
				Arrays.sort(elements, adapter.getModelElementComparator());
				final IRegion range = adapter.getContinuousSourceRange(elements);
				if (range != null) {
					selectInEditor(new TextSelection(range.getOffset(), range.getLength()));
				}
			}
		}
		
	}
	
	
	private final CommonRefactoringFactory fRefactoring;
	
	
	public SourceEditor2OutlinePage(final SourceEditor1 editor, final String mainType,
			final CommonRefactoringFactory refactoring, final String contextMenuId) {
		super(editor, mainType, contextMenuId);
		fRefactoring = refactoring;
	}
	
	
	protected CommonRefactoringFactory getRefactoringFactory() {
		return fRefactoring;
	}
	
	@Override
	protected void initActions(final IServiceLocator serviceLocator, final HandlerCollection handlers) {
		super.initActions(serviceLocator, handlers);
		
		final IHandlerService handlerService = (IHandlerService) serviceLocator.getService(IHandlerService.class);
		{	final AbstractElementsHandler handler = new CutElementsHandler(fRefactoring);
			handlers.add(IWorkbenchCommandConstants.EDIT_CUT, handler);
			registerHandlerToUpdate(handler);
			handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_CUT, handler);
		}
		{	final AbstractElementsHandler handler = new CopyElementsHandler(fRefactoring);
			handlers.add(IWorkbenchCommandConstants.EDIT_COPY, handler);
			registerHandlerToUpdate(handler);
			handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_COPY, handler);
		}
		{	final AbstractElementsHandler handler = new CopyNamesHandler(fRefactoring);
			handlers.add(ISourceEditorCommandIds.COPY_ELEMENT_NAME, handler);
			registerHandlerToUpdate(handler);
			handlerService.activateHandler(ISourceEditorCommandIds.COPY_ELEMENT_NAME, handler);
		}
		{	final AbstractElementsHandler handler = new PasteElementsHandler(getSourceEditor(), fRefactoring);
			handlers.add(IWorkbenchCommandConstants.EDIT_PASTE, handler);
			handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_PASTE, handler);
		}
		{	final AbstractElementsHandler handler = new DeleteElementsHandler(fRefactoring);
			handlers.add(IWorkbenchCommandConstants.EDIT_DELETE, handler);
			handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_DELETE, handler);
		}
	}
	
	@Override
	protected void contextMenuAboutToShow(final IMenuManager m) {
		final IPageSite site = getSite();
		
		m.add(new SelectCodeRangeAction(fRefactoring));
		
		m.add(new Separator(SharedUIResources.EDIT_COPYPASTE_MENU_ID));
		m.add(new CommandContributionItem(new CommandContributionItemParameter(
				site, null, IWorkbenchCommandConstants.EDIT_CUT, CommandContributionItem.STYLE_PUSH)));
		m.add(new CommandContributionItem(new CommandContributionItemParameter(
				site, null, IWorkbenchCommandConstants.EDIT_COPY, CommandContributionItem.STYLE_PUSH)));
		m.add(new CommandContributionItem(new CommandContributionItemParameter(
				site, null, ISourceEditorCommandIds.COPY_ELEMENT_NAME, CommandContributionItem.STYLE_PUSH)));
		m.add(new CommandContributionItem(new CommandContributionItemParameter(
				site, null, IWorkbenchCommandConstants.EDIT_PASTE, CommandContributionItem.STYLE_PUSH)));
		
		super.contextMenuAboutToShow(m);
	}
	
}
