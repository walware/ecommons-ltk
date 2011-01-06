/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

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

import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.ISourceStructElement;
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
		
		protected final RefactoringAdapter fLTK;
		
		public SelectCodeRangeAction(final RefactoringAdapter ltk) {
			super();
			
			setText(EditingMessages.SelectSourceCode_label);
			
			fLTK = ltk;
			setEnabled(!getSelection().isEmpty());
		}
		
		@Override
		public void run() {
			final ISourceStructElement[] elements = LTKSelectionUtil.getSelectedSourceStructElements(getSelection());
			if (elements != null) {
				Arrays.sort(elements, fLTK.getModelElementComparator());
				final IRegion range = fLTK.getContinuousSourceRange(elements);
				if (range != null) {
					selectInEditor(new TextSelection(range.getOffset(), range.getLength()));
				}
			}
		}
		
	}
	
	
	private final CommonRefactoringFactory fRefactoring;
	private final RefactoringAdapter fLTK;
	
	
	public SourceEditor2OutlinePage(final SourceEditor1 editor, final String mainType,
			final CommonRefactoringFactory refactoring, final String contextMenuId) {
		super(editor, mainType, contextMenuId);
		fRefactoring = refactoring;
		fLTK = refactoring.createAdapter();
	}
	
	
	@Override
	protected void initActions() {
		super.initActions();
		final IPageSite site = getSite();
		
		final IHandlerService handlerSvc = (IHandlerService) site.getService(IHandlerService.class);
		final AbstractElementsHandler cutHandler = new CutElementsHandler(fLTK, fRefactoring);
		registerHandlerToUpdate(cutHandler);
		handlerSvc.activateHandler(IWorkbenchCommandConstants.EDIT_CUT, cutHandler);
		final AbstractElementsHandler copyHandler = new CopyElementsHandler(fLTK);
		registerHandlerToUpdate(copyHandler);
		handlerSvc.activateHandler(IWorkbenchCommandConstants.EDIT_COPY, copyHandler);
		final AbstractElementsHandler copyNamesHandler = new CopyNamesHandler(fLTK);
		registerHandlerToUpdate(copyNamesHandler);
		handlerSvc.activateHandler(ISourceEditorCommandIds.COPY_ELEMENT_NAME, copyNamesHandler);
		final AbstractElementsHandler pasteHandler = new PasteElementsHandler(getSourceEditor(), fLTK);
		handlerSvc.activateHandler(IWorkbenchCommandConstants.EDIT_PASTE, pasteHandler);
		final AbstractElementsHandler deleteHandler = new DeleteElementsHandler(fLTK, fRefactoring);
		handlerSvc.activateHandler(IWorkbenchCommandConstants.EDIT_DELETE, deleteHandler);
	}
	
	@Override
	protected void contextMenuAboutToShow(final IMenuManager m) {
		final IPageSite site = getSite();
		
		m.add(new SelectCodeRangeAction(fLTK));
		
		m.add(new Separator(SharedUIResources.EDIT_COPYPASTE_MENU_ID));
		m.add(new CommandContributionItem(new CommandContributionItemParameter(
				site, null, IWorkbenchCommandConstants.EDIT_CUT, CommandContributionItem.STYLE_PUSH)));
		m.add(new CommandContributionItem(new CommandContributionItemParameter(
				site, null, IWorkbenchCommandConstants.EDIT_COPY, CommandContributionItem.STYLE_PUSH)));
		m.add(new CommandContributionItem(new CommandContributionItemParameter(
				site, null, ISourceEditorCommandIds.COPY_ELEMENT_NAME, CommandContributionItem.STYLE_PUSH)));
		m.add(new CommandContributionItem(new CommandContributionItemParameter(
				site, null, IWorkbenchCommandConstants.EDIT_PASTE, CommandContributionItem.STYLE_PUSH)));
		
		final Separator additions = new Separator(SharedUIResources.ADDITIONS_MENU_ID);
		m.add(additions);
	}
	
}
