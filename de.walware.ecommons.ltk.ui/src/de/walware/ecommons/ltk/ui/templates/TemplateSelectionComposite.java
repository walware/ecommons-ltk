/*=============================================================================#
 # Copyright (c) 2012-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.templates;

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

import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.ViewerUtil;
import de.walware.ecommons.ui.util.ViewerUtil.TableComposite;

import de.walware.ecommons.ltk.internal.ui.TemplatesMessages;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;


public class TemplateSelectionComposite extends Composite implements ISelectionChangedListener {
	
	
	private static final Template NONE = new Template("none", "None", "", "", false); //$NON-NLS-1$
	
	
	private ContextTypeRegistry fContextRegistry;
	private Template[] fTemplates;
	
	private SourceEditorViewerConfigurator fViewerConfigurator;
	private TableViewer fTableViewer;
	private TemplatePreview fPreview;
	
	private Template fSelectedTemplate;
	
	
	
	public TemplateSelectionComposite(final Composite parent) {
		super(parent, SWT.NONE);
		
		createControls();
	}
	
	
	public TableViewer getSelectionViewer() {
		return fTableViewer;
	}
	
	public TemplatePreview getPreview() {
		return fPreview;
	}
	
	public void setConfigurator(final SourceEditorViewerConfigurator patternConfigurator) {
		fViewerConfigurator = patternConfigurator;
	}
	
	public void setInput(final Template[] templates, final boolean allowNone,
			final ContextTypeRegistry contextRegistry) {
		fTemplates = (allowNone) ? addNone(templates) : templates;
		fContextRegistry = contextRegistry;
		
		fTableViewer.setInput(fTemplates);
	}
	
	private Template[] addNone(final Template[] templates) {
		final Template[] combined = new Template[templates.length + 1];
		combined[0] = NONE;
		System.arraycopy(templates, 0, combined, 1, templates.length);
		return combined;
	}
	
	public void setSelection(final String name) {
		if (name != null) {
			for (int i = 0; i < fTemplates.length; i++) {
				if (fTemplates[i].getName().equals(name)) {
					setSelection(fTemplates[i]);
					return;
				}
			}
		}
		setSelection(NONE);
	}
	
	public void setSelection(final Template template) {
		fTableViewer.setSelection(new StructuredSelection(template));
		updateSourceViewerInput();
	}
	
	
	protected void createControls() {
		setLayout(LayoutUtil.createCompositeGrid(1));
		
		{	final Control control = createTableViewer(this);
			control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
		fPreview = new TemplatePreview();
		{	final Label label = new Label(this, SWT.LEFT);
			label.setText(TemplatesMessages.Preview_label);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		{	final SourceViewer viewer = fPreview.createSourceViewer(this);
			final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = new PixelConverter(viewer.getControl()).convertHeightInCharsToPixels(5);
			viewer.getControl().setLayoutData(gd);
		}
	}
	
	protected Control createTableViewer(final Composite parent) {
		final TableComposite tableComposite = new ViewerUtil.TableComposite(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableComposite.addColumn("Name", SWT.LEFT, new ColumnWeightData(100));
		
		final TableViewer viewer = tableComposite.viewer;
		viewer.setContentProvider(new ArrayContentProvider());
		configureViewer(viewer);
		
		viewer.addSelectionChangedListener(this);
		
		fTableViewer = viewer;
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
		final Template template = fSelectedTemplate;
		fPreview.updateSourceViewerInput(template, fContextRegistry, fViewerConfigurator);
	}
	
	@Override
	public void selectionChanged(final SelectionChangedEvent event) {
		final Template template = (Template) ((IStructuredSelection) event.getSelection()).getFirstElement();
		fSelectedTemplate = (template != NONE) ? template : null;
		
		updateSourceViewerInput();
	}
	
	public Template getSelectedTemplate() {
		return fSelectedTemplate;
	}
	
}
