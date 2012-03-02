/*******************************************************************************
 * Copyright (c) 2005-2011 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.templates;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.preferences.ui.ManagedConfigurationBlock;
import de.walware.ecommons.templates.TemplateVariableProcessor;
import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.components.ButtonGroup;
import de.walware.ecommons.ui.components.DataAdapter;
import de.walware.ecommons.ui.util.LayoutUtil;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.internal.ui.TemplatesMessages;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;
import de.walware.ecommons.ltk.ui.templates.CodeTemplatesConfigurationBlock.TemplateItem;


/**
 * The page to configure the codegenerator templates.
 */
public class CodeTemplatesConfigurationBlock extends ManagedConfigurationBlock
		implements ButtonGroup.IActions<TemplateItem>, ButtonGroup.IImportExportActions<TemplateItem> {
	
	
	public static final String ATT_ID = "id"; //$NON-NLS-1$
	public static final String ATT_NAME = "name"; //$NON-NLS-1$
	public static final String ATT_CLASS = "providerClass"; //$NON-NLS-1$
	
	
	public static class TemplateGroup {
		
		private final String fId;
		
		private final Image fImage;
		private final String fLabel;
		
		private final Image fItemImage;
		
		private final Set<String> fItemNames = new HashSet<String>();
		
		
		public TemplateGroup(final String id,
				final Image groupImage, final String label, final Image itemImage) {
			fId = id;
			
			fImage = groupImage;
			fLabel = label;
			
			fItemImage = itemImage;
		}
		
		
		public String getLabel() {
			return fLabel;
		}
		
		public Image getImage() {
			return fImage;
		}
		
		public Image getItemImage() {
			return fItemImage;
		}
		
	}
	
	protected static class TemplateItem {
		
		private final TemplateGroup fGroup;
		
		private final TemplatePersistenceData fData;
		
		private final ITemplateContribution fContrib;
		
		
		public TemplateItem(final TemplateGroup group, final TemplatePersistenceData data,
				final ITemplateContribution contrib) {
			fGroup = group;
			fData = data;
			fContrib = contrib;
		}
		
		
		@Override
		public int hashCode() {
			return fData.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof TemplateItem)) {
				return false;
			}
			final TemplateItem other = (TemplateItem) obj;
			return (fContrib == other.fContrib && fGroup.equals(other.fGroup)
					&& fData.equals(other.fData) );
		}
		
	}
	
	public static interface ITemplateContribution {
		
		
		List<String> getGroups();
		
		TemplatePersistenceData[] getTemplates(String groupId);
		
		boolean add(TemplatePersistenceData data);
		
		void delete(TemplatePersistenceData data);
		
		boolean hasDeleted();
		
		void restoreDeleted();
		
		void restoreDefaults();
		
		void load() throws IOException;
		
		void save() throws IOException;
		
		ContextTypeRegistry getContextRegistry();
		
		String getDefaultContextTypeId(String groupdId);
		
		String getViewerConfiguraterId(TemplatePersistenceData data);
		
		SourceEditorViewerConfigurator createViewerConfiguator(TemplatePersistenceData data,
				TemplateVariableProcessor templateProcessor, IProject project);
		
		
	}
	
	public abstract static class TemplateStoreContribution implements ITemplateContribution {
		
		
		private final ContextTypeRegistry fContextTypeRegistry;
		private final TemplateStore fTemplateStore;
		
		
		public TemplateStoreContribution(final ContextTypeRegistry contexts, final TemplateStore store) {
			fContextTypeRegistry = contexts;
			fTemplateStore = store;
		}
		
		
		@Override
		public abstract List<String> getGroups();
		
		@Override
		public TemplatePersistenceData[] getTemplates(final String groupId) {
			return fTemplateStore.getTemplateData(false);
		}
		
		@Override
		public boolean add(final TemplatePersistenceData data) {
			fTemplateStore.add(data);
			return true;
		}
		
		@Override
		public void delete(final TemplatePersistenceData data) {
			fTemplateStore.delete(data);
		}
		
		@Override
		public void load() throws IOException {
			fTemplateStore.load();
		}
		
		@Override
		public void save() throws IOException {
			fTemplateStore.save();
		}
		
		@Override
		public boolean hasDeleted() {
			return (fTemplateStore.getTemplateData(true).length != fTemplateStore.getTemplateData(false).length);
		}
		
		@Override
		public void restoreDeleted() {
			fTemplateStore.restoreDeleted();
		}
		
		@Override
		public void restoreDefaults() {
			fTemplateStore.restoreDefaults(false);
		}
		
		@Override
		public ContextTypeRegistry getContextRegistry() {
			return fContextTypeRegistry;
		}
		
		@Override
		public String getDefaultContextTypeId(final String groupdId) {
			return ((TemplateContextType) fContextTypeRegistry.contextTypes().next()).getId();
		}
		
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
				return ((TemplateItem) element).fGroup;
			}
			return null;
		}
		
		@Override
		public boolean hasChildren(final Object element) {
			return (element instanceof TemplateGroup);
		}
		
		@Override
		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof TemplateGroup) {
				final TemplateGroup group = (TemplateGroup) parentElement;
				final List<TemplateItem> items = new ArrayList<TemplateItem>();
				for (final ITemplateContribution contrib : fTemplateContributions) {
					final TemplatePersistenceData[] templates = contrib.getTemplates(group.fId);
					if (templates != null) {
						for (int i = 0; i < templates.length; i++) {
							group.fItemNames.add(templates[i].getTemplate().getName());
							items.add(new TemplateItem(group, templates[i], contrib));
						}
					}
				}
				return items.toArray();
			}
			return new Object[0];
		}
		
	}
	
	private static class ThisDataAdapter extends DataAdapter<TemplateItem> {
		
		
		public ThisDataAdapter(final ITreeContentProvider treeProvider, final IObservableValue defaultValue) {
			super(treeProvider, defaultValue);
		}
		
		@Override
		public boolean isContentItem(final Object element) {
			return (element instanceof TemplateItem);
		}
		
		@Override
		protected ITemplateContribution getContainerFor(final Object element) {
			if (element instanceof TemplateItem) {
				return ((TemplateItem) element).fContrib;
			}
			return null;
		}
		
		@Override
		protected Object getDefaultValue(final TemplateItem item) {
			return item.fData.getTemplate().getName();
		}
		
		@Override
		public Object change(final TemplateItem oldItem, final TemplateItem newItem,
				final Object parent, final Object container) {
			if (container == null) {
				return null;
			}
			final ITemplateContribution contrib = (ITemplateContribution) container;
			if (oldItem == null) {
				contrib.add(newItem.fData);
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
				final ITemplateContribution contrib = getContainerFor(element);
				if (contrib != null) {
					contrib.delete(((TemplateItem) element).fData);
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
			if (element instanceof TemplateGroup) {
				return ((TemplateGroup) element).getImage();
			}
			if (element instanceof TemplateItem) {
				final TemplateItem item = (TemplateItem) element;
				if (fDefaultValue != null && item.fData.getTemplate().getName()
						.equals(fDefaultValue.getValue()) ) {
					return getDefaultImage(((TemplateItem) element).fGroup.getItemImage());
				}
				return ((TemplateItem) element).fGroup.getItemImage();
			}
			return null;
		}
		
		@Override
		public String getText(final Object element) {
			if (element instanceof TemplateGroup) {
				return ((TemplateGroup) element).getLabel();
			}
			if (element instanceof TemplateItem) {
				return ((TemplateItem) element).fData.getTemplate().getDescription();
			}
			return super.getText(element);
		}
		
	}
	
	
	protected IProject fProject;
	
	private final String fTitle;
	
	private final boolean fEnableAdd;
	
	private TemplateGroup[] fTemplateGroups;
	private ITemplateContribution[] fTemplateContributions;
	private ITreeContentProvider fContentProvider;
	
	private TreeViewer fTreeViewer;
	private ButtonGroup<TemplateItem> fButtonGroup;
	
	private TemplatePreview fPreview;
	private String fPatternViewerConfiguredId = null;
	
	private final TemplateVariableProcessor fEditTemplateProcessor;
	
	private Preference<String> fDefaultPref;
	private IObservableValue fDefaultValue;
	private Map<Image, Image> fDefaultImages;
	
	
	
	public CodeTemplatesConfigurationBlock(final String title, final boolean enableAdd,
			final TemplateGroup[] groups, final ITemplateContribution[] contributions,
			final Preference<String> defaultPref) throws CoreException {
		this(title, enableAdd);
		init(groups, contributions);
		fDefaultPref = defaultPref;
	}
	
	public CodeTemplatesConfigurationBlock(final String title, final boolean enableAdd) throws CoreException {
		super(null);
		fTitle = title;
		fEnableAdd = enableAdd;
		
		fEditTemplateProcessor = new TemplateVariableProcessor();
	}
	
	
	protected void init(final TemplateGroup[] groups, final ITemplateContribution[] contributions) {
		fTemplateGroups = groups;
		fTemplateContributions = contributions;
	}
	
	private TemplateGroup getGroup(final String groupId) {
		for (final TemplateGroup group : fTemplateGroups) {
			if (group.fId.equals(groupId)) {
				return group;
			}
		}
		return null;
	}
	
	@Override
	public void dispose() {
		if (fDefaultImages != null) {
			for (final Image image : fDefaultImages.values()) {
				image.dispose();
			}
			fDefaultImages.clear();
			fDefaultImages = null;
		}
		super.dispose();
	}
	
	
/* GUI ************************************************************************/
	
	@Override
	protected void createBlockArea(final Composite pageComposite) {
		final Map<Preference<?>, String> prefs = new HashMap<Preference<?>, String>();
		
		prefs.put(fDefaultPref, null);
		
		setupPreferenceManager(prefs);
		
		{	final Label label = new Label(pageComposite, SWT.LEFT);
			label.setText(fTitle);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		fContentProvider = new ThisContentProvider();
		{	final Composite composite = new Composite(pageComposite, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			composite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 2));
			
			fTreeViewer = createTreeViewer(composite);
			fTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			fButtonGroup = new ButtonGroup<TemplateItem>(composite, this, false);
			fButtonGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			
			if (fEnableAdd) {
				fButtonGroup.addAddButton();
				fButtonGroup.addCopyButton();
			}
			fButtonGroup.addEditButton();
			if (fEnableAdd) {
				fButtonGroup.addDeleteButton();
			}
			
			if (fDefaultPref != null) {
				fButtonGroup.addSeparator();
				fButtonGroup.addDefaultButton();
			}
			
			fButtonGroup.addSeparator();
			fButtonGroup.addImportButton();
			fButtonGroup.addExportButton();
		}
		
		fPreview = new TemplatePreview();
		{	final Label label = new Label(pageComposite, SWT.LEFT);
			label.setText("Preview");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		{	final SourceViewer viewer = fPreview.createSourceViewer(pageComposite);
			final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = new PixelConverter(viewer.getControl()).convertHeightInCharsToPixels(6);
			viewer.getControl().setLayoutData(gd);
		}
		
		fDefaultValue = createObservable(fDefaultPref);
		fButtonGroup.connectTo(fTreeViewer, new ThisDataAdapter(fContentProvider, fDefaultValue));
		
		fTreeViewer.setInput(fTemplateGroups);
		if (fTemplateGroups.length == 1) {
			fTreeViewer.setExpandedState(fTemplateGroups[0], true);
		}
		fButtonGroup.refresh();
	}
	
	protected TreeViewer createTreeViewer(final Composite parent) {
		final TreeViewer viewer = new TreeViewer(parent,
				SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(new ThisLabelProvider());
		
		return viewer;
	}
	
	
	protected void updateSourceViewerInput(final TemplateItem item) {
		if (item != null) {
			final SourceEditorViewerConfigurator configurator;
			final String id = item.fContrib.getViewerConfiguraterId(item.fData);
			if (id != fPatternViewerConfiguredId) {
				configurator = item.fContrib.createViewerConfiguator(item.fData,
						fPreview.getTemplateVariableProcessor(), fProject );
				fPatternViewerConfiguredId = id;
			}
			else {
				configurator = null;
			}
			fPreview.updateSourceViewerInput(item.fData.getTemplate(),
					item.fContrib.getContextRegistry(), configurator );
		}
		else {
			fPreview.updateSourceViewerInput(null, null, null);
		}
	}
	
	private Image getDefaultImage(final Image image) {
		if (fDefaultImages == null) {
			fDefaultImages = new HashMap<Image, Image>();
		}
		Image defaultImage = fDefaultImages.get(image);
		if (defaultImage == null) {
			defaultImage = new DecorationOverlayIcon(image, new ImageDescriptor[] {
					null, null, null, SharedUIResources.getImages().getDescriptor(SharedUIResources.OVR_DEFAULT_MARKER_IMAGE_ID), null},
					new Point(image.getBounds().width, image.getBounds().height)).createImage();
			fDefaultImages.put(image, defaultImage);
		}
		return defaultImage;
	}
	
	
/* Execute Actions ************************************************************/
	
	ITemplateContribution getAddContribution(final TemplateGroup group) {
		return fTemplateContributions[0];
	}
	
	@Override
	public TemplateItem edit(final int command, TemplateItem item, final Object parent) {
		if (item == null) {
			final TemplateGroup group = (TemplateGroup) parent;
			final ITemplateContribution contrib = getAddContribution(group);
			final Template template = new Template("", "", //$NON-NLS-1$ //$NON-NLS-2$
					contrib.getDefaultContextTypeId(group.fId), "", false); //$NON-NLS-1$
			item = new TemplateItem((TemplateGroup) parent,
					new TemplatePersistenceData(template, true), contrib);
		}
		final EditTemplateDialog dialog = new EditTemplateDialog(
				getShell(), item.fData.getTemplate(), ((command & ButtonGroup.ADD_ANY) != 0),
				EditTemplateDialog.CUSTOM_TEMPLATE,
				item.fContrib.createViewerConfiguator(item.fData, fEditTemplateProcessor, fProject),
				fEditTemplateProcessor, item.fContrib.getContextRegistry() );
		if (dialog.open() == Window.OK) {
			if ((command & ButtonGroup.ADD_ANY) != 0) {
				final Template template = dialog.getTemplate();
				template.setName(newName(item.fGroup));
				item = new TemplateItem(item.fGroup,
						new TemplatePersistenceData(dialog.getTemplate(), true), item.fContrib );
			}
			else {
				item.fData.setTemplate(dialog.getTemplate());
			}
			return item;
		}
		return null;
	}
	
	private String newName(final TemplateGroup group) {
		String s;
		do {
			s = group + ":" + System.currentTimeMillis();
		} while (group.fItemNames.contains(s));
		return s;
	}
	
	@Override
	public void updateState(final IStructuredSelection selection) {
		final TemplateItem item = (selection.size() == 1 && fButtonGroup.getDataAdapter().isContentItem(selection.getFirstElement())) ?
				(TemplateItem) selection.getFirstElement() : null;
		updateSourceViewerInput(item);
	}
	
	@Override
	public void importItems() {
		final FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setText(TemplatesMessages.Config_Import_title);
		dialog.setFilterExtensions(new String[] { "*.xml", "*.*" });
		final String path = dialog.open();
		
		if (path == null) {
			return;
		}
		
		try {
			final TemplateReaderWriter reader = new TemplateReaderWriter();
			final File file = new File(path);
			if (file.exists()) {
				final InputStream input = new BufferedInputStream(new FileInputStream(file));
				try {
					final TemplatePersistenceData[] datas = reader.read(input, null);
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
			
			fButtonGroup.refresh();
		}
		catch (final FileNotFoundException e) {
			openReadErrorDialog(e);
		}
		catch (final IOException e) {
			openReadErrorDialog(e);
		}
	}
	
	private void updateTemplate(final TemplatePersistenceData data) {
		String groupId = data.getTemplate().getName();
		{	final int idx = groupId.indexOf(':');
			if (idx <= 0) {
				return;
			}
			groupId = groupId.substring(0, idx);
		}
		final TemplateGroup group = getGroup(groupId);
		if (group == null) {
			return;
		}
		if (group.fItemNames.isEmpty()) {
			fContentProvider.getChildren(group);
		}
		if (data.isUserAdded() && group.fItemNames.contains(data.getTemplate().getName())) {
			data.getTemplate().setName(newName(group));
		}
		for (final ITemplateContribution contrib : fTemplateContributions) {
			if (contrib.getGroups().contains(groupId)
					&& contrib.add(data) ) {
				return;
			}
		}
	}
	
	@Override
	public void exportItems(final List<? extends Object> items) {
		final TemplatePersistenceData[] array = new TemplatePersistenceData[items.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = ((TemplateItem) items.get(i)).fData;
		}
		doExport(array);
	}
	
	private void doExport(final TemplatePersistenceData[] templates) {
		final FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(NLS.bind(TemplatesMessages.Config_Export_title, String.valueOf(templates.length)));
		dialog.setFilterExtensions(new String[] { "*.xml", "*.*" });
		dialog.setFileName(TemplatesMessages.Config_Export_filename);
		final String path = dialog.open();
		
		if (path == null) {
			return;
		}
		
		final File file = new File(path);
		
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
		
		
		private Button fCompleteControl;
		private Button fDeletedControl;
		
		private boolean fCompletly;
		
		
		public DefaultsDialog(final Shell parentShell) {
			super(parentShell, TemplatesMessages.Config_RestoreDefaults_title, null,
					TemplatesMessages.Config_RestoreDefaults_title + ": " + fTitle,
					MessageDialog.QUESTION,
					new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0 );
			setShellStyle(getShellStyle() | SWT.SHEET);
		}
		
		
		@Override
		protected Control createCustomArea(final Composite parent) {
			final Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 1));
			{	fCompleteControl = new Button(composite, SWT.RADIO);
				final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
				gd.horizontalIndent = LayoutUtil.defaultIndent();
				fCompleteControl.setLayoutData(gd);
				fCompleteControl.setText(TemplatesMessages.Config_RestoreDefaults_Completely_label);
			}
			{	fDeletedControl = new Button(composite, SWT.RADIO);
				final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
				gd.horizontalIndent = LayoutUtil.defaultIndent();
				fDeletedControl.setLayoutData(gd);
				fDeletedControl.setText(TemplatesMessages.Config_RestoreDefaults_Deleted_label);
				
				if (!hasDeleted()) {
					fDeletedControl.setEnabled(false);
				}
			}
			
			fCompleteControl.setSelection(true);
			
			return composite;
		}
		
		@Override
		protected void buttonPressed(final int buttonId) {
			fCompletly = fCompleteControl.getSelection();
			super.buttonPressed(buttonId);
		}
		
	}
	
	@Override
	public void performDefaults() {
		boolean completly = true;
		if (fEnableAdd) {
			final DefaultsDialog dialog = new DefaultsDialog(getShell());
			if (dialog.open() != 0) {
				return;
			}
			completly = dialog.fCompletly;
		}
		if (completly) {
			for (final ITemplateContribution contrib : fTemplateContributions) {
				contrib.restoreDefaults();
			}
			for (final TemplateGroup group : fTemplateGroups) {
				group.fItemNames.clear();
			}
			super.performDefaults();
		}
		else {
			for (final ITemplateContribution contrib : fTemplateContributions) {
				contrib.restoreDeleted();
			}
		}
		
		// refresh
		fButtonGroup.refresh();
	}
	
	private boolean hasDeleted() {
		for (final ITemplateContribution contrib : fTemplateContributions) {
			if (contrib.hasDeleted()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean performOk() {
		try {
			for (final ITemplateContribution contrib : fTemplateContributions) {
				contrib.save();
			}
		}
		catch (final IOException e) {
			openWriteErrorDialog(e);
		}
		return super.performOk();
	}
	
	@Override
	public void performCancel() {
		try {
			for (final ITemplateContribution contrib : fTemplateContributions) {
				contrib.load();
			}
		}
		catch (final IOException e) {
			openReadErrorDialog(e);
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
