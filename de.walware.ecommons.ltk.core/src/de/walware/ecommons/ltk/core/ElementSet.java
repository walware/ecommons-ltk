/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.walware.ecommons.collections.ConstArrayList;

import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ISourceStructElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.IWorkspaceSourceUnit;
import de.walware.ecommons.ltk.LTKUtil;


public class ElementSet {
	
	
	public static String[] getAffectedProjectNatures(final ElementSet set)
			throws CoreException {
		return getAffectedProjectNatures(Collections.singletonList(set));
	}
	
	public static String[] getAffectedProjectNatures(final List<ElementSet> sets)
			throws CoreException {
		final Set<String> natureIds = new HashSet<String>();
		for (final ElementSet set : sets) {
			final Set<IProject> affectedProjects = set.getAffectedProjects();
			for (final IProject project : affectedProjects) {
				final String[] ids = project.getDescription().getNatureIds();
				for (final String id : ids) {
					natureIds.add(id);
				}
			}
		}
		return natureIds.toArray(new String[natureIds.size()]);
	}
	
	
	private static final int POST_INIT =    0x10000;
	private static final int POST_PROCESS = 0x70000;
	
	
	protected List<Object> fInitialElements;
	
	private List<IModelElement> fModelElements;
	private List<IResource> fResources;
	
	private int fProcessState = 0;
	private List<IResource> fResourcesOwnedByElements;
	private List<IFile> fFilesContainingElements;
	
	
	public ElementSet(final Object... elements) {
		fInitialElements = new ConstArrayList<Object>(elements);
		init(elements);
		
		if (fModelElements == null) {
			fModelElements = new ArrayList<IModelElement>(0);
		}
		if (fResources == null) {
			fResources = new ArrayList<IResource>(0);
		}
		if (countElements() == fInitialElements.size()) {
			fProcessState = POST_INIT;
		}
		else {
			fProcessState = -POST_INIT;
		}
	}
	
	
	protected void init(final Object[] elements) {
		for (final Object o : elements) {
			add(o);
		}
	}
	
	protected void add(final Object o) {
		if (o instanceof IModelElement) {
			if (fModelElements == null) {
				fModelElements = new ArrayList<IModelElement>();
			}
			fModelElements.add((IModelElement) o);
			return;
		}
		if (o instanceof IResource) {
			if (fResources == null) {
				fResources = new ArrayList<IResource>();
			}
			fResources.add((IResource) o);
			return;
		}
	}
	
	
	protected int countElements() {
		return fResources.size() + fModelElements.size();
	}
	
	public int getElementCount() {
		return countElements();
	}
	
	public boolean isOK() {
		return (fProcessState > 0);
	}
	
	public List<Object> getInitialObjects() {
		return fInitialElements;
	}
	
	public List<IResource> getResources() {
		return fResources;
	}
	
	public List<IModelElement> getModelElements() {
		return fModelElements;
	}
	
	public List<IResource> getResourcesOwnedByElements() {
		return fResourcesOwnedByElements;
	}
	
	public List<IFile> getFilesContainingElements() {
		return fFilesContainingElements;
	}
	
	public IResource getOwningResource(final IModelElement element) {
		if ((element.getElementType() & IModelElement.MASK_C2) < IModelElement.C2_SOURCE_CHUNK) {
			IResource resource;
			resource = (IResource) element.getAdapter(IResource.class);
			return resource;
		}
		return null;
	}
	
	public IResource getResource(final IModelElement element) {
		final ISourceUnit su = LTKUtil.getSourceUnit(element);
		if (su instanceof IWorkspaceSourceUnit) {
			return ((IWorkspaceSourceUnit) su).getResource();
		}
		return null;
	}
	
	public IProject getSingleProject() {
		IProject project = null;
		for (final IResource resource : fResources) {
			final IProject p = resource.getProject();
			if (project == null) {
				project = p;
				continue;
			}
			if (!project.equals(p)) {
				return null;
			}
		}
		for (final IModelElement element : fModelElements) {
			final IResource resource = getResource(element);
			if (resource == null) {
				continue;
			}
			final IProject p = resource.getProject();
			if (project == null) {
				project = p;
				continue;
			}
			if (!project.equals(p)) {
				return null;
			}
		}
		return project;
	}
	
	public Set<IProject> getProjects() {
		final Set<IProject> projects = new HashSet<IProject>();
		for (final IResource resource : fResources) {
			projects.add(resource.getProject());
		}
		for (final IModelElement element : fModelElements) {
			final IResource resource = getResource(element);
			if (resource != null) {
				projects.add(resource.getProject());
			}
		}
		return projects;
	}
	
	public Set<IProject> getAffectedProjects() {
		final Set<IProject> projects = getProjects();
		final IProject[] array = projects.toArray(new IProject[projects.size()]);
		for (int i = 0; i < array.length; i++) {
			final IProject[] referencingProjects = array[i].getReferencingProjects();
			if (referencingProjects.length > 0) {
				addAffectedProjects(referencingProjects, projects);
			}
		}
		return projects;
	}
	
	private void addAffectedProjects(final IProject[] projectToAdd, final Set<IProject> projects) {
		for (int i = 0; i < projectToAdd.length; i++) {
			if (projects.add(projectToAdd[i])) {
				final IProject[] referencingProjects = projectToAdd[i].getReferencingProjects();
				if (referencingProjects.length > 0) {
					addAffectedProjects(referencingProjects, projects);
				}
			}
		}
	}
	
	
	public void removeElementsWithAncestorsOnList() {
		if ((fProcessState & 0x10) == 0) {
			removeResourcesDescendantsOfResources();
			removeResourcesDescendantsOfModelElements();
			removeModelElementsDescendantsOfModelElements();
			fProcessState |= 0x10;
		}
	}
	
	private void removeResourcesDescendantsOfResources() {
		final Iterator<IResource> iter = fResources.iterator();
		ITER_RESOURCE : while (iter.hasNext()) {
			final IResource subResource = iter.next();
			for (final IResource superResource : fResources) {
				if (isDescendantOf(subResource, superResource)) {
					iter.remove();
					continue ITER_RESOURCE;
				}
			}
		}
	}
	
	private void removeResourcesDescendantsOfModelElements() {
		final Iterator<IResource> iter = fResources.iterator();
		ITER_RESOURCE : while (iter.hasNext()) {
			final IResource subResource = iter.next();
			for (final IModelElement superElement : fModelElements) {
				if (isDescendantOf(subResource, superElement)) {
					iter.remove();
					continue ITER_RESOURCE;
				}
			}
		}
	}
	
	private void removeModelElementsDescendantsOfModelElements() {
		final Iterator<IModelElement> iter = fModelElements.iterator();
		ITER_ELEMENT : while (iter.hasNext()) {
			final IModelElement subElement = iter.next();
			for (final IModelElement superElement : fModelElements) {
				if (isDescendantOf(subElement, superElement)) {
					iter.remove();
					continue ITER_ELEMENT;
				}
			}
		}
	}
	
	public boolean includes(final IModelElement element) {
		if (fModelElements.contains(element)) {
			return true;
		}
		for (final IModelElement e : fModelElements) {
			if (isDescendantOf(element, e)) {
				return true;
			}
		}
		// TODO check resources
		return false;
	}
	
	protected boolean isDescendantOf(final IResource subResource, final IResource superResource) {
		return !subResource.equals(superResource) && superResource.getFullPath().isPrefixOf(subResource.getFullPath());
	}
	
	protected boolean isDescendantOf(final IResource subResource, final IModelElement superElement) {
		final IResource superResource = getOwningResource(superElement);
		if (superResource != null) {
			return isDescendantOf(subResource, superResource);
		}
		return false;
	}
	
	protected boolean isDescendantOf(final IModelElement subElement, final IModelElement superElement) {
		if (subElement.equals(superElement)
				|| !(subElement instanceof ISourceStructElement)) {
			return false;
		}
		ISourceStructElement parent = ((ISourceStructElement) subElement).getSourceParent();
		while (parent != null){
			if (parent.equals(superElement)) {
				return true;
			}
			parent = parent.getSourceParent();
		}
		return false;
	}
	
	protected void setModelElements(final List<IModelElement> newElements) {
		if (fProcessState >= POST_PROCESS) {
			throw new IllegalSelectorException();
		}
		fModelElements = newElements;
	}
	
	public void postProcess() {
		if (fProcessState < 0) {
			throw new IllegalStateException();
		}
		if (fProcessState < POST_PROCESS) {
			fResourcesOwnedByElements = new ArrayList<IResource>(1);
			fFilesContainingElements = new ArrayList<IFile>(1);
			for (final IModelElement element : fModelElements) {
				IResource resource;
				resource = getOwningResource(element);
				if (resource != null) {
					fResourcesOwnedByElements.add(resource);
					continue;
				}
				resource = getResource(element);
				if (resource != null && resource.getType() == IResource.FILE) {
					fFilesContainingElements.add((IFile) resource);
					continue;
				}
			}
		}
		fProcessState = POST_PROCESS | (fProcessState & 0xffff);
	}
	
}
