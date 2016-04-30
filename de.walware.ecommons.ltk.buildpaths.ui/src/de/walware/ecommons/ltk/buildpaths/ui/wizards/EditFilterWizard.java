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

import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ltk.buildpaths.core.IBuildpathAttribute;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathListElement;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathsUIDescription;
import de.walware.ecommons.ltk.internal.buildpaths.ui.Messages;


public class EditFilterWizard extends BuildPathWizard {
	
	
	private FilterWizardPage filterPage;
	
	
	public EditFilterWizard(final ImList<BuildpathListElement> existingEntries,
			final BuildpathListElement newEntry, final BuildpathsUIDescription uiDescription) {
		super(existingEntries, newEntry, Messages.ExclusionInclusion_Dialog_title, null,
				uiDescription);
	}
	
	
	@Override
	public void addPages() {
		super.addPages();
		
		this.filterPage= new FilterWizardPage(getEntryToEdit(), getUIDescription());
		addPage(this.filterPage);
	}
	
	public void setFocus(final String attributeName) {
		switch (attributeName) {
		case IBuildpathAttribute.FILTER_INCLUSIONS:
		case IBuildpathAttribute.FILTER_EXCLUSIONS:
			this.filterPage.setFocus(attributeName);
			break;
		default:
			break;
		}
	}
	
	@Override
	public boolean performFinish() {
		final BuildpathListElement entryToEdit= getEntryToEdit();
		entryToEdit.setAttribute(IBuildpathAttribute.FILTER_INCLUSIONS, this.filterPage.getInclusionPatterns());
		entryToEdit.setAttribute(IBuildpathAttribute.FILTER_EXCLUSIONS, this.filterPage.getExclusionPatterns());
		
		return true;
	}
	
}
