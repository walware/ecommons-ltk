/*******************************************************************************
 * Copyright (c) 2010-2013 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.sourceediting.assist.AssistInvocationContext;
import de.walware.ecommons.ltk.ui.sourceediting.assist.IInfoHover;


public abstract class EditorInformationProvider
		implements IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2{
	
	
	private final ISourceEditor fEditor;
	
	private final IInfoHover[] fHovers;
	
	private IInfoHover fBestHover;
	
	
	public EditorInformationProvider(final ISourceEditor editor, final IInfoHover[] hovers) {
		fEditor = editor;
		fHovers = hovers;
	}
	
	
	public ISourceEditor getEditor() {
		return fEditor;
	}
	
	@Override
	public String getInformation(final ITextViewer textViewer, final IRegion region) {
		return null;
	}
	
	@Override
	public Object getInformation2(final ITextViewer textViewer, final IRegion region) {
		fBestHover = null;
		final SubMonitor progress = SubMonitor.convert(null);
		try {
			final AssistInvocationContext context = createContext(region, progress);
			if (context != null) {
				for (int i = 0; i < fHovers.length; i++) {
					final Object info = fHovers[i].getHoverInfo(context);
					if (info != null) {
						fBestHover = fHovers[i];
						return info;
					}
				}
			}
		}
		catch (final Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
					"An error occurred when preparing the information hover (command).", e ));
		}
		return null;
	}
	
	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fBestHover != null) {
			return fBestHover.getHoverControlCreator();
		}
		return null;
	}
	
	protected abstract AssistInvocationContext createContext(IRegion region, IProgressMonitor monitor);
	
}
