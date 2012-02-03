/*******************************************************************************
 * Copyright (c) 2000-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial implementation for AbstractTextEditor
 *     Stephan Wahlbrink - separate API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.ui.texteditor.ITextEditorExtension3.InsertMode;


public class TextViewerCustomCaretSupport {
	
	
	/**
	 * The caret width for the wide (double) caret.
	 * Value: {@value}
	 */
	private static final int WIDE_CARET_WIDTH = 2;
	
	/**
	 * The caret width for the narrow (single) caret.
	 * Value: {@value}
	 */
	private static final int SINGLE_CARET_WIDTH = 1;
	
	
	private class ToggleOverwriteHandler extends AbstractHandler {
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			toggleOverwriteMode();
			return null;
		}
		
	}
	
	
	private final TextViewer fTextViewer;
	
	private final IPreferenceStore fPreferenceStore;
	
	private final IPropertyChangeListener fPreferencePropertyListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent event) {
			final String property = event.getProperty();
			if (AbstractTextEditor.PREFERENCE_USE_CUSTOM_CARETS.equals(property)
					|| AbstractTextEditor.PREFERENCE_WIDE_CARET.equals(property) ) {
				updateCaret();
			}
		}
	};
	
	private final IPropertyChangeListener fFontPropertyListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent event) {
			final String property = event.getProperty();
			if (JFaceResources.TEXT_FONT.equals(property) ) {
				updateCaret();
			}
		}
	};
	
	/**
	 * Whether the overwrite mode is currently on.
	 */
	private boolean fIsOverwriting = false;
	
	/**
	 * The non-default caret.
	 */
	private Caret fNonDefaultCaret;
	
	/**
	 * The image used in non-default caret.
	 */
	private Image fNonDefaultCaretImage;
	
	/**
	 * The styled text's initial caret.
	 */
	private Caret fInitialCaret;
	
	
	public TextViewerCustomCaretSupport(final TextViewer textViewer, final IPreferenceStore preferences) {
		if (textViewer == null || preferences == null) {
			throw new NullPointerException();
		}
		fTextViewer = textViewer;
		fPreferenceStore = preferences;
		
		fTextViewer.getTextWidget().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				dispose();
			}
		});
		
		fPreferenceStore.addPropertyChangeListener(fPreferencePropertyListener);
		JFaceResources.getFontRegistry().addListener(fFontPropertyListener);
		
		updateCaret();
	}
	
	
	public void initActions(final IHandlerService handlerService) {
		fTextViewer.getTextWidget().setKeyBinding(SWT.INSERT, SWT.NULL);
		handlerService.activateHandler(ITextEditorActionDefinitionIds.TOGGLE_OVERWRITE,
				new ToggleOverwriteHandler());
	}
	
	private void toggleOverwriteMode() {
		if (isOverwriteEnabled()) {
			fIsOverwriting = !fIsOverwriting;
			fTextViewer.getTextWidget().invokeAction(ST.TOGGLE_OVERWRITE);
			updateCaret();
		}
	}
	
	private boolean isOverwriteEnabled() {
		return true;
	}
	
	private int getCaretWidthPreference() {
		return (fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_WIDE_CARET)) ?
				WIDE_CARET_WIDTH : SINGLE_CARET_WIDTH;
	}
	
	private void updateCaret() {
		if (fTextViewer == null) {
			return;
		}
		final StyledText styledText = fTextViewer.getTextWidget();
		
		final InsertMode mode = ITextEditorExtension3.SMART_INSERT;
		
		styledText.setCaret(null);
		disposeNonDefaultCaret();
		
		if (!fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_USE_CUSTOM_CARETS)) {
			assert (fNonDefaultCaret == null);
		}
		else if (fIsOverwriting) {
			fNonDefaultCaret = createOverwriteCaret(styledText);
		}
		else if (mode == ITextEditorExtension3.SMART_INSERT) {
			fNonDefaultCaret = createInsertCaret(styledText);
		}
		else if (mode == ITextEditorExtension3.INSERT) {
			fNonDefaultCaret = createRawInsertModeCaret(styledText);
		}
		
		if (fNonDefaultCaret != null) {
			styledText.setCaret(fNonDefaultCaret);
			fNonDefaultCaretImage= fNonDefaultCaret.getImage();
		}
		else if (fInitialCaret != styledText.getCaret()) {
			styledText.setCaret(fInitialCaret);
		}
	}
	
	private Caret createInsertCaret(final StyledText styledText) {
		final Caret caret = new Caret(styledText, SWT.NULL);
		
		caret.setSize(getCaretWidthPreference(), styledText.getLineHeight());
		caret.setFont(styledText.getFont());
		
		return caret;
	}
	
	private Caret createRawInsertModeCaret(final StyledText styledText) {
//		// don't draw special raw caret if no smart mode is enabled
//		if (!getLegalInsertModes().contains(SMART_INSERT)) {
//			return createInsertCaret(styledText);
//		}
		final Caret caret = new Caret(styledText, SWT.NULL);
		final Image image = createRawInsertModeCaretImage(styledText);
		if (image != null) {
			caret.setImage(image);
		}
		else {
			caret.setSize(getCaretWidthPreference(), styledText.getLineHeight());
		}
		
		caret.setFont(styledText.getFont());
		
		return caret;
	}
	
	private Image createRawInsertModeCaretImage(final StyledText styledText) {
		final PaletteData caretPalette = new PaletteData(new RGB[] {new RGB (0,0,0), new RGB (255,255,255)});
		final int width = getCaretWidthPreference();
		final int widthOffset = width - 1;
		
		final ImageData imageData= new ImageData(4 + widthOffset, styledText.getLineHeight(), 1, caretPalette);
		
		final Display display = styledText.getDisplay();
		final Image bracketImage = new Image(display, imageData);
		final GC gc = new GC (bracketImage);
		gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gc.setLineWidth(0); // NOTE: 0 means width is 1 but with optimized performance
		final int height = imageData.height / 3;
		for (int i = 0; i < width ; i++) {
			gc.drawLine(i, 0, i, height - 1);
			gc.drawLine(i, imageData.height - height, i, imageData.height - 1);
		}
		
		gc.dispose();
		
		return bracketImage;
	}
	
	private Caret createOverwriteCaret(final StyledText styledText) {
		final Caret caret = new Caret(styledText, SWT.NULL);
		final GC gc = new GC(styledText);
		// this overwrite box is not proportional-font aware
		// take 'a' as a medium sized character
		final Point charSize= gc.stringExtent("a"); //$NON-NLS-1$
		
		caret.setSize(charSize.x, styledText.getLineHeight());
		caret.setFont(styledText.getFont());
		
		gc.dispose();
		
		return caret;
	}
	
	private void disposeNonDefaultCaret() {
		if (fNonDefaultCaretImage != null) {
			fNonDefaultCaretImage.dispose();
			fNonDefaultCaretImage = null;
		}
		
		if (fNonDefaultCaret != null) {
			fNonDefaultCaret.dispose();
			fNonDefaultCaret = null;
		}
	}
	
	private void dispose() {
		fPreferenceStore.removePropertyChangeListener(fPreferencePropertyListener);
		JFaceResources.getFontRegistry().removeListener(fFontPropertyListener);
		disposeNonDefaultCaret();
		fInitialCaret = null;
	}
	
}
