/*=============================================================================#
 # Copyright (c) 2012-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.refactoring;

import de.walware.ecommons.ltk.core.ElementSet;
import de.walware.ecommons.ltk.core.model.ISourceElement;


public class RefactoringDestination extends ElementSet {
	
	
	public static enum Position {
		
		ABOVE,
		INTO,
		BELOW,
		AT,
		
	}
	
	
	private final Position fPosition;
	private int fOffset;
	
	
	public RefactoringDestination(final Object element) {
		this(element, Position.INTO);
	}
	
	public RefactoringDestination(final Object element, final Position pos) {
		super(element);
		fPosition = pos;
	}
	
	public RefactoringDestination(final ISourceElement element, final int offset) {
		super(element);
		fPosition = Position.AT;
		fOffset = offset;
	}
	
	
	public Position getPosition() {
		return fPosition;
	}
	
	public int getOffset() {
		return fOffset;
	}
	
}
