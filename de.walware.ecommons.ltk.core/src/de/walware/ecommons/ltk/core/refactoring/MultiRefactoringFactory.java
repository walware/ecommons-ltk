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

package de.walware.ecommons.ltk.core.refactoring;

import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.ltk.core.refactoring.participants.CopyProcessor;
import org.eclipse.ltk.core.refactoring.participants.DeleteProcessor;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import de.walware.ecommons.ltk.core.ElementSet;
import de.walware.ecommons.ltk.core.model.IModelElement;


public abstract class MultiRefactoringFactory extends CommonRefactoringFactory {
	
	
	private static final Object NO_FACTORY= new Object();
	
	
	private final String defaultTypeId;
	private final CommonRefactoringFactory defaultFactory;
	
	private final Map<String, Object> secondaryFactories= new IdentityHashMap<>(8);
	
	
	public MultiRefactoringFactory(final String defaultTypeId,
			final CommonRefactoringFactory defaultFactory) {
		this.defaultTypeId= defaultTypeId;
		this.defaultFactory= defaultFactory;
	}
	
	
	protected final CommonRefactoringFactory getFactory(final String modelTypeId) {
		if (modelTypeId == this.defaultTypeId || modelTypeId == null) {
			return this.defaultFactory;
		}
		Object factory= this.secondaryFactories.get(modelTypeId);
		if (factory == null) {
			factory= createFactory(modelTypeId);
			if (factory == null) {
				factory= NO_FACTORY;
			}
			this.secondaryFactories.put(modelTypeId, factory);
		}
		return (factory != NO_FACTORY) ? (CommonRefactoringFactory) factory : null;
	}
	
	protected abstract CommonRefactoringFactory createFactory(String modelTypeId);
	
	
	@Override
	public RefactoringAdapter createAdapter(Object elements) {
		String type= null;
		if (elements instanceof ElementSet) {
			elements= ((ElementSet) elements).getInitialObjects();
		}
		else if (elements instanceof Object[]) {
			final Object[] array= (Object[]) elements;
			for (int i= 0; i < array.length; i++) {
				if (array[i] instanceof IModelElement) {
					final String elementType= ((IModelElement) array[i]).getModelTypeId();
					if (type == null) {
						type= elementType;
						continue;
					}
					else if (type == elementType) {
						continue;
					}
					else {
						return null;
					}
				}
			}
		}
		else if (elements instanceof IModelElement) {
			type= ((IModelElement) elements).getModelTypeId();
		}
		
		final CommonRefactoringFactory factory= getFactory(type);
		return (factory != null) ? factory.createAdapter(elements) : null;
	}
	
	
	@Override
	public DeleteProcessor createDeleteProcessor(final Object elementsToDelete, final RefactoringAdapter adapter) {
		final CommonRefactoringFactory factory= getFactory(adapter.getModelTypeId());
		return (factory != null) ?
				factory.createDeleteProcessor(elementsToDelete, adapter) :
				null;
	}
	
	@Override
	public MoveProcessor createMoveProcessor(final Object elementsToMove, final RefactoringDestination destination,
			final RefactoringAdapter adapter) {
		final CommonRefactoringFactory factory= getFactory(adapter.getModelTypeId());
		return (factory != null) ?
				factory.createMoveProcessor(elementsToMove, destination, adapter) :
				null;
	}
	
	@Override
	public CopyProcessor createCopyProcessor(final Object elementsToCopy, final RefactoringDestination destination,
			final RefactoringAdapter adapter) {
		final CommonRefactoringFactory factory= getFactory(adapter.getModelTypeId());
		return (factory != null) ?
				factory.createCopyProcessor(elementsToCopy, destination, adapter) :
				null;
	}
	
	@Override
	public RefactoringProcessor createPasteProcessor(final Object elementsToPaste, final RefactoringDestination destination,
			final RefactoringAdapter adapter) {
		final CommonRefactoringFactory factory= getFactory(adapter.getModelTypeId());
		return (factory != null) ?
				factory.createPasteProcessor(elementsToPaste, destination, adapter) :
				null;
	}
	
}
