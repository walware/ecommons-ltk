/*=============================================================================#
 # Copyright (c) 2005-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;

import de.walware.ecommons.preferences.ui.SettingsUpdater;
import de.walware.ecommons.templates.TemplateVariableProcessor;
import de.walware.ecommons.text.ui.TextViewerEditorColorUpdater;
import de.walware.ecommons.text.ui.TextViewerJFaceUpdater;

import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;
import de.walware.ecommons.ltk.ui.sourceediting.ViewerSourceEditorAdapter;


public abstract class AbstractTemplatePreferencePage extends TemplatePreferencePage {
	
	
	private final SourceEditorViewerConfigurator viewerConfigurator;
	private final TemplateVariableProcessor templateProcessor;
	
	private final SourceEditorViewerConfigurator dialogViewerConfigurator;
	private final TemplateVariableProcessor dialogTemplateProcessor;
	
	
	public AbstractTemplatePreferencePage() {
		this.templateProcessor= new TemplateVariableProcessor();
		this.viewerConfigurator= createSourceViewerConfigurator(this.templateProcessor);
		
		this.dialogTemplateProcessor= new TemplateVariableProcessor();
		this.dialogViewerConfigurator= createSourceViewerConfigurator(this.dialogTemplateProcessor);
	}
	
	
	protected abstract SourceEditorViewerConfigurator createSourceViewerConfigurator(
			TemplateVariableProcessor templateProcessor);
	
	
	@Override
	public void setVisible(final boolean visible) {
		final String title= getTitle();
		super.setVisible(visible);
		if (title != null && !title.isEmpty()) {
			setTitle(title);
		}
	}
	
	@Override
	protected SourceViewer createViewer(final Composite parent) {
		final SourceViewer viewer= new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setEditable(false);	
		viewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		
		final ViewerSourceEditorAdapter adapter= new ViewerSourceEditorAdapter(viewer, null);
		this.viewerConfigurator.setTarget(adapter);
		// updater
		new SettingsUpdater(this.viewerConfigurator, viewer.getControl());
		new TextViewerJFaceUpdater(viewer, 
				this.viewerConfigurator.getSourceViewerConfiguration().getPreferences() );
		new TextViewerEditorColorUpdater(viewer, 
				this.viewerConfigurator.getSourceViewerConfiguration().getPreferences() );
		
		final IDocument document= new Document();
		this.viewerConfigurator.getDocumentSetupParticipant().setup(document);
		viewer.setDocument(document);
		
		return viewer;
	}
	
	@Override
	protected void updateViewerInput() {
		super.updateViewerInput();
		
		final IStructuredSelection selection= (IStructuredSelection) getTableViewer().getSelection();
		
		if (selection.size() == 1) {
			final TemplatePersistenceData data= (TemplatePersistenceData) selection.getFirstElement();
			final Template template= data.getTemplate();
			final TemplateContextType contextType= getContextTypeRegistry().getContextType(template.getContextTypeId());
			this.templateProcessor.setContextType(contextType);
			final AbstractDocument document= (AbstractDocument) getViewer().getDocument();
			configureContext(document, contextType, this.viewerConfigurator);
		}
	}
	
	@Override
	protected Template editTemplate(final Template template, final boolean edit, final boolean isNameModifiable) {
		final de.walware.ecommons.ltk.ui.templates.EditTemplateDialog dialog= new de.walware.ecommons.ltk.ui.templates.EditTemplateDialog(
				getShell(), template, edit,
				de.walware.ecommons.ltk.ui.templates.EditTemplateDialog.EDITOR_TEMPLATE,
				this.dialogViewerConfigurator, this.dialogTemplateProcessor, getContextTypeRegistry()) {
			
			@Override
			protected void configureForContext(final TemplateContextType contextType) {
				super.configureForContext(contextType);
				final SourceViewer sourceViewer= getSourceViewer();
				final AbstractDocument document= (AbstractDocument) sourceViewer.getDocument();
				AbstractTemplatePreferencePage.this.configureContext(document, contextType, getSourceViewerConfigurator());
			}
		};
		if (dialog.open() == Dialog.OK) {
			return dialog.getTemplate();
		}
		return null;
	}
	
	protected abstract void configureContext(AbstractDocument document,
			TemplateContextType contextType, SourceEditorViewerConfigurator configurator);
	
	
	@Override
	protected boolean isShowFormatterSetting() {
		return false;
	}
	
	@Override
	protected String getFormatterPreferenceKey() {
		return null;
	}
	
}
