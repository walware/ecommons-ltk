/*=============================================================================#
 # Copyright (c) 2010-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.ui;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.util.ImageDescriptorRegistry;
import de.walware.ecommons.ui.util.MessageUtil;

import de.walware.ecommons.ltk.buildpaths.core.BuildpathInitializer;
import de.walware.ecommons.ltk.buildpaths.core.IBuildpathAttribute;
import de.walware.ecommons.ltk.buildpaths.core.IBuildpathElement;
import de.walware.ecommons.ltk.internal.buildpaths.ui.BuildpathElementImageDescriptor;
import de.walware.ecommons.ltk.internal.buildpaths.ui.BuildpathsUIPlugin;
import de.walware.ecommons.ltk.internal.buildpaths.ui.Messages;


public class BuildpathListLabelProvider extends LabelProvider {
	
	
	private final BuildpathsUIResources uiResources;
	private final ISharedImages workbenchImages;
	
	private final ImageDescriptorRegistry registry;
	
	
	public BuildpathListLabelProvider() {
		this.uiResources= BuildpathsUIResources.INSTANCE;
		this.workbenchImages= PlatformUI.getWorkbench().getSharedImages();
		
		this.registry= BuildpathsUIPlugin.getInstance().getImageDescriptorRegistry();
	}
	
	
	@Override
	public Image getImage(final Object element) {
		if (element instanceof BuildpathListElement) {
			final BuildpathListElement listElement= (BuildpathListElement) element;
			ImageDescriptor imageDescriptor= getListElementBaseImage(listElement);
			if (imageDescriptor != null) {
				if (listElement.isMissing() || listElement.hasMissingChildren()) {
					imageDescriptor= new BuildpathElementImageDescriptor(imageDescriptor,
							BuildpathElementImageDescriptor.ERROR,
							SharedUIResources.INSTANCE.getIconDefaultSize() );
				}
				return this.registry.get(imageDescriptor);
			}
		}
		else if (element instanceof BuildpathListElementAttribute) {
			final BuildpathListElementAttribute attribute= (BuildpathListElementAttribute) element;
			switch (attribute.getName()) {
			case IBuildpathAttribute.SOURCE_ATTACHMENT:
				return this.uiResources.getImage(
						BuildpathsUIResources.OBJ_SOURCE_ATTACHMENT_ATTRIBUTE_IMAGE_ID );
			case IBuildpathAttribute.FILTER_INCLUSIONS:
				return this.uiResources.getImage(
						BuildpathsUIResources.OBJ_INCLUSION_FILTER_ATTRIBUTE_IMAGE_ID );
			case IBuildpathAttribute.FILTER_EXCLUSIONS:
				return this.uiResources.getImage(
						BuildpathsUIResources.OBJ_EXCLUSION_FILTER_ATTRIBUTE_IMAGE_ID );
			case IBuildpathAttribute.OUTPUT:
				return this.uiResources.getImage(
						BuildpathsUIResources.OBJ_OUTPUT_FOLDER_ATTRIBUTE_IMAGE_ID );
			default:
				break;
			}
		}
		return null;
	}
	
	@Override
	public String getText(final Object element) {
		if (element instanceof BuildpathListElement) {
			return getListElementText((BuildpathListElement) element);
		}
		if (element instanceof BuildpathListElementAttribute) {
			final BuildpathListElementAttribute attribute= (BuildpathListElementAttribute) element;
			final String text= getListElementAttributeText(attribute);
			if (attribute.getStatus().getCode() == BuildpathInitializer.READ_ONLY) {
				return NLS.bind(Messages.ListLabel_Attribute_NonModifiable_label, text);
			}
			return text;
		}
		return super.getText(element);
	}
	
	
	private ImageDescriptor getListElementBaseImage(final BuildpathListElement listElement) {
		switch (listElement.getType().getName()) {
		case IBuildpathElement.PROJECT:
			return this.workbenchImages.getImageDescriptor(
					IDE.SharedImages.IMG_OBJ_PROJECT );
		case IBuildpathElement.SOURCE:
			if (listElement.getPath().segmentCount() == 1) {
				return this.workbenchImages.getImageDescriptor(
						IDE.SharedImages.IMG_OBJ_PROJECT );
			}
			else {
				return this.workbenchImages.getImageDescriptor(
						ISharedImages.IMG_OBJ_FOLDER );
			}
		default:
			return null;
		}
	}
	
	public String getListElementText(final BuildpathListElement element) {
		final IPath path= element.getPath();
		switch (element.getType().getName()) {
		case IBuildpathElement.PROJECT:
			String label= path.lastSegment();
			if (element.isMissing()) {
				label= label + ' ' + Messages.ListLabel_Deco_Missing_label;
			}
			return label;
		case IBuildpathElement.SOURCE: {
			final String pathLabel= getPathString(path, false);
			final StringBuilder sb= new StringBuilder(pathLabel);
			final IPath linkTarget= element.getLinkTarget();
			if (linkTarget != null) {
				sb.append(" - ");
				sb.append(MessageUtil.processPath(linkTarget.toOSString()));
			}
			final IResource resource= element.getResource();
			if (resource != null && !resource.exists()) {
				if (element.isMissing()) {
					sb.append(' ');
					sb.append(Messages.ListLabel_Deco_Missing_label);
				}
				else {
					sb.append(' ');
					sb.append(Messages.ListLabel_Deco_New_label);
				}
			}
			else if (element.getOrginalPath() == null) {
				sb.append(' ');
				sb.append(Messages.ListLabel_Deco_New_label);
			}
			return sb.toString();
		}
		default:
			return Messages.ListLabel_Element_Unknown_label;
		}
	}
	
	public String getListElementAttributeText(final BuildpathListElementAttribute attribute) {
		final String key= attribute.getName();
		switch (key) {
		case IBuildpathAttribute.SOURCE_ATTACHMENT: {
				final String detail;
				final IPath path= (IPath) attribute.getValue();
				if (path != null && !path.isEmpty()) {
					if (attribute.getParent().getType().getName() == IBuildpathElement.VARIABLE) {
						detail= getVariableString(path);
					}
					else {
						detail= getPathString(path, path.getDevice() != null);
					}
				}
				else {
					detail= Messages.ListLabel_Value_None_label;
				}
				return NLS.bind(Messages.ListLabel_Attribute_SourceAttachment_label, detail);
			}
		case IBuildpathAttribute.FILTER_INCLUSIONS: {
				final String detail;
				final List<? extends IPath> patterns= (List<? extends IPath>) attribute.getValue();
				if (patterns != null && !patterns.isEmpty()) {
					final StringBuilder sb= new StringBuilder();
					final int patternsCount= appendPatternList(patterns, sb);
					if (patternsCount > 0) {
						detail= sb.toString();
					}
					else {
						detail= Messages.ListLabel_Value_Filter_None_label;
					}
				}
				else {
					detail= Messages.ListLabel_Value_Filter_All_label;
				}
				return NLS.bind(Messages.ListLabel_Attribute_Inclusion_label, detail);
			}
		case IBuildpathAttribute.FILTER_EXCLUSIONS: {
				final String detail;
				final List<? extends IPath> patterns= (List<? extends IPath>) attribute.getValue();
				if (patterns != null && !patterns.isEmpty()) {
					final StringBuilder sb= new StringBuilder();
					final int patternsCount= appendPatternList(patterns, sb);
					if (patternsCount > 0) {
						detail= sb.toString();
					}
					else {
						detail= Messages.ListLabel_Value_Filter_None_label;
					}
				}
				else {
					detail= Messages.ListLabel_Value_Filter_None_label;
				}
				return NLS.bind(Messages.ListLabel_Attribute_Exclusion_label, detail);
			}
		case IBuildpathAttribute.OUTPUT: {
				final String detail;
				final IPath path= (IPath) attribute.getValue();
				if (path != null) {
					detail= MessageUtil.processPath(path.toString());
				}
				else {
					detail= Messages.ListLabel_Value_Output_Default_label;
				}
				return NLS.bind(Messages.ListLabel_Attribute_OutputFolder_label, detail);
			}
		default: {
//				final ClasspathAttributeConfiguration config= this.fAttributeDescriptors.get(key);
//				if (config != null) {
//					final ClasspathAttributeAccess access= attribute.getClasspathAttributeAccess();
//					final String nameLabel= config.getNameLabel(access);
//					final String valueLabel= config.getValueLabel(access); // should be LTR marked
//					return Messages.format(Messages.ListLabel_Provider_attribute_label, new String[] { nameLabel, valueLabel });
//				}
				String value= (String) attribute.getValue();
				if (value == null) {
					value= Messages.ListLabel_Value_None_label;
				}
				return NLS.bind(Messages.ListLabel_Attribute_Generic_label, key, value);
			}
		}
	}
	
	private int appendPatternList(final List<? extends IPath> patterns, final StringBuilder sb) {
		int patternsCount= 0;
		for (final IPath pattern : patterns) {
			if (pattern.segmentCount() > 0) {
				final String text= MessageUtil.processPath(pattern.toString());
				if (patternsCount > 0) {
					sb.append(Messages.ListLabel_Value_Path_separator);
				}
				sb.append(text);
				patternsCount++;
			}
		}
		return patternsCount;
	}
	
	private String getPathString(final IPath path, final boolean isExternal) {
		return MessageUtil.processPath((isExternal) ? path.toOSString() : path.makeRelative().toString());
	}
	
	private String getVariableString(final IPath path) {
		return MessageUtil.processPath(path.toString());
	}
	
}
