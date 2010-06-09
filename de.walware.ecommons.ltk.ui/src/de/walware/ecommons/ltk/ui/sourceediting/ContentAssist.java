/*******************************************************************************
 * Copyright (c) 2009-2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.text.contentassist.ContentAssistant;


/**
 * LTK content assistant.
 */
public class ContentAssist extends ContentAssistant {
	
	
	private boolean fIsAutoInsertEnabled;
	private boolean fIsAutoInsertOverwritten;
	
	
	public ContentAssist() {
	}
	
	
	boolean isProposalPopupActive1() {
		return super.isProposalPopupActive();
	}
	
	boolean isContextInfoPopupActive1() {
		return super.isContextInfoPopupActive();
	}
	
	void hidePopups() {
		super.hide();
	}
	
	
	@Override
	public void enableAutoInsert(boolean enabled) {
		fIsAutoInsertEnabled = enabled;
		if (!fIsAutoInsertOverwritten) {
			super.enableAutoInsert(enabled);
		}
	}
	
	/**
	 * Overwrites the current (user) setting temporarily and enables auto insert until it is reset
	 * by calling {@link #enableAutoInsertSetting()}.
	 * 
	 * @see #enableAutoInsert(boolean)
	 */
	void enableAutoInsertTemporarily() {
		fIsAutoInsertOverwritten = true;
		super.enableAutoInsert(true);
	}
	
	/**
	 * Disables the overwriting of auto insert enabled by {@link #enableAutoInsertTemporarily()}
	 * and resets it to the (user) setting.
	 * 
	 * @see #enableAutoInsert(boolean)
	 */
	void enableAutoInsertSetting() {
		if (fIsAutoInsertOverwritten) {
			fIsAutoInsertOverwritten = false;
			super.enableAutoInsert(fIsAutoInsertEnabled);
		}
	}
	
}
