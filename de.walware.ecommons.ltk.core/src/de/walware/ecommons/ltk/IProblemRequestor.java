/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
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

import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * Accept problems by a problem checker.
 * 
 * {@link ISourceUnit#getProblemRequestor()}
 */
public interface IProblemRequestor {
	
	
	/**
	 * Notification of a discovered problem.
	 * 
	 * @param problem the problem.
	 */
	void acceptProblems(IProblem problem);
	
	/**
	 * Notification of a list of problems.
	 * 
	 * @param type the category of the problems.
	 * @param problems the problems.
	 */
	void acceptProblems(String categoryId, List<IProblem> problems);
	
	void finish();
	
}
