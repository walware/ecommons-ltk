/*=============================================================================#
 # Copyright (c) 2014-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.walware.ecommons.ltk.core.ISourceModelStamp;
import de.walware.ecommons.ltk.core.model.IModelElement.Filter;
import de.walware.ecommons.ltk.core.model.ISourceStructElement;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;


public class OutlineContentProvider implements ITreeContentProvider {
	
	
	public interface IOutlineContent {
		
		ISourceUnitModelInfo getModelInfo(Object inputElement);
		
		Filter getContentFilter();
		
	}
	
	
	private final IOutlineContent content;
	
	
	public OutlineContentProvider(final IOutlineContent content) {
		this.content= content;
	}
	
	
	protected final IOutlineContent getContent() {
		return this.content;
	}
	
	public ISourceModelStamp getStamp(final Object inputElement) {
		final ISourceUnitModelInfo modelInfo= getContent().getModelInfo(inputElement);
		return (modelInfo != null) ? modelInfo.getStamp() : null;
	}
	
	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
	}
	
	@Override
	public Object[] getElements(final Object inputElement) {
		final ISourceUnitModelInfo modelInfo= getContent().getModelInfo(inputElement);
		if (modelInfo != null) {
			final List<? extends ISourceStructElement> children= modelInfo.getSourceElement().getSourceChildren(getContent().getContentFilter());
			return children.toArray(new ISourceStructElement[children.size()]);
		}
		return new ISourceStructElement[0];
	}
	
	@Override
	public void dispose() {
	}
	
	@Override
	public Object getParent(final Object element) {
		final ISourceStructElement o= (ISourceStructElement) element;
		return o.getSourceParent();
	}
	
	@Override
	public boolean hasChildren(final Object element) {
		final ISourceStructElement o= (ISourceStructElement) element;
		return o.hasSourceChildren(getContent().getContentFilter());
	}
	
	@Override
	public Object[] getChildren(final Object parentElement) {
		// Check required for E bug #438919
		if (parentElement instanceof ISourceStructElement) {
			final ISourceStructElement o= (ISourceStructElement) parentElement;
			final List<? extends ISourceStructElement> children= o.getSourceChildren(getContent().getContentFilter());
			return children.toArray(new ISourceStructElement[children.size()]);
		}
		return new ISourceStructElement[0];
	}
	
}
