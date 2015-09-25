/*=============================================================================#
 # Copyright (c) 2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import java.lang.reflect.InvocationTargetException;

import de.walware.ecommons.collections.ImCollections;
import de.walware.ecommons.collections.ImList;

import de.walware.ecommons.ltk.ast.IAstNode;
import de.walware.ecommons.ltk.ast.ICommonAstVisitor;


public abstract class AbstractAstNode implements IAstNode {
	
	
	private static final ImList<Object> NO_ATTACHMENT= ImCollections.emptyList();
	
	
	private volatile ImList<Object> attachments= NO_ATTACHMENT;
	
	
	protected AbstractAstNode() {
	}
	
	
	@Override
	public final void accept(final ICommonAstVisitor visitor) throws InvocationTargetException {
		visitor.visit(this);
	}
	
	
	public String getText() {
		return null;
	}
	
	
	@Override
	public synchronized void addAttachment(final Object data) {
		this.attachments= ImCollections.addElement(this.attachments, data);
	}
	
	@Override
	public synchronized void removeAttachment(final Object data) {
		this.attachments= ImCollections.removeElement(this.attachments, data);
	}
	
	@Override
	public ImList<Object> getAttachments() {
		return this.attachments;
	}
	
	
}
