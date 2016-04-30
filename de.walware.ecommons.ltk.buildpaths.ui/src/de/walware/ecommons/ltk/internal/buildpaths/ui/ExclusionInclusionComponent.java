/*=============================================================================#
 # Copyright (c) 2000-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.buildpaths.ui;

import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ui.SharedMessages;
import de.walware.ecommons.ui.components.ButtonGroup;
import de.walware.ecommons.ui.components.ButtonGroup.SelectionHandler;
import de.walware.ecommons.ui.components.DropDownButton;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.MessageUtil;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.buildpaths.core.IBuildpathAttribute;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathListElement;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathsUIDescription;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathsUIResources;


public class ExclusionInclusionComponent implements ButtonGroup.IActions<IPath> {
	
	
	private static class ExclusionInclusionLabelProvider extends LabelProvider {
		
		private final Image elementImage;
		
		public ExclusionInclusionLabelProvider(final String imgKey) {
			this.elementImage= BuildpathsUIResources.INSTANCE.getImage(imgKey);
		}
		
		@Override
		public Image getImage(final Object element) {
			return this.elementImage;
		}
		
		@Override
		public String getText(final Object element) {
			return MessageUtil.processPathPattern(((IPath) element).toString());
		}
		
	}
	
	private static class Type {
		
		private final String attributeName;
		
		private final WritableList patternList;
		
		private TableViewer listControl;
		private ButtonGroup<IPath> buttons;
		
		public Type(final String key) {
			this.attributeName= key;
			
			this.patternList= new WritableList();
		}
		
	}
	
	private final static int ADD_NEW_MULTI= ButtonGroup.ADD_NEW | (0x1 << 8);
	
	
	private final BuildpathListElement element;
	private final IProject project;
	private IContainer sourceFolder;
	
	private final Type inclusionPatterns= new Type(IBuildpathAttribute.FILTER_INCLUSIONS);
	private final Type exclusionPatterns= new Type(IBuildpathAttribute.FILTER_EXCLUSIONS);
	
	private final BuildpathsUIDescription uiDescription;
	
	private Composite control;
	
	
	public ExclusionInclusionComponent(final BuildpathListElement element,
			final BuildpathsUIDescription uiDescription) {
		this.element= element;
		
		this.project= element.getProject();
		final IWorkspaceRoot root= this.project.getWorkspace().getRoot();
		final IResource res= root.findMember(element.getPath());
		if (res instanceof IContainer) {
			this.sourceFolder= (IContainer) res;
		}
		
		this.uiDescription= uiDescription;
	}
	
	
	private Type getType(final String key) {
		switch (key) {
		case IBuildpathAttribute.FILTER_INCLUSIONS:
			return this.inclusionPatterns;
		case IBuildpathAttribute.FILTER_EXCLUSIONS:
			return this.exclusionPatterns;
		default:
			throw new IllegalArgumentException(key);
		}
	}
	
	private Type getType(final List<IPath> list) {
		if (list == this.inclusionPatterns.patternList) {
			return this.inclusionPatterns;
		}
		if (list == this.exclusionPatterns.patternList) {
			return this.exclusionPatterns;
		}
		throw new IllegalArgumentException();
	}
	
	
	private Shell getShell() {
		return this.control.getShell();
	}
	
	public Control create(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(LayoutUtil.createCompositeGrid(2));
		
		createList(composite, this.inclusionPatterns,
				Messages.ExclusionInclusion_InclusionPattern_label,
				BuildpathsUIResources.OBJ_INCLUSION_FILTER_ATTRIBUTE_IMAGE_ID );
		createList(composite, this.exclusionPatterns,
				Messages.ExclusionInclusion_ExclusionPattern_label,
				BuildpathsUIResources.OBJ_EXCLUSION_FILTER_ATTRIBUTE_IMAGE_ID );
		
		this.control= composite;
		return composite;
	}
	
	public Control getControl() {
		return this.control;
	}
	
	private void createList(final Composite parent,
			final Type type, final String listLabel, final String imgKey) {
		final Label label= new Label(parent, SWT.NONE);
		label.setText(listLabel);
		
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		final TableViewer viewer= new TableViewer(parent,
				SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
		
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ExclusionInclusionLabelProvider(imgKey));
		viewer.setComparator(new ViewerComparator());
		viewer.setInput(type.patternList);
		
		final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint= LayoutUtil.hintHeight(viewer.getTable(), 6);
		gd.widthHint= LayoutUtil.hintWidth(viewer.getTable(), 60);
		viewer.getControl().setLayoutData(gd);
		type.listControl= viewer;
		
		final ButtonGroup<IPath> buttonGroup= new ButtonGroup<>(parent, this, false);
		{	final DropDownButton addButton= new DropDownButton(buttonGroup);
			final SelectionHandler<IPath> defaultHandler= new ButtonGroup.AddHandler();
			final Menu addMenu= addButton.getDropDownMenu();
			{	final MenuItem menuItem= new MenuItem(addMenu, SWT.PUSH);
				menuItem.setText(SharedMessages.CollectionEditing_AddItem_label + "...");
				menuItem.addSelectionListener(defaultHandler);
			}
			{	final MenuItem menuItem= new MenuItem(addMenu, SWT.PUSH);
				menuItem.setText(Messages.ExclusionInclusion_AddMulti_label);
				menuItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						buttonGroup.editElement(ADD_NEW_MULTI, null);
					}
				});
			}
			addButton.addSelectionListener(defaultHandler);
			addButton.setText(SharedMessages.CollectionEditing_AddItem_label + "...");
			buttonGroup.add(addButton, defaultHandler);
			addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		buttonGroup.addEditButton(null);
		buttonGroup.addDeleteButton(null);
		buttonGroup.connectTo(viewer, type.patternList, null);
		
		buttonGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		type.buttons= buttonGroup;
//		patternList.selectFirstElement();
	}
	
	public void updateTargets() {
		updateTargets(this.inclusionPatterns);
		updateTargets(this.exclusionPatterns);
	}
	
	private void updateTargets(final Type type) {
		type.patternList.clear();
		type.patternList.addAll((ImList<IPath>) this.element.getAttributeValue(type.attributeName));
		type.buttons.refresh();
	}
	
	@Override
	public IPath edit(final int command, final IPath item, final Object parent) {
		final Type type= getType((List<IPath>) parent);
		
		if (command == ADD_NEW_MULTI) {
			addMultiple(type);
			return null;
		}
		
		final ExclusionInclusionEntryDialog dialog= new ExclusionInclusionEntryDialog(getShell(),
				this.element, type.attributeName,
				item, type.patternList,
				this.uiDescription );
		if (dialog.open() == Window.OK) {
			return dialog.getPattern();
		}
		return null;
	}
	
	private void addMultiple(final Type type) {
		final String title;
		final String message;
		switch (type.attributeName) {
		case IBuildpathAttribute.FILTER_INCLUSIONS:
			title= Messages.ExclusionInclusion_Choose_Include_title;
			message= Messages.ExclusionInclusion_Choose_Include_Multi_description;
			break;
		case IBuildpathAttribute.FILTER_EXCLUSIONS:
			title= Messages.ExclusionInclusion_Choose_Exclude_title;
			message= Messages.ExclusionInclusion_Choose_Exclude_Multi_description;
			break;
		default:
			throw new IllegalStateException();
		}
		
		final List<IPath> patterns= ExclusionInclusionEntryDialog.chooseExclusionPattern(getShell(),
				this.sourceFolder, title, message, null, true );
		if (patterns != null && !patterns.isEmpty()) {
			type.patternList.addAll(patterns);
			type.buttons.refresh(patterns.get(0));
		}
	}
	
	@Override
	public void updateState(final IStructuredSelection selection) {
	}
	
	
	public ImList<IPath> getInclusionPatterns() {
		return ImCollections.toList(this.inclusionPatterns.patternList);
	}
	
	public ImList<IPath> getExclusionPatterns() {
		return ImCollections.toList(this.exclusionPatterns.patternList);
	}
	
	public void setFocus(String attributeName) {
		if (attributeName == null) {
			attributeName= IBuildpathAttribute.FILTER_INCLUSIONS;
		}
		final Type entry= getType(attributeName);
		if (UIAccess.isOkToUse(entry.listControl)) {
			entry.listControl.getControl().setFocus();
		}
	}
	
}
