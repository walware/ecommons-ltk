/*******************************************************************************
 * Copyright (c) 2008-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.text.quickassist.IQuickFixableAnnotation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.jface.text.source.ImageUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;

import de.walware.ecommons.ltk.IProblem;


/**
 * Annotation representing an <code>IProblem</code>.
 */
public class SourceProblemAnnotation extends Annotation implements IAnnotationPresentation, IQuickFixableAnnotation {
	
	public static final String TASK_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.task"; //$NON-NLS-1$
	public static final String SPELLING_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.spelling"; //$NON-NLS-1$
	
	
	public static class PresentationConfig {
		
		private final int fLevel;
		private Image fImage;
		
		private final String fImageKey;
		
		
		public PresentationConfig(final int level, final String image) {
			fLevel = level;
			fImageKey = image;
		}
		
		public PresentationConfig(final String referenceType, final int diff) {
			this(referenceType, diff, null);
		}
		
		private PresentationConfig(final String referenceType, final int diff, final String image) {
			final AnnotationPreferenceLookup lookup = EditorsUI.getAnnotationPreferenceLookup();
			final Annotation annotation = new Annotation(referenceType, false, null);
			final AnnotationPreference preference = lookup.getAnnotationPreference(annotation);
			fLevel = (diff != Integer.MIN_VALUE) ? 
					(((preference != null) ? preference.getPresentationLayer() : IAnnotationAccessExtension.DEFAULT_LAYER) + diff) :
					0;
			fImageKey = image;
		}
		
		public final int getLevel() {
			return fLevel;
		}
		
		public final Image getImage() {
			if (fImage == null && fImageKey != null) {
				loadImage();
			}
			return fImage;
		}
		
		
		private synchronized void loadImage() {
			// TODO which registry?
//			fImage = ECommonsUI.getImages().get(fImageKey);
		}
		
	}
	
	public static final PresentationConfig ERROR_CONFIG = new PresentationConfig("org.eclipse.ui.workbench.texteditor.error", +1); //$NON-NLS-1$
	public static final PresentationConfig WARNING_CONFIG = new PresentationConfig("org.eclipse.ui.workbench.texteditor.warning", +1); //$NON-NLS-1$
	public static final PresentationConfig INFO_CONFIG = new PresentationConfig("org.eclipse.ui.workbench.texteditor.info", +1); //$NON-NLS-1$
	
	
	private final IProblem fProblem;
	private boolean fIsQuickFixable = false;
	private boolean fIsQuickFixableStateSet = false;
	
	private final PresentationConfig fConfig;
	
	
	public SourceProblemAnnotation(final String type, final IProblem problem, final PresentationConfig config) {
		super(type, false, null);
		fProblem = problem;
		fConfig = config;
	}
	
	
	@Override
	public String getText() {
		return fProblem.getMessage();
	}
	
	public IProblem getProblem() {
		return fProblem;
	}
	
	
	@Override
	public int getLayer() {
		return fConfig.getLevel();
	}
	
	@Override
	public void paint(final GC gc, final Canvas canvas, final Rectangle bounds) {
		final Image image = fConfig.getImage();
		if (image != null) {
			ImageUtilities.drawImage(image, gc, canvas, bounds, SWT.CENTER, SWT.TOP);
		}
	}
	
	
	@Override
	public void setQuickFixable(final boolean state) {
		fIsQuickFixable = state;
		fIsQuickFixableStateSet = true;
	}
	
	@Override
	public boolean isQuickFixableStateSet() {
		return fIsQuickFixableStateSet;
	}
	
	@Override
	public boolean isQuickFixable() {
		return fIsQuickFixable;
	}
	
}
