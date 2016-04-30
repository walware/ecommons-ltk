/*=============================================================================#
 # Copyright (c) 2000-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImIdentitySet;

import de.walware.ecommons.databinding.jface.DataBindingSupport;
import de.walware.ecommons.ui.components.ButtonGroup;
import de.walware.ecommons.ui.components.DataAdapter;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.buildpaths.core.BuildpathElementType;
import de.walware.ecommons.ltk.buildpaths.core.IBuildpathAttribute;
import de.walware.ecommons.ltk.buildpaths.core.IBuildpathElement;
import de.walware.ecommons.ltk.buildpaths.ui.wizards.EditFilterWizard;
import de.walware.ecommons.ltk.internal.buildpaths.ui.Messages;


public class SourceContainerComponent implements ButtonGroup.IActions<Object> {
	
	
	private static final ImIdentitySet<String> FILTER_NO= ImCollections.emptyIdentitySet();
	
	private static final ImIdentitySet<String> FILTER_OUTPUT= ImCollections.newIdentitySet(
			IBuildpathAttribute.OUTPUT );
	
	private static final Object[] NO_CHILDREN= new Object[0];
	
	
	private class TreeContentProvider implements ITreeContentProvider {
		
		
		public TreeContentProvider() {
		}
		
		@Override
		public void dispose() {
		}
		
		
		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		}
		
		@Override
		public Object[] getElements(final Object inputElement) {
			return SourceContainerComponent.this.containerList.toArray();
		}
		
		@Override
		public Object getParent(final Object element) {
			if (element instanceof BuildpathListElementAttribute) {
				return ((BuildpathListElementAttribute) element).getParent();
			}
			return null;
		}
		
		@Override
		public boolean hasChildren(final Object element) {
			if (element instanceof BuildpathListElement) {
				return true;
			}
			return false;
		}
		
		private boolean isFilterOutputAttribute() {
			return (SourceContainerComponent.this.outputByContainerValue == null
					|| !((boolean) SourceContainerComponent.this.outputByContainerValue.getValue()) );
		}
		
		@Override
		public Object[] getChildren(final Object element) {
			if (element instanceof BuildpathListElement) {
				final ImIdentitySet<String> filter= (isFilterOutputAttribute()) ?
						FILTER_OUTPUT : FILTER_NO;
				return ((BuildpathListElement) element).getFilteredChildren(filter).toArray();
			}
			return NO_CHILDREN;
		}
		
	}
	
	private class ListElementDataAdapter extends DataAdapter.TreeAdapter<Object> {
		
		
		public ListElementDataAdapter(final ITreeContentProvider contentProvider) {
			super(contentProvider, null);
		}
		
		
		@Override
		public boolean isModifyAllowed(final Object element) {
			if (element instanceof BuildpathListElement) {
				final BuildpathListElement listElement= (BuildpathListElement) element;
				if (listElement.getType().getName() == IBuildpathElement.SOURCE) {
					if (listElement.getPath().equals(SourceContainerComponent.this.project.getFullPath())) {
						return false;
					}
				}
				return (SourceContainerComponent.this.uiDescription.getAllowEdit(listElement));
			}
			if (element instanceof BuildpathListElementAttribute) {
				final BuildpathListElementAttribute attribute= (BuildpathListElementAttribute) element;
				if (attribute.isBuiltin()) {
					return (SourceContainerComponent.this.uiDescription.getAllowEdit(attribute));
				}
				else {
					return false;
				}
			}
			return false;
		}
		
		@Override
		public boolean isDeleteAllowed(final Object element) {
			if (element instanceof BuildpathListElement) {
				final BuildpathListElement listElement= (BuildpathListElement) element;
				return (SourceContainerComponent.this.uiDescription.getAllowEdit(listElement));
			}
			if (element instanceof BuildpathListElementAttribute) {
				final BuildpathListElementAttribute attribute= (BuildpathListElementAttribute) element;
				if (SourceContainerComponent.this.uiDescription.getAllowEdit(attribute)) {
					if (attribute.isBuiltin()) {
						switch (attribute.getName()) {
						case IBuildpathAttribute.FILTER_INCLUSIONS:
						case IBuildpathAttribute.FILTER_EXCLUSIONS:
							return (!((List<?>) attribute.getValue()).isEmpty());
						default:
							return (attribute.getValue() != null);
						}
					}
					else {
						return true;
					}
				}
				return false;
			}
			return false;
		}
		
		@Override
		public void delete(final List<? extends Object> elements) {
			for (final Object object : elements) {
				if (object instanceof BuildpathListElement) {
					final BuildpathListElement element= (BuildpathListElement) object;
					SourceContainerComponent.this.containerList.remove(element);
					continue;
				}
				if (object instanceof BuildpathListElementAttribute) {
					final BuildpathListElementAttribute attribute= (BuildpathListElementAttribute) object;
					final String key= attribute.getName();
					if (attribute.isBuiltin()) {
						final Object value;
						switch (attribute.getName()) {
						case IBuildpathAttribute.FILTER_INCLUSIONS:
						case IBuildpathAttribute.FILTER_EXCLUSIONS:
							value= ImCollections.emptyList();
							break;
						default:
							value= null;
							break;
						}
						attribute.getParent().setAttribute(key, value);
						SourceContainerComponent.this.changedContainers.add(attribute.getParent());
						continue;
					}
					else {
						throw new UnsupportedOperationException("Not yet implemented");
//						continue;
					}
				}
			}
			updateBuildpathList();
		}
		
	}
	
	
	private final WritableList buildpathList;
	
	private final BuildpathElementType type;
	
	private final BuildpathsUIDescription uiDescription;
	
	private IProject project;
	
	private final WritableList/*<BuildpathListElement>*/ containerList= new WritableList();
	
	private final Set<BuildpathListElement> changedContainers= new HashSet<>();
	
	private final WritableValue outputPathValue;
	private final WritableValue outputByContainerValue;
	
	private Control control;
	
	private TreeViewer containerListViewer;
	private ButtonGroup<Object> containerListButtons;
	
	private Text outputPathControl;
	private Button outputByFolderControl;
	
	
	public SourceContainerComponent(final WritableList buildpathList,
			final BuildpathElementType sourceType, final BuildpathElementType outputType,
			final BuildpathsUIDescription uiDescription) {
		this.buildpathList= buildpathList;
		this.type= sourceType;
		
		this.uiDescription= uiDescription;
		
		this.outputPathValue= (outputType != null) ?
				new WritableValue(String.class, null) : null;
		this.outputByContainerValue= (outputType != null
						&& this.type.isAttributeBuiltin(IBuildpathAttribute.OUTPUT) ) ?
				new WritableValue(Boolean.TYPE, false) : null;
	}
	
	public void init(final IProject project) {
		this.project= project;
		if (Display.getCurrent() != null) {
			updateTargets(true);
		}
		else {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					updateTargets(true);
				}
			});
		}
	}
	
	
	private Shell getShell() {
		if (this.control != null) {
			return this.control.getShell();
		}
		return UIAccess.getActiveWorkbenchShell(true);
	}
	
	public Control create(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(LayoutUtil.createCompositeGrid(2));
		
		{	final Label label= new Label(composite, SWT.NONE);
			label.setText(Messages.SourceContainers_SourceFolders_label);
			
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		}
		{	final TreeViewer viewer= new TreeViewer(composite,
					SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
			final TreeContentProvider contentProvider= new TreeContentProvider();
			viewer.setContentProvider(contentProvider);
			viewer.setComparator(this.uiDescription.createListElementComparator());
			viewer.setLabelProvider(this.uiDescription.createListLabelProvider());
			viewer.setInput(this.containerList);
			
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint= LayoutUtil.hintHeight(viewer.getTree(), 8);
			gd.widthHint= LayoutUtil.hintWidth(viewer.getTree(), 60);
			viewer.getControl().setLayoutData(gd);
			this.containerListViewer= viewer;
			
			final ButtonGroup<Object> buttonGroup= new ButtonGroup<>(composite, this, false);
			buttonGroup.addAddButton(new ButtonGroup.AddHandler() {
				@Override
				public void update(final IStructuredSelection selection) {
					setEnabled(SourceContainerComponent.this.uiDescription.getAllowAdd(
							SourceContainerComponent.this.project, SourceContainerComponent.this.type ));
				}
			});
			buttonGroup.addEditButton(null);
			buttonGroup.addDeleteButton(null);
			buttonGroup.connectTo(viewer, new ListElementDataAdapter(contentProvider));
			
			buttonGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			this.containerListButtons= buttonGroup;
		}
		
		if (this.outputPathValue != null) {
			{	final Label label= new Label(composite, SWT.NONE);
				label.setText(Messages.SourceContainers_OutputFolder_label);
				
				label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			}
			{	final Text text= new Text(composite, SWT.BORDER | SWT.SINGLE);
				
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				this.outputPathControl= text;
				
				final Button button= new Button(composite, SWT.PUSH);
				button.setText(Messages.SourceContainers_OutputFolder_Choose_label);
			}
			if (this.outputByContainerValue != null) {
				final Button button= new Button(composite, SWT.CHECK);
				button.setText(Messages.SourceContainers_OutputBySourceFolders_label);
				button.setSelection(false);
				
				button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
				this.outputByFolderControl= button;
			}
		}
		
		this.control= composite;
		
		return this.control;
	}
	
	public void bind(final DataBindingSupport db) {
		if (this.outputPathValue != null) {
			db.getContext().bindValue(
					WidgetProperties.text(SWT.Modify).observe(this.outputPathValue),
					this.outputPathValue );
			
			if (this.outputByContainerValue != null) {
				db.getContext().bindValue(
						WidgetProperties.selection().observe(this.outputByFolderControl),
						this.outputByContainerValue );
				
				this.outputByContainerValue.addValueChangeListener(new IValueChangeListener() {
					@Override
					public void handleValueChange(final ValueChangeEvent event) {
						if (!((Boolean) event.diff.getNewValue())) {
							for (final Object object : SourceContainerComponent.this.containerList) {
								final BuildpathListElement element= (BuildpathListElement) object;
								final BuildpathListElementAttribute attribute= element
										.setAttribute(IBuildpathAttribute.OUTPUT, null);
								
								if (attribute != null) {
									SourceContainerComponent.this.changedContainers.add(element);
								}
							}
							SourceContainerComponent.this.containerListButtons.refresh();
						}
					}
				});
			}
		}
		
		updateExpandStates();
	}
	
	private void updateTargets(final boolean reset) {
		if (UIAccess.isOkToUse(this.control)) {
			if (reset) {
				this.containerListViewer.collapseAll();
			}
			this.containerList.clear();
			this.changedContainers.clear();
			
			final List<BuildpathListElement> folders= new ArrayList<>();
			boolean outputLocationByFolder= false;
			
			for (final Object obj : this.buildpathList) {
				final BuildpathListElement element= (BuildpathListElement) obj;
				if (isElement(element)) {
					folders.add(element);
					outputLocationByFolder|= (element.getAttributeValue(IBuildpathAttribute.OUTPUT) != null);
				}
			}
			this.containerList.addAll(folders);
			this.containerListButtons.refresh();
			updateExpandStates();
			
			if (this.outputByContainerValue != null) {
				this.outputByContainerValue.setValue(outputLocationByFolder);
			}
		}
	}
	
	private void updateBuildpathList() {
		final List<BuildpathListElement> workingList= new ArrayList<>(this.containerList);
		
		for (int idx= 0; idx < this.buildpathList.size();) {
			final BuildpathListElement element= (BuildpathListElement) this.buildpathList.get(idx);
			if (isElement(element)) {
				if (workingList.remove(element)) {
					if (this.changedContainers.remove(element)) {
						this.buildpathList.set(idx, element);
					}
					idx++;
				}
				else {
					this.buildpathList.remove(idx);
				}
			}
			else {
				idx++;
			}
		}
		if (!workingList.isEmpty()) {
			this.buildpathList.addAll(workingList);
		}
		this.changedContainers.clear();
	}
	
	private void updateExpandStates() {
		for (int i= 0; i < this.containerList.size(); i++) {
			final BuildpathListElement element= (BuildpathListElement) this.containerList.get(i);
			final List<IPath> inclusionPatterns= (List<IPath>)element.getAttributeValue(IBuildpathAttribute.FILTER_INCLUSIONS);
			final List<IPath> exclusionPatterns= (List<IPath>) element.getAttributeValue(IBuildpathAttribute.FILTER_EXCLUSIONS);
			final IPath outputFolder= (IPath) element.getAttributeValue(IBuildpathAttribute.OUTPUT);
			if (!inclusionPatterns.isEmpty() || !exclusionPatterns.isEmpty() || outputFolder != null) {
				this.containerListViewer.expandToLevel(element, 1);
			}
		}
	}
	
	protected boolean isElement(final BuildpathListElement element) {
		return (element.getType() == this.type);
	}
	
	
	public void setFocus() {
		this.containerListViewer.getTree().setFocus();
	}
	
	
	public List<Object> getSelection() {
		return ImCollections.toList(
				((StructuredSelection) this.containerListViewer.getSelection()).toList() );
	}
	
	public void setSelection(final List<BuildpathListElement> elements, final boolean expand) {
		this.containerListViewer.setSelection(new StructuredSelection(elements));
		if (expand) {
			for (final BuildpathListElement element : elements) {
				this.containerListViewer.expandToLevel(element, 1);
			}
		}
	}
	
	
	@Override
	public Object edit(final int command, final Object item, final Object parent) {
		if (command == ButtonGroup.ADD_NEW) {
			throw new UnsupportedOperationException();
		}
		else {
			if (item instanceof BuildpathListElement) {
				return editElement((BuildpathListElement) item);
			}
			if (item instanceof BuildpathListElementAttribute) {
				return editAttribute((BuildpathListElementAttribute) item);
			}
			throw new IllegalStateException();
		}
	}
	
	@Override
	public void updateState(final IStructuredSelection selection) {
	}
	
	private BuildpathListElement editElement(final BuildpathListElement element) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	private BuildpathListElementAttribute editAttribute(final BuildpathListElementAttribute attribute) {
		switch (attribute.getName()) {
		case IBuildpathAttribute.FILTER_INCLUSIONS:
		case IBuildpathAttribute.FILTER_EXCLUSIONS: {
			final EditFilterWizard wizard= new EditFilterWizard(
					ImCollections.toList((List<BuildpathListElement>) this.containerList),
					attribute.getParent(), this.uiDescription );
			final WizardDialog dialog= new WizardDialog(getShell(), wizard);
			dialog.create();
			wizard.setFocus(attribute.getName());
			if (dialog.open() == WizardDialog.OK) {
				this.containerListButtons.refresh(null);
				this.changedContainers.add(attribute.getParent());
				updateBuildpathList();
			}
			return null;
		}
		default:
			throw new UnsupportedOperationException("Not yet implemented");
		}
	}
	
}
