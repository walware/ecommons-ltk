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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;

import de.walware.ecommons.text.core.sections.DocContentSections;

import de.walware.ecommons.ltk.ui.sourceediting.assist.MultiContentSectionQuickAssistProcessor;


/**
 * Configuration for source viewers supporting multiple document content section types.
 */
public class MultiContentSectionSourceViewerConfiguration extends SourceEditorViewerConfiguration {
	
	
	private final DocContentSections sections;
	
	private SourceEditorViewerConfiguration primaryConfig;
	private final Map<String, SourceEditorViewerConfiguration> secondaryConfigs= new IdentityHashMap<>(8);
	
	
	public MultiContentSectionSourceViewerConfiguration(final DocContentSections sections,
			final ISourceEditor sourceEditor) {
		super(sourceEditor);
		
		this.sections= sections;
	}
	
	
	protected void registerConfig(final String sectionType, final SourceEditorViewerConfiguration config) {
		if (sectionType == null) {
			throw new NullPointerException("sectionType"); //$NON-NLS-1$
		}
		if (sectionType == this.sections.getPrimaryType()) {
			this.primaryConfig= config;
		}
		else {
			this.secondaryConfigs.put(sectionType, config);
		}
	}
	
	protected final SourceEditorViewerConfiguration getConfig(final String sectionType) {
		if (sectionType == this.sections.getPrimaryType()) {
			return this.primaryConfig;
		}
		else {
			return this.secondaryConfigs.get(sectionType);
		}
	}
	
	protected final SourceEditorViewerConfiguration getConfigSafe(final String sectionType) {
		if (sectionType == null || sectionType == this.sections.getPrimaryType()) {
			return this.primaryConfig;
		}
		final SourceEditorViewerConfiguration config= this.secondaryConfigs.get(sectionType);
		return (config != null) ? config : this.primaryConfig;
	}
	
	
	@Override
	public List<ISourceEditorAddon> getAddOns() {
		final List<ISourceEditorAddon> addOns= super.getAddOns();
		
		addOns.addAll(this.primaryConfig.getAddOns());
		for (final SourceEditorViewerConfiguration config : this.secondaryConfigs.values()) {
			addOns.addAll(config.getAddOns());
		}
		
		return addOns;
	}
	
	@Override
	public void handleSettingsChanged(final Set<String> groupIds, final Map<String, Object> options) {
		for (final SourceEditorViewerConfiguration config : this.secondaryConfigs.values()) {
			config.handleSettingsChanged(groupIds, options);
		}
		this.primaryConfig.handleSettingsChanged(groupIds, options);
		
		super.handleSettingsChanged(groupIds, options);
	}
	
	
	@Override
	public String getConfiguredDocumentPartitioning(final ISourceViewer sourceViewer) {
		return this.sections.getPartitioning();
	}
	
	
	@Override
	protected void initPresentationReconciler(final PresentationReconciler reconciler) {
		for (final SourceEditorViewerConfiguration config : this.secondaryConfigs.values()) {
			config.initPresentationReconciler(reconciler);
		}
		this.primaryConfig.initPresentationReconciler(reconciler);
	}
	
	
	@Override
	public int getTabWidth(final ISourceViewer sourceViewer) {
		final SourceEditorViewerConfiguration config= getConfigSafe(null);
		return config.getTabWidth(sourceViewer);
	}
	
	@Override
	public String[] getDefaultPrefixes(final ISourceViewer sourceViewer, final String contentType) {
		final SourceEditorViewerConfiguration config= getConfigSafe(this.sections.getTypeByPartition(contentType));
		return config.getDefaultPrefixes(sourceViewer, contentType);
	}
	
	@Override
	public String[] getIndentPrefixes(final ISourceViewer sourceViewer, final String contentType) {
		final SourceEditorViewerConfiguration config= getConfig(this.sections.getTypeByPartition(contentType));
		if (config != null) {
			return config.getIndentPrefixes(sourceViewer, contentType);
		}
		return null;
	}
	
	
	@Override
	public boolean isSmartInsertSupported() {
		final SourceEditorViewerConfiguration config= getConfigSafe(null);
		return config.isSmartInsertSupported();
	}
	
	@Override
	public boolean isSmartInsertByDefault() {
		final SourceEditorViewerConfiguration config= getConfigSafe(null);
		return config.isSmartInsertByDefault();
	}
	
	
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(final ISourceViewer sourceViewer, final String contentType) {
		final SourceEditorViewerConfiguration config= getConfig(this.sections.getTypeByPartition(contentType));
		if (config != null) {
			return config.getAutoEditStrategies(sourceViewer, contentType);
		}
		return null;
	}
	
	
	@Override
	protected IQuickAssistProcessor createQuickAssistProcessor() {
		return new MultiContentSectionQuickAssistProcessor(this.sections) {
			@Override
			protected IQuickAssistProcessor createProcessor(final String sectionType) {
				final SourceEditorViewerConfiguration config= getConfig(sectionType);
				if (config != null) {
					return config.createQuickAssistProcessor();
				}
				return null;
			}
		};
	}
	
	
	@Override
	public int[] getConfiguredTextHoverStateMasks(final ISourceViewer sourceViewer, final String contentType) {
		final SourceEditorViewerConfiguration config= getConfig(this.sections.getTypeByPartition(contentType));
		if (config != null) {
			return config.getConfiguredTextHoverStateMasks(sourceViewer, contentType);
		}
		return null;
	}
	
	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer, final String contentType, final int stateMask) {
		final SourceEditorViewerConfiguration config= getConfig(this.sections.getTypeByPartition(contentType));
		if (config != null) {
			return config.getTextHover(sourceViewer, contentType, stateMask);
		}
		return null;
	}
	
	@Override
	protected IInformationProvider getInformationProvider() {
		return new MultiContentSectionInformationProvider(this.sections) {
			@Override
			protected EditorInformationProvider createHandler(final String sectionType) {
				final SourceEditorViewerConfiguration config= getConfig(sectionType);
				if (config != null) {
					final IInformationProvider provider= config.getInformationProvider();
					if (provider instanceof EditorInformationProvider) {
						return (EditorInformationProvider) provider;
					}
				}
				return null;
			}
		};
	}
	
	
	@Override
	protected void collectHyperlinkDetectorTargets(final java.util.Map<String, IAdaptable> targets, final ISourceViewer sourceViewer) {
		this.primaryConfig.collectHyperlinkDetectorTargets(targets, sourceViewer);
		for (final SourceEditorViewerConfiguration config : this.secondaryConfigs.values()) {
			config.collectHyperlinkDetectorTargets(targets, sourceViewer);
		}
	}
	
}
