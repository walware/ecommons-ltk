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

package de.walware.ecommons.ltk.internal.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import de.walware.ecommons.FastList;
import de.walware.ecommons.ICommonStatusConstants;
import de.walware.ecommons.IDisposable;

import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitFactory;
import de.walware.ecommons.ltk.ISourceUnitManager;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.WorkingContext;


public class SourceUnitManager implements ISourceUnitManager, IDisposable {
	
	
	private static final String CONFIG_MODELTYPE_ID_ATTRIBUTE_NAME = "modelTypeId"; //$NON-NLS-1$
	private static final String CONFIG_CONTEXT_KEY_ATTRIBUTE_NAME = "contextKey"; //$NON-NLS-1$
	
	
	private static final class SuItem extends SoftReference<ISourceUnit> {
		
		private final String fKey;
		
		public SuItem(final String key, final ISourceUnit su, final ReferenceQueue<ISourceUnit> queue) {
			super(su);
			fKey = key;
		}
		
		public String getKey() {
			return fKey;
		}
		
		public void dispose() {
			final ISourceUnit su = get();
			if (su != null && su.isConnected()) {
				LTKCorePlugin.getSafe().log(
						new Status(IStatus.WARNING, LTKCorePlugin.PLUGIN_ID, -1,
								NLS.bind("Source Unit ''{0}'' disposed but connected.", su.getId()), null));
			}
			clear();
		}
		
	}
	
	private static class ContextItem {
		
		private final WorkingContext fContext;
		private final ISourceUnitFactory fFactory;
		private final HashMap<String, SuItem> fSus;
		private final ReferenceQueue<ISourceUnit> fSusToClean;
		
		public ContextItem(final WorkingContext context, final ISourceUnitFactory factory) {
			fContext = context;
			fFactory = factory;
			fSus = new HashMap<String, SuItem>();
			fSusToClean = new ReferenceQueue<ISourceUnit>();
		}
		
		@Override
		public int hashCode() {
			return fContext.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof ContextItem) {
				return ( ((ContextItem) obj).fContext == fContext);
			}
			return false;
		}
		
	}
	
	private static class ModelItem {
		
		
		private final String fModelTypeId;
		
		private final FastList<ContextItem> fContexts = new FastList<ContextItem>(ContextItem.class, FastList.IDENTITY);
		
		
		public ModelItem(final String modelTypeId) {
			fModelTypeId = modelTypeId;
		}
		
		public ContextItem getContextItem(final WorkingContext context, final boolean create) {
			final ContextItem[] contextItems = fContexts.toArray();
			for (int i = 0; i < contextItems.length; i++) {
				if (contextItems[i].fContext == context) {
					return contextItems[i];
				}
			}
			if (create) {
				synchronized (fContexts) {
					if (contextItems != fContexts.toArray()) {
						return getContextItem(context, true);
					}
					try {
						final IConfigurationElement[] elements = Platform.getExtensionRegistry().
								getConfigurationElementsFor("de.walware.ecommons.ltk.modelTypes"); //$NON-NLS-1$
						IConfigurationElement matchingElement = null;
						for (final IConfigurationElement element : elements) {
							if (element.getName().equals("unitType") && element.isValid()) { //$NON-NLS-1$
								final String typeIdOfElement = element.getAttribute(CONFIG_MODELTYPE_ID_ATTRIBUTE_NAME);
								final String contextKeyOfElement = element.getAttribute(CONFIG_CONTEXT_KEY_ATTRIBUTE_NAME);
								if (fModelTypeId.equals(typeIdOfElement)) {
									if ((contextKeyOfElement == null) || (contextKeyOfElement.length() == 0)) {
										matchingElement = element;
										continue;
									}
									if (contextKeyOfElement.equals(context.getKey())) {
										matchingElement = element;
										break;
									}
								}
							}
						}
						if (matchingElement != null) {
							final ISourceUnitFactory factory = (ISourceUnitFactory) matchingElement.createExecutableExtension("unitFactory"); //$NON-NLS-1$
							final ContextItem contextItem = new ContextItem(context, factory);
							fContexts.add(contextItem);
							return contextItem;
						}
					}
					catch (final Exception e) {
						LTKCorePlugin.getSafe().log(new Status(IStatus.ERROR, LTK.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
								"Error loading working context contributions", e)); //$NON-NLS-1$
					}
				}
			}
			return null;
		}
		
		@Override
		public int hashCode() {
			return fModelTypeId.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			return (obj instanceof ModelItem
					&& fModelTypeId.equals(((ModelItem) obj).fModelTypeId));
		}
		
	}
	
	private class CleanupJob extends Job {
		
		private final Object fScheduleLock = new Object();
		
		public CleanupJob() {
			super("SourceUnit Cleanup"); //$NON-NLS-1$
			setUser(false);
			setSystem(true);
			setPriority(DECORATE);
		}
		
		void initialSchedule() {
			synchronized (fScheduleLock) {
				schedule(180000);
			}
		}
		
		void dispose() {
			synchronized (fScheduleLock) {
				cancel();
			}
		}
		
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			final int count = performCleanup();
			
			synchronized (fScheduleLock) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				else {
					schedule(count > 0 ? 60000 : 180000);
					return Status.OK_STATUS;
				}
			}
		}
		
	}
	
	
	private final CleanupJob fCleanupJob = new CleanupJob();
	
	private final FastList<ModelItem> fModelItems = new FastList<ModelItem>(ModelItem.class, FastList.IDENTITY);
	
	
	public SourceUnitManager() {
		fCleanupJob.initialSchedule();
	}
	
	
	private int performCleanup() {
		int count = 0;
		final ModelItem[] modelItems = fModelItems.toArray();
		for (int i = 0; i < modelItems.length; i++) {
			final ContextItem[] contextItems = modelItems[i].fContexts.toArray();
			for (int j = 0; j < contextItems.length; j++) {
				final ContextItem contextItem = contextItems[j];
				SuItem suItem;
				while ((suItem = (SuItem) contextItem.fSusToClean.poll()) != null){
					synchronized (contextItem.fSus) {
						if (contextItem.fSus.get(suItem.getKey()) == suItem) {
							contextItem.fSus.remove(suItem.getKey());
						}
						suItem.dispose();
						count++;
					}
				}
			}
		}
		return count;
	}
	
	@Override
	public void dispose() {
		fCleanupJob.dispose();
	}
	
	@Override
	public ISourceUnit getSourceUnit(String modelTypeId, final WorkingContext context, final Object from,
			final boolean create, final IProgressMonitor monitor) {
		if (context == null) {
			throw new NullPointerException("Missing working context."); //$NON-NLS-1$
		}
		final ISourceUnit fromUnit = (from instanceof ISourceUnit) ? ((ISourceUnit) from) : null;
		if (modelTypeId == null) {
			if (fromUnit != null) {
				modelTypeId = fromUnit.getModelTypeId();
			}
			else {
				throw new IllegalArgumentException("Missing model type."); //$NON-NLS-1$
			}
		}
		
		final ModelItem modelItem = getModelItem(modelTypeId);
		final ContextItem contextItem = modelItem.getContextItem(context, create);
		ISourceUnit su = null;
		if (contextItem != null) {
			final String id = (fromUnit != null) ? fromUnit.getId() : contextItem.fFactory.createId(from);
			if (id != null) {
				synchronized (contextItem) {
					SuItem suItem = contextItem.fSus.get(id);
					if (suItem != null) {
						su = suItem.get();
						if (suItem.isEnqueued()) {
							su = null;
						}
					}
					else {
						if (create) {
							su = contextItem.fFactory.createSourceUnit(id, (fromUnit != null) ? fromUnit : from);
							if (su == null || !su.getModelTypeId().equals(modelItem.fModelTypeId)
									|| (su.getElementType() & IModelElement.MASK_C1) != IModelElement.C1_SOURCE) {
								// TODO log
								return null; 
							}
							suItem = new SuItem(id, su, contextItem.fSusToClean);
						}
						else {
							return null;
						}
					}
				}
			}
		}
		else {
			if (create) {
				throw new UnsupportedOperationException(NLS.bind(
						"Missing factory for model type ''{0}''.", modelTypeId)); //$NON-NLS-1$
			}
			else {
				return null;
			}
		}
		su.connect(monitor);
		if (fromUnit != null) {
			fromUnit.disconnect(null);
		}
		return su;
	}
	
	@Override
	public List<ISourceUnit> getOpenSourceUnits(final String modelTypeId, final WorkingContext context) {
		ModelItem[] includedModelItems = null;
		final ModelItem[] modelItems = fModelItems.toArray();
		if (modelTypeId != null) {
			for (int i = 0; i < modelItems.length; i++) {
				if (modelItems[i].fModelTypeId == modelTypeId) {
					includedModelItems = new ModelItem[] { modelItems[i] };
					break;
				}
			}
		}
		else {
			includedModelItems = modelItems;
		}
		if (includedModelItems == null || includedModelItems.length == 0) {
			return Collections.emptyList();
		}
		final ArrayList<ISourceUnit> list = new ArrayList<ISourceUnit>();
		for (int i = 0; i < includedModelItems.length; i++) {
			ContextItem[] includedContextItems = null;
			final ContextItem[] contextItems = includedModelItems[i].fContexts.toArray();
			if (context != null) {
				for (int j = 0; j < contextItems.length; j++) {
					if (contextItems[j].fContext == context) {
						includedContextItems = new ContextItem[] { contextItems[j] };
						break;
					}
				}
			}
			else {
				includedContextItems = contextItems;
			}
			if (includedContextItems == null || includedContextItems.length == 0) {
				continue;
			}
			for (int j = 0; j < contextItems.length; j++) {
				final ContextItem contextItem = contextItems[j];
				synchronized (contextItem) {
					final Collection<SuItem> suItems = contextItem.fSus.values();
					list.ensureCapacity(list.size()+suItems.size());
					for (final SuItem suItem : suItems) {
						final ISourceUnit su = suItem.get();
						if (su != null && su.isConnected() && !suItem.isEnqueued()) {
							list.add(su);
						}
					}
				}
			}
		}
		return list;
	}
	
	private ModelItem getModelItem(final String modelTypeId) {
		final ModelItem[] modelItems = fModelItems.toArray();
		for (int i = 0; i < modelItems.length; i++) {
			if (modelItems[i].fModelTypeId == modelTypeId) {
				return modelItems[i];
			}
		}
		synchronized (fModelItems) {
			if (modelItems != fModelItems.toArray()) {
				return getModelItem(modelTypeId);
			}
			final ModelItem modelItem = new ModelItem(modelTypeId);
			fModelItems.add(modelItem);
			return modelItem;
		}
	}
	
}
