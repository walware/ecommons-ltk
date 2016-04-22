/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.icu.text.Collator;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.preferences.core.Preference;
import de.walware.ecommons.preferences.ui.ManagedConfigurationBlock;
import de.walware.ecommons.ui.components.ButtonGroup;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.ViewerUtil;
import de.walware.ecommons.ui.util.ViewerUtil.CheckboxTableComposite;

import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditorCommandIds;


public class AdvancedContentAssistConfigurationBlock extends ManagedConfigurationBlock {
	
	
	private class CategoryKindLabelProvider extends CellLabelProvider {
		@Override
		public void update(final ViewerCell cell) {
			final ContentAssistCategory category= (ContentAssistCategory) cell.getElement();
			cell.setImage(getImage(category.getImageDescriptor()));
			cell.setText(category.getDisplayName());
		}
	}
	
	
	private static BindingManager gLocalBindingManager;
	
	
	private CheckboxTableViewer defaultList;
	
	private CheckboxTableViewer circlingList;
	private ButtonGroup<ContentAssistCategory> circlingOrderButtons;
	
	private final Map<Object, Image> images= new HashMap<>();
	
	private final ContentAssistComputerRegistry registry;
	
	private WritableList orderedCategories;
	
	private Command specificCommand;
	private IParameter specificParam;
	
	
	public AdvancedContentAssistConfigurationBlock(final ContentAssistComputerRegistry registry,
			final IStatusChangeListener statusListener) {
		super(null, statusListener);
		this.registry= registry;
		
		final ICommandService commandSvc= (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		this.specificCommand= commandSvc.getCommand(ISourceEditorCommandIds.SPECIFIC_CONTENT_ASSIST_COMMAND_ID);
	}
	
	private void prepareKeybindingInfo() {
		if (this.specificCommand == null) {
			return;
		}
		if (gLocalBindingManager == null) {
			gLocalBindingManager= new BindingManager(new ContextManager(), new CommandManager());
			final IBindingService bindingService= (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
			gLocalBindingManager.setLocale(bindingService.getLocale());
			gLocalBindingManager.setPlatform(bindingService.getPlatform());
			
			final Scheme[] definedSchemes= bindingService.getDefinedSchemes();
			if (definedSchemes != null) {
				try {
					for (int i= 0; i < definedSchemes.length; i++) {
						final Scheme scheme= definedSchemes[i];
						final Scheme copy= gLocalBindingManager.getScheme(scheme.getId());
						copy.define(scheme.getName(), scheme.getDescription(), scheme.getParentId());
					}
				}
				catch (final NotDefinedException e) {
				}
			}
		}
		try {
			this.specificParam= this.specificCommand.getParameters()[0];
		}
		catch (final Exception x) {
			this.specificCommand= null;
			this.specificParam= null;
		}
	}
	
	private String getDefaultKeybindingAsString() {
		final ICommandService commandSvc= (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		final Command command= commandSvc.getCommand(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		final ParameterizedCommand pCmd= new ParameterizedCommand(command, null);
		final String key= getKeybindingAsString(pCmd);
		return key;
	}
	private String getSpecificKeybindingAsString(final ContentAssistCategory category) {
		if (gLocalBindingManager == null || category == null) {
			return null;
		}
		final Parameterization[] params= { new Parameterization(this.specificParam, category.getId()) };
		final ParameterizedCommand pCmd= new ParameterizedCommand(this.specificCommand, params);
		return getKeybindingAsString(pCmd);
	}
	
	private String getKeybindingAsString(final ParameterizedCommand command) {
		if (gLocalBindingManager == null) {
			return null;
		}
		final IBindingService bindingService= (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		try {
			gLocalBindingManager.setBindings(bindingService.getBindings());
			final Scheme activeScheme= bindingService.getActiveScheme();
			if (activeScheme != null) {
				gLocalBindingManager.setActiveScheme(activeScheme);
			}
			
			final TriggerSequence[] binding= gLocalBindingManager.getActiveBindingsDisregardingContextFor(command);
			if (binding.length > 0) {
				return binding[0].format();
			}
			return null;
		}
		catch (final NotDefinedException e) {
			return null;
		}
	}
	
	@Override
	protected void createBlockArea(final Composite pageComposite) {
		final Map<Preference<?>, String> prefs= new HashMap<>();
		
		prefs.put(this.registry.getPrefDefaultDisabledCategoryIds(), this.registry.getSettingsGroupId());
		prefs.put(this.registry.getPrefCirclingOrderedCategoryIds(), this.registry.getSettingsGroupId());
		
		setupPreferenceManager(prefs);
		
		prepareKeybindingInfo();
		
		final Composite composite= new Composite(pageComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(LayoutUtil.createCompositeGrid(1));
//		final Composite composite= pageComposite;
		
		final Link control= addLinkControl(composite,
				EditingMessages.ContentAssistAdvancedConfig_message_KeyBindingHint);
		control.setLayoutData(applyWrapWidth(new GridData(SWT.FILL, SWT.FILL, true, false)));
		
		{	final Label defaultKeyBindingLabel= new Label(composite, SWT.NONE);
			final String defaultKeyBinding= getDefaultKeybindingAsString();
			defaultKeyBindingLabel.setText((defaultKeyBinding != null) ? 
					NLS.bind(EditingMessages.ContentAssistAdvancedConfig_message_DefaultKeyBinding, defaultKeyBinding) :
					EditingMessages.ContentAssistAdvancedConfig_message_NoDefaultKeyBinding );
			defaultKeyBindingLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		{	// Default
			final Group group= new Group(composite, SWT.NONE);
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			group.setLayout(LayoutUtil.createGroupGrid(1));
			group.setText(EditingMessages.ContentAssistAdvancedConfig_Default_label);
			
			final Label label= new Label(group, SWT.NONE);
			label.setText(EditingMessages.ContentAssistAdvancedConfig_DefaultTable_label);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			final Composite table= createDefaultTable(group);
			table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
		
		{	// Cicling
			final Group group= new Group(composite, SWT.NONE);
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			group.setLayout(LayoutUtil.createGroupGrid(2));
			group.setText(EditingMessages.ContentAssistAdvancedConfig_Cicling_label);
			
			final Label label= new Label(group, SWT.WRAP);
			label.setText(EditingMessages.ContentAssistAdvancedConfig_CiclingTable_label);
			label.setLayoutData(applyWrapWidth(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1)));
			
			final Composite table= createCirclingTable(group);
			table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			this.circlingOrderButtons= new ButtonGroup<>(group);
			this.circlingOrderButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			this.circlingOrderButtons.addUpButton(null);
			this.circlingOrderButtons.addDownButton(null);
		}
		
		this.orderedCategories= new WritableList();
		this.circlingList.setInput(this.orderedCategories);
		this.circlingOrderButtons.connectTo(this.circlingList, this.orderedCategories, null);
		ViewerUtil.scheduleStandardSelection(this.circlingList);
		
		ViewerUtil.scheduleStandardSelection(this.defaultList);
		
		updateControls();
	}
	
	protected Composite createDefaultTable(final Composite parent) {
		final CheckboxTableComposite composite= new ViewerUtil.CheckboxTableComposite(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		this.defaultList= composite.viewer;
		composite.table.setHeaderVisible(true);
		composite.table.setLinesVisible(true);
		composite.viewer.setContentProvider(new ArrayContentProvider());
		
		{	final TableViewerColumn column= new TableViewerColumn(composite.viewer, SWT.NONE);
			composite.layout.setColumnData(column.getColumn(), new ColumnWeightData(3));
			column.getColumn().setText(EditingMessages.ContentAssistAdvancedConfig_ProposalKinds_label);
			column.setLabelProvider(new CategoryKindLabelProvider());
		}
		
		{	final TableViewerColumn column= new TableViewerColumn(composite.viewer, SWT.NONE);
			composite.layout.setColumnData(column.getColumn(), new ColumnWeightData(1));
			column.getColumn().setText(EditingMessages.ContentAssistAdvancedConfig_KeyBinding_label);
			column.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final ContentAssistCategory category= (ContentAssistCategory) cell.getElement();
					final String keybindingAsString= getSpecificKeybindingAsString(category);
					cell.setText((keybindingAsString != null) ? keybindingAsString : ""); //$NON-NLS-1$
				}
			});
		}
		
		return composite;
	}
	
	protected Composite createCirclingTable(final Composite parent) {
		final CheckboxTableComposite composite= new ViewerUtil.CheckboxTableComposite(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		this.circlingList= composite.viewer;
		composite.table.setHeaderVisible(true);
		composite.table.setLinesVisible(true);
		composite.viewer.setContentProvider(new ArrayContentProvider());
		
		{	final TableViewerColumn column= new TableViewerColumn(composite.viewer, SWT.NONE);
			composite.layout.setColumnData(column.getColumn(), new ColumnWeightData(1));
			column.getColumn().setText(EditingMessages.ContentAssistAdvancedConfig_ProposalKinds_label);
			column.setLabelProvider(new CategoryKindLabelProvider());
		}
		return composite;
	}
	
	private Image getImage(final ImageDescriptor imgDesc) {
		if (imgDesc == null) {
			return null;
		}
		Image img= this.images.get(imgDesc);
		if (img == null) {
			img= imgDesc.createImage(false);
			this.images.put(imgDesc, img);
		}
		return img;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		for (final Image img : this.images.values()) {
			img.dispose();
		}
		this.images.clear();
	}
	
	
	@Override
	protected void updateControls() {
		final List<ContentAssistCategory> orderedCategories= this.registry.applyPreferences(this, this.registry.getCopyOfCategories());
		
		final List<ContentAssistCategory> defaultCategories= new ArrayList<>(orderedCategories);
		Collections.sort(defaultCategories, new Comparator<ContentAssistCategory>() {
			private final Collator NAMES_COLLARTOR= Collator.getInstance();
			@Override
			public int compare(final ContentAssistCategory o1, final ContentAssistCategory o2) {
				return this.NAMES_COLLARTOR.compare(o1.getDisplayName(), o2.getDisplayName());
			}
		});
		this.defaultList.setInput(defaultCategories);
		for (final ContentAssistCategory category : defaultCategories) {
			this.defaultList.setChecked(category, category.isIncludedInDefault);
		}
		
		this.orderedCategories.clear();
		this.orderedCategories.addAll(orderedCategories);
		this.circlingList.refresh();
		for (final ContentAssistCategory category : orderedCategories) {
			this.circlingList.setChecked(category, category.isEnabledAsSeparate);
		}
		circlingOrderButtons.refresh();
	}
	
	@Override
	protected void updatePreferences() {
		final List<ContentAssistCategory> orderedCategories= new ArrayList<>(this.orderedCategories);
		for (final ContentAssistCategory category : orderedCategories) {
			category.isIncludedInDefault= this.defaultList.getChecked(category);
			category.isEnabledAsSeparate= this.circlingList.getChecked(category);
		}
		
		final Map<Preference<?>, Object> preferences= this.registry.createPreferences(orderedCategories);
		setPrefValues(preferences);
	}
	
}
