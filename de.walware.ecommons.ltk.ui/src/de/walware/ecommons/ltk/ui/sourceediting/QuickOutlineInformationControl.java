/*=============================================================================#
 # Copyright (c) 2000, 2012 IBM Corporation and others.
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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import de.walware.ecommons.ui.dialogs.QuickTreeInformationControl;
import de.walware.ecommons.ui.util.DialogUtil;

import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ISourceElement;
import de.walware.ecommons.ltk.ISourceStructElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.LTKUI;
import de.walware.ecommons.ltk.ui.sourceediting.actions.OpenDeclaration;


/**
 * Show outline in light-weight control.
 */
public class QuickOutlineInformationControl extends QuickTreeInformationControl {
	
	
	protected static final String INHERITED_COLOR_NAME = "org.eclipse.jdt.ui.ColoredLabels.inherited"; //$NON-NLS-1$
	
	
	protected class OutlineContentProvider implements ITreeContentProvider {
		
		
		private long currentModelStamp;
		
		
		public OutlineContentProvider() {
		}
		
		
		public long getStamp(final Object inputElement) {
			final ISourceUnitModelInfo modelInfo = getModelInfo(inputElement);
			if (modelInfo != null) {
				return modelInfo.getStamp();
			}
			return ISourceUnit.UNKNOWN_MODIFICATION_STAMP;
		}
		
		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		}
		
		@Override
		public Object[] getElements(final Object inputElement) {
			final ISourceUnitModelInfo modelInfo = getModelInfo(inputElement);
			if (modelInfo != null) {
				this.currentModelStamp = modelInfo.getStamp();
				final List<? extends ISourceStructElement> children = modelInfo.getSourceElement().getSourceChildren(getContentFilter());
				return children.toArray(new ISourceStructElement[children.size()]);
			}
			return new ISourceStructElement[0];
		}
		
		@Override
		public void dispose() {
		}
		
		@Override
		public Object getParent(final Object element) {
			final ISourceStructElement o = (ISourceStructElement) element;
			return o.getSourceParent();
		}
		
		@Override
		public boolean hasChildren(final Object element) {
			final ISourceStructElement o = (ISourceStructElement) element;
			return o.hasSourceChildren(getContentFilter());
		}
		
		@Override
		public Object[] getChildren(final Object parentElement) {
			final ISourceStructElement o = (ISourceStructElement) parentElement;
			final List<? extends ISourceStructElement> children = o.getSourceChildren(getContentFilter());
			return children.toArray(new ISourceStructElement[children.size()]);
		}
	}
	
	
	private final String modelType;
	
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
			final String modelType, final String commandId, final OpenDeclaration opener) {
		super(parent, SWT.RESIZE, true, commandId);
		
		this.modelType = modelType;
		this.opener = opener;
	}
	
	
	public String getModelType() {
		return this.modelType;
	}
	
	@Override
	protected IDialogSettings getDialogSettings() {
		return DialogUtil.getSection(LTKUIPlugin.getDefault().getDialogSettings(), "EditorStructurePopup"); //$NON-NLS-1$
	}
	
	@Override
	protected String getDescription() {
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
		this.requireFullName = (pattern.indexOf('*') >= 0);
		super.setMatcherString(pattern, update);
	}
	
	@Override
	protected String getElementName(final Object element) {
		if (element instanceof IModelElement && !this.requireFullName) {
			return ((IModelElement) element).getElementName().getSegmentName();
		}
		return super.getElementName(element);
	}
	
	protected IModelElement.Filter getContentFilter() {
		return null;
	}
	
	protected OutlineContentProvider createContentProvider() {
		return new OutlineContentProvider();
	}
	
	@Override
	protected void configureViewer(final TreeViewer viewer) {
		this.contentProvider = createContentProvider();
		viewer.setContentProvider(this.contentProvider);
	}
	
	
	@Override
	public void setInput(final Object information) {
		if (information instanceof ISourceElement) {
			final ISourceElement element = (ISourceElement) information;
			final ISourceUnit su = element.getSourceUnit();
			if (su != null) {
				inputChanged(su, element);
				return;
			}
		}
		inputChanged(null, null);
	}
	
	@Override
	protected void openElement(final Object element) throws CoreException {
		if (element instanceof ISourceElement) {
			this.opener.open((ISourceElement) element, true);
		}
	}
	
	
	protected ISourceUnitModelInfo getModelInfo(final Object input) {
		if (input instanceof ISourceUnit) {
			return ((ISourceUnit) input).getModelInfo(getModelType(), 0, null);
		}
		return null;
	}
	
}
