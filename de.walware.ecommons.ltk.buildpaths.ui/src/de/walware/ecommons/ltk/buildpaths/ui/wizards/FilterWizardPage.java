/*=============================================================================#
 # Copyright (c) 2000-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.ui.wizards;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ui.util.LayoutUtil;

import de.walware.ecommons.ltk.buildpaths.ui.BuildpathListElement;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathsUIDescription;
import de.walware.ecommons.ltk.internal.buildpaths.ui.ExclusionInclusionComponent;
import de.walware.ecommons.ltk.internal.buildpaths.ui.Messages;


public class FilterWizardPage extends WizardPage {
	
	
	private static final String PAGE_NAME= "FilterWizardPage"; //$NON-NLS-1$
	
	
	private final BuildpathListElement element;
	
	private final ExclusionInclusionComponent component;
	
	private String focusAttributeName;
	
	
	public FilterWizardPage(final BuildpathListElement element,
			final BuildpathsUIDescription uiDescription) {
		super(PAGE_NAME);
		
		this.element= element;
		
		setTitle(Messages.ExclusionInclusion_Dialog_title);
		setDescription(Messages.ExclusionInclusion_Dialog_description);
		
		this.component= new ExclusionInclusionComponent(element, uiDescription);
	}
	
	
	@Override
	public void createControl(final Composite parent) {
		initializeDialogUnits(parent);
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(LayoutUtil.createContentGrid(1));
		
		{	final Control control= this.component.create(composite);
			control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
		
		Dialog.applyDialogFont(composite);
		setControl(composite);
		
		this.component.updateTargets();
	}
	
	
	public void setFocus(final String attributeName) {
		final Control control= getControl();
		if (control != null) {
			this.component.setFocus(attributeName);
		}
		else {
			this.focusAttributeName= attributeName;
		}
	}
	
	@Override
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (visible) {
			if (this.focusAttributeName != null) {
				this.component.setFocus(this.focusAttributeName);
				this.focusAttributeName= null;
			}
			else {
				this.component.getControl().setFocus();
			}
		}
	}
	
	private void updateStatus() {
//		this.element.setAttribute(CPListElement.INCLUSION, getInclusionPattern());
//		this.element.setAttribute(CPListElement.EXCLUSION, getExclusionPattern());
//		IStatus status= buildpath.validateBuildpath(this.element.getProject(),
//				BuildpathListElement.convertToCoreEntries(this.buildpath) );
//		updateStatus(statusInfo);
	}
	
	
	public ImList<IPath> getInclusionPatterns() {
		return this.component.getInclusionPatterns();
	}
	
	public ImList<IPath> getExclusionPatterns() {
		return this.component.getExclusionPatterns();
	}
	
}
