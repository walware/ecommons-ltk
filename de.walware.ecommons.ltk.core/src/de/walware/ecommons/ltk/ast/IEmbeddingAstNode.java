/*=============================================================================#
 # Copyright (c) 2014-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ast;


public interface IEmbeddingAstNode extends IAstNode {
	
	byte EMBED_CHUNK=                                       0x0000000_1;
	byte EMBED_INLINE=                                      0x0000000_2;
	
	
	void setForeignNode(IAstNode node);
	IAstNode getForeignNode();
	
	String getForeignTypeId();
	int getEmbedDescr();
	
}
