/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
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

import de.walware.ecommons.collections.ImCollections;
import de.walware.ecommons.collections.ImList;

import de.walware.ecommons.ltk.LTKUtil;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceStructElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.IWorkspaceSourceUnit;


public class ElementSet {
	
	
	public static String[] getAffectedProjectNatures(final ElementSet set)
			throws CoreException {
		return getAffectedProjectNatures(Collections.singletonList(set));
	}
	
	public static String[] getAffectedProjectNatures(final List<ElementSet> sets)
			throws CoreException {
		final Set<String> natureIds= new HashSet<>();
		for (final ElementSet set : sets) {
			final Set<IProject> affectedProjects= set.getAffectedProjects();
			for (final IProject project : affectedProjects) {
				final String[] ids= project.getDescription().getNatureIds();
				for (final String id : ids) {
					natureIds.add(id);
				}
			}
		}
		return natureIds.toArray(new String[natureIds.size()]);
	}
	
	
	private static final int POST_INIT=    0x10000;
	private static final int POST_PROCESS= 0x70000;
	
	
	protected ImList<Object> initialElements;
	
	private List<IModelElement> modelElements;
	private List<IResource> resources;
	
	private int processState= 0;
	private List<IResource> fResourcesOwnedByElements;
	private List<IFile> fFilesContainingElements;
	
	
	public ElementSet(final List<Object> elements) {
		this.initialElements= ImCollections.toList(elements);
		init(elements);
		
		if (this.modelElements == null) {
			this.modelElements= new ArrayList<>(0);
		}
		if (this.resources == null) {
			this.resources= new ArrayList<>(0);
		}
		if (countElements() == this.initialElements.size()) {
			this.processState= POST_INIT;
		}
		else {
			this.processState= -POST_INIT;
		}
	}
	
	public ElementSet(final Object... elements) {
		this(ImCollections.newList(elements));
	}
	
	
	protected void init(final List<Object> elements) {
		for (final Object o : elements) {
			add(o);
		}
	}
	
	protected void add(final Object o) {
		if (o instanceof IModelElement) {
			if (this.modelElements == null) {
				this.modelElements= new ArrayList<>();
			}
			this.modelElements.add((IModelElement) o);
			return;
		}
		if (o instanceof IResource) {
			if (this.resources == null) {
				this.resources= new ArrayList<>();
			}
			this.resources.add((IResource) o);
			return;
		}
	}
	
	
	protected int countElements() {
		return this.resources.size() + this.modelElements.size();
	}
	
	public int getElementCount() {
		return countElements();
	}
	
	public boolean isOK() {
		return (this.processState > 0);
	}
	
	public List<Object> getInitialObjects() {
		return this.initialElements;
	}
	
	public List<IResource> getResources() {
		return this.resources;
	}
	
	public List<IModelElement> getModelElements() {
		return this.modelElements;
	}
	
	public List<IResource> getResourcesOwnedByElements() {
		return this.fResourcesOwnedByElements;
	}
	
	public List<IFile> getFilesContainingElements() {
		return this.fFilesContainingElements;
	}
	
	public IResource getOwningResource(final IModelElement element) {
		if ((element.getElementType() & IModelElement.MASK_C2) < IModelElement.C2_SOURCE_CHUNK) {
			IResource resource;
			resource= (IResource) element.getAdapter(IResource.class);
			return resource;
		}
		return null;
	}
	
	public IResource getResource(final IModelElement element) {
		final ISourceUnit su= LTKUtil.getSourceUnit(element);
		if (su instanceof IWorkspaceSourceUnit) {
			return ((IWorkspaceSourceUnit) su).getResource();
		}
		return null;
	}
	
	public IProject getSingleProject() {
		IProject project= null;
		for (final IResource resource : this.resources) {
			final IProject p= resource.getProject();
			if (project == null) {
				project= p;
				continue;
			}
			if (!project.equals(p)) {
				return null;
			}
		}
		for (final IModelElement element : this.modelElements) {
			final IResource resource= getResource(element);
			if (resource == null) {
				continue;
			}
			final IProject p= resource.getProject();
			if (project == null) {
				project= p;
				continue;
			}
			if (!project.equals(p)) {
				return null;
			}
		}
		return project;
	}
	
	public Set<IProject> getProjects() {
		final Set<IProject> projects= new HashSet<>();
		for (final IResource resource : this.resources) {
			projects.add(resource.getProject());
		}
		for (final IModelElement element : this.modelElements) {
			final IResource resource= getResource(element);
			if (resource != null) {
				projects.add(resource.getProject());
			}
		}
		return projects;
	}
	
	public Set<IProject> getAffectedProjects() {
		final Set<IProject> projects= getProjects();
		final IProject[] array= projects.toArray(new IProject[projects.size()]);
		for (int i= 0; i < array.length; i++) {
			final IProject[] referencingProjects= array[i].getReferencingProjects();
			if (referencingProjects.length > 0) {
				addAffectedProjects(referencingProjects, projects);
			}
		}
		return projects;
	}
	
	private void addAffectedProjects(final IProject[] projectToAdd, final Set<IProject> projects) {
		for (int i= 0; i < projectToAdd.length; i++) {
			if (projects.add(projectToAdd[i])) {
				final IProject[] referencingProjects= projectToAdd[i].getReferencingProjects();
				if (referencingProjects.length > 0) {
					addAffectedProjects(referencingProjects, projects);
				}
			}
		}
	}
	
	
	public void removeElementsWithAncestorsOnList() {
		if ((this.processState & 0x10) == 0) {
			removeResourcesDescendantsOfResources();
			removeResourcesDescendantsOfModelElements();
			removeModelElementsDescendantsOfModelElements();
			this.processState |= 0x10;
		}
	}
	
	private void removeResourcesDescendantsOfResources() {
		final Iterator<IResource> iter= this.resources.iterator();
		ITER_RESOURCE : while (iter.hasNext()) {
			final IResource subResource= iter.next();
			for (final IResource superResource : this.resources) {
				if (isDescendantOf(subResource, superResource)) {
					iter.remove();
					continue ITER_RESOURCE;
				}
			}
		}
	}
	
	private void removeResourcesDescendantsOfModelElements() {
		final Iterator<IResource> iter= this.resources.iterator();
		ITER_RESOURCE : while (iter.hasNext()) {
			final IResource subResource= iter.next();
			for (final IModelElement superElement : this.modelElements) {
				if (isDescendantOf(subResource, superElement)) {
					iter.remove();
					continue ITER_RESOURCE;
				}
			}
		}
	}
	
	private void removeModelElementsDescendantsOfModelElements() {
		final Iterator<IModelElement> iter= this.modelElements.iterator();
		ITER_ELEMENT : while (iter.hasNext()) {
			final IModelElement subElement= iter.next();
			for (final IModelElement superElement : this.modelElements) {
				if (isDescendantOf(subElement, superElement)) {
					iter.remove();
					continue ITER_ELEMENT;
				}
			}
		}
	}
	
	public boolean includes(final IModelElement element) {
		if (this.modelElements.contains(element)) {
			return true;
		}
		for (final IModelElement e : this.modelElements) {
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
		final IResource superResource= getOwningResource(superElement);
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
		ISourceStructElement parent= ((ISourceStructElement) subElement).getSourceParent();
		while (parent != null){
			if (parent.equals(superElement)) {
				return true;
			}
			parent= parent.getSourceParent();
		}
		return false;
	}
	
	protected void setModelElements(final List<IModelElement> newElements) {
		if (this.processState >= POST_PROCESS) {
			throw new IllegalSelectorException();
		}
		this.modelElements= newElements;
	}
	
	public void postProcess() {
		if (this.processState < 0) {
			throw new IllegalStateException();
		}
		if (this.processState < POST_PROCESS) {
			this.fResourcesOwnedByElements= new ArrayList<>(1);
			this.fFilesContainingElements= new ArrayList<>(1);
			for (final IModelElement element : this.modelElements) {
				IResource resource;
				resource= getOwningResource(element);
				if (resource != null) {
					this.fResourcesOwnedByElements.add(resource);
					continue;
				}
				resource= getResource(element);
				if (resource != null && resource.getType() == IResource.FILE) {
					this.fFilesContainingElements.add((IFile) resource);
					continue;
				}
			}
		}
		this.processState= POST_PROCESS | (this.processState & 0xffff);
	}
	
}
