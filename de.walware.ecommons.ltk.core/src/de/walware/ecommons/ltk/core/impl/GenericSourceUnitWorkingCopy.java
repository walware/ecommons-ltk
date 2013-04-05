/*******************************************************************************
 * Copyright (c) 2007-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.core.impl;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.SourceContent;


/**
 * Generic source unit for working copies based on the same unit in the underlying context
 */
public abstract class GenericSourceUnitWorkingCopy implements ISourceUnit {
	
	
	protected final ISourceUnit fFrom;
	private IWorkingBuffer fBuffer;
	
	private int fCounter = 0;
	
	
	/**
	 * Creates new working copy of the source unit
	 * 
	 * @param from the underlying unit to create a working copy from
	 */
	public GenericSourceUnitWorkingCopy(final ISourceUnit from) {
		fFrom = from;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ISourceUnit getUnderlyingUnit() {
		return fFrom;
	}
	
	@Override
	public boolean isSynchronized() {
		return fBuffer.isSynchronized();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getModelTypeId() {
		return fFrom.getModelTypeId();
	}
	
	@Override
	public String getContentTypeId() {
		return fFrom.getContentTypeId();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getElementType() {
		return fFrom.getElementType();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IElementName getElementName() {
		return fFrom.getElementName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return fFrom.getId();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exists() {
		return fCounter > 0;
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
		return fBuffer.checkState(validate, monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getResource() {
		return fFrom.getResource();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractDocument getDocument(final IProgressMonitor monitor) {
		return fBuffer.getDocument(monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getContentStamp(final IProgressMonitor monitor) {
		return fBuffer.getContentStamp(monitor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public SourceContent getContent(final IProgressMonitor monitor) {
		return fBuffer.getContent(monitor);
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
		return NO_CHILDREN; 
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized final void connect(final IProgressMonitor monitor) {
		fCounter++;
		if (fCounter == 1) {
			final SubMonitor progress = SubMonitor.convert(monitor, 1);
			if (fBuffer == null) {
				progress.setWorkRemaining(2);
				fBuffer = createWorkingBuffer(progress.newChild(1));
			}
			register();
			fFrom.connect(progress.newChild(1));
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized final void disconnect(final IProgressMonitor monitor) {
		fCounter--;
		if (fCounter == 0) {
			final SubMonitor progress = SubMonitor.convert(monitor, 2);
			fBuffer.releaseDocument(progress.newChild(1));
			unregister();
			fFrom.disconnect(progress.newChild(1));
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean isConnected() {
		return (fCounter > 0);
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
		return fFrom.getAdapter(required);
	}
	
}
