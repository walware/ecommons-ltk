/*******************************************************************************
 * Copyright (c) 2005-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import de.walware.ecommons.FastList;
import de.walware.ecommons.templates.TemplateVariableProcessor;
import de.walware.ecommons.templates.WordFinder;
import de.walware.ecommons.text.PairMatcher;
import de.walware.ecommons.text.ui.DefaultBrowserInformationInput;
import de.walware.ecommons.text.ui.settings.AssistPreferences;
import de.walware.ecommons.text.ui.settings.DecorationPreferences;
import de.walware.ecommons.ui.ColorManager;
import de.walware.ecommons.ui.ISettingsChangedHandler;

import de.walware.ecommons.ltk.ui.sourceediting.InfoHoverRegistry.EffectiveHovers;


/**
 * Abstract configuration for {@link ISourceEditor}s.
 */
public abstract class SourceEditorViewerConfiguration extends TextSourceViewerConfiguration
		implements ISettingsChangedHandler {
	
	
	private static IInformationControlCreator ASSIST_INFO_CREATOR;
	
	
	private static class AssistInformationControlCreator extends AbstractReusableInformationControlCreator {
		
		@Override
		protected IInformationControl doCreateInformationControl(final Shell parent) {
			if (BrowserInformationControl.isAvailable(parent)) {
				return new BrowserInformationControl(parent, JFaceResources.DIALOG_FONT, false) {
					
					@Override
					public void setInformation(String content) {
						if (content.startsWith("...<br")) { // spell correction change proposal //$NON-NLS-1$
							content = content.replace("\\t", "    "); //$NON-NLS-1$ //$NON-NLS-2$
							final StringBuffer s = new StringBuffer(content.length()+1000);
							s.append("<pre>"); //$NON-NLS-1$
							s.append(content);
							s.append("</pre>"); //$NON-NLS-1$
							setInput(new DefaultBrowserInformationInput(null, "", s.toString(),  //$NON-NLS-1$
									DefaultBrowserInformationInput.FORMAT_HTMLBODY_INPUT));
						}
						else {
							setInput(new DefaultBrowserInformationInput(null, "", content, //$NON-NLS-1$
									DefaultBrowserInformationInput.FORMAT_TEXT_INPUT));
						}
					}
				};
			}
			else {
				return new DefaultInformationControl(parent, new HTMLTextPresenter(false));
			}
		}
		
	};
	
	private static final IInformationControlCreator DEFAULT_INFORMATION_CONTROL_CREATOR =
			new IInformationControlCreator() {
				
				@Override
				public IInformationControl createInformationControl(final Shell parent) {
					return new DefaultInformationControl(parent, true);
				}
				
			};
	
	
	private final ISourceEditor fSourceEditor;
	
	private ColorManager fColorManager;
	private final FastList<ISettingsChangedHandler> fSettingsHandler = new FastList<ISettingsChangedHandler>(ISettingsChangedHandler.class);
	private ContentAssistant fContentAssistant;
	private IQuickAssistAssistant fQuickAssistant;
	
	private DecorationPreferences fDecorationPreferences;
	private AssistPreferences fAssistPreferences;
	
	private EffectiveHovers fEffectiveHovers;
	
	
	public SourceEditorViewerConfiguration(final ISourceEditor sourceEditor) {
		fSourceEditor = sourceEditor;
	}
	
	
	protected void setup(final IPreferenceStore preferenceStore, final ColorManager colorManager,
			final DecorationPreferences decoPrefs, final AssistPreferences assistPrefs) {
		assert (preferenceStore != null);
		fPreferenceStore = preferenceStore;
		fColorManager = colorManager;
		fDecorationPreferences = decoPrefs;
		fAssistPreferences = assistPrefs;
	}
	
	/**
	 * Initializes the scanners.
	 */
	protected void setScanners(final org.eclipse.jface.text.rules.ITokenScanner[] scanners) {
		for (int i = 0; i < scanners.length; i++) {
			if (scanners[i] instanceof ISettingsChangedHandler) {
				fSettingsHandler.add((ISettingsChangedHandler) scanners[i]);
			}
		}
	}
	
	protected ISourceEditor getSourceEditor() {
		return fSourceEditor;
	}
	
	public IPreferenceStore getPreferences() {
		return fPreferenceStore;
	}
	
	protected ColorManager getColorManager() {
		return fColorManager;
	}
	
	
	public DecorationPreferences getDecorationPreferences() {
		return fDecorationPreferences;
	}
	
	public AssistPreferences getAssistPreferences() {
		return fAssistPreferences;
	}
	
	
	@Override
	public void handleSettingsChanged(final Set<String> groupIds, final Map<String, Object> options) {
		if (fAssistPreferences != null && groupIds.contains(fAssistPreferences.getGroupId())) {
			if (fContentAssistant != null) {
				fAssistPreferences.configure(fContentAssistant);
			}
			if (fQuickAssistant != null) {
				fAssistPreferences.configure(fQuickAssistant);
			}
		}
		for (final ISettingsChangedHandler handler : fSettingsHandler.toArray()) {
			handler.handleSettingsChanged(groupIds, options);
		}
	}
	public List<ISourceEditorAddon> getAddOns() {
		return new ArrayList<ISourceEditorAddon>();
	}
	
	
	@Override
	public IContentAssistant getContentAssistant(final ISourceViewer sourceViewer) {
		if (fContentAssistant == null) {
			fContentAssistant = createContentAssistant(sourceViewer);
			if (fContentAssistant != null) {
				if (fAssistPreferences != null) {
					fAssistPreferences.configure(fContentAssistant);
				}
				fContentAssistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
				fContentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
				fContentAssistant.setInformationControlCreator(getAssistInformationControlCreator(sourceViewer));
			}
		}
		return fContentAssistant;
	}
	
	protected ContentAssistant createContentAssistant(final ISourceViewer sourceViewer) {
		return null;
	}
	
	
	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(final ISourceViewer sourceViewer) {
		if (fQuickAssistant == null) {
			fQuickAssistant = createQuickAssistant(sourceViewer);
			if (fQuickAssistant != null) {
				if (fAssistPreferences != null) {
					fAssistPreferences.configure(fQuickAssistant);
				}
				fQuickAssistant.setInformationControlCreator(getAssistInformationControlCreator(sourceViewer));
			}
		}
		return fQuickAssistant;
	}
	
	protected IQuickAssistAssistant createQuickAssistant(final ISourceViewer sourceViewer) {
		return super.getQuickAssistAssistant(sourceViewer);
	}
	
	protected IInformationControlCreator getAssistInformationControlCreator(final ISourceViewer sourceViewer) {
		if (ASSIST_INFO_CREATOR == null) {
			ASSIST_INFO_CREATOR = new AssistInformationControlCreator();
		}
		return ASSIST_INFO_CREATOR;
	}
	
	@Override
	public int[] getConfiguredTextHoverStateMasks(final ISourceViewer sourceViewer, final String contentType) {
		if (fSourceEditor != null) {
			final String[] partitioning = getConfiguredContentTypes(sourceViewer);
			if (partitioning != null && partitioning.length > 0 && partitioning[0].equals(contentType)) {
				final InfoHoverRegistry registry = getInfoHoverRegistry();
				if (registry != null) {
					fEffectiveHovers = registry.getEffectiveHoverDescriptors();
					if (fEffectiveHovers != null) {
						return fEffectiveHovers.getStateMasks();
					}
					return null;
				}
			}
		}
		return super.getConfiguredTextHoverStateMasks(sourceViewer, contentType);
	}
	
	public EffectiveHovers getEffectiveHovers() {
		return fEffectiveHovers;
	}
	
	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer, final String contentType) {
		return getTextHover(sourceViewer, contentType, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
	}
	
	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer, final String contentType, final int stateMask) {
		return null;
	}
	
	protected InfoHoverRegistry getInfoHoverRegistry() {
		return null;
	}
	
	
	@Override
	public IInformationPresenter getInformationPresenter(final ISourceViewer sourceViewer) {
		final InformationPresenter presenter = new InformationPresenter(
				DEFAULT_INFORMATION_CONTROL_CREATOR);
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		// Register information provider
		final IInformationProvider provider = getInformationProvider();
		final String[] contentTypes = getConfiguredContentTypes(sourceViewer);
		for (int i= 0; i < contentTypes.length; i++) {
			presenter.setInformationProvider(provider, contentTypes[i]);
		}
		// sizes: see org.eclipse.jface.text.TextViewer.TEXT_HOVER_*_CHARS
		presenter.setSizeConstraints(100, 12, true, true);
		return presenter;
	}
	
	protected IInformationProvider getInformationProvider() {
		return null;
	}
	
/* For TemplateEditors ********************************************************/
	
	protected static class TemplateVariableTextHover implements ITextHover {
		
		private final TemplateVariableProcessor fProcessor;
		
		/**
		 * @param processor the template variable processor
		 */
		public TemplateVariableTextHover(final TemplateVariableProcessor processor) {
			fProcessor = processor;
		}
		
		@Override
		public String getHoverInfo(final ITextViewer textViewer, final IRegion subject) {
			try {
				final IDocument doc= textViewer.getDocument();
				final int offset= subject.getOffset();
				if (offset >= 2 && "${".equals(doc.get(offset-2, 2))) {  //$NON-NLS-1$
					final String varName= doc.get(offset, subject.getLength());
					final TemplateContextType contextType= fProcessor.getContextType();
					if (contextType != null) {
						final Iterator iter= contextType.resolvers();
						while (iter.hasNext()) {
							final TemplateVariableResolver var= (TemplateVariableResolver) iter.next();
							if (varName.equals(var.getType())) {
								return var.getDescription();
							}
						}
					}
				}
			} catch (final BadLocationException e) {
			}
			return null;
		}
		
		@Override
		public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
			if (textViewer != null) {
				return WordFinder.findWord(textViewer.getDocument(), offset);
			}
			return null;
		}
		
	}
	
	protected ContentAssistant createTemplateVariableContentAssistant(final ISourceViewer sourceViewer, final TemplateVariableProcessor processor) {
		final ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		for (final String contentType : getConfiguredContentTypes(sourceViewer)) {
			assistant.setContentAssistProcessor(processor, contentType);
		}
		return assistant;
	}
	
	public boolean isSmartInsertSupported() {
		return false;
	}
	
	public boolean isSmartInsertByDefault() {
		return true;
	}
	
	public PairMatcher getPairMatcher() {
		return null;
	}
	
}
