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

package de.walware.ecommons.ltk.ui.util;

import org.eclipse.core.resources.IFile;
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
 * Hyperlink opening an editor for a {@link IFile}.
 */
public class OpenWorkspaceFileHyperlink implements IHyperlink {
	
	
	private final IRegion region;
	private final IFile file;
	
	
	public OpenWorkspaceFileHyperlink(final IRegion region, final IFile file) {
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
		return NLS.bind(EditingMessages.Hyperlink_OpenFile2_label,
				this.file.getName(), this.file.getParent().getFullPath() );
	}
	
	@Override
	public void open() {
		try {
			IDE.openEditor(UIAccess.getActiveWorkbenchPage(true), this.file);
		}
		catch (final PartInitException e) {
			Display.getCurrent().beep();
			StatusManager.getManager().handle(new Status(IStatus.INFO, SharedUIResources.PLUGIN_ID, -1,
					NLS.bind(EditingMessages.Hyperlink_OpenFile_error_message, this.file.toString()),
			e ));
		}
	}
	
}
