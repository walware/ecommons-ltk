/*=============================================================================#
 # Copyright (c) 2012-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates;

import java.util.List;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import de.walware.ecommons.collections.ImCollections;
import de.walware.ecommons.collections.ImList;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.ViewerUtil;
import de.walware.ecommons.ui.util.ViewerUtil.TableComposite;

import de.walware.ecommons.ltk.internal.ui.TemplatesMessages;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;


public class TemplateSelectionComposite extends Composite implements ISelectionChangedListener {
	
	
	private static final Template NONE= new Template("none", "None", "", "", false); //$NON-NLS-1$
	
	
	private ContextTypeRegistry contextRegistry;
	private ImList<Template> templates;
	
	private SourceEditorViewerConfigurator viewerConfigurator;
	private TableViewer tableViewer;
	private TemplatePreview preview;
	
	private Template selectedTemplate;
	
	
	
	public TemplateSelectionComposite(final Composite parent) {
		super(parent, SWT.NONE);
		
		createControls();
	}
	
	
	public TableViewer getSelectionViewer() {
		return this.tableViewer;
	}
	
	public TemplatePreview getPreview() {
		return this.preview;
	}
	
	public void setConfigurator(final SourceEditorViewerConfigurator patternConfigurator) {
		this.viewerConfigurator= patternConfigurator;
	}
	
	public void setInput(final List<Template> templates, final boolean allowNone,
			final ContextTypeRegistry contextRegistry) {
		this.templates= (allowNone) ?
				ImCollections.addElement(templates, 0, NONE) :
				ImCollections.toList(templates);
		this.contextRegistry= contextRegistry;
		
		this.tableViewer.setInput(this.templates);
	}
	
	public void setSelection(final String name) {
		if (name != null) {
			for (final Template template : this.templates) {
				if (template.getName().equals(name)) {
					setSelection(template);
					return;
				}
			}
		}
		setSelection(NONE);
	}
	
	public void setSelection(final Template template) {
		this.tableViewer.setSelection(new StructuredSelection(template));
		updateSourceViewerInput();
	}
	
	
	protected void createControls() {
		setLayout(LayoutUtil.createCompositeGrid(1));
		
		{	final Control control= createTableViewer(this);
			control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
		this.preview= new TemplatePreview();
		{	final Label label= new Label(this, SWT.LEFT);
			label.setText(TemplatesMessages.Preview_label + ':');
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		{	final SourceViewer viewer= this.preview.createSourceViewer(this);
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint= new PixelConverter(viewer.getControl()).convertHeightInCharsToPixels(5);
			viewer.getControl().setLayoutData(gd);
		}
	}
	
	protected Control createTableViewer(final Composite parent) {
		final TableComposite tableComposite= new ViewerUtil.TableComposite(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableComposite.addColumn("Name", SWT.LEFT, new ColumnWeightData(100));
		
		final TableViewer viewer= tableComposite.viewer;
		viewer.setContentProvider(new ArrayContentProvider());
		configureViewer(viewer);
		
		viewer.addSelectionChangedListener(this);
		
		this.tableViewer= viewer;
		return tableComposite;
	}
	
	protected void configureViewer(final TableViewer viewer) {
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(final Object element) {
				return ((Template) element).getDescription();
			}
		});
	}
	
	protected void updateSourceViewerInput() {
		final Template template= this.selectedTemplate;
		this.preview.updateSourceViewerInput(template, this.contextRegistry, this.viewerConfigurator);
	}
	
	@Override
	public void selectionChanged(final SelectionChangedEvent event) {
		final Template template= (Template) ((IStructuredSelection) event.getSelection()).getFirstElement();
		this.selectedTemplate= (template != NONE) ? template : null;
		
		updateSourceViewerInput();
	}
	
	public Template getSelectedTemplate() {
		return this.selectedTemplate;
	}
	
}
