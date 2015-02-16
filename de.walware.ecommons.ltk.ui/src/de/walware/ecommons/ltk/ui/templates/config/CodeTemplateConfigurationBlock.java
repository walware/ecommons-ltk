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

package de.walware.ecommons.ltk.ui.templates.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.collections.ImCollections;
import de.walware.ecommons.collections.ImList;
import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.preferences.ui.ManagedConfigurationBlock;
import de.walware.ecommons.templates.TemplateVariableProcessor;
import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.components.ButtonGroup;
import de.walware.ecommons.ui.components.DataAdapter;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.ViewerUtil;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.internal.ui.TemplatesMessages;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;
import de.walware.ecommons.ltk.ui.templates.EditTemplateDialog;
import de.walware.ecommons.ltk.ui.templates.TemplatePreview;
import de.walware.ecommons.ltk.ui.templates.config.CodeTemplateConfigurationBlock.TemplateItem;


/**
 * The page to configure the codegenerator templates.
 */
public class CodeTemplateConfigurationBlock extends ManagedConfigurationBlock
		implements ButtonGroup.IActions<TemplateItem>, ButtonGroup.IImportExportActions<TemplateItem> {
	
	
	public static final int ADD_ITEM=                       0b0_00000000_00000001;
	
	public static final int LAZY_LOADING=                   0b0_00000000_00010000;
	
	private static final int DEFAULT_MASK=                  0b0_00001111_00000000;
	public static final int DEFAULT_SINGLE=                 0b0_00000001_00000000;
	public static final int DEFAULT_BY_CATEGORY=            0b0_00000010_00000000;
	
	
	protected static class TemplateItem {
		
		private final TemplateCategory category;
		
		private final ITemplateContribution contrib;
		
		private final TemplatePersistenceData data;
		
		
		public TemplateItem(final TemplateCategory category, final ITemplateContribution contrib,
				final TemplatePersistenceData data) {
			this.category= category;
			this.contrib= contrib;
			this.data= data;
		}
		
		
		public TemplateCategory getCategory() {
			return this.category;
		}
		
		public TemplatePersistenceData getData() {
			return this.data;
		}
		
		
		@Override
		public int hashCode() {
			return this.data.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof TemplateItem)) {
				return false;
			}
			final TemplateItem other= (TemplateItem) obj;
			return (this.contrib == other.contrib && this.category.equals(other.category)
					&& this.data.equals(other.data) );
		}
		
	}
	
	private static boolean equalContent(final Template t1, final Template t2) {
		return (t1.getDescription().equals(t2.getDescription())
				&& t1.getContextTypeId().equals(t2.getContextTypeId())
				&& t1.getPattern().equals(t2.getPattern()) );
	}
	
	
	private class ThisContentProvider implements ITreeContentProvider {
		
		
		public ThisContentProvider() {
		}
		
		
		@Override
		public void dispose() {
		}
		
		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		}
		
		@Override
		public Object[] getElements(final Object inputElement) {
			return (Object[]) inputElement;
		}
		
		@Override
		public Object getParent(final Object element) {
			if (element instanceof TemplateItem) {
				return ((TemplateItem) element).category;
			}
			return null;
		}
		
		@Override
		public boolean hasChildren(final Object element) {
			return (element instanceof TemplateCategory);
		}
		
		@Override
		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof TemplateCategory) {
				final TemplateCategory category= (TemplateCategory) parentElement;
				final List<TemplateItem> items= new ArrayList<>();
				if (category.initNames() || !category.isTemplateLoaded()) {
					BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
						@Override
						public void run() {
							doGetChildren(category, items);
						}
					});
				}
				else {
					doGetChildren(category, items);
				}
				return items.toArray(new TemplateItem[items.size()]);
			}
			return new Object[0];
		}
		
		protected void doGetChildren(final TemplateCategory category, final List<TemplateItem> items) {
			final ITemplateContribution contrib= category.getTemplateContrib(true);
			final List<TemplatePersistenceData> templates= contrib.getTemplates(category.getId());
			if (templates != null) {
				for (final TemplatePersistenceData templateData : templates) {
					final Template contribTemplate= templateData.getTemplate();
					if (getCategoryId(contribTemplate).equals(category.getId())) {
						category.addName(contribTemplate.getName());
						items.add(new TemplateItem(category, contrib, templateData));
					}
				}
			}
		}
		
		public TemplateItem findItemByName(final TemplateCategory category, final String name) {
			final ITemplateContribution contrib= category.getTemplateContrib(true);
			final List<TemplatePersistenceData> templates= contrib.getTemplates(category.getId());
			if (templates != null) {
				for (final TemplatePersistenceData templateData : templates) {
					final Template contribTemplate= templateData.getTemplate();
					if (contribTemplate.getName().equals(name) ) {
						return new TemplateItem(category, contrib, templateData);
					}
				}
			}
			return null;
		}
		
		public boolean containsItem(final TemplateCategory category, final Template template) {
			final TemplateItem item= findItemByName(category, template.getName());
			return (item != null && template.equals(item.data.getTemplate()));
		}
		
		public boolean containsItemContent(final TemplateCategory category, final Template template) {
			final ITemplateContribution contrib= category.getTemplateContrib(true);
			final List<TemplatePersistenceData> templates= contrib.getTemplates(category.getId());
			if (templates != null) {
				for (final TemplatePersistenceData templateData : templates) {
					final Template contribTemplate= templateData.getTemplate();
					if (getCategoryId(contribTemplate).equals(category.getId())
							&& equalContent(contribTemplate, template)) {
						return true;
					}
				}
			}
			return false;
		}
		
	}
	
	private class ThisDataAdapter extends DataAdapter<TemplateItem> {
		
		
		private final Map<String, IObservableValue> defaultValues;
		private final Map<Preference<?>, String> prefs;
		
		
		public ThisDataAdapter(final ITreeContentProvider treeProvider,
				final IObservableValue defaultValue) {
			super(treeProvider, defaultValue);
			this.prefs= null;
			this.defaultValues= null;
		}
		
		public ThisDataAdapter(final ITreeContentProvider treeProvider,
				final Map<Preference<?>, String> prefs) {
			super(treeProvider, null);
			this.prefs= prefs;
			this.defaultValues= new HashMap<>();
		}
		
		@Override
		public boolean isContentItem(final Object element) {
			return (element instanceof TemplateItem);
		}
		
		@Override
		protected ITemplateContribution getContainerFor(final Object element) {
			if (element instanceof TemplateItem) {
				return ((TemplateItem) element).contrib;
			}
			return null;
		}
		
		@Override
		protected Object getDefaultValue(final TemplateItem item) {
			return item.data.getTemplate().getName();
		}
		
		@Override
		protected IObservableValue getDefaultFor(final TemplateItem item) {
			if (this.defaultValues != null) {
				return getDefaultFor(item.category);
			}
			return super.getDefaultFor(item);
		}
		
		protected IObservableValue getDefaultFor(final TemplateCategory category) {
			IObservableValue observableValue= this.defaultValues.get(category.getId());
			if (observableValue == null) {
				final Preference<String> pref= category.getDefaultPref();
				if (pref != null) {
					this.prefs.put(pref, null);
					observableValue= createObservable(pref);
					this.defaultValues.put(category.getId(), observableValue);
				}
			}
			return observableValue;
		}
		
		@Override
		public Object change(final TemplateItem oldItem, final TemplateItem newItem,
				final Object parent, final Object container) {
			final ITemplateContribution contrib= newItem.contrib;
			if (oldItem == null) {
				contrib.add(newItem.category.getId(), newItem.data);
			}
			else {
				changeDefault(oldItem, newItem);
			}
			changeChecked(oldItem, newItem);
			return newItem;
		}
		@Override
		public void delete(final List<? extends Object> elements) {
			deleteDefault(elements);
			for (final Object element : elements) {
				final ITemplateContribution contrib= getContainerFor(element);
				if (contrib != null) {
					contrib.delete(((TemplateItem) element).data);
				}
			}
			deleteChecked(elements);
		}
	}
	
	private class ThisLabelProvider extends LabelProvider {
		
		public ThisLabelProvider() {
		}
		
		@Override
		public Image getImage(final Object element) {
			if (element instanceof TemplateCategory) {
				return getLocalImage(((TemplateCategory) element).getImage());
			}
			if (element instanceof TemplateItem) {
				final TemplateItem item= (TemplateItem) element;
				final Image image= getLocalImage(item.category.getItemImage());
				final IObservableValue defaultValue= CodeTemplateConfigurationBlock.this.dataAdapter.getDefaultFor(item);
				if (defaultValue != null
						&& item.data.getTemplate().getName().equals(defaultValue.getValue()) ) {
					return getLocalImage(new DecorationOverlayIcon(image, new ImageDescriptor[] {
							null, null, null, SharedUIResources.getImages().getDescriptor(SharedUIResources.OVR_DEFAULT_MARKER_IMAGE_ID), null},
							new Point(image.getBounds().width, image.getBounds().height) ));
				}
				return image;
			}
			return null;
		}
		
		@Override
		public String getText(final Object element) {
			if (element instanceof TemplateCategory) {
				return ((TemplateCategory) element).getLabel();
			}
			if (element instanceof TemplateItem) {
				return ((TemplateItem) element).data.getTemplate().getDescription();
			}
			return super.getText(element);
		}
		
	}
	
	
	private final int mode;
	
	private ImList<TemplateCategory> templateCategories;
	
	private ThisDataAdapter dataAdapter;
	private ThisContentProvider contentProvider;
	
	private TreeViewer treeViewer;
	private ButtonGroup<TemplateItem> buttonGroup;
	
	private TemplatePreview preview;
	private String patternViewerConfiguredId= null;
	
	private final TemplateVariableProcessor editTemplateProcessor;
	
	private final Preference<String> defaultPref;
	
	private LocalResourceManager uiResources;
	
	
	public CodeTemplateConfigurationBlock(final String title, final int mode,
			final List<? extends TemplateCategory> categories,
			final Preference<String> defaultPref) throws CoreException {
		this(title, mode, defaultPref);
		
		setCategories(categories);
	}
	
	public CodeTemplateConfigurationBlock(final String title, int mode,
			final Preference<String> defaultPref) throws CoreException {
		super(null, title, null);
		
		if (defaultPref != null) {
			mode|= DEFAULT_SINGLE;
		}
		switch (mode & DEFAULT_MASK) {
		case 0:
			break;
		case DEFAULT_SINGLE:
			if (defaultPref == null) {
				throw new NullPointerException("defaultPref"); //$NON-NLS-1$
			}
			break;
		case DEFAULT_BY_CATEGORY:
			break;
		default:
			throw new IllegalArgumentException("mode");
		}
		this.mode= mode;
		
		this.editTemplateProcessor= new TemplateVariableProcessor();
		
		this.defaultPref= defaultPref;
	}
	
	
	protected String getListLabel() {
		return TemplatesMessages.Config_DocTemplates_label;
	}
	
	protected void setCategories(final List<? extends TemplateCategory> categories) {
		this.templateCategories= ImCollections.<TemplateCategory>toList(categories);
	}
	
	protected List<TemplateCategory> getCategories() {
		return this.templateCategories;
	}
	
	protected TemplateCategory getCategory(final String categoryId) {
		if (categoryId != null) {
			for (final TemplateCategory category : this.templateCategories) {
				if (category.getId().equals(categoryId)) {
					return category;
				}
			}
		}
		return null;
	}
	
	protected String getCategoryId(final Template template) {
		final String name= template.getName();
		final int idx= name.indexOf(':');
		return (idx > 0) ? name.substring(0, idx) : name;
	}
	
	protected List<TemplateItem> getTemplates(final TemplateCategory category) {
		final TemplateItem[] children= (TemplateItem[]) this.contentProvider.getChildren(category);
		return ImCollections.newList(children);
	}
	
	protected List<ITemplateContribution> getTemplateContributions(final boolean activate) {
		final List<ITemplateContribution> list= new ArrayList<>();
		for (final TemplateCategory category : this.templateCategories) {
			final ITemplateContribution contrib= category.getTemplateContrib(activate);
			if (!list.contains(contrib)) {
				list.add(contrib);
			}
		}
		return list;
	}
	
	
	@Override
	public void dispose() {
		super.dispose();
		
		if (this.uiResources != null) {
			this.uiResources.dispose();
		}
	}
	
	
/* GUI ************************************************************************/
	
	@Override
	protected void createBlockArea(final Composite pageComposite) {
		final Map<Preference<?>, String> prefs= new HashMap<>();
		
		if (this.defaultPref != null) {
			prefs.put(this.defaultPref, null);
		}
		
		setupPreferenceManager(prefs);
		
		{	final Label label= new Label(pageComposite, SWT.LEFT);
			label.setText(getListLabel() + ':');
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		
		this.contentProvider= new ThisContentProvider();
		switch (this.mode & DEFAULT_MASK) {
		case DEFAULT_SINGLE:
			this.dataAdapter= new ThisDataAdapter(this.contentProvider,
					createObservable(this.defaultPref) );
			break;
		case DEFAULT_BY_CATEGORY:
			this.dataAdapter= new ThisDataAdapter(this.contentProvider, prefs);
			break;
		default:
			this.dataAdapter= new ThisDataAdapter(this.contentProvider, (IObservableValue) null);
			break;
		}
		
		{	final Composite composite= new Composite(pageComposite, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			composite.setLayout(LayoutUtil.createCompositeGrid(2));
			
			this.treeViewer= createTreeViewer(composite);
			this.treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			this.buttonGroup= new ButtonGroup<>(composite, this, false);
			this.buttonGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			
			if ((this.mode & ADD_ITEM) != 0) {
				this.buttonGroup.addAddButton(null);
				this.buttonGroup.addCopyButton(null);
			}
			this.buttonGroup.addEditButton(null);
			if ((this.mode & ADD_ITEM) != 0) {
				this.buttonGroup.addDeleteButton(null);
			}
			
			if ((this.mode & DEFAULT_MASK) != 0) {
				this.buttonGroup.addSeparator();
				this.buttonGroup.addDefaultButton(null);
			}
			
			this.buttonGroup.addSeparator();
			this.buttonGroup.addImportButton(null);
			this.buttonGroup.addExportButton(null);
		}
		
		this.preview= new TemplatePreview();
		{	final Label label= new Label(pageComposite, SWT.LEFT);
			label.setText(TemplatesMessages.Preview_label + ':');
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		{	final SourceViewer viewer= this.preview.createSourceViewer(pageComposite);
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint= new PixelConverter(viewer.getControl()).convertHeightInCharsToPixels(6);
			viewer.getControl().setLayoutData(gd);
		}
		
		this.buttonGroup.connectTo(this.treeViewer, this.dataAdapter);
		
		this.treeViewer.setInput(this.templateCategories.toArray());
		ViewerUtil.scheduleStandardSelection(this.treeViewer);
		
		this.buttonGroup.refresh();
	}
	
	protected TreeViewer createTreeViewer(final Composite parent) {
		final TreeViewer viewer= new TreeViewer(parent,
				SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
		viewer.setContentProvider(this.contentProvider);
		viewer.setLabelProvider(new ThisLabelProvider());
		
		return viewer;
	}
	
	
	protected void updateSourceViewerInput(final TemplateItem item) {
		if (item != null) {
			final SourceEditorViewerConfigurator configurator;
			final ITemplateCategoryConfiguration categoryConfig= item.category.getConfiguration();
			final String viewerConfigId= categoryConfig.getViewerConfigId(item.data);
			if (viewerConfigId != this.patternViewerConfiguredId) {
				configurator= categoryConfig.createViewerConfiguator(viewerConfigId, item.data,
						this.preview.getTemplateVariableProcessor(), getProject() );
				this.patternViewerConfiguredId= viewerConfigId;
			}
			else {
				configurator= null;
			}
			this.preview.updateSourceViewerInput(item.data.getTemplate(),
					item.category.getContextTypeRegistry(), configurator );
		}
		else {
			this.preview.updateSourceViewerInput(null, null, null);
		}
	}
	
	private Image getLocalImage(final ImageDescriptor imageDescriptor) {
		if (imageDescriptor == null) {
			return SharedUIResources.getImages().get(SharedUIResources.PLACEHOLDER_IMAGE_ID);
		}
		if (this.uiResources == null) {
			this.uiResources= new LocalResourceManager(JFaceResources.getResources());
		}
		return this.uiResources.createImage(imageDescriptor);
	}
	
	
/* Execute Actions ************************************************************/
	
	@Override
	public TemplateItem edit(final int command, TemplateItem item, final Object parent) {
		if (item == null) {
			final TemplateCategory category= (TemplateCategory) parent;
			final ITemplateContribution contrib= category.getTemplateContrib(true);
			if (contrib == null) {
				return null;
			}
			final Template template= new Template("", "", //$NON-NLS-1$ //$NON-NLS-2$
					category.getConfiguration().getDefaultContextTypeId(),
					"", false ); //$NON-NLS-1$
			item= new TemplateItem(category, contrib,
					new TemplatePersistenceData(template, true) );
		}
		
		final ITemplateCategoryConfiguration categoryConfig= item.category.getConfiguration();
		final String viewerConfigId= categoryConfig.getViewerConfigId(item.data);
		final EditTemplateDialog dialog= createEditDialog(
				item.data.getTemplate(), command,
				categoryConfig.createViewerConfiguator(viewerConfigId, item.data,
						this.editTemplateProcessor, getProject() ),
				this.editTemplateProcessor, item.category.getContextTypeRegistry() );
		if (dialog.open() == Window.OK) {
			if ((command & ButtonGroup.ADD_ANY) != 0) {
				final Template template= dialog.getTemplate();
				template.setName(newName(item.category));
				item= new TemplateItem(item.category, item.contrib,
						new TemplatePersistenceData(dialog.getTemplate(), true) );
			}
			else {
				item.data.setTemplate(dialog.getTemplate());
			}
			return item;
		}
		return null;
	}
	
	protected EditTemplateDialog createEditDialog(final Template template, final int command,
			final SourceEditorViewerConfigurator configurator,
			final  TemplateVariableProcessor processor, final ContextTypeRegistry registry) {
		return new EditTemplateDialog(
				getShell(), template, ((command & ButtonGroup.ADD_ANY) != 0),
				EditTemplateDialog.CUSTOM_TEMPLATE, configurator, processor, registry );
	}
	
	private String newName(final TemplateCategory category) {
		String s;
		do {
			s= category.getId() + ":" + System.currentTimeMillis(); //$NON-NLS-1$
		} while (category.hasName(s));
		return s;
	}
	
	@Override
	public void updateState(final IStructuredSelection selection) {
		final TemplateItem item= (selection.size() == 1
						&& this.buttonGroup.getDataAdapter().isContentItem(selection.getFirstElement())) ?
				(TemplateItem) selection.getFirstElement() : null;
		updateSourceViewerInput(item);
	}
	
	@Override
	public void importItems() {
		final FileDialog dialog= new FileDialog(getShell(), SWT.OPEN);
		dialog.setText(TemplatesMessages.Config_Import_title);
		dialog.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
		final String path= dialog.open();
		
		if (path == null) {
			return;
		}
		
		try {
			final TemplateReaderWriter reader= new TemplateReaderWriter();
			final File file= new File(path);
			if (file.exists()) {
				final InputStream input= new BufferedInputStream(new FileInputStream(file));
				try {
					final TemplatePersistenceData[] datas= reader.read(input, null);
					for (int i= 0; i < datas.length; i++) {
						updateTemplate(datas[i]);
					}
				}
				finally {
					try {
						input.close();
					} catch (final IOException x) {}
				}
			}
			
			this.buttonGroup.refresh();
		}
		catch (final FileNotFoundException e) {
			openReadErrorDialog(e);
		}
		catch (final IOException e) {
			openReadErrorDialog(e);
		}
	}
	
	private void updateTemplate(final TemplatePersistenceData data) {
		final Template template= data.getTemplate();
		final TemplateCategory category= getCategory(getCategoryId(template));
		if (category == null) {
			return;
		}
		if (category.initNames()) {
			this.contentProvider.getChildren(category);
		}
		if (data.isUserAdded() && category.hasName(data.getTemplate().getName())) {
			if (this.contentProvider.containsItem(category, data.getTemplate())) {
				return;
			}
			data.getTemplate().setName(newName(category));
		}
		if (data.isUserAdded()
				&& this.contentProvider.containsItemContent(category, data.getTemplate()) ) {
			return;
		}
		
		final ITemplateContribution contribution= category.getTemplateContrib(true);
		if (contribution == null) {
			return;
		}
		contribution.add(category.getId(), data);
	}
	
	@Override
	public void exportItems(final List<? extends Object> items) {
		final TemplatePersistenceData[] array= new TemplatePersistenceData[items.size()];
		for (int i= 0; i < array.length; i++) {
			array[i]= ((TemplateItem) items.get(i)).data;
		}
		doExport(array);
	}
	
	private void doExport(final TemplatePersistenceData[] templates) {
		final FileDialog dialog= new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(NLS.bind(TemplatesMessages.Config_Export_title, String.valueOf(templates.length)));
		dialog.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFileName(TemplatesMessages.Config_Export_filename);
		final String path= dialog.open();
		
		if (path == null) {
			return;
		}
		
		final File file= new File(path);
		
		if (file.isHidden()) {
			MessageDialog.openError(getShell(),
					TemplatesMessages.Config_Export_error_title,
					NLS.bind(TemplatesMessages.Config_Export_error_Hidden_message, file.getAbsolutePath()) );
			return;
		}
		
		if (file.exists() && !file.canWrite()) {
			MessageDialog.openError(getShell(),
					TemplatesMessages.Config_Export_error_title,
					NLS.bind(TemplatesMessages.Config_Export_error_CanNotWrite_message, file.getAbsolutePath()) );
			return;
		}
		
		if (!file.exists() || confirmOverwrite(file)) {
			OutputStream output= null;
			try {
				output= new BufferedOutputStream(new FileOutputStream(file));
				final TemplateReaderWriter writer= new TemplateReaderWriter();
				writer.save(templates, output);
				output.close();
			} catch (final IOException e) {
				if (output != null) {
					try {
						output.close();
					} catch (final IOException e2) {
						// ignore
					}
				}
				openWriteErrorDialog(e);
			}
		}
	}
	
	private boolean confirmOverwrite(final File file) {
		return MessageDialog.openQuestion(getShell(),
				TemplatesMessages.Config_Export_Exists_title,
				NLS.bind(TemplatesMessages.Config_Export_Exists_message, file.getAbsolutePath()) );
	}
	
	
/* IConfigurationBlock ********************************************************/
	
	
	private class DefaultsDialog extends MessageDialog {
		
		
		private Button completeControl;
		private Button deletedControl;
		
		private boolean completely;
		
		
		public DefaultsDialog(final Shell parentShell) {
			super(parentShell, TemplatesMessages.Config_RestoreDefaults_title, null,
					TemplatesMessages.Config_RestoreDefaults_title + ": " + getTitle(), //$NON-NLS-1$
					MessageDialog.QUESTION,
					new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0 );
			setShellStyle(getShellStyle() | SWT.SHEET);
		}
		
		
		@Override
		protected Control createCustomArea(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(LayoutUtil.createCompositeGrid(1));
			{	this.completeControl= new Button(composite, SWT.RADIO);
				final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, false);
				gd.horizontalIndent= LayoutUtil.defaultIndent();
				this.completeControl.setLayoutData(gd);
				this.completeControl.setText(TemplatesMessages.Config_RestoreDefaults_Completely_label);
			}
			{	this.deletedControl= new Button(composite, SWT.RADIO);
				final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, false);
				gd.horizontalIndent= LayoutUtil.defaultIndent();
				this.deletedControl.setLayoutData(gd);
				this.deletedControl.setText(TemplatesMessages.Config_RestoreDefaults_Deleted_label);
				
				if (!hasDeleted()) {
					this.deletedControl.setEnabled(false);
				}
			}
			
			this.completeControl.setSelection(true);
			
			return composite;
		}
		
		@Override
		protected void buttonPressed(final int buttonId) {
			this.completely= this.completeControl.getSelection();
			super.buttonPressed(buttonId);
		}
		
	}
	
	@Override
	public void performDefaults() {
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@Override
			public void run() {
				doPerformDefaults();
			}
		});
	}
	
	protected void doPerformDefaults() {
		boolean completly= true;
		if ((this.mode & ADD_ITEM) != 0) {
			final DefaultsDialog dialog= new DefaultsDialog(getShell());
			if (dialog.open() != 0) {
				return;
			}
			completly= dialog.completely;
		}
		final List<ITemplateContribution> templateContributions= getTemplateContributions(true);
		if (completly) {
			for (final ITemplateContribution contrib : templateContributions) {
				if (contrib != null) {
					contrib.restoreDefaults();
				}
			}
			for (final TemplateCategory category : this.templateCategories) {
				if (this.dataAdapter.defaultValues != null) {
					this.dataAdapter.getDefaultFor(category); // force loading
				}
				category.clearNames();
			}
			super.performDefaults();
		}
		else {
			for (final ITemplateContribution contrib : templateContributions) {
				if (contrib != null) {
					contrib.restoreDeleted();
				}
			}
		}
		
		// refresh
		this.buttonGroup.refresh();
	}
	
	private boolean hasDeleted() {
		final List<ITemplateContribution> templateContributions= getTemplateContributions(
				(this.mode & LAZY_LOADING) == 0 );
		for (final ITemplateContribution contrib : templateContributions) {
			if (contrib == null || contrib.hasDeleted()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean performOk() {
		final List<ITemplateContribution> templateContributions= getTemplateContributions(false);
		for (final ITemplateContribution contrib : templateContributions) {
			if (contrib != null) {
				try {
					contrib.saveEdits();
				}
				catch (final IOException e) {
					openWriteErrorDialog(e);
				}
			}
		}
		return super.performOk();
	}
	
	@Override
	public void performCancel() {
		final List<ITemplateContribution> templateContributions= getTemplateContributions(false);
		for (final ITemplateContribution contrib : templateContributions) {
			if (contrib != null) {
				try {
					contrib.revertEdits();
				}
				catch (final IOException e) {
					openReadErrorDialog(e);
				}
			}
		}
		super.performCancel();
	}
	
	
/* Error Dialogs **************************************************************/
	
	private void openReadErrorDialog(final Exception e) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, 0,
				TemplatesMessages.Config_error_Read_message, e), StatusManager.LOG | StatusManager.SHOW);
	}
	
	private void openWriteErrorDialog(final Exception e) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, 0,
				TemplatesMessages.Config_error_Write_message, e), StatusManager.LOG | StatusManager.SHOW);
	}
	
}
