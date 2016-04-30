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

package de.walware.ecommons.ltk.buildpaths.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ltk.internal.buildpaths.core.Messages;


public abstract class BuildpathInitializer {
	
	
	/**
	 * Status code indicating that an attribute is not supported.
	 */
	public static final int NOT_SUPPORTED= 1 << 16;
	
	/**
	 * Status code indicating that an attribute is not modifiable.
	 */
	public static final int READ_ONLY= 1 << 17;
	
	
	/**
	 * Status constant indicating that a buildpath entry was invalid
	 */
	public static final int INVALID_BUILDPATH= 964;
	
	/**
	 * Status constant indicating that a naming collision would occur
	 * if the operation proceeded.
	 */
	public static final int NAME_COLLISION= 977;
	
	
	private static final IStatus VERIFIED_OK= new Status(IStatus.OK, LTKBuildpathsCore.PLUGIN_ID, 0, "OK", null); //$NON-NLS-1$
	
	
	public boolean canUpdateContainer(final IPath path, final IProject project) {
		return false;
	}
	
	public IStatus getAttributeStatus(final IPath path, final IProject project, final String key) {
		return null;
	}
	
	
	public IResource getWorkspaceTarget(final IPath path) {
		if (path == null || path.getDevice() != null) {
			return null;
		}
		final IWorkspace workspace= ResourcesPlugin.getWorkspace();
		if (workspace == null) {
			return null;
		}
		return workspace.getRoot().findMember(path);
	}
	
	protected String getEntryPathMsg(final IProject project, final IPath path) {
		return (project.getName().equals(path.segment(0))) ?
				path.removeFirstSegments(1).makeRelative().toString() :
				path.toString();
	}
	
	
	/**
	 * Validate a given buildpath and output location for a project, using the following rules:
	 * <ul>
	 *   <li>Buildpath entries cannot collide with each other; that is, all entry paths must be unique.
	 *   <li>The project output location path cannot be null, must be absolute and located inside the project.
	 *   <li>Specific output locations (specified on source entries) can be null, if not they must be located inside the project,
	 *   <li>A project entry cannot refer to itself directly (that is, a project cannot prerequisite itself).
	 *   <li>Buildpath entries or output locations cannot coincidate or be nested in each other, except for the following scenario listed below:
	 *     <ul><li>A source folder can coincidate with its own output location, in which case this output can then contain library archives.
	 *         However, a specific output location cannot coincidate with any library or a distinct source folder than the one referring to it. </li>
	 *         <li>A source/library folder can be nested in any source folder as long as the nested folder is excluded from the enclosing one. </li>
	 *         <li>An output location can be nested in a source folder, if the source folder coincidates with the project itself, or if the output
	 *             location is excluded from the source folder. </li>
	 *    </ul>
	 * </ul>
	 * 
	 * Note that the buildpath entries are not validated automatically. Only bound variables or containers are considered
	 * in the checking process (this allows to perform a consistency check on a buildpath which has references to
	 * yet non existing projects, folders, ...).
	 * <p>
	 * This validation is intended to anticipate buildpath issues prior to assigning it to a project. In particular, it will automatically
	 * be performed during the buildpath setting operation (if validation fails, the buildpath setting will not complete).
	 * <p>
	 * 
	 * @param project the given java project
	 * @param rawBuildpath a given buildpath
	 * @param projectOutputLocation a given output location
	 * @return a status object with code <code>IStatus.OK</code> if
	 *     the given buildpath and output location are compatible, otherwise a status
	 *     object indicating what is wrong with the buildpath or output location
	 */
	public IStatus validateBuildpath(final IProject project, final List<IBuildpathElement> rawBuildpath) {
		// tolerate null path, it will be reset to default
		if (rawBuildpath == null) {
			return VERIFIED_OK;
		}
		
		// check duplicate entries on raw buildpath only
		{	final Set<IPath> pathes= new HashSet<>(rawBuildpath.size());
			for (final IBuildpathElement element : rawBuildpath) {
				final IPath path= element.getPath();
				if (!pathes.add(path)){
					return createErrorStatus(NAME_COLLISION, NLS.bind(
							Messages.BuildpathStatus_DuplicateEntryPath_message,
							getEntryPathMsg(project, path), project.getName() ));
				}
			}
		}
		
		for (final IBuildpathElement element : rawBuildpath) {
			final IStatus status= validateBuildpathEntry(project, element);
			if (status.getSeverity() == IStatus.ERROR) {
				return status;
			}
		}
		
		return VERIFIED_OK;
	}
	
	protected IStatus validateBuildpathEntry(final IProject project, final IBuildpathElement entry) {
		final IPath path= entry.getPath();
		
		// Build some common strings for status message
		final String projectName= project.getName();
		
		switch (entry.getTypeName()) {
		case IBuildpathElement.SOURCE:
			if (!path.isAbsolute() || path.isEmpty()) {
				return createErrorStatus(INVALID_BUILDPATH, NLS.bind(
								Messages.BuildpathStatus_Entry_IllegalSourceFolderPath_message,
								getEntryPathMsg(project, path), projectName ));
			}
			{	final IPath projectPath= project.getProject().getFullPath();
				if (!projectPath.isPrefixOf(path) || getWorkspaceTarget(path) == null) {
					return createErrorStatus(INVALID_BUILDPATH, NLS.bind(
							Messages.BuildpathStatus_Entry_UnboundSourceFolder_message,
							getEntryPathMsg(project, path), projectName ));
				}
			}
			break;
		}
		
		// Validate extra attributes
		final ImList<IBuildpathAttribute> extraAttributes= entry.getExtraAttributes();
		if (extraAttributes != null) {
			final int length= extraAttributes.size();
			final Set<String> set= new HashSet<>(length);
			for (final IBuildpathAttribute attribute : extraAttributes) {
				if (!set.add(attribute.getName())) {
					return createErrorStatus(NAME_COLLISION, NLS.bind(
							Messages.BuildpathStatus_Entry_DuplicateExtraAttribute_message, new String[] {
									attribute.getName(),
									getEntryPathMsg(project, path),
									projectName
							}));
				}
			}
		}
		
		return VERIFIED_OK;
	}
	
	protected IStatus createErrorStatus(final int code, final String message) {
		return new Status(IStatus.ERROR, LTKBuildpathsCore.PLUGIN_ID, code, message, null);
	}
	
}
