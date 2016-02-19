/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.ui;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.internal.ui.EditingMessages;


/**
 * Hyperlink opening an editor for a {@link IFileStore}.
 */
public class OpenFileHyperlink implements IHyperlink {
	
	
	private final IRegion region;
	private final IFileStore file;
	
	
	public OpenFileHyperlink(final IRegion region, final IFileStore file) {
		this.region= region;
		this.file= file;
	}
	
	
	@Override
	public String getTypeLabel() {
		return null;
	}
	
	@Override
	public IRegion getHyperlinkRegion() {
		return this.region;
	}
	
	@Override
	public String getHyperlinkText() {
		return NLS.bind(EditingMessages.Hyperlink_OpenFile_label, this.file.toString());
	}
	
	@Override
	public void open() {
		try {
			IDE.openEditorOnFileStore(UIAccess.getActiveWorkbenchPage(true), this.file);
		}
		catch (final PartInitException e) {
			Display.getCurrent().beep();
			StatusManager.getManager().handle(new Status(IStatus.INFO, SharedUIResources.PLUGIN_ID, -1,
					NLS.bind(EditingMessages.Hyperlink_OpenFile_error_message, this.file.toString()),
					e ));
		}
	}
	
}
