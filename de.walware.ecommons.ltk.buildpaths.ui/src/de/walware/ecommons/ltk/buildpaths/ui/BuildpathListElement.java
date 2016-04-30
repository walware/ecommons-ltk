/*=============================================================================#
 # Copyright (c) 2010-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import de.walware.jcommons.collections.ImCollection;
import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ltk.buildpaths.core.BuildpathElement;
import de.walware.ecommons.ltk.buildpaths.core.BuildpathElementType;
import de.walware.ecommons.ltk.buildpaths.core.BuildpathInitializer;
import de.walware.ecommons.ltk.buildpaths.core.IBuildpathAttribute;
import de.walware.ecommons.ltk.buildpaths.core.IBuildpathElement;


public class BuildpathListElement {
	
	
	private final BuildpathElementType type;
	
	private final IProject project;
	
	private IPath path;
	private final IPath orginalPath;
	private final IResource resource;
	
	private boolean isMissing;
	
	private Object parent;
	private final ArrayList<Object> children;
	
	private IPath linkTarget;
	private final IPath orginalLinkTarget;
	
	private IBuildpathElement cachedElement;
	
	
	public BuildpathListElement(final Object parent, final IProject project, final BuildpathElementType type,
			final IPath path, final IResource resource, final IPath linkTarget, final boolean newElement) {
		this.type= type;
		
		this.parent= parent;
		this.children= new ArrayList<>();
		
		this.project= project;
		
		this.isMissing= false;
		
		this.path= path;
		this.orginalPath= (newElement) ? null : path;
		this.linkTarget= linkTarget;
		this.orginalLinkTarget= linkTarget;
		this.resource= resource;
		
		initBuiltinAttributes();
	}
	
	public BuildpathListElement(final IProject project, final Object parent,
			final IBuildpathElement element, final boolean newElement) {
		this.type= element.getType();
		
		this.parent= parent;
		this.children= new ArrayList<>();
		
		this.project= project;
		
		this.isMissing= false;
		
		{	IPath path= element.getPath();
			final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			
			// get the resource
			IResource resource= null;
			IPath linkTarget= null;
			
			
			switch (element.getType().getName()) {
			case IBuildpathElement.SOURCE:
				path= path.removeTrailingSeparator();
				resource= root.findMember(path);
				if (resource == null) {
					if (root.getWorkspace().validatePath(path.toString(), IResource.FOLDER).isOK()) {
						resource= root.getFolder(path);
					}
					this.isMissing= true;
				}
				else if (resource.isLinked()) {
					linkTarget= resource.getLocation();
				}
				break;
			case IBuildpathElement.PROJECT:
				resource= root.findMember(path);
				this.isMissing= (resource == null);
				break;
			default:
				throw new UnsupportedOperationException(element.getType().getName());
			}
			
			this.path= path;
			this.orginalPath= (newElement) ? null : path;
			this.resource= resource;
			
			this.linkTarget= linkTarget;
			this.orginalLinkTarget= linkTarget;
		}
		
		initBuiltinAttributes();
		setAttribute(IBuildpathAttribute.FILTER_INCLUSIONS, element.getInclusionPatterns());
		setAttribute(IBuildpathAttribute.FILTER_EXCLUSIONS, element.getExclusionPatterns());
		setAttribute(IBuildpathAttribute.SOURCE_ATTACHMENT, element.getSourceAttachmentPath());
		setAttribute(IBuildpathAttribute.OUTPUT, element.getOutputPath());
		
		final ImList<IBuildpathAttribute> extraAttributes= element.getExtraAttributes();
		for (final IBuildpathAttribute attribute : extraAttributes) {
			if (setAttribute(attribute.getName(), attribute.getValue()) == null) {
				addExtraAttribute(attribute.getName(), attribute.getValue());
			}
		}
	}
	
	
	/**
	 * Gets the class path entry kind.
	 * @return the entry kind
	 * @see IBuildpathElement#getType()
	 */
	public BuildpathElementType getType() {
		return this.type;
	}
	
	/**
	 * Entries without resource are either non existing or a variable entry
	 * External jars do not have a resource
	 * @return returns the resource
	 */
	public IResource getResource() {
		return this.resource;
	}
	
	/**
	 * Gets the project.
	 * @return Returns a IProject
	 */
	public IProject getProject() {
		return this.project;
	}
	
	/**
	 * Gets the class path entry path.
	 * @return returns the path
	 * @see IBuildpathElement#getPath()
	 */
	public IPath getPath() {
		return this.path;
	}
	
	public IPath getOrginalPath() {
		return this.orginalPath;
	}
	
	public void setPath(final IPath path) {
		this.cachedElement= null;
		this.path= path;
	}
	
	
	public IPath getLinkTarget() {
		return this.linkTarget;
	}
	
	public IPath getOrginalLinkTarget() {
		return this.orginalLinkTarget;
	}
	
	public void setLinkTarget(final IPath linkTarget) {
		this.cachedElement= null;
		this.linkTarget= linkTarget;
	}
	
	
	public IBuildpathElement getCoreElement() {
		if (this.cachedElement == null) {
			this.cachedElement= newCoreElement();
		}
		return this.cachedElement;
	}
	
	private IBuildpathElement newCoreElement() {
		return new BuildpathElement(this.type, this.path,
				(this.type.isAttributeBuiltin(IBuildpathAttribute.FILTER_INCLUSIONS)) ?
						(ImList<IPath>) getAttributeValue(IBuildpathAttribute.FILTER_INCLUSIONS) : null,
				(this.type.isAttributeBuiltin(IBuildpathAttribute.FILTER_EXCLUSIONS)) ?
						(ImList<IPath>) getAttributeValue(IBuildpathAttribute.FILTER_EXCLUSIONS) : null,
				(this.type.isAttributeBuiltin(IBuildpathAttribute.SOURCE_ATTACHMENT)) ?
						(IPath) getAttributeValue(IBuildpathAttribute.SOURCE_ATTACHMENT) : null,
				null,
				(this.type.isAttributeBuiltin(IBuildpathAttribute.OUTPUT)) ?
						(IPath) getAttributeValue(IBuildpathAttribute.OUTPUT) : null,
				null,
				false,
				ImCollections.toList(getExtraAttributes()) );
	}
	
	
	public List<Object> getChildren() {
		return getFilteredChildren(ImCollections.<String>emptySet());
	}
	
	private void initBuiltinAttributes() {
		for (final String attributeKey : this.type.getAttributeBuiltinKeys()) {
			switch (attributeKey) {
			case IBuildpathAttribute.FILTER_INCLUSIONS:
			case IBuildpathAttribute.FILTER_EXCLUSIONS:
				createAttribute(attributeKey, ImCollections.emptyList(), true);
				continue;
			case IBuildpathAttribute.OUTPUT:
				createAttribute(attributeKey, null, true);
				continue;
			default:
				throw new IllegalStateException(attributeKey);
			}
		}
	}
	
	private List<IBuildpathAttribute> getExtraAttributes() {
		final ArrayList<IBuildpathAttribute> list= new ArrayList<>(Math.min(this.children.size(), 8));
		for (final Object child : this.children) {
			if (child instanceof BuildpathListElementAttribute) {
				final BuildpathListElementAttribute attribute= (BuildpathListElementAttribute) child;
				if (!attribute.isBuiltin() && attribute.getValue() != null) {
					list.add(attribute.getCoreAttribute());
				}
			}
		}
		return list;
	}
	
	private boolean isFiltered(final Object child, final ImCollection<String> filteredAttributeKeys) {
		if (child instanceof BuildpathListElementAttribute) {
			final BuildpathListElementAttribute attribute= (BuildpathListElementAttribute) child;
			if (filteredAttributeKeys.contains(attribute.getName())) {
				return true;
			}
			if (attribute.getStatus().getCode() == BuildpathInitializer.NOT_SUPPORTED) {
				return true;
			}
			if (!attribute.isBuiltin()) {
//				return !BuildpathsUtils.getDefault().getClasspathAttributeConfigurationDescriptors().containsKey(key);
			}
		}
		return false;
	}
	
	public List<Object> getFilteredChildren(final ImCollection<String> filteredAttributeKeys) {
		final int nChildren= this.children.size();
		final ArrayList<Object> filtered= new ArrayList<>(nChildren);
		
		for (int i= 0; i < nChildren; i++) {
			final Object child= this.children.get(i);
			if (!isFiltered(child, filteredAttributeKeys)) {
				filtered.add(child);
			}
		}
		return filtered;
	}
	
	public BuildpathListElementAttribute findAttribute(final String name) {
		for (final Object child : this.children) {
			if (child instanceof BuildpathListElementAttribute) {
				final BuildpathListElementAttribute attribute= (BuildpathListElementAttribute) child;
				if (name.equals(attribute.getName())) {
					return attribute;
				}
			}
		}
		return null;
	}
	
	public Object getAttributeValue(final String name) {
		final BuildpathListElementAttribute attribute= findAttribute(name);
		if (attribute != null) {
			return attribute.getValue();
		}
		return null;
	}
	
	public List<BuildpathListElementAttribute> getAllAttributes() {
		final ArrayList<BuildpathListElementAttribute> attributes= new ArrayList<>();
		for (final Object child : this.children) {
			if (child instanceof BuildpathListElementAttribute) {
				attributes.add((BuildpathListElementAttribute) child);
			}
		}
		return attributes;
	}
	
	/**
	 * Notifies that an attribute has changed
	 *
	 * @param name name of changed attribute
	 */
	protected void attributeChanged(final String name) {
		this.cachedElement= null;
	}
	
	public BuildpathListElementAttribute setAttribute(final String name, final Object value) {
		final BuildpathListElementAttribute attribute= findAttribute(name);
		if (attribute == null) {
			return null;
		}
		
		switch(name) {
		case IBuildpathAttribute.FILTER_INCLUSIONS:
		case IBuildpathAttribute.FILTER_EXCLUSIONS:
			if (!(value instanceof ImList<?>)) {
				throw new IllegalArgumentException();
			}
			break;
		default:
			break;
		}
		
		attribute.setValue(value);
		return attribute;
	}
	
	private BuildpathListElementAttribute createAttribute(final String name,
			final Object value, final boolean builtin) {
		final BuildpathListElementAttribute attribute= new BuildpathListElementAttribute(this,
				name, value, builtin );
		this.children.add(attribute);
		return attribute;
	}
	
	public void setAttributesFromExisting(final BuildpathListElement existing) {
		Assert.isTrue(existing.getType() == getType());
		final List<BuildpathListElementAttribute> attributes= existing.getAllAttributes();
		for (final BuildpathListElementAttribute attribute : attributes) {
			final BuildpathListElementAttribute elem= findAttribute(attribute.getName());
			if (elem == null) {
				createAttribute(attribute.getName(), attribute.getValue(), false);
			}
			else {
				elem.setValue(attribute.getValue());
			}
		}
	}
	
	public BuildpathListElementAttribute addExtraAttribute(final String name, final Object value) {
		return createAttribute(name, value, false);
	}
	
	
	public Object getParent() {
		return this.parent;
	}
	
	/**
	 * Sets the parent container.
	 * 
	 * @param parent the parent container
	 * @since 3.7
	 */
	void setParentContainer(final Object parent) {
		this.parent= parent;
	}
	
	
	private IStatus evaluateContainerChildStatus(final BuildpathListElementAttribute attrib) {
		return null;
	}
	
	public IStatus getContainerChildStatus(final BuildpathListElementAttribute attrib) {
		if (this.parent instanceof BuildpathListElement) {
			final BuildpathListElement parentElement= (BuildpathListElement) this.parent;
			return parentElement.getContainerChildStatus(attrib);
		}
		return Status.OK_STATUS;
	}
	
	public boolean isInContainer(final String containerName) {
		if (this.parent instanceof BuildpathListElement) {
			final BuildpathListElement elem= (BuildpathListElement) this.parent;
			return new Path(containerName).isPrefixOf(elem.getPath());
		}
		return false;
	}
	
	
	/**
	 * Returns if a entry is missing.
	 * @return Returns a boolean
	 */
	public boolean isMissing() {
		return this.isMissing;
	}
	
	/**
	 * Returns if a entry has children that are missing
	 * @return Returns a boolean
	 */
	public boolean hasMissingChildren() {
		for (final Object child : this.children) {
			if (child instanceof BuildpathListElement
					&& ((BuildpathListElement) child).isMissing()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sets the 'missing' state of the entry.
	 * @param isMissing the new state
	 */
	public void setIsMissing(final boolean isMissing) {
		this.isMissing= isMissing;
	}
	
	
	public boolean isDeprecated() {
		return false;
	}
	
	public String getDeprecationMessage() {
		return null;
	}
	
	
	@Override
	public int hashCode() {
		return this.type.hashCode() + this.path.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj.getClass().equals(getClass())) {
			final BuildpathListElement other= (BuildpathListElement) obj;
			return getCoreElement().equals(other.getCoreElement());
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getCoreElement().toString();
	}
	
}
