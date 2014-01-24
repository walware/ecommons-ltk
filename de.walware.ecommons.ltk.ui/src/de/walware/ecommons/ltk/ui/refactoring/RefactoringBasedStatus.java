/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.refactoring;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


/**
 * {@link IStatus} wrapping a {@link RefactoringStatus}.
 */
public class RefactoringBasedStatus implements IStatus {
	
	
	private final RefactoringStatus fStatus;
	
	
	public RefactoringBasedStatus(final RefactoringStatus status) {
		fStatus = status;
	}
	
	
	@Override
	public String getPlugin() {
		return LTKUIPlugin.PLUGIN_ID;
	}
	
	@Override
	public int getSeverity() {
		return convertSeverity(fStatus.getSeverity());
	}
	
	@Override
	public boolean isOK() {
		return (fStatus.getSeverity() == RefactoringStatus.OK);
	}
	
	@Override
	public String getMessage() {
		return fStatus.getMessageMatchingSeverity(fStatus.getSeverity());
	}
	
	@Override
	public int getCode() {
		return 0;
	}
	
	@Override
	public Throwable getException() {
		return null;
	}
	
	@Override
	public boolean isMultiStatus() {
		return false;
	}
	
	@Override
	public IStatus[] getChildren() {
		return null;
	}
	
	@Override
	public boolean matches(final int severityMask) {
		return (getSeverity() & severityMask) != 0;
	}
	
	
	public static int convertSeverity(final int severity) {
		switch (severity) {
		case RefactoringStatus.FATAL:
			return IStatus.ERROR;
		case RefactoringStatus.ERROR:
		case RefactoringStatus.WARNING:
			return IStatus.WARNING;
		case RefactoringStatus.INFO:
			return IStatus.INFO;
		default:
			return IStatus.OK;
		}
	}
	
}
