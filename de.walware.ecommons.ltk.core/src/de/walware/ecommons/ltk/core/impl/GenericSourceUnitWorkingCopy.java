/*=============================================================================#
 # Copyright (c) 2007-2014 Stephan Wahlbrink (WalWare.de) and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.SourceContent;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;


/**
 * Generic source unit for working copies based on the same unit in the underlying context
 */
public abstract class GenericSourceUnitWorkingCopy implements ISourceUnit {
	
	
	private final ISourceUnit from;
	private IWorkingBuffer buffer;
	
	private int counter= 0;
	
	
	/**
	 * Creates new working copy of the source unit
	 * 
	 * @param from the underlying unit to create a working copy from
	 */
	public GenericSourceUnitWorkingCopy(final ISourceUnit from) {
		this.from= from;
	}
	
	
	@Override
	public final ISourceUnit getUnderlyingUnit() {
		return this.from;
	}
	
	protected final IWorkingBuffer getWorkingBuffer() {
		return this.buffer;
	}
	
	@Override
	public boolean isSynchronized() {
		return this.buffer.isSynchronized();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getModelTypeId() {
		return this.from.getModelTypeId();
	}
	
	@Override
	public String getContentTypeId() {
		return this.from.getContentTypeId();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getElementType() {
		return this.from.getElementType();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IElementName getElementName() {
		return this.from.getElementName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return this.from.getId();
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
	 */
	@Override
	public Object getResource() {
		return this.from.getResource();
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
			final SubMonitor progress= SubMonitor.convert(monitor, 1);
			if (this.buffer == null) {
				progress.setWorkRemaining(2);
				this.buffer= createWorkingBuffer(progress.newChild(1));
			}
			register();
			this.from.connect(progress.newChild(1));
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
			this.from.disconnect(progress.newChild(1));
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAdapter(final Class required) {
		return this.from.getAdapter(required);
	}
	
}
