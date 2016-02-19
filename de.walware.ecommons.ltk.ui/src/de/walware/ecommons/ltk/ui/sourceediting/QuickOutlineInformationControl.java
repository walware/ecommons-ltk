/*=============================================================================#
 # Copyright (c) 2000-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation for JDT
 #     Stephan Wahlbrink - initial API and implementation for LTK
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import de.walware.ecommons.ui.dialogs.QuickTreeInformationControl;
import de.walware.ecommons.ui.util.DialogUtil;

import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.IModelElement.Filter;
import de.walware.ecommons.ltk.core.model.ISourceElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.LTKUI;
import de.walware.ecommons.ltk.ui.sourceediting.actions.OpenDeclaration;


/**
 * Show outline in light-weight control.
 */
public abstract class QuickOutlineInformationControl extends QuickTreeInformationControl {
	
	
	protected static final String INHERITED_COLOR_NAME= "org.eclipse.jdt.ui.ColoredLabels.inherited"; //$NON-NLS-1$
	
	
	protected class OutlineContent implements OutlineContentProvider.IOutlineContent {
		
		
		public OutlineContent() {
		}
		
		
		@Override
		public ISourceUnitModelInfo getModelInfo(final Object input) {
			return QuickOutlineInformationControl.this.getModelInfo(input);
		}
		@Override
		public Filter getContentFilter() {
			return QuickOutlineInformationControl.this.getContentFilter();
		}
		
	}
	
	
	private final OpenDeclaration opener;
	
	private OutlineContentProvider contentProvider;
	
	private boolean requireFullName;
	
	
	/**
	 * Creates a new outline information control.
	 *
	 * @param parent the parent shell
	 * @param shellStyle the additional styles for the shell
	 * @param commandId the id of the command that invoked this control or <code>null</code>
	 */
	public QuickOutlineInformationControl(final Shell parent,
			final String commandId, final int pageCount,
			final OpenDeclaration opener) {
		super(parent, SWT.RESIZE, true, commandId, pageCount);
		
		this.opener= opener;
	}
	
	
	public abstract String getModelTypeId();
	
	@Override
	protected IDialogSettings getDialogSettings() {
		return DialogUtil.getSection(LTKUIPlugin.getDefault().getDialogSettings(), "EditorStructurePopup"); //$NON-NLS-1$
	}
	
	@Override
	protected String getDescription(final int page) {
		if (getCommandId() == LTKUI.SHOW_QUICK_SOURCE_OUTLINE_COMMAND_ID) {
			return "Document Outline";
		}
		if (getCommandId() == LTKUI.SHOW_QUICK_ELEMENT_OUTLINE_COMMAND_ID) {
			return "Object Outline";
		}
		return ""; //$NON-NLS-1$
	}
	
	
	@Override
	protected void setMatcherString(final String pattern, final boolean update) {
		this.requireFullName= (pattern.indexOf('*') >= 0);
		super.setMatcherString(pattern, update);
	}
	
	@Override
	protected String getElementName(final IAdaptable element) {
		if (element instanceof IModelElement && !this.requireFullName) {
			return ((IModelElement) element).getElementName().getSegmentName();
		}
		return super.getElementName(element);
	}
	
	
	@Override
	protected void configureViewer(final TreeViewer viewer) {
		this.contentProvider= createContentProvider();
		viewer.setContentProvider(this.contentProvider);
	}
	
	protected OutlineContentProvider createContentProvider() {
		return new OutlineContentProvider(new OutlineContent());
	}
	
	protected ISourceUnitModelInfo getModelInfo(final Object input) {
		if (input instanceof ISourceUnit) {
			return ((ISourceUnit) input).getModelInfo(getModelTypeId(), 0, null);
		}
		return null;
	}
	
	protected IModelElement.Filter getContentFilter() {
		return null;
	}
	
	
	protected int getInitialIterationPage(final ISourceElement element) {
		return 0;
	}
	
	@Override
	public void setInput(final Object information) {
		if (information instanceof ISourceElement) {
			final ISourceElement element= (ISourceElement) information;
			final ISourceUnit su= element.getSourceUnit();
			if (su != null) {
				inputChanged(getInitialIterationPage(element), su, element);
				return;
			}
		}
		
		inputChanged(0, null, null);
	}
	
	@Override
	protected void openElement(final Object element) throws CoreException {
		if (element instanceof ISourceElement) {
			this.opener.open((ISourceElement) element, true);
		}
	}
	
}
