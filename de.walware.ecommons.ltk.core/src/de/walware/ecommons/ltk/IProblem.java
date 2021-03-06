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

import org.eclipse.core.resources.IMarker;

import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * Problem
 */
public interface IProblem {
	
	
	/** 
	 * Error severity constant indicating an error.
	 * 
	 * {@link IMarker#SEVERITY_ERROR}
	 */
	int SEVERITY_ERROR = 2;
	
	/** 
	 * Error severity constant indicating a warning.
	 * 
	 * {@link IMarker#SEVERITY_WARNING}
	 */
	int SEVERITY_WARNING = 1;
	
	/** 
	 * Error severity constant indicating an information only.
	 * 
	 * {@link IMarker#SEVERITY_INFO}
	 */
	int SEVERITY_INFO = 0;
	
	
	String getCategoryId();
	ISourceUnit getSourceUnit();
	
	int getSourceLine();
	int getSourceStartOffset();
	int getSourceStopOffset();
	
	int getSeverity();
	int getCode();
	String getMessage();
	
}
