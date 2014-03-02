/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitFactory;
import de.walware.ecommons.ltk.LTK;


/**
 * Abstract factory for {@link LTK#PERSISTENCE_CONTEXT}.
 */
public abstract class AbstractFilePersistenceSourceUnitFactory implements ISourceUnitFactory {
	
	
	private final static String IFILE_PREFIX = "platform:/resource"; //$NON-NLS-1$
	
	
	public static String createResourceId(final IResource file) {
		if (file != null) {
			final IPath path = file.getFullPath();
			if (path != null) {
				return IFILE_PREFIX + path.toPortableString(); // eclipse-platform-resource
			}
		}
		return null;
	}
	
	public static String createResourceId(URI uri) {
		if (uri != null) {
			uri = uri.normalize();
			if (uri.getScheme() == null) {
				return "xxx:"+uri.toString(); //$NON-NLS-1$
			}
			else {
				return uri.toString();
			}
		}
		return null;
	}
	
	
	public AbstractFilePersistenceSourceUnitFactory() {
	}
	
	
	@Override
	public String createId(final Object from) {
		if (from instanceof IFile) {
			return createResourceId((IFile) from);
		}
		if (from instanceof String) {
			final String s = (String) from;
			if (s.startsWith(IFILE_PREFIX)) {
				return s;
			}
		}
		return null;
	}
	
	@Override
	public ISourceUnit createSourceUnit(final String id, final Object from) {
		IFile ifile;
		if (from instanceof IFile) {
			ifile = (IFile) from;
		}
		else {
			final IPath path = new Path(id.substring(IFILE_PREFIX.length()));
			ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		}
		return createSourceUnit(id, ifile);
	}
	
	
	protected abstract ISourceUnit createSourceUnit(final String id, final IFile file);
	
}
