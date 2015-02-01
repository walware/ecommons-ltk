/*=============================================================================#
 # Copyright (c) 2006-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;


/**
 * Dynamic validation change with support for refactoring descriptors.
 */
public final class DynamicValidationRefactoringChange extends DynamicValidationChange {
	
	
	/** The refactoring descriptor */
	private final RefactoringDescriptor fDescriptor;
	
	private ReorgExecutionLog fExecutionLog;
	
	
	/**
	 * Creates a new dynamic validation refactoring change.
	 * 
	 * @param descriptor
	 *     the refactoring descriptor
	 * @param name
	 *     the name of the change
	 */
	public DynamicValidationRefactoringChange(final RefactoringDescriptor descriptor, final String name) {
		super(name);
		assert (descriptor != null);
		fDescriptor = descriptor;
	}
	
	/**
	 * Creates a new dynamic validation refactoring change.
	 * 
	 * @param descriptor the refactoring descriptor
	 * @param name the name of the change
	 * @param changes the changes
	 * @param executionLog optional reorg execution log
	 */
	public DynamicValidationRefactoringChange(final RefactoringDescriptor descriptor, final String name,
			final Change[] changes, final ReorgExecutionLog executionLog) {
		super(name, changes);
		assert (descriptor != null);
		fDescriptor = descriptor;
		fExecutionLog = executionLog;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ChangeDescriptor getDescriptor() {
		return new RefactoringChangeDescriptor(fDescriptor);
	}
	
	
	@Override
	public Change perform(final IProgressMonitor progress) throws CoreException {
		try {
			return super.perform(progress);
		}
		catch (final OperationCanceledException e) {
			if (fExecutionLog != null) {
				fExecutionLog.markAsCanceled();
			}
			throw e;
		}
	}
	
	
	@Override
	public Object getAdapter(final Class required) {
		if (ReorgExecutionLog.class.equals(required)) {
			return fExecutionLog;
		}
		return super.getAdapter(required);
	}
	
}
