/*******************************************************************************
 * Copyright (c) 2009-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.walware.ecommons.FastList;

import de.walware.ecommons.ltk.IElementChangedListener;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.WorkingContext;


public abstract class AbstractModelManager implements IModelManager {
	
	
	protected static class ContextItem {
		
		public final WorkingContext context;
		
		public final FastList<IElementChangedListener> listeners = new FastList<IElementChangedListener>(IElementChangedListener.class);
		
		
		protected ContextItem(final WorkingContext context) {
			this.context = context;
		}
		
		
		@Override
		public final int hashCode() {
			return this.context.hashCode();
		}
		
		@Override
		public final boolean equals(final Object obj) {
			return (this == obj);
		}
		
	}
	
	private static final IElementChangedListener[] NO_LISTENERS = new IElementChangedListener[0];
	
	private class RefreshJob extends Job {
		
		
		private final List<ISourceUnit> fList;
		
		
		public RefreshJob(final WorkingContext context) {
			super("Model Refresh"); //$NON-NLS-1$
			setUser(false);
			setSystem(true);
			setPriority(DECORATE);
			
			fList = LTK.getSourceUnitManager().getOpenSourceUnits(fTypeId, context);
		}
		
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			for (final ISourceUnit su : fList) {
				reconcile(su, (IModelManager.MODEL_FILE | IModelManager.RECONCILER), monitor);
			}
			return Status.OK_STATUS;
		}
		
	}
	
	
	
	private final String fTypeId;
	
	private final FastList<ContextItem> fContexts = new FastList<ContextItem>(ContextItem.class, FastList.IDENTITY);
	
	
	public AbstractModelManager(final String typeId) {
		fTypeId = typeId;
	}
	
	
	public String getModelTypeId() {
		return fTypeId;
	}
	
	protected ContextItem getContextItem(final WorkingContext context, final boolean create) {
		while (true) {
			final ContextItem[] contextItems = fContexts.toArray();
			for (int i = 0; i < contextItems.length; i++) {
				if (contextItems[i].context == context) {
					return contextItems[i];
				}
			}
			if (!create) {
				return null;
			}
			synchronized (fContexts) {
				if (contextItems == fContexts.toArray()) {
					final ContextItem item = doCreateContextItem(context);
					fContexts.add(item);
					return item;
				}
			}
		}
	}
	
	protected ContextItem doCreateContextItem(final WorkingContext context) {
		return new ContextItem(context);
	}
	
	protected ContextItem getExistingContext(final WorkingContext context) {
		final ContextItem[] contextItems = fContexts.toArray();
		for (int i = 0; i < contextItems.length; i++) {
			if (contextItems[i].context == context) {
				return contextItems[i];
			}
		}
		return null;
	}
	
	@Override
	public void addElementChangedListener(final IElementChangedListener listener, final WorkingContext context) {
		final ContextItem contextItem = getContextItem(context, true);
		contextItem.listeners.add(listener);
	}
	
	@Override
	public void removeElementChangedListener(final IElementChangedListener listener, final WorkingContext context) {
		final ContextItem contextItem = getContextItem(context, false);
		if (contextItem != null) {
			contextItem.listeners.remove(listener);
		}
	}
	
	protected IElementChangedListener[] getElementChangedListeners(final WorkingContext context) {
		final ContextItem contextItem = getExistingContext(context);
		if (context == null) {
			return NO_LISTENERS;
		}
		return contextItem.listeners.toArray();
	}
	
	/**
	 * Refresh reuses existing ast
	 */
	@Override
	public void refresh(final WorkingContext context) {
		new RefreshJob(context).schedule();
	}
	
	@Override
	public void registerDependentUnit(final ISourceUnit su) {
	}
	
	@Override
	public void deregisterDependentUnit(final ISourceUnit su) {
	}
	
	
	protected abstract void reconcile(ISourceUnit su, int level, IProgressMonitor monitor);
	
}
