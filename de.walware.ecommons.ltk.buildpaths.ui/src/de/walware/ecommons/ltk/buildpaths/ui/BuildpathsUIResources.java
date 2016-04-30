/*=============================================================================#
 # Copyright (c) 2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import de.walware.ecommons.ltk.internal.buildpaths.ui.BuildpathsUIPlugin;


public class BuildpathsUIResources {
	
	
	private static final String NS= "de.walware.ecommons.ltk.buildpath"; //$NON-NLS-1$
	
	
	public static final String OBJ_SOURCE_ATTACHMENT_ATTRIBUTE_IMAGE_ID= NS + "/image/obj/attribute-source_attachment"; //$NON-NLS-1$
	public static final String OBJ_INCLUSION_FILTER_ATTRIBUTE_IMAGE_ID= NS + "/image/obj/attribute-inclusion_filter"; //$NON-NLS-1$
	public static final String OBJ_EXCLUSION_FILTER_ATTRIBUTE_IMAGE_ID= NS + "/image/obj/attribute-exclusion_filter"; //$NON-NLS-1$
	public static final String OBJ_OUTPUT_FOLDER_ATTRIBUTE_IMAGE_ID= NS + "/image/obj/attribute-output_folder"; //$NON-NLS-1$
	
	
	public static final BuildpathsUIResources INSTANCE= new BuildpathsUIResources();
	
	
	private final ImageRegistry registry;
	
	
	private BuildpathsUIResources() {
		this.registry= BuildpathsUIPlugin.getInstance().getImageRegistry();
	}
	
	public ImageDescriptor getImageDescriptor(final String id) {
		return this.registry.getDescriptor(id);
	}
	
	public Image getImage(final String id) {
		return this.registry.get(id);
	}
	
}
