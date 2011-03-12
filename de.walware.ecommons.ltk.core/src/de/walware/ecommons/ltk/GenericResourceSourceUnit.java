/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;

import de.walware.ecommons.text.IMarkerPositionResolver;


/**
 * Generic source unit for files in the Eclipse workspace (IFile).
 */
public abstract class GenericResourceSourceUnit implements IWorkspaceSourceUnit {
	
	
	public static String createResourceId(final IResource file) {
		return AbstractFilePersistenceSourceUnitFactory.createResourceId(file);
	}
	
	
	private final String fId;
	private final IElementName fName;
	
	private final IFile fFile;
	private IWorkingBuffer fBuffer;
	
	private int fCounter = 0;
	
	
	public GenericResourceSourceUnit(final String id, final IFile file) {
		if (file == null) {
			throw new NullPointerException();
		}
		fId = id;
		fFile = file;
		fName = createElementName();
	}
	
	protected IElementName createElementName() {
		return new IElementName() {
			public int getType() {
				return 0x011; // see RElementName
			}
			public String getDisplayName() {
				return fFile.toString();
			}
			public String getSegmentName() {
				return fId;
			}
			public IElementName getNamespace() {
				return null;
			}
			public IElementName getNextSegment() {
				return null;
			}
		};
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type usually doesn't have a underlying unit.
	 */
	public ISourceUnit getUnderlyingUnit() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final String getId() {
		return fId;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type usually belongs to the
	 * {@link LTK#PERSISTENCE_CONTEXT}.
	 */
	public WorkingContext getWorkingContext() {
		return LTK.PERSISTENCE_CONTEXT;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually of the type
	 * {@link IModelElement#C2_SOURCE_FILE C2_SOURCE_FILE}.
	 */
	public int getElementType() {
		return C2_SOURCE_FILE;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final IElementName getElementName() {
		return fName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean exists() {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually not read only.
	 */
	public boolean isReadOnly() {
		// true only for e.g. libraries, not because of read only flag
		return false;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final IResource getResource() {
		return fFile;
	}
	
	public IMarkerPositionResolver getMarkerPositionResolver() {
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean checkState(final boolean validate, final IProgressMonitor monitor) {
		return fBuffer.checkState(validate, monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public AbstractDocument getDocument(final IProgressMonitor monitor) {
		return fBuffer.getDocument(monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public SourceContent getContent(final IProgressMonitor monitor) {
		return fBuffer.getContent(monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void syncExec(final SourceDocumentRunnable runnable) throws InvocationTargetException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object getAdapter(final Class required) {
		if (required.equals(IResource.class)) {
			return getResource();
		}
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public AstInfo<?> getAstInfo(final String type, final boolean ensureSync, final IProgressMonitor monitor) {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ISourceUnitModelInfo getModelInfo(final String type, final int syncLevel, final IProgressMonitor monitor) {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IModelElement getModelParent() {
		return null; // directory
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean hasModelChildren(final Filter filter) {
		return false; 
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<? extends IModelElement> getModelChildren(final Filter filter) {
		return NO_CHILDREN; 
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IProblemRequestor getProblemRequestor() {
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized final void connect(final IProgressMonitor monitor) {
		fCounter++;
		if (fCounter == 1) {
			if (fBuffer == null) {
				fBuffer = new WorkingBuffer(this);
			}
			register();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized final void disconnect(final IProgressMonitor monitor) {
		fCounter--;
		if (fCounter == 0) {
			final SubMonitor progress = SubMonitor.convert(monitor, 2);
			fBuffer.releaseDocument(progress.newChild(1));
			unregister();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized boolean isConnected() {
		return (fCounter > 0);
	}
	
	protected void register() {
	}
	
	protected void unregister() {
	}
	
	
	@Override
	public int hashCode() {
		return fId.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ISourceUnit)) {
			return false;
		}
		final ISourceUnit other = (ISourceUnit) obj;
		return (getElementType() == other.getElementType()
				&& getWorkingContext() == other.getWorkingContext()
				&& getId().equals(other.getId())
				&& getModelTypeId().equals(other.getModelTypeId()) );
	}
	
}
