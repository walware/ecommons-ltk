/*=============================================================================#
 # Copyright (c) 2013-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import de.walware.ecommons.text.core.util.NonDeletingPositionUpdater;


public class SourceEditorViewer extends ProjectionViewer {
	
	
	/**
	 * Text operation code for requesting the outline for the current input.
	 */
	public static final int SHOW_SOURCE_OUTLINE=            51;
	
	/**
	 * Text operation code for requesting the outline for the element at the current position.
	 */
	public static final int SHOW_ELEMENT_OUTLINE=           52;
	
	/**
	 * Text operation code for requesting the hierarchy for the current input.
	 */
	public static final int SHOW_ELEMENT_HIERARCHY=         53;
	
	
	public static final int VARIABLE_LINE_HEIGHT= 0b0_0000_0000_0001_0000;
	
	
	private static final int QUICK_PRESENTER_START = SHOW_SOURCE_OUTLINE;
	private static final int QUICK_PRESENTER_END = SHOW_ELEMENT_HIERARCHY;
	
	
	private final int flags;
	
	private int lastSentSelectionOffset;
	private int lastSentSelectionLength;
	
	private IInformationPresenter sourceOutlinePresenter;
	private IInformationPresenter elementOutlinePresenter;
	private IInformationPresenter elementHierarchyPresenter;
	
	
	public SourceEditorViewer(final Composite parent, final IVerticalRuler ruler,
			final IOverviewRuler overviewRuler, final boolean showsAnnotationOverview, final int styles,
			final int flags) {
		super(parent, ruler, overviewRuler, showsAnnotationOverview, styles);
		this.flags= flags;
		
		addPostSelectionChangedListener(new ISelectionChangedListener() {
			/** 
			 * By default source viewers do not caret changes to selection change listeners, only 
			 * to post selection change listeners.  This sents these post selection changes after
			 * validation to the selection change listeners too.
			 */
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				final ITextSelection selection= (ITextSelection) event.getSelection();
				if (SourceEditorViewer.this.lastSentSelectionOffset != selection.getOffset()
						|| SourceEditorViewer.this.lastSentSelectionLength != selection.getLength()) {
					final Point currentSelection= getSelectedRange();
					if (currentSelection.x == selection.getOffset() && currentSelection.y == selection.getLength()) {
						fireSelectionChanged(currentSelection.x, currentSelection.y);
					}
				}
			}
		});
	}
	
	
	@Override
	protected void fireSelectionChanged(final SelectionChangedEvent event) {
		final ITextSelection selection= (ITextSelection) event.getSelection();
		this.lastSentSelectionOffset= selection.getOffset();
		this.lastSentSelectionLength= selection.getLength();
		
		super.fireSelectionChanged(event);
	}
	
	
	private IInformationPresenter getPresenter(final int operation) {
		switch (operation) {
		case SHOW_SOURCE_OUTLINE:
			return this.sourceOutlinePresenter;
		case SHOW_ELEMENT_OUTLINE:
			return this.elementOutlinePresenter;
		case SHOW_ELEMENT_HIERARCHY:
			return this.elementHierarchyPresenter;
		default:
			return null;
		}
	}
	
	private void setPresenter(final int operation, final IInformationPresenter presenter) {
		switch (operation) {
		case SHOW_SOURCE_OUTLINE:
			this.sourceOutlinePresenter = presenter;
			return;
		case SHOW_ELEMENT_OUTLINE:
			this.elementOutlinePresenter = presenter;
			return;
		case SHOW_ELEMENT_HIERARCHY:
			this.elementHierarchyPresenter = presenter;
			return;
		default:
			if (presenter != null) {
				presenter.uninstall();
			}
			return;
		}
	}
	
	@Override
	public boolean canDoOperation(final int operation) {
		switch (operation) {
		case SHOW_SOURCE_OUTLINE:
		case SHOW_ELEMENT_OUTLINE:
		case SHOW_ELEMENT_HIERARCHY:
			return (getPresenter(operation) != null);
		default:
			return super.canDoOperation(operation);
		}
	}
	
	@Override
	public void doOperation(final int operation) {
		switch (operation) {
		case SHOW_SOURCE_OUTLINE:
		case SHOW_ELEMENT_OUTLINE:
		case SHOW_ELEMENT_HIERARCHY: {
			final IInformationPresenter presenter = getPresenter(operation);
			if (presenter != null) {
				presenter.showInformation();
			}
			return; }
		default:
			super.doOperation(operation);
			return;
		}
	}
	
	@Override
	public void configure(final SourceViewerConfiguration configuration) {
		super.configure(configuration);
		
		if (configuration instanceof SourceEditorViewerConfiguration) {
			for (int operation = QUICK_PRESENTER_START; operation < QUICK_PRESENTER_END; operation++) {
				final IInformationPresenter presenter = ((SourceEditorViewerConfiguration) configuration).getQuickPresenter(this, operation);
				if (presenter != null) {
					presenter.install(this);
				}
				setPresenter(operation, presenter);
			}
		}
	}
	
	@Override
	public void unconfigure() {
		for (int operation = QUICK_PRESENTER_START; operation < QUICK_PRESENTER_END; operation++) {
			final IInformationPresenter presenter = getPresenter(operation);
			if (presenter != null) {
				presenter.uninstall();
				setPresenter(operation, null);
			}
		}
		
		super.unconfigure();
	}
	
	public String[] getDefaultPrefixes(final String contentType) {
		return (this.fDefaultPrefixChars != null) ?
				(String[]) this.fDefaultPrefixChars.get(contentType) :
				null;
	}
	
	
/*[ Workaround for E-Bug 480312 ]==============================================*/
	
	
	private final class ViewerState {
		
		/** The position tracking the selection. */
		private Position selection;
		
		/** The position tracking the visually stable line. */
		private Position stableLine;
		/** The pixel offset of the stable line measured from the client area. */
		private int stablePixel;
		
		/** The position updater for {@link #selection} and {@link #stableLine}. */
		private IPositionUpdater updater;
		/** The document that the position updater and the positions are registered with. */
		private IDocument updaterDocument;
		/** The position category used by {@link #updater}. */
		private String updaterCategory;
		
		private int topPixel;
		
		
		/**
		 * Creates a new viewer state instance and connects it to the current document.
		 */
		public ViewerState() {
			final IDocument document= getDocument();
			if (document != null) {
				connect(document);
			}
		}
		
		
		public void updateSelection(final int offset, final int length) {
			if (this.selection == null) {
				this.selection= new Position(offset, length);
				if (isConnected()) {
					try {
						this.updaterDocument.addPosition(this.updaterCategory, this.selection);
					}
					catch (final BadLocationException | BadPositionCategoryException e) {}
				}
			}
			else {
				updatePosition(this.selection, offset, length);
			}
		}
		
		/**
		 * Updates the viewport, trying to keep the
		 * {@linkplain StyledText#getLinePixel(int) line pixel} of the caret line stable. If the
		 * selection has been updated while in redraw(false) mode, the new selection is revealed.
		 */
		private void updateViewport() {
			final StyledText textWidget= getTextWidget();
			if (this.selection != null) {
				textWidget.setTopPixel(this.topPixel);
				revealRange(this.selection.getOffset(), this.selection.getLength());
			}
			else if (this.stableLine != null) {
				int stableLine;
				try {
					stableLine= this.updaterDocument.getLineOfOffset(this.stableLine.getOffset());
				}
				catch (final BadLocationException x) {
					// ignore and return silently
					textWidget.setTopPixel(this.topPixel);
					return;
				}
				final int stableWidgetLine= getClosestWidgetLineForModelLine(stableLine);
				if (stableWidgetLine == -1) {
					textWidget.setTopPixel(this.topPixel);
					return;
				}
				final int linePixel= textWidget.getLinePixel(stableWidgetLine);
				final int delta= this.stablePixel - linePixel;
				final int topPixel= textWidget.getTopPixel();
				textWidget.setTopPixel(topPixel - delta);
			}
		}
		
		/**
		 * Remembers the viewer state.
		 *
		 * @param document the document to remember the state of
		 */
		private void connect(final IDocument document) {
			Assert.isLegal(document != null);
			Assert.isLegal(!isConnected());
			this.updaterDocument= document;
			try {
				final StyledText textWidget= getTextWidget();
				this.updaterCategory= "ViewerState-" + hashCode();
				this.updater= new NonDeletingPositionUpdater(this.updaterCategory);
				this.updaterDocument.addPositionCategory(this.updaterCategory);
				this.updaterDocument.addPositionUpdater(this.updater);
				
				final int stableLine= getStableLine();
				final int stableWidgetLine= modelLine2WidgetLine(stableLine);
				this.stablePixel= textWidget.getLinePixel(stableWidgetLine);
				final IRegion stableLineInfo= this.updaterDocument.getLineInformation(stableLine);
				this.stableLine= new Position(stableLineInfo.getOffset(), stableLineInfo.getLength());
				this.updaterDocument.addPosition(this.updaterCategory, this.stableLine);
				
				this.topPixel= textWidget.getTopPixel();
			}
			catch (final BadPositionCategoryException e) {
				// cannot happen
				Assert.isTrue(false);
			}
			catch (final BadLocationException e) {
				// should not happen except on concurrent modification
				// ignore and disconnect
				disconnect();
			}
		}
		
		private void updatePosition(final Position position, final int offset, final int length) {
			position.setOffset(offset);
			position.setLength(length);
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=32795
			position.isDeleted= false;
		}
		
		/**
		 * Returns the document line to keep visually stable. If the caret line is (partially)
		 * visible, it is returned, otherwise the topmost (partially) visible line is returned.
		 *
		 * @return the visually stable line of this viewer state
		 */
		private int getStableLine() {
			int stableLine; // the model line that we try to keep stable
			final int caretLine= getTextWidget().getLineAtOffset(getTextWidget().getCaretOffset());
			if (caretLine < JFaceTextUtil.getPartialTopIndex(getTextWidget()) || caretLine > JFaceTextUtil.getPartialBottomIndex(getTextWidget())) {
				stableLine= JFaceTextUtil.getPartialTopIndex(SourceEditorViewer.this);
			}
			else {
				stableLine= widgetLine2ModelLine(caretLine);
			}
			return stableLine;
		}
		
		/**
		 * Returns <code>true</code> if the viewer state is being tracked, <code>false</code>
		 * otherwise.
		 *
		 * @return the tracking state
		 */
		private boolean isConnected() {
			return (this.updater != null);
		}
		
		/**
		 * Disconnects from the document.
		 */
		private void disconnect() {
			if (isConnected()) {
				try {
					this.updaterDocument.removePosition(this.updaterCategory, this.stableLine);
					this.updaterDocument.removePositionUpdater(this.updater);
					this.updater= null;
					this.updaterDocument.removePositionCategory(this.updaterCategory);
					this.updaterCategory= null;
				}
				catch (final BadPositionCategoryException x) {
					// cannot happen
					Assert.isTrue(false);
				}
			}
		}
	}
	
	
	private ViewerState viewerState;
	
	
	@Override
	protected void disableRedrawing() {
		if ((this.flags & VARIABLE_LINE_HEIGHT) != 0) {
			this.viewerState= new ViewerState();
		}
		
		super.disableRedrawing();
	}
	
	@Override
	protected void enabledRedrawing(final int topIndex) {
		super.enabledRedrawing(topIndex);
		
		if (this.viewerState != null) {
			this.viewerState.disconnect();
			if (topIndex == -1) {
				this.viewerState.updateViewport();
			}
			this.viewerState= null;
		}
	}
	
	@Override
	public void setSelectedRange(final int selectionOffset, final int selectionLength) {
		if (this.viewerState != null && !redraws()) {
			this.viewerState.updateSelection(selectionOffset, selectionLength);
			return;
		}
		
		super.setSelectedRange(selectionOffset, selectionLength);
	}
	
}
