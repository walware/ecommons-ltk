/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import de.walware.ecommons.collections.ImCollections;
import de.walware.ecommons.collections.ImList;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;


public abstract class AbstractSourceModelInfo implements ISourceUnitModelInfo {
	
	
	private static final ImList<Object> NO_ATTACHMENTS= ImCollections.emptyList();
	
	
	private final AstInfo ast;
	
	private volatile ImList<Object> attachments= NO_ATTACHMENTS;
	
	
	public AbstractSourceModelInfo(final AstInfo ast) {
		this.ast= ast;
	}
	
	
	@Override
	public long getStamp() {
		return this.ast.stamp;
	}
	
	@Override
	public AstInfo getAst() {
		return this.ast;
	}
	
	
	@Override
	public synchronized void addAttachment(final Object data) {
		this.attachments= ImCollections.addElement(this.attachments, data);
	}
	
	@Override
	public void removeAttachment(final Object data) {
		this.attachments= ImCollections.removeElement(this.attachments, data);
	}
	
	@Override
	public ImList<Object> getAttachments() {
		return this.attachments;
	}
	
}
