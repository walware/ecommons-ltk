/*=============================================================================#
 # Copyright (c) 2015-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ast;

import org.eclipse.jface.text.IRegion;


/**
 * Detail for the status code of an AST node.
 */
public class StatusDetail implements IRegion {
	
	
	public static final StatusDetail getStatusDetail(final IAstNode node) {
		for (final Object aAttachment : node.getAttachments()) {
			if (aAttachment instanceof StatusDetail) {
				return (StatusDetail) aAttachment;
			}
		}
		return null;
	}
	
	
	private final int offset;
	private final int length;
	
	private final String text;
	
	
	public StatusDetail(final int offset, final int length, final String text) {
		this.offset= offset;
		this.length= length;
		this.text= text;
	}
	
	
	@Override
	public int getOffset() {
		return this.offset;
	}
	
	@Override
	public int getLength() {
		return this.length;
	}
	
	public String getText() {
		return this.text;
	}
	
}
