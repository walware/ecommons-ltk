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

package de.walware.ecommons.ltk.ui.sourceediting.folding;

import org.eclipse.jface.text.Position;


public abstract class AbstractFoldingPosition<E extends AbstractFoldingPosition<E>> extends Position {
	
	
	public AbstractFoldingPosition(final int offset, final int length) {
		super(offset, length);
	}
	
	
	protected abstract boolean update(E newPosition);
	
	
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof AbstractFoldingPosition<?>
				&& this.getClass() == obj.getClass() ) {
			final AbstractFoldingPosition<?> other= (AbstractFoldingPosition<?>) obj;
			return ((this.offset == other.offset) && (this.length == other.length));
		}
		return false;
	}
	
}
