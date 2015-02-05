/*=============================================================================#
 # Copyright (c) 2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import de.walware.ecommons.ltk.core.ISourceModelStamp;


public class SourceModelStamp implements ISourceModelStamp {
	
	
	private final long sourceStamp;
	
	
	public SourceModelStamp(final long sourceStamp) {
		this.sourceStamp= sourceStamp;
	}
	
	
	@Override
	public final long getSourceStamp() {
		return this.sourceStamp;
	}
	
	
	@Override
	public int hashCode() {
		return (int) (this.sourceStamp ^ (this.sourceStamp >>> 32));
	}
	
	@Override
	public boolean equals(final Object other) {
		return (other instanceof ISourceModelStamp
				&& getClass() == other.getClass()
				&& this.sourceStamp == ((ISourceModelStamp) other).getSourceStamp() );
	}
	
}
