/*******************************************************************************
 * Copyright (c) 2007-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.SubMonitor;

import de.walware.ecommons.text.IMarkerPositionResolver;
import de.walware.ecommons.text.ui.AnnotationMarkerPositionResolver;

import de.walware.ecommons.ltk.GenericSourceUnitWorkingCopy;
import de.walware.ecommons.ltk.IWorkingBuffer;
import de.walware.ecommons.ltk.IWorkspaceSourceUnit;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.SourceDocumentRunnable;
import de.walware.ecommons.ltk.WorkingContext;


public abstract class GenericEditorWorkspaceSourceUnitWorkingCopy
		extends GenericSourceUnitWorkingCopy implements IWorkspaceSourceUnit {
	
	
	public GenericEditorWorkspaceSourceUnitWorkingCopy(final IWorkspaceSourceUnit from) {
		super(from);
	}
	
	
	@Override
	public WorkingContext getWorkingContext() {
		return LTK.EDITOR_CONTEXT;
	}
	
	@Override
	public IResource getResource() {
		return ((IWorkspaceSourceUnit) fFrom).getResource();
	}
	
	@Override
	public IMarkerPositionResolver getMarkerPositionResolver() {
		return AnnotationMarkerPositionResolver.createIfRequired(getResource());
	}
	
	@Override
	protected IWorkingBuffer createWorkingBuffer(final SubMonitor progress) {
		return new FileBufferWorkingBuffer(this);
	}
	
	@Override
	public void syncExec(final SourceDocumentRunnable runnable) throws InvocationTargetException {
		FileBufferWorkingBuffer.syncExec(runnable);
	}
	
}
