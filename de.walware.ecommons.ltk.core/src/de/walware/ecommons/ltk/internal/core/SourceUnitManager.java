/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ICommonStatusConstants;
import de.walware.ecommons.IDisposable;

import de.walware.ecommons.ltk.IExtContentTypeManager;
import de.walware.ecommons.ltk.ISourceUnitFactory;
import de.walware.ecommons.ltk.ISourceUnitManager;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.WorkingContext;
import de.walware.ecommons.ltk.core.IModelTypeDescriptor;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;


public class SourceUnitManager implements ISourceUnitManager, IDisposable {
	
	
	private static final String CONFIG_MODELTYPE_ID_ATTRIBUTE_NAME= "modelTypeId"; //$NON-NLS-1$
	private static final String CONFIG_CONTEXT_KEY_ATTRIBUTE_NAME= "contextKey"; //$NON-NLS-1$
	
	
	private static final class SuItem extends SoftReference<ISourceUnit> {
		
		private final String key;
		
		public SuItem(final String key, final ISourceUnit su, final ReferenceQueue<ISourceUnit> queue) {
			super(su);
			this.key= key;
		}
		
		public String getKey() {
			return this.key;
		}
		
		public void dispose() {
			final ISourceUnit su= get();
			if (su != null && su.isConnected()) {
				LTKCorePlugin.log(
						new Status(IStatus.WARNING, LTKCorePlugin.PLUGIN_ID, -1,
								NLS.bind("Source Unit ''{0}'' disposed but connected.", su.getId()), null));
			}
			clear();
		}
		
	}
	
	private static class ContextItem {
		
		private final WorkingContext context;
		private final ISourceUnitFactory factory;
		private final HashMap<String, SuItem> sus;
		private final ReferenceQueue<ISourceUnit> susToClean;
		
		public ContextItem(final WorkingContext context, final ISourceUnitFactory factory) {
			this.context= context;
			this.factory= factory;
			this.sus= new HashMap<>();
			this.susToClean= new ReferenceQueue<>();
		}
		
		@Override
		public int hashCode() {
			return this.context.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof ContextItem) {
				return ( ((ContextItem) obj).context == this.context);
			}
			return false;
		}
		
		
		public synchronized ISourceUnit getOpenSu(final Object from) {
			final String id= this.factory.createId(from);
			if (id != null) {
				final SuItem suItem= this.sus.get(id);
				if (suItem != null) {
					final ISourceUnit su= suItem.get();
					if (su != null && !suItem.isEnqueued()) {
						return su;
					}
				}
			}
			return null;
		}
		
		public synchronized void appendOpenSus(final ArrayList<ISourceUnit> list) {
			final Collection<SuItem> suItems= this.sus.values();
			list.ensureCapacity(list.size() + suItems.size());
			for (final SuItem suItem : suItems) {
				final ISourceUnit su= suItem.get();
				if (su != null && !suItem.isEnqueued()) {
					list.add(su);
				}
			}
		}
		
	}
	
	private static class ModelItem {
		
		
		private final String modelTypeId;
		
		private volatile ImList<ContextItem> contextItems= ImCollections.newList();
		
		
		public ModelItem(final String modelTypeId) {
			this.modelTypeId= modelTypeId;
		}
		
		public ContextItem getContextItem(final WorkingContext context, final boolean create) {
			final ImList<ContextItem> contextItems= this.contextItems;
			for (final ContextItem contextItem : contextItems) {
				if (contextItem.context == context) {
					return contextItem;
				}
			}
			if (create) {
				synchronized (this) {
					if (contextItems != this.contextItems) {
						return getContextItem(context, true);
					}
					try {
						final IConfigurationElement[] elements= Platform.getExtensionRegistry().
								getConfigurationElementsFor("de.walware.ecommons.ltk.modelTypes"); //$NON-NLS-1$
						IConfigurationElement matchingElement= null;
						for (final IConfigurationElement element : elements) {
							if (element.getName().equals("unitType") && element.isValid()) { //$NON-NLS-1$
								final String typeIdOfElement= element.getAttribute(CONFIG_MODELTYPE_ID_ATTRIBUTE_NAME);
								final String contextKeyOfElement= element.getAttribute(CONFIG_CONTEXT_KEY_ATTRIBUTE_NAME);
								if (this.modelTypeId.equals(typeIdOfElement)) {
									if ((contextKeyOfElement == null) || (contextKeyOfElement.length() == 0)) {
										matchingElement= element;
										continue;
									}
									if (contextKeyOfElement.equals(context.getKey())) {
										matchingElement= element;
										break;
									}
								}
							}
						}
						if (matchingElement != null) {
							final ISourceUnitFactory factory= (ISourceUnitFactory) matchingElement.createExecutableExtension("unitFactory"); //$NON-NLS-1$
							final ContextItem contextItem= new ContextItem(context, factory);
							this.contextItems= ImCollections.addElement(contextItems, contextItem);
							return contextItem;
						}
					}
					catch (final Exception e) {
						LTKCorePlugin.log(new Status(IStatus.ERROR, LTK.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
								"Error loading working context contributions", e )); //$NON-NLS-1$
					}
				}
			}
			return null;
		}
		
		public ImList<ContextItem> getOpenContextItems(final WorkingContext context) {
			final ImList<ContextItem> contextItems= this.contextItems;
			if (context != null) {
				for (final ContextItem contextItem : contextItems) {
					if (contextItem.context == context) {
						return ImCollections.newList(contextItem);
					}
				}
				return ImCollections.emptyList();
			}
			else {
				return contextItems;
			}
		}
		
		@Override
		public int hashCode() {
			return this.modelTypeId.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			return (obj instanceof ModelItem
					&& this.modelTypeId.equals(((ModelItem) obj).modelTypeId));
		}
		
	}
	
	private class CleanupJob extends Job {
		
		private final Object scheduleLock= new Object();
		
		public CleanupJob() {
			super("SourceUnit Cleanup"); //$NON-NLS-1$
			setUser(false);
			setSystem(true);
			setPriority(DECORATE);
		}
		
		void initialSchedule() {
			synchronized (this.scheduleLock) {
				schedule(180000);
			}
		}
		
		void dispose() {
			synchronized (this.scheduleLock) {
				cancel();
			}
		}
		
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			final int count= performCleanup();
			
			synchronized (this.scheduleLock) {
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
	
	
	private final CleanupJob cleanupJob= new CleanupJob();
	
	private volatile ImList<ModelItem> modelItems= ImCollections.newList();
	
	private final IExtContentTypeManager contentManager= LTK.getExtContentTypeManager();
	
	
	public SourceUnitManager() {
		this.cleanupJob.initialSchedule();
	}
	
	
	private int performCleanup() {
		int count= 0;
		final ImList<ModelItem> modelItems= this.modelItems;
		for (final ModelItem modelItem : modelItems) {
			final List<ContextItem> contextItems= modelItem.contextItems;
			for (final ContextItem contextItem : contextItems) {
				SuItem suItem;
				while ((suItem= (SuItem) contextItem.susToClean.poll()) != null){
					synchronized (contextItem.sus) {
						if (contextItem.sus.get(suItem.getKey()) == suItem) {
							contextItem.sus.remove(suItem.getKey());
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
		this.cleanupJob.dispose();
	}
	
	@Override
	public ISourceUnit getSourceUnit(final String modelTypeId, final WorkingContext context,
			final Object from, final boolean create,
			final IProgressMonitor monitor) {
		if (modelTypeId == null) {
			throw new NullPointerException("modelTypeId"); //$NON-NLS-1$
		}
		if (context == null) {
			throw new NullPointerException("context"); //$NON-NLS-1$
		}
		
		return doGetSourceUnit(modelTypeId, context, from, null, create, monitor);
	}
	
	@Override
	public ISourceUnit getSourceUnit(final WorkingContext context,
			final Object from, IContentType contentType, final boolean create,
			final IProgressMonitor monitor) {
		if (context == null) {
			throw new NullPointerException("context"); //$NON-NLS-1$
		}
		
		String modelTypeId;
		if (from instanceof ISourceUnit) {
			modelTypeId= ((ISourceUnit) from).getModelTypeId();
		}
		else {
			if (contentType == null) {
				contentType= detectContentType(from);
				if (contentType == null) {
					return null;
				}
			}
			final IModelTypeDescriptor modelType= this.contentManager.getModelTypeForContentType(contentType.getId());
			if (modelType == null) {
				return null;
			}
			modelTypeId= modelType.getId();
		}
		
		return doGetSourceUnit(modelTypeId, context, from, contentType, create, monitor);
	}
	
	private ISourceUnit doGetSourceUnit(final String modelTypeId, final WorkingContext context,
			final Object from, final IContentType contentType, final boolean create,
			final IProgressMonitor monitor) {
		final ISourceUnit fromUnit= (from instanceof ISourceUnit) ? ((ISourceUnit) from) : null;
		
		final ModelItem modelItem= getModelItem(modelTypeId);
		final ContextItem contextItem= modelItem.getContextItem(context, create);
		ISourceUnit su= null;
		if (contextItem != null) {
			final String id= (fromUnit != null) ? fromUnit.getId() : contextItem.factory.createId(from);
			if (id != null) {
				synchronized (contextItem) {
					SuItem suItem= contextItem.sus.get(id);
					if (suItem != null) {
						su= suItem.get();
						if (suItem.isEnqueued()) {
							su= null;
						}
					}
					else {
						if (create) {
							su= contextItem.factory.createSourceUnit(id, from);
							if (su == null || !su.getModelTypeId().equals(modelItem.modelTypeId)
									|| (su.getElementType() & IModelElement.MASK_C1) != IModelElement.C1_SOURCE) {
								// TODO log
								return null; 
							}
							suItem= new SuItem(id, su, contextItem.susToClean);
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
		
		if (su != null) {
			su.connect(monitor);
			
			if (fromUnit != null) {
				fromUnit.disconnect(null);
			}
			
			return su;
		}
		else {
			return null;
		}
	}
	
	@Override
	public List<ISourceUnit> getOpenSourceUnits(final String modelTypeId,
			final WorkingContext context) {
		final List<ModelItem> modelItems= getOpenModelItems(modelTypeId);
		if (modelItems.isEmpty()) {
			return Collections.emptyList();
		}
		
		final ArrayList<ISourceUnit> list= new ArrayList<>();
		
		for (int i= 0; i < modelItems.size(); i++) {
			final ImList<ContextItem> contextItems= modelItems.get(i).getOpenContextItems(context);
			if (contextItems.isEmpty()) {
				continue;
			}
			
			for (final ContextItem contextItem : contextItems) {
				contextItem.appendOpenSus(list);
			}
		}
		return list;
	}
	
	@Override
	public List<ISourceUnit> getOpenSourceUnits(final List<String> modelTypeIds,
			final WorkingContext context) {
		final List<ModelItem> includedModelItems= getOpenModelItems(modelTypeIds);
		if (includedModelItems.isEmpty()) {
			return Collections.emptyList();
		}
		
		final ArrayList<ISourceUnit> list= new ArrayList<>(4);
		
		for (int i= 0; i < includedModelItems.size(); i++) {
			final ImList<ContextItem> contextItems= this.modelItems.get(i).getOpenContextItems(context);
			if (contextItems.isEmpty()) {
				continue;
			}
			
			for (final ContextItem contextItem : contextItems) {
				contextItem.appendOpenSus(list);
			}
		}
		return list;
	}
	
	@Override
	public List<ISourceUnit> getOpenSourceUnits(final List<String> modelTypeIds,
			final WorkingContext context, final Object from) {
		final List<ModelItem> includedModelItems= getOpenModelItems(modelTypeIds);
		if (includedModelItems.isEmpty()) {
			return Collections.emptyList();
		}
		
		final ArrayList<ISourceUnit> list= new ArrayList<>(4);
		
		for (int i= 0; i < includedModelItems.size(); i++) {
			final ImList<ContextItem> contextItems= this.modelItems.get(i).getOpenContextItems(context);
			if (contextItems.isEmpty()) {
				continue;
			}
			
			for (final ContextItem contextItem : contextItems) {
				final ISourceUnit su= contextItem.getOpenSu(from);
				if (su != null) {
					list.add(su);
				}
			}
		}
		return list;
	}
	
	
	private IContentType detectContentType(final Object from) {
		try {
			if (from instanceof IFile) {
				final IFile file= (IFile) from;
				final IContentDescription contentDescription= file.getContentDescription();
				if (contentDescription != null) {
					return contentDescription.getContentType();
				}
				else {
					return null;
				}
			}
			else if (from instanceof IFileStore) {
				final IFileStore file= (IFileStore) from;
				try (final InputStream stream= file.openInputStream(EFS.NONE, null)) {
					final IContentDescription contentDescription= Platform.getContentTypeManager()
							.getDescriptionFor(stream, file.getName(), IContentDescription.ALL);
					if (contentDescription != null) {
						return contentDescription.getContentType();
					}
					else {
						return null;
					}
				}
			}
			else {
				return null;
			}
		}
		catch (final CoreException | IOException | UnsupportedOperationException e) {
			LTKCorePlugin.log(new Status(IStatus.ERROR, LTK.PLUGIN_ID, 0,
					"An error occurred when trying to detect content type of " + from,
					e ));
			return null;
		}
	}
	
	
	
	
	private ModelItem getModelItem(final String modelTypeId) {
		final ImList<ModelItem> modelItems= this.modelItems;
		for (final ModelItem modelItem : modelItems) {
			if (modelItem.modelTypeId == modelTypeId) {
				return modelItem;
			}
		}
		synchronized (this) {
			if (modelItems != this.modelItems) {
				return getModelItem(modelTypeId);
			}
			final ModelItem modelItem= new ModelItem(modelTypeId);
			this.modelItems= ImCollections.addElement(modelItems, modelItem);
			return modelItem;
		}
	}
	
	private List<ModelItem> getOpenModelItems(final String modelTypeId) {
		final ImList<ModelItem> modelItems= this.modelItems;
		if (modelTypeId != null) {
			for (final ModelItem modelItem : modelItems) {
				if (modelItem.modelTypeId == modelTypeId) {
					return ImCollections.newList(modelItem);
				}
			}
			return ImCollections.emptyList();
		}
		else {
			return modelItems;
		}
	}
	
	private List<ModelItem> getOpenModelItems(final List<String> modelTypeIds) {
		final ImList<ModelItem> modelItems= this.modelItems;
		if (modelTypeIds != null) {
			final List<ModelItem> matches= new ArrayList<>(modelTypeIds.size());
			for (final ModelItem modelItem : modelItems) {
				if (modelTypeIds.contains(modelItem.modelTypeId)) {
					matches.add(modelItem);
				}
			}
			return matches;
		}
		else {
			return modelItems;
		}
	}
	
}
