/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
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
import org.eclipse.jface.text.ISynchronizable;

import de.walware.ecommons.text.ISourceFragment;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.core.SourceContent;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;


public abstract class GenericFragmentSourceUnit implements ISourceUnit {
	
	
	private final IElementName name;
	
	private final ISourceFragment fragment;
	private final long timestamp;
	
	private AbstractDocument document;
	
	private int counter= 0;
	
	
	public GenericFragmentSourceUnit(final String id, final ISourceFragment fragment) {
		if (fragment == null) {
			throw new NullPointerException("fragment"); //$NON-NLS-1$
		}
		this.fragment = fragment;
		this.name = new IElementName() {
			@Override
			public int getType() {
				return 0x011;
			}
			@Override
			public String getDisplayName() {
				return GenericFragmentSourceUnit.this.fragment.getName();
			}
			@Override
			public String getSegmentName() {
				return GenericFragmentSourceUnit.this.fragment.getName();
			}
			@Override
			public IElementName getNextSegment() {
				return null;
			}
		};
		this.timestamp = System.currentTimeMillis();
	}
	
	
	@Override
	public ISourceUnit getUnderlyingUnit() {
		return null;
	}
	
	@Override
	public boolean isSynchronized() {
		return true;
	}
	
	@Override
	public String getId() {
		return this.fragment.getId();
	}
	
	public ISourceFragment getFragment() {
		return this.fragment;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually of the type
	 * {@link IModelElement#C2_SOURCE_CHUNK C2_SOURCE_CHUNK}.
	 */
	@Override
	public int getElementType() {
		return IModelElement.C2_SOURCE_CHUNK;
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
		return true;
	}
	
	@Override
	public boolean checkState(final boolean validate, final IProgressMonitor monitor) {
		return false;
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually doesn't have a resource/path.
	 */
	@Override
	public Object getResource() {
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized AbstractDocument getDocument(final IProgressMonitor monitor) {
		if (this.document == null) {
			this.document = this.fragment.getDocument();
		}
		return this.document;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getContentStamp(final IProgressMonitor monitor) {
		return this.timestamp;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public SourceContent getContent(final IProgressMonitor monitor) {
		final AbstractDocument document = getDocument(monitor);
		Object lockObject = null;
		if (document instanceof ISynchronizable) {
			lockObject = ((ISynchronizable) document).getLockObject();
		}
		if (lockObject == null) {
			lockObject = this.fragment;
		}
		synchronized (lockObject) {
			return new SourceContent(document.getModificationStamp(), document.get());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAdapter(final Class required) {
		if (ISourceFragment.class.equals(required)) {
			return this.fragment;
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
			final SubMonitor progress = SubMonitor.convert(monitor, 1);
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
	public String toString() {
		return getModelTypeId() + '/' + getWorkingContext() + ": " + getId(); //$NON-NLS-1$
	}
	
}
