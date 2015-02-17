/*=============================================================================#
 # Copyright (c) 2007-2015 Stephan Wahlbrink (WalWare.de) and others.
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.text.core.sections.IDocContentSections;
import de.walware.ecommons.ui.util.DialogUtil;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.sourceediting.assist.ContentAssist;
import de.walware.ecommons.ltk.ui.sourceediting.assist.MultiContentSectionQuickAssistProcessor;


/**
 * Configuration for source viewers supporting multiple document content section types.
 */
public class MultiContentSectionSourceViewerConfiguration extends SourceEditorViewerConfiguration {
	
	
	private class LazyPresentationReconciler extends PresentationReconciler {
		
		
		private IDocument document;
		
		
		public LazyPresentationReconciler() {
		}
		
		
		@Override
		public void setDamager(final IPresentationDamager damager, final String contentType) {
			if (damager != null && this.document != null) {
				damager.setDocument(this.document);
			}
			super.setDamager(damager, contentType);
		}
		
		@Override
		public void setRepairer(final IPresentationRepairer repairer, final String contentType) {
			if (repairer != null && this.document != null) {
				repairer.setDocument(this.document);
			}
			super.setRepairer(repairer, contentType);
		}
		
		@Override
		public IPresentationDamager getDamager(final String contentType) {
			IPresentationDamager damager= super.getDamager(contentType);
			if (damager == null && checkInit(
					MultiContentSectionSourceViewerConfiguration.this.sections.getTypeByPartition(contentType) )) {
				damager= super.getDamager(contentType);
			}
			if (DEBUG && damager == null) {
				StatusManager.getManager().handle(new Status(IStatus.WARNING, LTKUIPlugin.PLUGIN_ID,
						"No presentation damager for contentType= " + contentType + "." ));
			}
			return damager;
		}
		
		@Override
		public IPresentationRepairer getRepairer(final String contentType) {
			IPresentationRepairer repairer= super.getRepairer(contentType);
			if (repairer == null && checkInit(
					MultiContentSectionSourceViewerConfiguration.this.sections.getTypeByPartition(contentType) )) {
				repairer= super.getRepairer(contentType);
			}
			if (DEBUG && repairer == null) {
				StatusManager.getManager().handle(new Status(IStatus.WARNING, LTKUIPlugin.PLUGIN_ID,
						"No presentation repairer for contentType= " + contentType + "." ));
			}
			return repairer;
		}
		
		@Override
		protected void setDocumentToDamagers(final IDocument document) {
			this.document= document;
			super.setDocumentToDamagers(document);
		}
		
	}
	
	private class LazyContentAssist extends ContentAssist {
		
		
		public LazyContentAssist() {
		}
		
		
		@Override
		public IContentAssistProcessor getContentAssistProcessor(final String contentType) {
			IContentAssistProcessor processor= super.getContentAssistProcessor(contentType);
			if (processor == null && checkInit(
					MultiContentSectionSourceViewerConfiguration.this.sections.getTypeByPartition(contentType) )) {
				processor= super.getContentAssistProcessor(contentType);
			}
			return processor;
		}
		
	}
	
	
	private final IDocContentSections sections;
	
	private SourceEditorViewerConfiguration primaryConfig;
	private final Map<String, SourceEditorViewerConfiguration> secondaryConfigs= new IdentityHashMap<>(8);
	private final Map<String, Integer> secondaryConfigStates= new IdentityHashMap<>();
	
	
	public MultiContentSectionSourceViewerConfiguration(final IDocContentSections sections,
			final ISourceEditor sourceEditor) {
		super(sections, sourceEditor);
		
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
	
	
	protected boolean checkInit(final String type) {
		if (type == null) {
			return false;
		}
		
		final SourceEditorViewerConfiguration config= this.secondaryConfigs.get(type);
		if (config != null) {
			final Integer state= this.secondaryConfigStates.put(type, Integer.valueOf(1));
			if (state == null) {
				config.initPresentationReconciler(getPresentationReconciler());
				
				{	final ContentAssist contentAssist= getContentAssist();
					if (contentAssist != null) {
						config.initContentAssist(contentAssist);
					}
				}
				
				return true;
			}
		}
		
		return false;
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
	protected IPresentationReconciler createPresentationReconciler() {
		final PresentationReconciler reconciler= new LazyPresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(null));
		
		initPresentationReconciler(reconciler);
		
		return reconciler;
	}
	
	@Override
	protected void initPresentationReconciler(final PresentationReconciler reconciler) {
		this.primaryConfig.initPresentationReconciler(reconciler);
		// secondary are initialized lazily
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
	protected ContentAssistant createContentAssistant(final ISourceViewer sourceViewer) {
		if (getSourceEditor() != null) {
			final ContentAssist assistant= new LazyContentAssist();
			assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(null));
			assistant.setRestoreCompletionProposalSize(DialogUtil.getDialogSettings(LTKUIPlugin.getDefault(), "ContentAssist.Proposal.size")); //$NON-NLS-1$
			
			initContentAssist(assistant);
			return assistant;
		}
		return null;
	};
	
	@Override
	protected void initContentAssist(final ContentAssist assistant) {
		this.primaryConfig.initContentAssist(assistant);
		
		// secondary are initialized lazily
		if (!this.secondaryConfigStates.isEmpty()) {
			// a secondary type is already initialized?
			for (final String type : this.secondaryConfigStates.keySet()) {
				// this.secondaryConfigStates.get(type) > 0
				this.secondaryConfigs.get(type).initContentAssist(assistant);
			}
		}
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
