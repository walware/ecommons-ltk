/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import de.walware.ecommons.ltk.IProblem;
import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * Problem in a source unit. Default implementation of {@link IProblem}.
 */
public class Problem implements IProblem {
	
	
	private final ISourceUnit fUnit;
	private final int fLine;
	private final int fStart;
	private final int fStop;
	
	private final String fModelTypeId;
	private final int fSeverity;
	private final int fCode;
	private final String fMessage;
	
	
	public Problem(final String modelTypeId,
			final int severity, final int code, final String message,
			final ISourceUnit unit, final int line, final int startOffset, final int stopOffset) {
		fModelTypeId = modelTypeId;
		fSeverity = severity;
		fCode = code;
		fMessage = message;
		
		fUnit = unit;
		fLine = line;
		fStart = startOffset;
		fStop = (stopOffset - startOffset > 0) ? stopOffset : startOffset + 1;
	}
	
	public Problem(final String modelTypeId,
			final int severity, final int code, final String message,
			final ISourceUnit unit, final int startOffset, final int stopOffset) {
		fModelTypeId = modelTypeId;
		fSeverity = severity;
		fCode = code;
		fMessage = message;
		
		fUnit = unit;
		fLine = -1;
		fStart = startOffset;
		fStop = (stopOffset - startOffset > 0) ? stopOffset : startOffset + 1;
	}
	
	
	@Override
	public String getCategoryId() {
		return fModelTypeId;
	}
	
	@Override
	public ISourceUnit getSourceUnit() {
		return fUnit;
	}
	
	@Override
	public int getSourceLine() {
		return fLine;
	}
	
	@Override
	public int getSourceStartOffset() {
		return fStart;
	}
	
	@Override
	public int getSourceStopOffset() {
		return fStop;
	}
	
	@Override
	public int getSeverity() {
		return fSeverity;
	}
	
	@Override
	public int getCode() {
		return fCode;
	}
	
	@Override
	public String getMessage() {
		return fMessage;
	}
	
}
