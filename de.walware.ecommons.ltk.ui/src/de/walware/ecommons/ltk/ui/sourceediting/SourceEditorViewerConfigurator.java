/*=============================================================================#
 # Copyright (c) 2007-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import de.walware.ecommons.text.IIndentSettings;
import de.walware.ecommons.text.core.sections.DocContentSections;
import de.walware.ecommons.text.ui.presentation.ITextPresentationConstants;
import de.walware.ecommons.text.ui.settings.DecorationPreferences;
import de.walware.ecommons.ui.ISettingsChangedHandler;


/**
 * Controls the configuration of an {@link ISourceEditor}.
 */
public abstract class SourceEditorViewerConfigurator implements ISettingsChangedHandler,
		PropertyChangeListener {
	
	
	private final SourceEditorViewerConfiguration fConfiguration;
	
	private ISourceEditor fSourceEditor;
	
	private final List<ISourceEditorAddon> fAddons = new ArrayList<>();
	private List<ISourceEditorAddon> fConfigurationAddons;
	
	protected boolean fIsConfigured;
	
	protected boolean fUpdateCompleteConfig;
	protected boolean fUpdateTextPresentation;
	protected boolean fUpdateTabSize;
	protected boolean fUpdateIndent;
	protected boolean fUpdateInfoHovers;
	
	
	protected SourceEditorViewerConfigurator(final SourceEditorViewerConfiguration config) {
		if (config == null) {
			throw new NullPointerException("config");
		}
		fConfiguration = config;
	}
	
	
	/**
	 * A setup participant for the document of the editor.
	 * 
	 * @return a document setup participant or <code>null</code>.
	 */
	public abstract IDocumentSetupParticipant getDocumentSetupParticipant();
	
	public final DocContentSections getDocumentContentInfo() {
		return fConfiguration.getDocumentContentInfo();
	}
	
	protected abstract Set<String> getResetGroupIds();
	
	public SourceEditorViewerConfiguration getSourceViewerConfiguration() {
		return fConfiguration;
	}
	
	public void configureSourceViewerDecorationSupport(final SourceViewerDecorationSupport support) {
		final ICharacterPairMatcher pairMatcher = fConfiguration.getPairMatcher();
		final DecorationPreferences preferences = fConfiguration.getDecorationPreferences();
		if (pairMatcher != null && preferences != null) {
			support.setCharacterPairMatcher(pairMatcher);
			support.setMatchingCharacterPainterPreferenceKeys(
					preferences.getMatchingBracketsEnabled().getKey(),
					preferences.getMatchingBracketsColor().getKey() );
		}
	}
	
	
	public void setTarget(final ISourceEditor sourceEditor) {
		assert (sourceEditor != null);
		fSourceEditor = sourceEditor;
		if (!(fSourceEditor instanceof AbstractDecoratedTextEditor)) {
			fSourceEditor.getViewer().getControl().addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent e) {
					if (fIsConfigured) {
						uninstallCurrentAddons();
					}
				}
			});
			configureTarget();
		}
		else {
			fIsConfigured = true;
			installCurrentAddons();
		}
		handleSettingsChanged(null, null);
	}
	
	protected ISourceViewer getSourceViewer() {
		if (fSourceEditor != null) {
			return fSourceEditor.getViewer();
		}
		return null;
	}
	
	public final void unconfigureTarget() {
		if (fSourceEditor != null) {
			fIsConfigured = false;
			uninstallCurrentAddons();
			fSourceEditor.getViewer().unconfigure();
		}
	}
	
	public final void configureTarget() {
		if (fSourceEditor != null) {
			fIsConfigured = true;
			fSourceEditor.getViewer().configure(fConfiguration);
			installCurrentAddons();
		}
	}
	
	private void installCurrentAddons() {
		fConfigurationAddons = getSourceViewerConfiguration().getAddOns();
		for (final ISourceEditorAddon addon : fConfigurationAddons) {
			addon.install(fSourceEditor);
		}
		for (final ISourceEditorAddon addon : fAddons) {
			addon.install(fSourceEditor);
		}
	}
	
	private void uninstallCurrentAddons() {
		for (final ISourceEditorAddon addon : fAddons) {
			addon.uninstall();
		}
		if (fConfigurationAddons != null) {
			for (final ISourceEditorAddon addon : fConfigurationAddons) {
				addon.uninstall();
			}
			fConfigurationAddons = null;
		}
	}
	
	public final void installAddon(final ISourceEditorAddon installable) {
		fAddons.add(installable);
		if (fIsConfigured) {
			installable.install(fSourceEditor);
		}
	}
	
	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		final String name = event.getPropertyName();
		if (name.equals(IIndentSettings.TAB_SIZE_PROP)) {
			fUpdateTabSize = true;
			fUpdateIndent = true;
			return;
		}
		if (name.equals(IIndentSettings.INDENT_SPACES_COUNT_PROP)
				|| name.equals(IIndentSettings.INDENT_DEFAULT_TYPE_PROP)) {
			fUpdateIndent = true;
			return;
		}
	}
	
	@Override
	public void handleSettingsChanged(Set<String> groupIds, Map<String, Object> options) {
		final SourceViewer viewer;
		if (fSourceEditor == null || (viewer = fSourceEditor.getViewer()) == null) {
			return;
		}
		final Point selectedRange = viewer.getSelectedRange();
		if (groupIds == null) {
			groupIds = getResetGroupIds();
		}
		if (options == null) {
			options = new HashMap<>();
		}
		options.put(ISettingsChangedHandler.VIEWER_KEY, viewer);
		
		checkSettingsChanges(groupIds, options);
		
		if (options.containsKey(ITextPresentationConstants.SETTINGSCHANGE_AFFECTSPRESENTATION_KEY)) {
			fUpdateTextPresentation = true;
		}
		
		updateSourceViewer(viewer);
		viewer.setSelectedRange(selectedRange.x, selectedRange.y);
	}
	
	protected void checkSettingsChanges(final Set<String> groupIds, final Map<String, Object> options) {
		fConfiguration.handleSettingsChanged(groupIds, options);
	}
	
	
	protected void updateSourceViewer(final ISourceViewer viewer) {
		if (!fIsConfigured) {
			return;
		}
		if (fUpdateCompleteConfig) {
			if (fSourceEditor instanceof ITextEditor) {
				SpellingProblem.removeAllInActiveEditor((ITextEditor) fSourceEditor, null);
			}
			reconfigureSourceViewer();
		}
		else {
			if (fUpdateTabSize) {
				viewer.getTextWidget().setTabs(getSourceViewerConfiguration().getTabWidth(viewer));
			}
			if (fUpdateTextPresentation) {
				viewer.invalidateTextPresentation();
			}
			if (fUpdateIndent && fSourceEditor instanceof SourceEditor1) {
				((SourceEditor1) fSourceEditor).updateIndentSettings();
			}
			if (fUpdateInfoHovers) {
				updateConfiguredInfoHovers();
			}
		}
		
		fUpdateCompleteConfig = false;
		fUpdateTextPresentation = false;
		fUpdateTabSize = false;
		fUpdateIndent = false;
	}
	
	private final void reconfigureSourceViewer() {
		if (fIsConfigured) {
			fIsConfigured = false;
			fSourceEditor.getViewer().unconfigure();
			fIsConfigured = true;
			fSourceEditor.getViewer().configure(fConfiguration);
		}
	}
	
	private void updateConfiguredInfoHovers() {
		final SourceViewer viewer = fSourceEditor.getViewer();
		final String[] contentTypes = fConfiguration.getConfiguredContentTypes(viewer);
		for (final String contentType : contentTypes) {
			((ITextViewerExtension2)viewer).removeTextHovers(contentType);
			final int[] stateMasks = fConfiguration.getConfiguredTextHoverStateMasks(viewer, contentType);
			if (stateMasks != null) {
				for (int j = 0; j < stateMasks.length; j++)	{
					final int stateMask = stateMasks[j];
					final ITextHover textHover = fConfiguration.getTextHover(viewer, contentType, stateMask);
					if (textHover != null) {
						viewer.setTextHover(textHover, contentType, stateMask);
					}
				}
			}
			else {
				final ITextHover textHover = fConfiguration.getTextHover(viewer, contentType);
				if (textHover != null) {
					viewer.setTextHover(textHover, contentType, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
				}
			}
		}
	}
	
}
