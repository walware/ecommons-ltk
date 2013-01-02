/*******************************************************************************
 * Copyright (c) 2005-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;


public class TextHandlerUtil {
	
	
	private static final boolean IS_MAC, IS_GTK, IS_MOTIF;
	static {
		final String platform = SWT.getPlatform();
		IS_MAC = "carbon".equals(platform) || "cocoa".equals(platform);
		IS_GTK = "gtk".equals(platform);
		IS_MOTIF = "motif".equals(platform);
	}
	
	
	public static void disable(final StyledText widget, final String commandId) {
		if (commandId.equals(ITextEditorActionDefinitionIds.DELETE_NEXT)) {
			widget.setKeyBinding(SWT.DEL, SWT.NULL);
			if (IS_MAC) {
				widget.setKeyBinding((SWT.DEL | SWT.MOD2), SWT.NULL);
			}
		}
		else if (commandId.equals(ITextEditorActionDefinitionIds.WORD_NEXT)) {
			if (IS_MAC) {
				widget.setKeyBinding((SWT.MOD3 | SWT.ARROW_RIGHT), SWT.NULL);
			}
			else {
				widget.setKeyBinding((SWT.MOD1 | SWT.ARROW_RIGHT), SWT.NULL);
			}
		}
		else if (commandId.equals(ITextEditorActionDefinitionIds.WORD_PREVIOUS)) {
			if (IS_MAC) {
				widget.setKeyBinding((SWT.MOD3 | SWT.ARROW_LEFT), SWT.NULL);
			}
			else {
				widget.setKeyBinding((SWT.MOD1 | SWT.ARROW_LEFT), SWT.NULL);
			}
		}
		else if (commandId.equals(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT)) {
			if (IS_MAC) {
				widget.setKeyBinding((SWT.MOD2 | SWT.MOD3 | SWT.ARROW_RIGHT), SWT.NULL);
			}
			else {
				widget.setKeyBinding((SWT.MOD1 | SWT.MOD2 | SWT.ARROW_RIGHT), SWT.NULL);
			}
		}
		else if (commandId.equals(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS)) {
			if (IS_MAC) {
				widget.setKeyBinding((SWT.MOD2 | SWT.MOD3 | SWT.ARROW_LEFT), SWT.NULL);
			}
			else {
				widget.setKeyBinding((SWT.MOD1 | SWT.MOD2 | SWT.ARROW_LEFT), SWT.NULL);
			}
		}
		else if (commandId.equals(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD)) {
			widget.setKeyBinding((SWT.MOD1 | SWT.DEL), SWT.NULL);
			if (IS_MAC) {
				widget.setKeyBinding((SWT.MOD3 | SWT.DEL), SWT.NULL);
			}
		}
		else if (commandId.equals(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD)) {
			widget.setKeyBinding((SWT.MOD1 | SWT.BS), SWT.NULL);
			if (IS_MAC) {
				widget.setKeyBinding((SWT.MOD3 | SWT.BS), SWT.NULL);
			}
		}
	}
	
}
