/*=============================================================================#
 # Copyright (c) 2005-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text;

import org.eclipse.jface.text.rules.IWhitespaceDetector;


public class DefaultWhitespaceDetector implements IWhitespaceDetector {
	
	
	@Override
	public boolean isWhitespace(final char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
	
}
