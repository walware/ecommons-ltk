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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;

import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ltk.buildpaths.ui.BuildpathListElement;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathsUIDescription;
import de.walware.ecommons.ltk.internal.buildpaths.ui.BuildpathsUIPlugin;


public abstract class BuildPathWizard extends Wizard {
	
	
	private final ImList<BuildpathListElement> existingEntries;
	
	private final BuildpathListElement entryToEdit;
	
	private final BuildpathsUIDescription uiDescription;
	
	
	public BuildPathWizard(final ImList<BuildpathListElement> existingEntries, final BuildpathListElement newEntry,
			final String titel, final ImageDescriptor image, final BuildpathsUIDescription uiDescription) {
		this.existingEntries= existingEntries;
		this.entryToEdit= newEntry;
		
		this.uiDescription= uiDescription;
		
		if (image != null) {
			setDefaultPageImageDescriptor(image);
		}
		setDialogSettings(BuildpathsUIPlugin.getInstance().getDialogSettings());
		setWindowTitle(titel);
	}
	
	
	public ImList<BuildpathListElement> getExistingEntries() {
		return this.existingEntries;
	}
	
	protected BuildpathListElement getEntryToEdit() {
		return this.entryToEdit;
	}
	
	
	protected BuildpathsUIDescription getUIDescription() {
		return this.uiDescription;
	}
	
	
	public List<BuildpathListElement> getInsertedElements() {
		return new ArrayList<>();
	}
	
	public List<BuildpathListElement> getRemovedElements() {
		return new ArrayList<>();
	}
	
	public List<BuildpathListElement> getModifiedElements() {
		final ArrayList<BuildpathListElement> result= new ArrayList<>(1);
		result.add(this.entryToEdit);
		return result;
	}
	
}
