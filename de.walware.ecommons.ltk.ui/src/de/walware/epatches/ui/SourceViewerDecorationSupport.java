/*******************************************************************************
 * Copyright (c) 2000-2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package de.walware.epatches.ui;

import org.eclipse.jface.text.source.AnnotationPainter;
import org.eclipse.jface.text.source.AnnotationPainter.ITextStyleStrategy;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.internal.ui.OverwriteTextStyleStrategy;


/**
 * Source viewer decoration support with additional overwrite annotation type.
 */
public class SourceViewerDecorationSupport extends org.eclipse.ui.texteditor.SourceViewerDecorationSupport {
	
	
	private static ITextStyleStrategy fgOverwriteStrategy = new OverwriteTextStyleStrategy();
	
	
	/**
	 * Creates a new decoration support for the given viewer.
	 *
	 * @param sourceViewer the source viewer
	 * @param overviewRuler the viewer's overview ruler
	 * @param annotationAccess the annotation access
	 * @param sharedTextColors the shared text color manager
	 */
	public SourceViewerDecorationSupport(final ISourceViewer sourceViewer, final IOverviewRuler overviewRuler, final IAnnotationAccess annotationAccess, final ISharedTextColors sharedTextColors) {
		super(sourceViewer, overviewRuler, annotationAccess, sharedTextColors);
	}
	
	@Override
	protected AnnotationPainter createAnnotationPainter() {
		final AnnotationPainter painter = super.createAnnotationPainter();
		
		painter.addTextStyleStrategy("de.walware.overwrite", fgOverwriteStrategy);
		painter.addAnnotationType("de.walware.ecommons.text.editorAnnotations.ContentAssistOverwrite", "de.walware.overwrite");
		final Color color = SharedUIResources.getColors().getColor(new RGB(255, 0, 0));
		painter.setAnnotationTypeColor("de.walware.ecommons.text.editorAnnotations.ContentAssistOverwrite", color);
		
		return painter;
	}
	
}
