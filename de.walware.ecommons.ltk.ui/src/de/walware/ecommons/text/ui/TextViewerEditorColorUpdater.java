/*******************************************************************************
 * Copyright (c) 2011 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui;

import static org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND;
import static org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT;
import static org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND;
import static org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT;
import static org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND;
import static org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT;
import static org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND;
import static org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import de.walware.ecommons.ui.util.UIAccess;


public class TextViewerEditorColorUpdater {
	
	
	protected final ISourceViewer fViewer;
	protected final IPreferenceStore fPreferenceStore;
	
	private IPropertyChangeListener fListener;
	
	private Color fForegroundColor;
	private Color fBackgroundColor;
	private Color fSelectionForegroundColor;
	private Color fSelectionBackgroundColor;
	
	
	public TextViewerEditorColorUpdater(final SourceViewer viewer, final IPreferenceStore preferenceStore) {
		assert (viewer != null);
		assert (preferenceStore != null);
		
		fViewer = viewer;
		fPreferenceStore = preferenceStore;
		
		viewer.getTextWidget().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				dispose();
			}
		});
		
		fListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				if (PREFERENCE_COLOR_FOREGROUND.equals(event.getProperty())
						|| PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT.equals(event.getProperty())
						|| PREFERENCE_COLOR_BACKGROUND.equals(event.getProperty())
						|| PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(event.getProperty())
						|| PREFERENCE_COLOR_SELECTION_FOREGROUND.equals(event.getProperty())
						|| PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT.equals(event.getProperty())
						|| PREFERENCE_COLOR_SELECTION_BACKGROUND.equals(event.getProperty())
						|| PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT.equals(event.getProperty()) ) {
					updateColors();
				}
			}
		};
		fPreferenceStore.addPropertyChangeListener(fListener);
		
		updateColors();
	}
	
	
	protected void updateColors() {
		final StyledText styledText = fViewer.getTextWidget();
		if (UIAccess.isOkToUse(styledText)) {
			{	// foreground color
				final Color color = fPreferenceStore.getBoolean(
						PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT) ? null :
						createColor(fPreferenceStore, PREFERENCE_COLOR_FOREGROUND,
								styledText.getDisplay() );
				styledText.setForeground(color);
				if (fForegroundColor != null) {
					fForegroundColor.dispose();
				}
				fForegroundColor = color;
			}
			{	// background color
				final Color color = fPreferenceStore.getBoolean(
						PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null :
						createColor(fPreferenceStore, PREFERENCE_COLOR_BACKGROUND,
								styledText.getDisplay() );
				styledText.setBackground(color);
				if (fBackgroundColor != null) {
					fBackgroundColor.dispose();
				}
				fBackgroundColor = color;
			}
			{	// selection foreground color
				final Color color = fPreferenceStore.getBoolean(
						PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT) ? null :
						createColor(fPreferenceStore, PREFERENCE_COLOR_SELECTION_FOREGROUND,
								styledText.getDisplay() );
				styledText.setSelectionForeground(color);
				if (fSelectionForegroundColor != null) {
					fSelectionForegroundColor.dispose();
				}
				fSelectionForegroundColor = color;
			}
			{	// selection background color
				final Color color = fPreferenceStore.getBoolean(
						PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT) ? null :
						createColor(fPreferenceStore, PREFERENCE_COLOR_SELECTION_BACKGROUND,
								styledText.getDisplay() );
				styledText.setSelectionBackground(color);
				if (fSelectionBackgroundColor != null) {
					fSelectionBackgroundColor.dispose();
				}
				fSelectionBackgroundColor = color;
			}
		}
	}
	
	protected Color createColor(final IPreferenceStore store, final String key,
			final Display display) {
		final RGB rgb = PreferenceConverter.getColor(store, key);
		return (rgb != null) ? new Color(display, rgb) : null;
	}
	
	
	protected void dispose() {
		if (fListener != null) {
			fPreferenceStore.removePropertyChangeListener(fListener);
			fListener = null;
		}
		
		final StyledText styledText = fViewer.getTextWidget();
		if (UIAccess.isOkToUse(styledText)) {
			styledText.setForeground(null);
			styledText.setBackground(null);
			styledText.setSelectionForeground(null);
			styledText.setSelectionBackground(null);
		}
		if (fForegroundColor != null) {
			fForegroundColor.dispose();
			fForegroundColor = null;
		}
		if (fBackgroundColor != null) {
			fBackgroundColor.dispose();
			fBackgroundColor = null;
		}
		if (fSelectionForegroundColor != null) {
			fSelectionForegroundColor.dispose();
			fSelectionForegroundColor = null;
		}
		if (fSelectionBackgroundColor != null) {
			fSelectionBackgroundColor.dispose();
			fSelectionBackgroundColor = null;
		}
	}
	
}
