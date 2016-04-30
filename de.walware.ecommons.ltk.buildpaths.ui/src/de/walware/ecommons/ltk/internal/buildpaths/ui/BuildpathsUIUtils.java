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

package de.walware.ecommons.ltk.internal.buildpaths.ui;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import de.walware.ecommons.ltk.buildpaths.core.IBuildpathElement;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathListElement;


public class BuildpathsUIUtils {
	
	
	public static boolean isProjectSourceFolder(final List<BuildpathListElement> listElements,
			final IProject project) {
		final IPath projectPath= project.getProject().getFullPath();
		for (final BuildpathListElement listElement : listElements) {
			final IBuildpathElement element= listElement.getCoreElement();
			if (element.getTypeName() == IBuildpathElement.SOURCE) {
				if (projectPath.equals(element.getPath())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static void insert(final List<BuildpathListElement> list, final BuildpathListElement element) {
		final int length= list.size();
		int i= 0;
		while (i < length && list.get(i).getType() != element.getType()) {
			i++;
		}
		if (i < length) {
			i++;
			while (i < length && list.get(i).getType() == element.getType()) {
				i++;
			}
			list.add(i, element);
			return;
		}
		
		switch (element.getType().getName()) {
		case IBuildpathElement.SOURCE:
			list.add(0, element);
			break;
		case IBuildpathElement.PROJECT:
		default:
			list.add(element);
			break;
		}
	}
	
	
	public static IBuildpathElement[] convertToCoreElements(final List<BuildpathListElement> listElements) {
		final IBuildpathElement[] result= new IBuildpathElement[listElements.size()];
		int i= 0;
		for (final Iterator<BuildpathListElement> iter= listElements.iterator(); iter.hasNext();) {
			final BuildpathListElement cur= iter.next();
			result[i]= cur.getCoreElement();
			i++;
		}
		return result;
	}
	
}
