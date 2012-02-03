/*******************************************************************************
 * Copyright (c) 2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.core.refactoring;

import de.walware.ecommons.ltk.ISourceElement;


public class RefactoringDestination extends RefactoringElementSet {
	
	
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
		super(new Object[] { element });
		fPosition = pos;
	}
	
	public RefactoringDestination(final ISourceElement element, final int offset) {
		super(new Object[] { element });
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
