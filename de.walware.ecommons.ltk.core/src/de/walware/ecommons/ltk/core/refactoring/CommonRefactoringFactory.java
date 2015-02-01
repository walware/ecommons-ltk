/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.refactoring;

import org.eclipse.ltk.core.refactoring.participants.CopyProcessor;
import org.eclipse.ltk.core.refactoring.participants.DeleteProcessor;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import de.walware.ecommons.ltk.core.ElementSet;


/**
 * Must be implemented for each language.
 */
public class CommonRefactoringFactory {
	
	
	public RefactoringAdapter createAdapter(final Object elements) {
		return null;
	}
	
	public DeleteProcessor createDeleteProcessor(final Object elementsToDelete,
			final RefactoringAdapter adapter) {
		return null;
	}
	
	public MoveProcessor createMoveProcessor(final Object elementsToMove, final RefactoringDestination destination,
			final RefactoringAdapter adapter) {
		return null;
	}
	
	public CopyProcessor createCopyProcessor(final Object elementsToCopy, final RefactoringDestination destination,
			final RefactoringAdapter adapter) {
		return null;
	}
	
	public RefactoringProcessor createPasteProcessor(final Object elementsToPaste, final RefactoringDestination destination,
			final RefactoringAdapter adapter) {
		return null;
	}
	
	
	protected ElementSet createElementSet(final Object elements) {
		if (elements instanceof ElementSet) {
			return (ElementSet) elements;
		}
		if (elements instanceof Object[]) {
			return new ElementSet((Object[]) elements);
		}
		return new ElementSet(elements);
	}
	
}
