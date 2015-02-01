/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IRegion;

import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceElement;
import de.walware.ecommons.ltk.core.model.ISourceStructElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;


public class LTKUtil {
	
	
	public static IModelElement getModelElement(final Object element) {
		if (element instanceof IModelElement) {
			return (IModelElement) element;
		}
		if (element instanceof IAdaptable) {
			return (IModelElement) ((IAdaptable) element).getAdapter(IModelElement.class);
		}
		return null;
	}
	
	public static ISourceUnit getSourceUnit(final IModelElement element) {
		if (element instanceof ISourceUnit) {
			return (ISourceUnit) element;
		}
		if (element instanceof ISourceElement) {
			return ((ISourceElement) element).getSourceUnit();
		}
		return null;
	}
	
	public static ISourceStructElement getCoveringSourceElement(final ISourceStructElement root, final IRegion region) {
		return getCoveringSourceElement(root, region.getOffset(), region.getOffset()+region.getLength());
	}
	
	public static ISourceStructElement getCoveringSourceElement(final ISourceStructElement root,
			final int startOffset, final int endOffset) {
		ISourceStructElement ok = root;
		CHECK: while (ok != null) {
			final List<? extends ISourceStructElement> children = ok.getSourceChildren(null);
			for (final ISourceStructElement child : children) {
				final IRegion sourceRange = child.getSourceRange();
				final IRegion docRange = child.getDocumentationRange();
				final int childOffset = (docRange != null) ?
						Math.min(sourceRange.getOffset(), docRange.getOffset()) :
						sourceRange.getOffset();
				if (startOffset >= childOffset) {
					final int childEnd = (docRange != null) ?
							Math.max(sourceRange.getOffset()+sourceRange.getLength(), docRange.getOffset()+docRange.getLength()) :
							sourceRange.getOffset()+sourceRange.getLength();
					if (startOffset < endOffset ? 
							(endOffset <= childEnd) : (endOffset < childEnd)) {
						ok = child;
						continue CHECK;
					}
				}
				else {
					break CHECK;
				}
			}
			break CHECK;
		}
		return ok;
	}
	
	public static int searchCoveringSourceElement(final List<? extends ISourceStructElement> elements,
			final int offset) {
		// binary search
		int low = 0;
		int high = elements.size() - 1;
		while (low <= high) {
			final int mid = (low + high) >> 1;
			final IRegion region = elements.get(mid).getSourceRange();
			
			if (region.getOffset()+region.getLength() < offset) {
				low = mid + 1;
			}
			else if (region.getOffset() > offset) {
				high = mid - 1;
			}
			else {
				return mid;
			}
		}
		return -(low + 1);
	}
	
	public static <T extends ISourceStructElement> T getCoveringSourceElement(final List<T> elements,
			final int offset) {
		final int idx = searchCoveringSourceElement(elements, offset);
		if (idx >= 0) {
			return elements.get(idx);
		}
		return null;
	}
	
}
