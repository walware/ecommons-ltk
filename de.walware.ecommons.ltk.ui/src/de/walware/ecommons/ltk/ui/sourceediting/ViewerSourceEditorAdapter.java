/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.text.PartitioningConfiguration;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.ISourceUnit;


/**
 * Simple {@link ISourceEditor} for snippet editors or previewers.
 */
public class ViewerSourceEditorAdapter implements ISourceEditor {
	
	
	private final SourceViewer fSourceViewer;
	private final SourceEditorViewerConfigurator fConfigurator;
	
	
	/**
	 * Creates a new adapter for the given viewer.
	 * 
	 * @param viewer the viewer
	 * @param configurator a configurator used for {@link ISourceEditorAddon}, may be <code>null</code> (disables modules)
	 */
	public ViewerSourceEditorAdapter(final SourceViewer viewer, final SourceEditorViewerConfigurator configurator) {
		fSourceViewer = viewer;
		fConfigurator = configurator;
	}
	
	
	@Override
	public String getModelTypeId() {
		return null;
	}
	
	@Override
	public ISourceUnit getSourceUnit() {
		return null;
	}
	
	@Override
	public IWorkbenchPart getWorkbenchPart() {
		return null;
	}
	
	@Override
	public IServiceLocator getServiceLocator() {
		return null;
	}
	
	@Override
	public SourceViewer getViewer() {
		return fSourceViewer;
	}
	
	@Override
	public PartitioningConfiguration getPartitioning() {
		return fConfigurator.getPartitioning();
	}
	
	
	@Override
	public ITextEditToolSynchronizer getTextEditToolSynchronizer() {
		return null;
	}
	
	@Override
	public boolean isEditable(final boolean validate) {
		return fSourceViewer.isEditable();
	}
	
	@Override
	public void selectAndReveal(final int offset, final int length) {
		if (UIAccess.isOkToUse(fSourceViewer)) {
			fSourceViewer.setSelectedRange(offset, length);
			fSourceViewer.revealRange(offset, length);
		}
	}
	
	
	@Override
	public Object getAdapter(final Class adapter) {
		return null;
	}
	
}
