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

package de.walware.ecommons.ltk.buildpaths.ui;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import de.walware.ecommons.ltk.buildpaths.core.IBuildpathElement;


public class BuildpathListElementComparator extends ViewerComparator {
	
	
	protected static final int SOURCE= 0;
	protected static final int PROJECT= 1;
	protected static final int LIBRARY= 2;
	protected static final int VARIABLE= 3;
	protected static final int CONTAINER= 4;
	
	protected static final int ATTRIBUTE= 11;
	protected static final int CONTAINER_ENTRY= 12;
	
	protected static final int OTHER= 100;
	
	
	public BuildpathListElementComparator() {
	}
	
	
	@Override
	public int category(final Object obj) {
		if (obj instanceof BuildpathListElement) {
			final BuildpathListElement element= (BuildpathListElement) obj;
			if (element.getParent() != null) {
				return CONTAINER_ENTRY;
			}
			return getCategory(element);
		}
		else if (obj instanceof BuildpathListElementAttribute) {
			return ATTRIBUTE;
		}
		return OTHER;
	}
	
	protected int getCategory(final BuildpathListElement element) {
		final String typeName= element.getType().getName();
		if (typeName == IBuildpathElement.SOURCE) {
			return SOURCE;
		}
		else if (typeName == IBuildpathElement.PROJECT) {
			return PROJECT;
		}
		else if (typeName == IBuildpathElement.LIBRARY) {
			return LIBRARY;
		}
		else if (typeName == IBuildpathElement.VARIABLE) {
			return VARIABLE;
		}
		else {
			return OTHER;
		}
	}
	
	@Override
	public int compare(final Viewer viewer, final Object e1, final Object e2) {
		final int cat1= category(e1);
		final int cat2= category(e2);
		
		if (cat1 != cat2) {
			return cat1 - cat2;
		}
		if (cat1 == ATTRIBUTE || cat1 == CONTAINER_ENTRY) {
			return 0; // do not sort attributes or container entries
		}
		if (viewer instanceof ContentViewer) {
			final IBaseLabelProvider prov= ((ContentViewer) viewer).getLabelProvider();
			if (prov instanceof ILabelProvider) {
				final ILabelProvider lprov= (ILabelProvider) prov;
				final String name1= lprov.getText(e1);
				final String name2= lprov.getText(e2);
				return getComparator().compare(name1, name2);
			}
		}
		return 0;
	}
	
}
