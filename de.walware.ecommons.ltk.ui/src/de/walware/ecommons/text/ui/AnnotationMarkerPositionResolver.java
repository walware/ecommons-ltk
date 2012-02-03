/*******************************************************************************
 * Copyright (c) 2011-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;

import de.walware.ecommons.text.IMarkerPositionResolver;


public class AnnotationMarkerPositionResolver implements IMarkerPositionResolver {
	
	
	public static IMarkerPositionResolver createIfRequired(final IResource file) {
		if (file.getType() == IResource.FILE) {
			final ITextFileBuffer buffer = FileBuffers.getTextFileBufferManager()
					.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
			if (buffer != null) {
				final IDocument document = buffer.getDocument();
				final IAnnotationModel model = buffer.getAnnotationModel();
				if (model instanceof AbstractMarkerAnnotationModel) {
					return new AnnotationMarkerPositionResolver(document,
							(AbstractMarkerAnnotationModel) model );
				}
			}
		}
		return null;
	}
	
	
	
	private final IDocument fDocument;
	private final AbstractMarkerAnnotationModel fAnnotationModel;
	
	
	public AnnotationMarkerPositionResolver(final IDocument document,
			final AbstractMarkerAnnotationModel model) {
		fDocument = document;
		fAnnotationModel = model;
	}
	
	
	@Override
	public IDocument getDocument() {
		return fDocument;
	}
	
	@Override
	public Position getPosition(final IMarker marker) {
		synchronized (fAnnotationModel.getLockObject()) {
			return fAnnotationModel.getMarkerPosition(marker);
		}
	}
	
	@Override
	public int getLine(final IMarker marker) {
		final Position position = getPosition(marker);
		if (position != null) {
			try {
				return fDocument.getLineOfOffset(position.getOffset()) + 1;
			}
			catch (final BadLocationException e) {}
		}
		return -1;
	}
	
}
