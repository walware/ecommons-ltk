/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.jface.text.contentassist.ContentAssistant;


/**
 * LTK content assistant.
 */
public class ContentAssist extends ContentAssistant {
	
	
	private boolean isAutoInsertEnabled;
	private boolean isAutoInsertOverwritten;
	
	
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
	
	public void showPossibleCompletions(final boolean restart, final boolean autostart) {
		class AutoAssist extends AutoAssistListener {
			
			public static final int SHOW_PROPOSALS= 1;
			
			@Override
			public void start(final int showStyle) {
				showAssist(showStyle);
			}
			
		}
		
		if (restart) {
			super.hide();
		}
		if (autostart) {
			new AutoAssist().start(AutoAssist.SHOW_PROPOSALS);
		}
		else {
			super.showPossibleCompletions();
		}
	}
	
	@Override
	public void enableAutoInsert(final boolean enabled) {
		this.isAutoInsertEnabled= enabled;
		if (!this.isAutoInsertOverwritten) {
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
		this.isAutoInsertOverwritten= true;
		super.enableAutoInsert(true);
	}
	
	/**
	 * Disables the overwriting of auto insert enabled by {@link #enableAutoInsertTemporarily()}
	 * and resets it to the (user) setting.
	 * 
	 * @see #enableAutoInsert(boolean)
	 */
	void enableAutoInsertSetting() {
		if (this.isAutoInsertOverwritten) {
			this.isAutoInsertOverwritten= false;
			super.enableAutoInsert(this.isAutoInsertEnabled);
		}
	}
	
}
