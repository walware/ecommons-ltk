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

package de.walware.ecommons.text.ui.presentation;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;

import de.walware.ecommons.text.ui.settings.TextStyleManager;


public class SingleTokenScanner implements ITokenScanner {
	
	private static final byte EOF= 0;
	private static final byte DEFAULT= 1;
	
	
	private final TextStyleManager textStyles;
	
	private final IToken defaultToken;
	
	private int offset;
	private int length;
	
	private byte state;
	
	
	public SingleTokenScanner(final TextStyleManager textStyles, final String defaultTokenKey) {
		this.textStyles= textStyles;
		
		this.defaultToken= this.textStyles.getToken(defaultTokenKey);
	}
	
	
	@Override
	public void setRange(final IDocument document, final int offset, final int length) {
		this.offset= offset;
		this.length= length;
		this.state= DEFAULT;
	}
	
	@Override
	public IToken nextToken() {
		if (this.state == DEFAULT) {
			this.state= EOF;
			return this.defaultToken;
		}
		return Token.EOF;
	}
	
	@Override
	public int getTokenOffset() {
		return this.offset;
	}
	
	@Override
	public int getTokenLength() {
		return this.length;
	}
	
}
