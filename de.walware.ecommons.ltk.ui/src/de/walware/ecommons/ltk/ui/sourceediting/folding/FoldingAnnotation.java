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

import org.eclipse.jface.text.source.projection.ProjectionAnnotation;


public final class FoldingAnnotation extends ProjectionAnnotation {
	
	
	static final int EXPANDED_STATE= 1;
	static final int COLLAPSED_STATE= 2;
	
	
	private final String type;
	
	private int initialState;
	
	private final AbstractFoldingPosition<?> position;
	
	
	public FoldingAnnotation(final String type, final boolean collapse,
			final AbstractFoldingPosition<?> position) {
		super(collapse);
		
		this.type= type;
		this.initialState= (collapse) ? COLLAPSED_STATE : EXPANDED_STATE;
		this.position= position;
	}
	
	
	int getInitialState() {
		return this.initialState;
	}
	
	int getState() {
		return (isCollapsed()) ? COLLAPSED_STATE : EXPANDED_STATE;
	}
	
	void applyState(final int state) {
		switch (state) {
		case EXPANDED_STATE:
			markExpanded();
			break;
		case COLLAPSED_STATE:
			markCollapsed();
			break;
		}
	}
	
	
	public AbstractFoldingPosition<?> getPosition() {
		return this.position;
	}
	
	protected boolean update(final FoldingAnnotation newAnn) {
		if (this.type == newAnn.type && newAnn.getClass() == FoldingAnnotation.class
				&& this.position.getClass() == newAnn.position.getClass()
				&& ((AbstractFoldingPosition) this.position).update(newAnn.position) ) {
			this.initialState= newAnn.initialState;
			return true;
		}
		return false;
	}
	
}
