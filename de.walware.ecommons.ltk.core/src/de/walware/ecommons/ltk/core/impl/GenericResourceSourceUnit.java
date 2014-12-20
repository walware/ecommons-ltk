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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;

import de.walware.ecommons.text.IMarkerPositionResolver;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.SourceDocumentRunnable;
import de.walware.ecommons.ltk.WorkingContext;
import de.walware.ecommons.ltk.core.SourceContent;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.core.model.IWorkspaceSourceUnit;


/**
 * Generic source unit for files in the Eclipse workspace (IFile).
 */
public abstract class GenericResourceSourceUnit implements IWorkspaceSourceUnit {
	
	
	public static String createResourceId(final IResource file) {
		return AbstractFilePersistenceSourceUnitFactory.createResourceId(file);
	}
	
	
	private final String id;
	private final IElementName name;
	
	private final IFile file;
	private IWorkingBuffer buffer;
	
	private int counter= 0;
	
	
	public GenericResourceSourceUnit(final String id, final IFile file) {
		if (file == null) {
			throw new NullPointerException("file"); //$NON-NLS-1$
		}
		this.id= id;
		this.file= file;
		this.name= createElementName();
	}
	
	protected IElementName createElementName() {
		return new IElementName() {
			@Override
			public int getType() {
				return 0x011; // see RElementName
			}
			@Override
			public String getDisplayName() {
				return GenericResourceSourceUnit.this.file.getName();
			}
			@Override
			public String getSegmentName() {
				return GenericResourceSourceUnit.this.file.getName();
			}
			@Override
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
	@Override
	public ISourceUnit getUnderlyingUnit() {
		return null;
	}
	
	@Override
	public boolean isSynchronized() {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String getId() {
		return this.id;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type usually belongs to the
	 * {@link LTK#PERSISTENCE_CONTEXT}.
	 */
	@Override
	public WorkingContext getWorkingContext() {
		return LTK.PERSISTENCE_CONTEXT;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually of the type
	 * {@link IModelElement#C2_SOURCE_FILE C2_SOURCE_FILE}.
	 */
	@Override
	public int getElementType() {
		return C2_SOURCE_FILE;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final IElementName getElementName() {
		return this.name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exists() {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually not read only.
	 */
	@Override
	public boolean isReadOnly() {
		// true only for e.g. libraries, not because of read only flag
		return false;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final IResource getResource() {
		return this.file;
	}
	
	@Override
	public IMarkerPositionResolver getMarkerPositionResolver() {
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean checkState(final boolean validate, final IProgressMonitor monitor) {
		return this.buffer.checkState(validate, monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractDocument getDocument(final IProgressMonitor monitor) {
		return this.buffer.getDocument(monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getContentStamp(final IProgressMonitor monitor) {
		return this.buffer.getContentStamp(monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public SourceContent getContent(final IProgressMonitor monitor) {
		return this.buffer.getContent(monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void syncExec(final SourceDocumentRunnable runnable) throws InvocationTargetException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAdapter(final Class required) {
		if (required.equals(IResource.class)) {
			return getResource();
		}
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public AstInfo getAstInfo(final String type, final boolean ensureSync, final IProgressMonitor monitor) {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ISourceUnitModelInfo getModelInfo(final String type, final int syncLevel, final IProgressMonitor monitor) {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IModelElement getModelParent() {
		return null; // directory
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasModelChildren(final Filter filter) {
		return false; 
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<? extends IModelElement> getModelChildren(final Filter filter) {
		return Collections.EMPTY_LIST; 
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized final void connect(final IProgressMonitor monitor) {
		this.counter++;
		if (this.counter == 1) {
			if (this.buffer == null) {
				this.buffer= new WorkingBuffer(this);
			}
			register();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized final void disconnect(final IProgressMonitor monitor) {
		this.counter--;
		if (this.counter == 0) {
			final SubMonitor progress= SubMonitor.convert(monitor, 2);
			this.buffer.releaseDocument(progress.newChild(1));
			unregister();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean isConnected() {
		return (this.counter > 0);
	}
	
	protected void register() {
	}
	
	protected void unregister() {
	}
	
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ISourceUnit)) {
			return false;
		}
		final ISourceUnit other= (ISourceUnit) obj;
		return (getElementType() == other.getElementType()
				&& getWorkingContext() == other.getWorkingContext()
				&& getId().equals(other.getId())
				&& getModelTypeId().equals(other.getModelTypeId()) );
	}
	
	@Override
	public String toString() {
		return getModelTypeId() + '/' + getWorkingContext() + ": " + getId(); //$NON-NLS-1$
	}
	
}
