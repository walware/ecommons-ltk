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

import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;


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
			public int getType() {
				return 0x011; // see RElementName
			}
			public String getDisplayName() {
				return fStore.toString();
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
		fStore = store;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public ISourceUnit getUnderlyingUnit() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getId() {
		return fId;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually of the type
	 * {@link IModelElement#C2_SOURCE_FILE C2_SOURCE_FILE}.
	 */
	public int getElementType() {
		return IModelElement.C2_SOURCE_FILE;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IElementName getElementName() {
		return fName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean exists() {
		return fCounter > 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isReadOnly() {
		return false;
	}
	
	public boolean checkState(final boolean validate, final IProgressMonitor monitor) {
		return fBuffer.checkState(validate, monitor);
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * A source unit of this type is usually doesn't have a resource/path.
	 */
	public Object getResource() {
		return fStore;
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
	public Object getAdapter(final Class required) {
		if (IFileStore.class.equals(required)) {
			return fStore;
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
		return null;
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
	
	protected abstract IWorkingBuffer createWorkingBuffer(SubMonitor progress);
	
	protected void register() {
	}
	
	protected void unregister() {
	}
	
}
