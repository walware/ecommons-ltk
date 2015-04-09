/*=============================================================================#
 # Copyright (c) 2014-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.folding;

import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;

import de.walware.ecommons.ltk.ast.IAstNode;
import de.walware.ecommons.ltk.ast.ICommonAstVisitor;
import de.walware.ecommons.ltk.ui.sourceediting.folding.FoldingEditorAddon.FoldingStructureComputationContext;


public interface NodeFoldingProvider extends FoldingProvider {
	
	
	class VisitorMap extends IdentityHashMap<String, ICommonAstVisitor> {
		
		
		private static final ICommonAstVisitor NONE= new ICommonAstVisitor() {
			@Override
			public void visit(final IAstNode node) throws InvocationTargetException {
			}
		};
		
		
		private final Map<String, ? extends NodeFoldingProvider> providers;
		
		
		public VisitorMap(final Map<String, ? extends NodeFoldingProvider> embeddedProviders) {
			super(embeddedProviders.size() + 4);
			this.providers= embeddedProviders;
		}
		
		
		public ICommonAstVisitor getOrCreate(final String type,
				final FoldingStructureComputationContext context) {
			ICommonAstVisitor visitor= super.get(type);
			if (visitor == null) {
				final NodeFoldingProvider provider= this.providers.get(type);
				if (provider != null) {
					visitor= provider.createVisitor(context);
				}
				if (visitor == null) {
					visitor= NONE;
				}
				put(type, visitor);
			}
			return (visitor != NONE) ? visitor : null;
		}
		
		@Override
		public ICommonAstVisitor get(final Object key) {
			final ICommonAstVisitor visitor= super.get(key);
			return (visitor != NONE) ? visitor : null;
		}
		
		
	}
	
	
	ICommonAstVisitor createVisitor(FoldingStructureComputationContext context);
	
}
