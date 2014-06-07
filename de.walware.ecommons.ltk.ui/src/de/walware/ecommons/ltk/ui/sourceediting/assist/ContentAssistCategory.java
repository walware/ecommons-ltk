/*=============================================================================#
 # Copyright (c) 2005-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #     Stephan Wahlbrink - adapted API and improvements
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;

import de.walware.ecommons.ui.util.MessageUtil;



/**
 * Describes a category for {@link ContentAssistProcessor}s.
 */
public final class ContentAssistCategory {
	
	
	private static final List<IContentAssistComputer> NO_COMPUTERS= Collections.emptyList();
	
	
	private final String id;
	
	private final String name;
	
	/** The image descriptor for this category, or <code>null</code> if none specified. */
	private final ImageDescriptor image;
	
	boolean isEnabledAsSeparate= false;
	
	boolean isIncludedInDefault= false;
	
	private final int sortOrder= 0x10000;
	
	private final List<ContentAssistComputerRegistry.ComputerDescriptor> computerDescriptors;
	private final Map<String, List<IContentAssistComputer>> computersByPartition;
	
	
	public ContentAssistCategory(final String partitionId, final List<IContentAssistComputer> computers) {
		this.id= "explicite:" + partitionId; //$NON-NLS-1$
		this.name= null;
		this.image= null;
		this.computerDescriptors= Collections.emptyList();
		this.computersByPartition= new HashMap<>();
		this.computersByPartition.put(partitionId, computers);
		this.isIncludedInDefault= true;
	}
	
	ContentAssistCategory(final String id, final String name, final ImageDescriptor imageDsrc,
			final List<ContentAssistComputerRegistry.ComputerDescriptor> computers) {
		this.id= id;
		this.name= name;
		this.image= imageDsrc;
		this.computerDescriptors= computers;
		this.computersByPartition= new HashMap<>();
	}
	
	ContentAssistCategory(final ContentAssistCategory template) {
		this.id= template.id;
		this.name= template.name;
		this.image= template.image;
		this.computerDescriptors= template.computerDescriptors;
		this.computersByPartition= template.computersByPartition;
	}
	
	/**
	 * Returns the identifier of the described extension.
	 *
	 * @return Returns the id
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * Returns the name of the described extension.
	 * 
	 * @return Returns the name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the name of the described extension
	 * without mnemonic hint in order to be displayed
	 * in a message.
	 * 
	 * @return Returns the name
	 */
	public String getDisplayName() {
		return MessageUtil.removeMnemonics(this.name);
	}
	
	/**
	 * Returns the image descriptor of the described category.
	 * 
	 * @return the image descriptor of the described category
	 */
	public ImageDescriptor getImageDescriptor() {
		return this.image;
	}
	
	public boolean isEnabledInDefault() {
		return this.isIncludedInDefault;
	}
	
	public boolean isEnabledInCircling() {
		return this.isEnabledAsSeparate;
	}
	
//	public int getSortOrder() {
//		return fSortOrder;
//	}
//	
	public boolean hasComputers(final String contentTypeId) {
		final List<IContentAssistComputer> computers= this.computersByPartition.get(contentTypeId);
		if (computers == null) {
			for (final ContentAssistComputerRegistry.ComputerDescriptor dscr : this.computerDescriptors) {
				if (dscr.getPartitions().contains(contentTypeId)) {
					return true;
				}
			}
			this.computersByPartition.put(contentTypeId, NO_COMPUTERS);
			return false;
		}
		else {
			return !computers.isEmpty();
		}
	}
	
	public List<IContentAssistComputer> getComputers(final String contentTypeId) {
		List<IContentAssistComputer> computers= this.computersByPartition.get(contentTypeId);
		if (computers == null) {
			computers= initComputers(contentTypeId);
		}
		return computers;
	}
	
	private List<IContentAssistComputer> initComputers(final String contentTypeId) {
		final List<IContentAssistComputer> computers= new ArrayList<>();
		for (final ContentAssistComputerRegistry.ComputerDescriptor dscr : this.computerDescriptors) {
			if (dscr.getPartitions().contains(contentTypeId)) {
				final IContentAssistComputer computer= dscr.getComputer();
				if (computer != null) {
					computers.add(computer);
				}
			}
		}
		this.computersByPartition.put(contentTypeId, computers);
		return computers;
	}
	
	
	@Override
	public String toString() {
		return this.id;
	}
	
}
