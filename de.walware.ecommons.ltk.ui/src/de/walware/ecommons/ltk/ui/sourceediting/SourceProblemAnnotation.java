/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;

import de.walware.ecommons.ltk.IProblem;


/**
 * Annotation representing an <code>IProblem</code>.
 */
public class SourceProblemAnnotation extends Annotation implements IAnnotationPresentation, IQuickFixableAnnotation {
	
	public static final String TASK_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.task"; //$NON-NLS-1$
	public static final String SPELLING_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.spelling"; //$NON-NLS-1$
	
	
	public static class PresentationConfig {
		
		private final int level;
		
		private Image image;
		
		
		private PresentationConfig(final String referenceType, final int levelDiff) {
			final AnnotationPreference preference= EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreference(referenceType);
			
			if (levelDiff != Integer.MIN_VALUE) {
				this.level= ((preference != null) ?
								preference.getPresentationLayer() :
								IAnnotationAccessExtension.DEFAULT_LAYER ) +
						levelDiff;
			}
			else {
				this.level= 0;
			}
			
			if (preference != null) {
				final String symbolicImageName= preference.getSymbolicImageName();
				if (symbolicImageName != null) {
					final String imageKey= DefaultMarkerAnnotationAccess.getSharedImageName(preference.getSymbolicImageName());
					if (imageKey != null) {
						this.image= PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
					}
				}
			}
		}
		
		public final int getLevel() {
			return this.level;
		}
		
		public final Image getImage() {
			return this.image;
		}
		
	}
	
	
	public static final PresentationConfig ERROR_CONFIG = new PresentationConfig("org.eclipse.ui.workbench.texteditor.error", +1); //$NON-NLS-1$
	public static final PresentationConfig WARNING_CONFIG = new PresentationConfig("org.eclipse.ui.workbench.texteditor.warning", +1); //$NON-NLS-1$
	public static final PresentationConfig INFO_CONFIG = new PresentationConfig("org.eclipse.ui.workbench.texteditor.info", +1); //$NON-NLS-1$
	
	
	private final IProblem problem;
	
	private boolean isQuickFixable= false;
	private boolean isQuickFixableStateSet= false;
	
	private final PresentationConfig config;
	
	
	public SourceProblemAnnotation(final String type, final IProblem problem, final PresentationConfig config) {
		super(type, false, null);
		this.problem = problem;
		this.config = config;
	}
	
	
	@Override
	public String getText() {
		return this.problem.getMessage();
	}
	
	public IProblem getProblem() {
		return this.problem;
	}
	
	
	@Override
	public int getLayer() {
		return this.config.getLevel();
	}
	
	@Override
	public void paint(final GC gc, final Canvas canvas, final Rectangle bounds) {
		final Image image = this.config.getImage();
		if (image != null) {
			ImageUtilities.drawImage(image, gc, canvas, bounds, SWT.CENTER, SWT.TOP);
		}
	}
	
	
	@Override
	public void setQuickFixable(final boolean state) {
		this.isQuickFixable = state;
		this.isQuickFixableStateSet = true;
	}
	
	@Override
	public boolean isQuickFixableStateSet() {
		return this.isQuickFixableStateSet;
	}
	
	@Override
	public boolean isQuickFixable() {
		return this.isQuickFixable;
	}
	
}
