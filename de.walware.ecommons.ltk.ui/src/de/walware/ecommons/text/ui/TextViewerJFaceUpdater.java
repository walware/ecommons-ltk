/*******************************************************************************
 * Copyright (c) 2000-2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Wahlbrink
 *******************************************************************************/

// ORG: org.eclipse.jdt.internal.ui.preferences.JavaSourcePreviewerUpdater

package de.walware.ecommons.text.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;

import de.walware.ecommons.ui.util.UIAccess;


/**
 * Handles editor font and properties changes for source viewers.
 * <p>
 * It disposes itself automatically.</p>
 */
public class TextViewerJFaceUpdater {
	
	
	protected final ISourceViewer fViewer;
	protected final IPreferenceStore fPreferenceStore;
	
	private IPropertyChangeListener fFontChangeListener;
	
	private final String fSymbolicFontName;
	
	
	/**
	 * Creates a source preview updater for the given viewer and preference store.
	 * 
	 * @param viewer the viewer
	 * @param preferenceStore the preference store
	 */
	public TextViewerJFaceUpdater(final ISourceViewer viewer, final IPreferenceStore preferenceStore) {
		this(viewer, preferenceStore, JFaceResources.TEXT_FONT);
	}
	
	public TextViewerJFaceUpdater(final ISourceViewer viewer, final IPreferenceStore preferenceStore,
			final String symbolicFontName) {
		assert (viewer != null);
		assert (preferenceStore != null);
		assert (symbolicFontName != null);
		
		fViewer = viewer;
		fPreferenceStore = preferenceStore;
		
		fSymbolicFontName = symbolicFontName;
		
		viewer.getTextWidget().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				dispose();
			}
		});
		
		fFontChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				if (fSymbolicFontName.equals(event.getProperty())) {
					updateFont();
				}
			}
		};
		JFaceResources.getFontRegistry().addListener(fFontChangeListener);
		
		updateFont();
	}
	
	
	protected void updateFont() {
		final Font font = JFaceResources.getFont(fSymbolicFontName);
		final StyledText styledText = fViewer.getTextWidget();
		if (UIAccess.isOkToUse(styledText) && font != null) {
			fViewer.getTextWidget().setFont(font);
		}
	}
	
	
	public final void dispose() {
		if (fFontChangeListener != null) {
			JFaceResources.getFontRegistry().removeListener(fFontChangeListener);
		}
		fFontChangeListener = null;
	}
	
}
