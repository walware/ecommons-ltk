/*=============================================================================#
 # Copyright (c) 2011-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.impl;

import java.util.List;

import de.walware.ecommons.collections.CollectionUtils;
import de.walware.ecommons.collections.ConstArrayList;
import de.walware.ecommons.collections.ConstList;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;


public abstract class AbstractSourceModelInfo implements ISourceUnitModelInfo {
	
	
	private static final ConstList<Object> NO_ATTACHMENTS= CollectionUtils.emptyConstList();
	
	
	private final AstInfo ast;
	
	private ConstList<Object> attachments= NO_ATTACHMENTS;
	
	
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
	public void addAttachment(final Object data) {
		this.attachments= ConstArrayList.concat(this.attachments, data);
	}
	
	@Override
	public List<Object> getAttachments() {
		return this.attachments;
	}
	
}
