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

import java.util.Collections;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.core.SourceContent;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;


/**
 * Generic source unit for external files (URI/EFS).
 */
public abstract class GenericUriSourceUnit implements ISourceUnit {
	
	
	private final String id;
	private final IElementName name;
	
	private final IFileStore fileStore;
	private IWorkingBuffer buffer;
	
	private int counter= 0;
	
	
	public GenericUriSourceUnit(final String id, final IFileStore fileStore) {
		if (fileStore == null) {
			throw new NullPointerException("fileStore"); //$NON-NLS-1$
		}
		this.id= id;
		this.name= new IElementName() {
			@Override
			public int getType() {
				return 0x011; // see RElementName
			}
			@Override
			public String getDisplayName() {
				return GenericUriSourceUnit.this.fileStore.toString();
			}
			@Override
			public String getSegmentName() {
				return GenericUriSourceUnit.this.id;
			}
			@Override
			public IElementName getNextSegment() {
				return null;
			}
		};
		this.fileStore= fileStore;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ISourceUnit getUnderlyingUnit() {
		return null;
	}
	
	@Override
	public boolean isSynchronized() {
		return this.buffer.isSynchronized();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return this.id;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually of the type
	 * {@link IModelElement#C2_SOURCE_FILE C2_SOURCE_FILE}.
	 */
	@Override
	public int getElementType() {
		return IModelElement.C2_SOURCE_FILE;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IElementName getElementName() {
		return this.name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exists() {
		return this.counter > 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isReadOnly() {
		return false;
	}
	
	@Override
	public boolean checkState(final boolean validate, final IProgressMonitor monitor) {
		return this.buffer.checkState(validate, monitor);
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually doesn't have a resource/path.
	 */
	@Override
	public Object getResource() {
		return this.fileStore;
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
	public Object getAdapter(final Class required) {
		if (IFileStore.class.equals(required)) {
			return this.fileStore;
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
		return null;
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
			final SubMonitor progress= SubMonitor.convert(monitor, 1);
			if (this.buffer == null) {
				progress.setWorkRemaining(2);
				this.buffer= createWorkingBuffer(progress.newChild(1));
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
	
	protected abstract IWorkingBuffer createWorkingBuffer(SubMonitor progress);
	
	protected void register() {
	}
	
	protected void unregister() {
	}
	
	
	@Override
	public String toString() {
		return getModelTypeId() + '/' + getWorkingContext() + ": " + getId(); //$NON-NLS-1$
	}
	
}
