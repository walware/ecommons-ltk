/*=============================================================================#
 # Copyright (c) 2010-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.text.core.util.TextUtils;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.sourceediting.assist.AssistInvocationContext;
import de.walware.ecommons.ltk.ui.sourceediting.assist.IInfoHover;


public abstract class EditorInformationProvider
		implements IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2 {
	
	
	private final ISourceEditor editor;
	
	private final IInfoHover[] hovers;
	
	private IInfoHover bestHover;
	
	
	public EditorInformationProvider(final ISourceEditor editor, final IInfoHover[] hovers) {
		this.editor = editor;
		this.hovers = hovers;
	}
	
	
	public ISourceEditor getEditor() {
		return this.editor;
	}
	
	@Override
	public String getInformation(final ITextViewer textViewer, final IRegion region) {
		return null;
	}
	
	@Override
	public Object getInformation2(final ITextViewer textViewer, final IRegion region) {
		this.bestHover = null;
		final SubMonitor progress = SubMonitor.convert(null);
		try {
			final String contentType= (region instanceof TypedRegion) ?
					((TypedRegion) region).getType() :
					TextUtils.getContentType(this.editor.getViewer().getDocument(),
							this.editor.getDocumentContentInfo(), region.getOffset(),
							region.getLength() == 0 );
			
			final AssistInvocationContext context = createContext(region, contentType, progress);
			if (context != null) {
				for (int i = 0; i < this.hovers.length; i++) {
					final Object info = this.hovers[i].getHoverInfo(context);
					if (info != null) {
						this.bestHover = this.hovers[i];
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
		if (this.bestHover != null) {
			return this.bestHover.getHoverControlCreator();
		}
		return null;
	}
	
	protected abstract AssistInvocationContext createContext(IRegion region, String contentType,
			IProgressMonitor monitor );
	
}
