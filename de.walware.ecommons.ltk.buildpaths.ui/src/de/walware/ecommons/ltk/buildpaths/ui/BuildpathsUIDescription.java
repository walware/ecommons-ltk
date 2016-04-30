/*=============================================================================#
 # Copyright (c) 2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.ui;

import java.util.List;

import org.eclipse.core.resources.IProject;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ltk.buildpaths.core.BuildpathElementType;
import de.walware.ecommons.ltk.buildpaths.core.IBuildpathElement;


public class BuildpathsUIDescription {
	
	
	public void toListElements(final IProject project, final List<IBuildpathElement> coreElements,
			final List<BuildpathListElement> listElements) {
		for (final IBuildpathElement element : coreElements) {
			listElements.add(new BuildpathListElement(project, null, element, false));
		}
	}
	
	public ImList<IBuildpathElement> toCoreElements(final List<BuildpathListElement> listElements) {
		final IBuildpathElement[] coreElements= new IBuildpathElement[listElements.size()];
		int j= 0;
		for (final BuildpathListElement listElement : listElements) {
			coreElements[j++]= listElement.getCoreElement();
		}
		return ImCollections.newList(coreElements);
	}
	
	
	public BuildpathListElementComparator createListElementComparator() {
		return new BuildpathListElementComparator();
	}
	
	public BuildpathListLabelProvider createListLabelProvider() {
		return new BuildpathListLabelProvider();
	}
	
	
	public String getDefaultExt(final BuildpathListElement element) {
		return "ext";
	}
	
	
	public boolean getAllowAdd(final IProject project, final BuildpathElementType type) {
		return true;
	}
	
	public boolean getAllowEdit(final BuildpathListElement element) {
		return true;
	}
	
	public boolean getAllowEdit(final BuildpathListElementAttribute attribute) {
		return true;
	}
	
}
