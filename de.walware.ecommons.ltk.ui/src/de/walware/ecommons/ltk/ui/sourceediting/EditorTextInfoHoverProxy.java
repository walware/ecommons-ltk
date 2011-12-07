/*******************************************************************************
 * Copyright (c) 2010-2011 WalWare/StatET-Project (www.walware.de/goto/statet).
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.sourceediting.InfoHoverRegistry.EffectiveHovers;


/**
 * Wraps an LTK {@link IInfoHover} to an editor text hover.
 */
public abstract class EditorTextInfoHoverProxy implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {
	
	
	private final InfoHoverDescriptor fDescriptor;
	
	private IInfoHover fHover;
	
	private final SourceEditorViewerConfiguration fSourceEditorConfig;
	
	
	public EditorTextInfoHoverProxy(final InfoHoverDescriptor descriptor, final 
			SourceEditorViewerConfiguration config) {
		fDescriptor = descriptor;
		fSourceEditorConfig = config;
	}
	
	
	protected ISourceEditor getEditor() {
		return fSourceEditorConfig.getSourceEditor();
	}
	
	protected boolean ensureHover() {
		if (fHover == null) {
			fHover = fDescriptor.createHover();
			if (fHover instanceof CombinedHover) {
				final EffectiveHovers effectiveHovers = fSourceEditorConfig.getEffectiveHovers();
				if (effectiveHovers != null) {
					((CombinedHover) fHover).setHovers(effectiveHovers.getDescriptorsForCombined());
				}
				else {
					fHover = null;
				}
			}
		}
		return (fHover != null);
	}
	
	@Override
	public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
		return null;
	}
	
	@Override
	public String getHoverInfo(final ITextViewer textViewer, final IRegion hoverRegion) {
		return null;
	}
	
	@Override
	public Object getHoverInfo2(final ITextViewer textViewer, final IRegion hoverRegion) {
		if (ensureHover()) {
			try {
				final AssistInvocationContext context = createContext(hoverRegion, new NullProgressMonitor());
				if (context != null) {
					return fHover.getHoverInfo(context);
				}
			}
			catch (final Exception e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
						NLS.bind("An error occurred when preparing the information hover ''{0}'' (mouse).",
								fDescriptor.getName() ), e ));
			}
		}
		return null;
	}
	
	protected abstract AssistInvocationContext createContext(IRegion region, IProgressMonitor monitor);
	
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (ensureHover()) {
			return fHover.getHoverControlCreator();
		}
		return null;
	}
	
}
