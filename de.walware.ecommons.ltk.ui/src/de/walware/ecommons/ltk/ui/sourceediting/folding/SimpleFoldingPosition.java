/*=============================================================================#
 # Copyright (c) 2014-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.folding;


public final class SimpleFoldingPosition extends AbstractFoldingPosition<SimpleFoldingPosition> {
	
	
	public SimpleFoldingPosition(final int offset, final int length) {
		super(offset, length);
	}
	
	
	@Override
	protected boolean update(final SimpleFoldingPosition newPosition) {
		return true;
	}
	
}
