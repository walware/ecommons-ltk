/*=============================================================================#
 # Copyright (c) 2000-2013 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.buildpaths.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.buildpaths.ui.BuildpathsUIResources;


public class BuildpathElementImageDescriptor extends CompositeImageDescriptor {
	
	
	/** Flag to render the info adornment. */
	public final static int INFO=                           0b0_0000_0000_0001;
	
	/** Flag to render the warning adornment. */
	public final static int WARNING=                        0b0_0000_0000_0010;
	
	/** Flag to render the error adornment. */
	public final static int ERROR=                          0b0_0000_0000_0100;
	
	
	/**
	 * Flag to render the 'deprecated' adornment.
	 */
	public final static int DEPRECATED=                     0b0_0000_0001_0000;
	
	/**
	 * Flag to render the 'ignore optional compile problems' adornment.
	 */
	public final static int IGNORE_OPTIONAL_PROBLEMS=       0b0_0001_0000_0000;
	
	
	private static ImageData getImageData(final ImageDescriptor descriptor) {
		ImageData data= descriptor.getImageData(); // see bug 51965: getImageData can return null
		if (data == null) {
			data= DEFAULT_IMAGE_DATA;
			StatusManager.getManager().handle(new Status(IStatus.WARNING, BuildpathsUIPlugin.PLUGIN_ID,
					"Image data not available: " + descriptor.toString() )); //$NON-NLS-1$
		}
		return data;
	}
	
	
	private final ImageDescriptor baseImage;
	private final int flags;
	private final Point size;
	
	
	/**
	 * Creates a new JavaElementImageDescriptor.
	 *
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered. See {@link #setAdornments(int)}
	 * 	for valid values.
	 * @param size the size of the resulting image
	 */
	public BuildpathElementImageDescriptor(final ImageDescriptor baseImage, final int flags, final Point size) {
		if (baseImage == null) {
			throw new NullPointerException("baseImage"); //$NON-NLS-1$
		}
		
		this.baseImage= baseImage;
		this.flags= flags;
		if (size != null) {
			this.size= size;
		}
		else {
			final ImageData data= getImageData(baseImage);
			this.size= new Point(data.width, data.height);
		}
	}
	
	
	protected final ImageDescriptor getBaseImage() {
		return this.baseImage;
	}
	
	protected final int getFlags() {
		return this.flags;
	}
	
	@Override
	protected final Point getSize() {
		return this.size;
	}
	
	
	@Override
	protected void drawCompositeImage(final int width, final int height) {
		if ((this.flags & DEPRECATED) != 0) { // draw *behind* the full image
			final Point size= getSize();
			final ImageData data= getImageData(SharedUIResources.INSTANCE.getImageDescriptor(
					SharedUIResources.OVR_DEPRECATED_IMAGE_ID ));
			drawImage(data, 0, size.y - data.height);
		}
		
		{	final ImageData data= getImageData(getBaseImage());
			drawImage(data, 0, 0);
		}
		
		drawTopRight();
		drawBottomRight();
		drawBottomLeft();
	}
	
	private void addTopRightImage(final ImageDescriptor desc, final Point pos) {
		final ImageData data= getImageData(desc);
		final int x= pos.x - data.width;
		if (x >= 0) {
			drawImage(data, x, pos.y);
			pos.x= x;
		}
	}
	
	private void addBottomRightImage(final ImageDescriptor desc, final Point pos) {
		final ImageData data= getImageData(desc);
		final int x= pos.x - data.width;
		final int y= pos.y - data.height;
		if (x >= 0 && y >= 0) {
			drawImage(data, x, y);
			pos.x= x;
		}
	}
	
	private void addBottomLeftImage(final ImageDescriptor desc, final Point pos) {
		final ImageData data= getImageData(desc);
		final int x= pos.x;
		final int y= pos.y - data.height;
		if (x + data.width < getSize().x && y >= 0) {
			drawImage(data, x, y);
			pos.x= x + data.width;
		}
	}
	
	
	private void drawTopRight() {
	}
	
	private void drawBottomRight() {
	}
	
	private void drawBottomLeft() {
		final Point pos= new Point(0, getSize().y);
		if ((this.flags & INFO) != 0) {
			addBottomLeftImage(SharedUIResources.INSTANCE.getImageDescriptor(
					SharedUIResources.OVR_INFO_IMAGE_ID ), pos );
		}
		if ((this.flags & WARNING) != 0) {
			addBottomLeftImage(SharedUIResources.INSTANCE.getImageDescriptor(
					SharedUIResources.OVR_WARNING_IMAGE_ID ), pos );
		}
		if ((this.flags & ERROR) != 0) {
			addBottomLeftImage(SharedUIResources.INSTANCE.getImageDescriptor(
					SharedUIResources.OVR_WARNING_IMAGE_ID ), pos );
		}
		if ((this.flags & IGNORE_OPTIONAL_PROBLEMS) != 0) {
			addBottomLeftImage(BuildpathsUIResources.INSTANCE.getImageDescriptor(
					BuildpathsUIPlugin.OVR_IGNORE_OPTIONAL_PROBLEMS_IMAGE_ID ), pos );
		}
	}
	
	
	@Override
	public int hashCode() {
		return (this.baseImage.hashCode() ^ this.flags) + this.size.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && getClass().equals(obj.getClass())) {
			final BuildpathElementImageDescriptor other= (BuildpathElementImageDescriptor) obj;
			return (this.baseImage.equals(other.baseImage)
					&& this.flags == other.flags
					&& this.size.equals(other.size) );
		}
		return false;
	}
	
}
