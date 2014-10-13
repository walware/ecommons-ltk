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

package de.walware.ecommons.ltk.internal.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import de.walware.ecommons.IDisposable;

import de.walware.ecommons.ltk.IExtContentTypeManager;
import de.walware.ecommons.ltk.ISourceUnitManager;


/**
 * The activator class controls the plug-in life cycle
 */
public final class LTKCorePlugin extends Plugin {
	
	
	public static final String PLUGIN_ID= "de.walware.ecommons.ltk.core"; //$NON-NLS-1$
	
	
	/** The shared instance. */
	private static LTKCorePlugin instance;
	private static LTKCorePlugin gSafe;
	
	/**
	 * Returns the shared plug-in instance
	 * 
	 * @return the shared instance
	 */
	public static LTKCorePlugin getSafe() {
		return gSafe;
	}
	
	/**
	 * Returns the shared plug-in instance
	 * 
	 * @return the shared instance
	 */
	public static LTKCorePlugin getInstance() {
		return instance;
	}
	
	public static void log(final IStatus status) {
		final LTKCorePlugin plugin= getInstance();
		if (plugin != null) {
			plugin.getLog().log(status);
		}
	}
	
	
	private boolean fStarted;
	
	private final List<IDisposable> fDisposables = new ArrayList<>();
	
	private ExtContentTypeServices fContentTypeServices;
	private SourceUnitManager fSourceUnitManager;
	
	private AdapterFactory fModelAdapterFactory;
	
	
	/**
	 * The default constructor
	 */
	public LTKCorePlugin() {
		instance= this;
		gSafe= this;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		
		synchronized (this) {
			fStarted = true;
			
			fContentTypeServices = new ExtContentTypeServices();
			addStoppingListener(fContentTypeServices);
			fSourceUnitManager = new SourceUnitManager();
			addStoppingListener(fSourceUnitManager);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		try {
			synchronized (this) {
				fStarted = false;
				
				fContentTypeServices = null;
				fSourceUnitManager = null;
			}
			
			try {
				for (final IDisposable listener : fDisposables) {
					listener.dispose();
				}
			}
			finally {
				fDisposables.clear();
			}
		}
		finally {
			instance= null;
			super.stop(context);
		}
	}
	
	
	public void addStoppingListener(final IDisposable listener) {
		if (listener == null) {
			throw new NullPointerException();
		}
		synchronized (this) {
			if (!fStarted) {
				throw new IllegalStateException("Plug-in is not started.");
			}
			fDisposables.add(listener);
		}
	}
	
	public IExtContentTypeManager getContentTypeServices() {
		return fContentTypeServices;
	}
	
	public ISourceUnitManager getSourceUnitManager() {
		return fSourceUnitManager;
	}
	
	public synchronized AdapterFactory getModelAdapterFactory() {
		if (fModelAdapterFactory == null) {
			if (!fStarted) {
				throw new IllegalStateException("Plug-in is not started.");
			}
			fModelAdapterFactory = new AdapterFactory("de.walware.ecommons.ltk.modelAdapters"); //$NON-NLS-1$
		}
		return fModelAdapterFactory;
	}
	
	
//	public synchronized SshSessionManager getSshSessionManager() {
//		if (fSshSessions == null) {
//			if (!fStarted) {
//				throw new IllegalStateException("Plug-in is not started.");
//			}
//			fSshSessions = new SshSessionManager();
//			addStoppingListener(fSshSessions);
//		}
//		return fSshSessions;
//	}
	
}
