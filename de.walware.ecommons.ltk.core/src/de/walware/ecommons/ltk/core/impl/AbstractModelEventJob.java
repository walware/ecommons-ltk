/*******************************************************************************
 * Copyright (c) 2008-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.core.impl;

import java.util.HashMap;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.walware.ecommons.ltk.ElementChangedEvent;
import de.walware.ecommons.ltk.IElementChangedListener;
import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.IModelElementDelta;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.LTKUtil;
import de.walware.ecommons.ltk.WorkingContext;
import de.walware.ecommons.ltk.internal.core.LTKCorePlugin;


/**
 * Abstract model update event job
 */
public abstract class AbstractModelEventJob<ElementType extends IModelElement, InfoType extends ISourceUnitModelInfo> extends Job {
	
	
	private static class SafeRunnable implements ISafeRunnable {
		
		final ElementChangedEvent fEvent;
		IElementChangedListener fListener;
		
		public SafeRunnable(final ElementChangedEvent event) {
			fEvent = event;
		}
		
		@Override
		public void run() {
			fListener.elementChanged(fEvent);
		}
		
		@Override
		public void handleException(final Throwable exception) {
			LTKCorePlugin.getSafe().log(new Status(IStatus.ERROR, LTK.PLUGIN_ID, -1,
					"An error occured while notifying an ElementChangedListener.", exception));  //$NON-NLS-1$
		}
		
	}
	
	protected class Task {
		
		private final ElementType fElement;
		private InfoType fOldInfo;
		private InfoType fNewInfo;
		
		
		public Task(final ElementType element) {
			fElement = element;
		}
		
		
		public ElementType getElement() {
			return fElement;
		}
		
		public InfoType getOldInfo() {
			return fOldInfo;
		}
		
		public InfoType getNewInfo() {
			return fNewInfo;
		}
		
		
		void run() {
			final IModelElementDelta delta = createDelta(this);
			fireDelta(delta);
		}
	}
	
	
	private final AbstractModelManager fModelManager;
	
	private final Object fTasksLock = new Object();
	private final LinkedList<IModelElement> fTaskQueue = new LinkedList<IModelElement>();
	private final HashMap<IModelElement, Task> fTaskDetail = new HashMap<IModelElement, Task>();
	
	private boolean fWorking = false;
	private boolean fStop = false;
	
	
	public AbstractModelEventJob(final AbstractModelManager manager) {
		super("Model Events for " + manager.getModelTypeId()); //$NON-NLS-1$
		setPriority(BUILD);
		setSystem(true);
		setUser(false);
		
		fModelManager = manager;
	}
	
	
	public void addUpdate(final ElementType element,
			final InfoType oldModel, final InfoType newModel) {
		synchronized (fTasksLock) {
			Task task = fTaskDetail.get(element);
			if (task == null) {
				task = new Task(element);
				task.fOldInfo = oldModel;
				fTaskDetail.put(element, task);
			}
			else {
				fTaskQueue.remove(element);
			}
			task.fNewInfo = newModel;
			fTaskQueue.add(element);
			
			if (!fWorking) {
				schedule();
			}
		}
	}
	
	protected abstract IModelElementDelta createDelta(Task task);
	
	
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		while (true) {
			Task task;
			synchronized (fTasksLock) {
				final IModelElement element = (!fTaskQueue.isEmpty()) ? fTaskQueue.removeFirst() : null;
				if (element == null || fStop) {
					fWorking = false;
					return Status.OK_STATUS;
				}
				fWorking = true;
				task = fTaskDetail.remove(element);
			}
			try {
				task.run();
			}
			catch (final Throwable e) {
				LTKCorePlugin.getSafe().log(new Status(IStatus.ERROR, LTK.PLUGIN_ID, -1,
						"An error occurred when firing model event for " + fModelManager.getModelTypeId() + ".", e )); //$NON-NLS-1$
			}
		}
	}
	
	protected void dispose() {
		synchronized (fTasksLock) {
			fStop = true;
			fTaskQueue.clear();
			fTaskDetail.clear();
		}
	}
	
	private void fireDelta(final IModelElementDelta delta) {
		final ISourceUnit su = LTKUtil.getSourceUnit(delta.getModelElement());
		if (su == null) {
			return;
		}
		final WorkingContext context = su.getWorkingContext();
		final ElementChangedEvent event = new ElementChangedEvent(delta, context);
		final SafeRunnable runnable = new SafeRunnable(event);
		final IElementChangedListener[] listeners = fModelManager.getElementChangedListeners(context);
		for (int i = 0; i < listeners.length; i++) {
			runnable.fListener = listeners[i];
			SafeRunner.run(runnable);
		}
	}
	
}
