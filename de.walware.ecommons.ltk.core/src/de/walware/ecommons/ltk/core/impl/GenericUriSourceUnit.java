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

import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
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
 * Generic source unit for external files (URI/EFS).
 */
public abstract class GenericUriSourceUnit implements ISourceUnit {
	
	
	private final String fId;
	private final IElementName fName;
	
	private final IFileStore fStore;
	private IWorkingBuffer fBuffer;
	
	private int fCounter = 0;
	
	
	public GenericUriSourceUnit(final String id, final IFileStore store) {
		fId = id;
		fName = new IElementName() {
			@Override
			public int getType() {
				return 0x011; // see RElementName
			}
			@Override
			public String getDisplayName() {
				return fStore.toString();
			}
			@Override
			public String getSegmentName() {
				return fId;
			}
			@Override
			public IElementName getNextSegment() {
				return null;
			}
		};
		fStore = store;
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
		return fBuffer.isSynchronized();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return fId;
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
		return fName;
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
	 * 
	 * A source unit of this type is usually doesn't have a resource/path.
	 */
	@Override
	public Object getResource() {
		return fStore;
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
	public Object getAdapter(final Class required) {
		if (IFileStore.class.equals(required)) {
			return fStore;
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
	
}
