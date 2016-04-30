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

package de.walware.ecommons.ltk.buildpaths.core;

import java.util.Objects;

import org.eclipse.core.runtime.IPath;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;
import de.walware.jcommons.lang.ObjectUtils;


public class BuildpathElement implements IBuildpathElement {
	
	
	private static final ImList<String> UNINITIALIZED_PATTERNS= ImCollections.newList("UNINITIALIZED_PATTERNS"); //$NON-NLS-1$
	
	
	private final BuildpathElementType type;
	
	/**
	 * The meaning of the path of a classpath entry depends on its entry kind:<ul>
	 *  <li>Source code in the current project (<code>SOURCE</code>) -
	 *     The path associated with this entry is the absolute path to the root folder. </li>
	 *  <li>A binary library in the current project (<code>LIBRARY</code>) - the path
	 *     associated with this entry is the absolute path to the JAR (or root folder), and
	 *     in case it refers to an external JAR, then there is no associated resource in
	 *     the workbench.
	 *  <li>A required project (<code>PROJECT</code>) - the path of the entry denotes the
	 *     path to the corresponding project resource.</li>
	 *  <li>A variable entry (<code>VARIABLE</code>) - the first segment of the path
	 *      is the name of a classpath variable. If this classpath variable
	 *     is bound to the path <it>P</it>, the path of the corresponding classpath entry
	 *     is computed by appending to <it>P</it> the segments of the returned
	 *     path without the variable.</li>
	 *   <li> A container entry (<code>CONTAINER</code>) - the first segment of the path is denoting
	 *     the unique container identifier (for which a <code>ClasspathContainerInitializer</code> could be
	 *     registered), and the remaining segments are used as additional hints for resolving the container entry to
	 *     an actual <code>IClasspathContainer</code>.</li>
	 */
	private final IPath path;
	
	/**
	 * Patterns allowing to include/exclude portions of the resource tree denoted by this entry path.
	 */
	private final ImList<IPath> inclusionPatterns;
	private volatile ImList<String> fullInclusionPatterns;
	private final ImList<IPath> exclusionPatterns;
	private volatile ImList<String> fullExclusionPatterns;
	
	
	/**
	 * Describes the path to the source archive associated with this
	 * classpath entry, or <code>null</code> if this classpath entry has no
	 * source attachment.
	 * <p>
	 * Only library and variable classpath entries may have source attachments.
	 * For library classpath entries, the result path (if present) locates a source
	 * archive. For variable classpath entries, the result path (if present) has
	 * an analogous form and meaning as the variable path, namely the first segment
	 * is the name of a classpath variable.
	 */
	private final IPath sourceAttachmentPath;
	
	/**
	 * Describes the path within the source archive where package fragments
	 * are located. An empty path indicates that packages are located at
	 * the root of the source archive. Returns a non-<code>null</code> value
	 * if and only if <code>getSourceAttachmentPath</code> returns
	 * a non-<code>null</code> value.
	 */
	private final IPath sourceAttachmentRootPath;
	
	/**
	 * See {@link IBuildpathElement#getReferencingEntry()}
	 */
	private final IBuildpathElement referencingElement;
	
	/**
	 * Specific output location (for this source entry)
	 */
	private final IPath specificOutputPath;
	
	/**
	 * The export flag
	 */
	private final boolean isExported;
	
	/**
	 * The extra attributes
	 */
	private final ImList<IBuildpathAttribute> extraAttributes;
	
	
	/**
	 * Creates a class path entry of the specified kind with the given path.
	 */
	public BuildpathElement(final BuildpathElementType type,
			final IPath path, final ImList<IPath> inclusionPatterns, final ImList<IPath> exclusionPatterns,
			final IPath sourceAttachmentPath, final IPath sourceAttachmentRootPath,
			final IPath specificOutputLocation,
			final IBuildpathElement referencingEntry,
			final boolean isExported,
			final ImList<IBuildpathAttribute> extraAttributes) {
		if (type == null) {
			throw new NullPointerException("type"); //$NON-NLS-1$
		}
		if (path == null) {
			throw new NullPointerException("path"); //$NON-NLS-1$
		}
		
		this.type= type;
		this.path= path;
		this.inclusionPatterns= inclusionPatterns;
		this.exclusionPatterns= exclusionPatterns;
		this.referencingElement= referencingEntry;
		
		if (this.inclusionPatterns != null && !this.inclusionPatterns.isEmpty()) {
			this.fullInclusionPatterns= UNINITIALIZED_PATTERNS;
		}
		if (this.exclusionPatterns != null && !this.exclusionPatterns.isEmpty()) {
			this.fullExclusionPatterns= UNINITIALIZED_PATTERNS;
		}
		
		this.sourceAttachmentPath= sourceAttachmentPath;
		this.sourceAttachmentRootPath= sourceAttachmentRootPath;
		this.specificOutputPath= specificOutputLocation;
		
		this.isExported= isExported;
		
		this.extraAttributes= (extraAttributes != null) ? extraAttributes : ImCollections.<IBuildpathAttribute>emptyList();
	}
	
	
	@Override
	public final BuildpathElementType getType() {
		return this.type;
	}
	
	@Override
	public final String getTypeName() {
		return this.type.getName();
	}
	
	@Override
	public final IPath getPath() {
		return this.path;
	}
	
	@Override
	public final ImList<IPath> getInclusionPatterns() {
		return this.inclusionPatterns;
	}
	
	@Override
	public final ImList<IPath> getExclusionPatterns() {
		return this.exclusionPatterns;
	}
	
	private ImList<String> createFullPatterns(final ImList<IPath> patterns) {
		final int length= patterns.size();
		final String[] fullPatterns= new String[length];
		final IPath prefixPath= this.path.removeTrailingSeparator();
		for (int i= 0; i < length; i++) {
			fullPatterns[i]= prefixPath.append(patterns.get(i)).toString();
		}
		return ImCollections.newList(fullPatterns);
	}
	
	public ImList<String> getFullInclusionPatterns() {
		ImList<String> patterns= this.fullInclusionPatterns;
		if (patterns == UNINITIALIZED_PATTERNS) {
			patterns= this.fullInclusionPatterns= createFullPatterns(this.inclusionPatterns);
		}
		return patterns;
	}
	
	public ImList<String> getFullExclusionPatterns() {
		ImList<String> patterns= this.fullExclusionPatterns;
		if (patterns == UNINITIALIZED_PATTERNS) {
			patterns= this.fullExclusionPatterns= createFullPatterns(this.exclusionPatterns);
		}
		return patterns;
	}
	
	@Override
	public IPath getOutputPath() {
		return this.specificOutputPath;
	}
	
	
	@Override
	public ImList<IBuildpathAttribute> getExtraAttributes() {
		return this.extraAttributes;
	}
	
	public IBuildpathAttribute getAttribute(final String name) {
		for (final IBuildpathAttribute attribute : this.extraAttributes) {
			if (attribute.getName().equals(name)) {
				return attribute;
			}
		}
		return null;
	}
	
	
	@Override
	public IPath getSourceAttachmentPath() {
		return this.sourceAttachmentPath;
	}
	
	public IPath getSourceAttachmentRootPath() {
		return this.sourceAttachmentRootPath;
	}
	
	public IBuildpathElement getReferencingEntry() {
		return this.referencingElement;
	}
	
	@Override
	public boolean isExported() {
		return this.isExported;
	}
	
	
	@Override
	public int hashCode() {
		return this.type.hashCode() ^ this.path.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof BuildpathElement) {
			final BuildpathElement other= (BuildpathElement) obj;
			return (this.type == other.type
					&& this.isExported == other.isExported()
					&& this.path.equals(other.getPath())
					&& Objects.equals(this.sourceAttachmentPath, other.sourceAttachmentPath)
					&& Objects.equals(getSourceAttachmentRootPath(), other.sourceAttachmentRootPath)
					&& BuildpathsUtils.equalPatterns(this.inclusionPatterns, other.getInclusionPatterns())
					&& BuildpathsUtils.equalPatterns(this.exclusionPatterns, other.getExclusionPatterns())
					&& Objects.equals(this.specificOutputPath, other.specificOutputPath)
					&& this.extraAttributes.equals(other.extraAttributes) );
		}
		return false;
	}
	
	@Override
	public String toString() {
		final ObjectUtils.ToStringBuilder sb= new ObjectUtils.ToStringBuilder("BuildpathElement"); //$NON-NLS-1$
		sb.append(" ["); //$NON-NLS-1$
		sb.append(this.type.getName());
		sb.append("]:"); //$NON-NLS-1$
		sb.append(getPath().toString());
		sb.addProp("including", getInclusionPatterns()); //$NON-NLS-1$
		sb.addProp("excluding", getExclusionPatterns()); //$NON-NLS-1$
		if (getSourceAttachmentPath() != null) {
			sb.addProp("sourcePath", getSourceAttachmentPath().toString()); //$NON-NLS-1$
			if (getSourceAttachmentRootPath() != null) {
				sb.append(':');
				sb.append(getSourceAttachmentRootPath().toString());
			}
		}
		if (getOutputPath() != null) {
			sb.addProp("outputPath", getOutputPath()); //$NON-NLS-1$
		}
		sb.addProp("isExported", this.isExported);
		final ImList<IBuildpathAttribute> attributes= getExtraAttributes();
		if (!attributes.isEmpty()) {
			sb.addProp("extraAttributes", attributes); //$NON-NLS-1$
		}
		return sb.toString();
	}
	
}
