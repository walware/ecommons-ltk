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

package de.walware.ecommons.ltk.core.util;

import java.util.Comparator;

import de.walware.ecommons.ltk.ast.IAstNode;
import de.walware.ecommons.ltk.core.model.INameAccess;


public class NameUtils {
	
	
	public static final Comparator<INameAccess<?, ?>> NAME_POSITION_COMPARATOR=
			new Comparator<INameAccess<?, ?>>() {
				private int getOffset(final IAstNode nameNode) {
					return (nameNode != null) ? nameNode.getOffset() : Integer.MAX_VALUE;
				}
				@Override
				public int compare(final INameAccess<?, ?> access1, final INameAccess<?, ?> access2) {
					return getOffset(access1.getNameNode()) - getOffset(access2.getNameNode()); 
				}
			};
	
	
}
