/*=============================================================================#
 # Copyright (c) 2015-2016 Stephan Wahlbrink (WalWare.de) and others.1
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.buildpaths.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.walware.ecommons.ui.util.ImageDescriptorRegistry;
import de.walware.ecommons.ui.util.ImageRegistryUtil;

import de.walware.ecommons.ltk.buildpaths.ui.BuildpathsUIResources;


public class BuildpathsUIPlugin extends AbstractUIPlugin {
	
	
	public static final String PLUGIN_ID= "de.walware.ecommons.ltk.buildpaths.ui"; //$NON-NLS-1$
	
	
	public static final String OVR_IGNORE_OPTIONAL_PROBLEMS_IMAGE_ID= PLUGIN_ID + "/image/ovr/ignore_optional_problems"; //$NON-NLS-1$
	
	
	/** The shared instance */
	private static BuildpathsUIPlugin instance;
	
	/**
	 * Returns the shared plug-in instance
	 *
	 * @return the shared instance
	 */
	public static BuildpathsUIPlugin getInstance() {
		return instance;
	}
	
	public static final void log(final IStatus status) {
		final Plugin plugin= getInstance();
		if (plugin != null) {
			plugin.getLog().log(status);
		}
	}
	
	
	private boolean started;
	
	private ImageDescriptorRegistry imageDescriptorRegistry;
	
	
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		instance= this;
		
		synchronized (this) {
			this.started= true;
		}
	}
	
	@Override
	public void stop(final BundleContext context) throws Exception {
		try {
			synchronized (this) {
				this.started= false;
			}
			
			if (this.imageDescriptorRegistry != null) {
				this.imageDescriptorRegistry.dispose();
				this.imageDescriptorRegistry= null;
			}
		}
		finally {
			instance= null;
			super.stop(context);
		}
	}
	
	
	private void checkStarted() {
		if (!this.started) {
			throw new IllegalStateException("Plug-in is not started.");
		}
	}
	
	@Override
	protected void initializeImageRegistry(final ImageRegistry reg) {
		checkStarted();
		
		final ImageRegistryUtil util= new ImageRegistryUtil(this);
		
		util.register(BuildpathsUIResources.OBJ_SOURCE_ATTACHMENT_ATTRIBUTE_IMAGE_ID,
				ImageRegistryUtil.T_OBJ, "source_attachment.png" ); //$NON-NLS-1$
		util.register(BuildpathsUIResources.OBJ_INCLUSION_FILTER_ATTRIBUTE_IMAGE_ID,
				ImageRegistryUtil.T_OBJ, "inclusion_filter.png" ); //$NON-NLS-1$
		util.register(BuildpathsUIResources.OBJ_EXCLUSION_FILTER_ATTRIBUTE_IMAGE_ID,
				ImageRegistryUtil.T_OBJ, "exclusion_filter.png" ); //$NON-NLS-1$
		util.register(BuildpathsUIResources.OBJ_OUTPUT_FOLDER_ATTRIBUTE_IMAGE_ID,
				ImageRegistryUtil.T_OBJ, "output_folder.png" ); //$NON-NLS-1$
		
		util.register(OVR_IGNORE_OPTIONAL_PROBLEMS_IMAGE_ID,
				ImageRegistryUtil.T_OVR, "ignore_optional_problems.png" ); //$NON-NLS-1$
	}
	
	public ImageDescriptorRegistry getImageDescriptorRegistry() {
		checkStarted();
		
		if (this.imageDescriptorRegistry == null) {
			this.imageDescriptorRegistry= new ImageDescriptorRegistry();
		}
		return this.imageDescriptorRegistry;
	}
	
}
