/*******************************************************************************
 * Copyright (c) 2007-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.IUpdate;


/**
 * Action to restore the last structure selection.
 * 
 * @see StructureSelectHandler
 */
public class StructureSelectionHistoryBackHandler extends AbstractHandler implements IUpdate {
	
	
	private final ISourceEditor fSourceEditor;
	private final StructureSelectionHistory fHistory;
	
	
	public StructureSelectionHistoryBackHandler(final ISourceEditor editor, final StructureSelectionHistory history) {
		super();
		assert (editor != null);
		assert (history != null);
		fSourceEditor = editor;
		fHistory = history;
		update();
	}
	
	
	@Override
	public void update() {
		setBaseEnabled(!fHistory.isEmpty());
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IRegion old = fHistory.getLast();
		if (old != null) {
			try {
				fHistory.ignoreSelectionChanges();
				fSourceEditor.selectAndReveal(old.getOffset(), old.getLength());
			}
			finally {
				fHistory.listenToSelectionChanges();
			}
		}
		return null;
	}
	
}
