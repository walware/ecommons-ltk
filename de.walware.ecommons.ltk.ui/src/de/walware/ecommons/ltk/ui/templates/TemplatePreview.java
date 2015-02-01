/*=============================================================================#
 # Copyright (c) 2005-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates;

import java.util.Map;
import java.util.Set;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.EditorsUI;

import de.walware.ecommons.preferences.ui.SettingsUpdater;
import de.walware.ecommons.templates.TemplateVariableProcessor;
import de.walware.ecommons.text.ui.TextViewerEditorColorUpdater;
import de.walware.ecommons.text.ui.TextViewerJFaceUpdater;
import de.walware.ecommons.ui.ISettingsChangedHandler;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;
import de.walware.ecommons.ltk.ui.sourceediting.ViewerSourceEditorAdapter;


public class TemplatePreview {
	
	
	private final TemplateVariableProcessor fTemplateProcessor;
	
	private SourceViewer fViewer;
	
	private TextViewerJFaceUpdater fViewerUpdater = null;
	private SourceEditorViewerConfigurator fConfigurator;
	private ISourceEditor fEditor;
	
	
	public TemplatePreview() {
		fTemplateProcessor = new TemplateVariableProcessor();
	}
	
	
	public TemplateVariableProcessor getTemplateVariableProcessor() {
		return fTemplateProcessor;
	}
	
	public SourceViewer createSourceViewer(final Composite parent) {
		final SourceViewer viewer = new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setEditable(false);
		viewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		new TextViewerEditorColorUpdater(viewer, EditorsUI.getPreferenceStore());
		
		final IDocument document = new Document();
		viewer.setDocument(document);
		
		new SettingsUpdater(new ISettingsChangedHandler() {
			@Override
			public void handleSettingsChanged(final Set<String> groupIds, final Map<String, Object> options) {
				if (fConfigurator != null) {
					fConfigurator.handleSettingsChanged(groupIds, options);
				}
			}
		}, viewer.getControl());
		
		return fViewer = viewer;
	}
	
	public void updateSourceViewerInput(final Template template,
			final ContextTypeRegistry contextRegistry,
			final SourceEditorViewerConfigurator patternConfigurator) {
		if (fViewer == null || !(UIAccess.isOkToUse(fViewer.getControl())) ) {
			return;
		}
		
		if (template != null) {
			final TemplateContextType type = contextRegistry.getContextType(template.getContextTypeId());
			fTemplateProcessor.setContextType(type);
			
			if (patternConfigurator != null && patternConfigurator != fConfigurator) {
				if (fViewerUpdater != null) {
					fViewerUpdater.dispose();
					fViewerUpdater = null;
				}
				if (fConfigurator != null) {
					fConfigurator.unconfigureTarget();
					fConfigurator = null;
				}
				
				fConfigurator = patternConfigurator;
				fEditor = new ViewerSourceEditorAdapter(fViewer, fConfigurator);
				fConfigurator.setTarget(fEditor);
				fViewerUpdater = new TextViewerJFaceUpdater(fViewer,
						fConfigurator.getSourceViewerConfiguration().getPreferences() );
				
				final IDocument document = new Document(template.getPattern());
				fConfigurator.getDocumentSetupParticipant().setup(document);
				fViewer.setDocument(document);
			}
			else {
				fViewer.getDocument().set(template.getPattern());
			}
			
		}
		else {
			fViewer.getDocument().set(""); //$NON-NLS-1$
		}
	}
	
}
