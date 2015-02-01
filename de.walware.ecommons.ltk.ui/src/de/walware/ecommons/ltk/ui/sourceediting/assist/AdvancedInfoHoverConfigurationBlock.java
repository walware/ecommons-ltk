/*=============================================================================#
 # Copyright (c) 2010-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - copy from JDT
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.preferences.ui.ManagedConfigurationBlock;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.MessageUtil;
import de.walware.ecommons.ui.util.ViewerUtil.CheckboxTableComposite;


/**
 * Configures Editor hover preferences.
 */
public class AdvancedInfoHoverConfigurationBlock extends ManagedConfigurationBlock {
	
	
	private class CheckProvider implements ICheckStateProvider, ICheckStateListener {
		
		@Override
		public boolean isGrayed(final Object element) {
			return false;
		}
		
		@Override
		public boolean isChecked(final Object element) {
			final InfoHoverDescriptor descriptor= (InfoHoverDescriptor) element;
			return descriptor.isEnabled;
		}
		
		@Override
		public void checkStateChanged(final CheckStateChangedEvent event) {
			final InfoHoverDescriptor descriptor= (InfoHoverDescriptor) event.getElement();
			descriptor.isEnabled= event.getChecked();
			handleHoverListSelection();
		}
		
	}
	
	
	private final InfoHoverRegistry registry;
	
	private List<InfoHoverDescriptor> descriptors;
	
	private CheckboxTableViewer hoverTableViewer;
	
	private Text modifierEditor;
	
	
	public AdvancedInfoHoverConfigurationBlock(final InfoHoverRegistry registry,
			final IStatusChangeListener statusListener) {
		super(null, statusListener);
		this.registry= registry;
	}
	
	
	@Override
	protected void createBlockArea(final Composite pageComposite) {
		final Map<Preference<?>, String> prefs= new HashMap<>();
		
		prefs.put(this.registry.getPrefSeparateSettings(), this.registry.getSettingsGroupId());
		
		setupPreferenceManager(prefs);
		
		final Composite composite= new Composite(pageComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(LayoutUtil.createCompositeGrid(2));
		
		final CheckboxTableComposite tableComposite= new CheckboxTableComposite(composite,
				SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		this.hoverTableViewer= tableComposite.viewer;
		tableComposite.table.setHeaderVisible(true);
		tableComposite.table.setLinesVisible(true);
		
		{	final TableViewerColumn column= tableComposite.addColumn("Hover Type",
					SWT.LEFT, new ColumnWeightData(1) );
			column.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final InfoHoverDescriptor descriptor= (InfoHoverDescriptor) cell.getElement();
					cell.setText(descriptor.getName());
				}
			});
		}
		{	final TableViewerColumn column= tableComposite.addColumn("Modifier Keys",
					SWT.LEFT, new ColumnWeightData(1) );
			column.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final InfoHoverDescriptor descriptor= (InfoHoverDescriptor) cell.getElement();
					cell.setText(MessageUtil.getModifierString(descriptor.getStateMask()));
				}
			});
		}
		
		this.hoverTableViewer.setUseHashlookup(true);
		this.hoverTableViewer.setContentProvider(new ArrayContentProvider());
		this.hoverTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				handleHoverListSelection();
			}
		});
		final CheckProvider checkProvider= new CheckProvider();
		this.hoverTableViewer.setCheckStateProvider(checkProvider);
		this.hoverTableViewer.addCheckStateListener(checkProvider);
		
		// Text field for modifier string
		{	final Label label= new Label(composite, SWT.LEFT);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			label.setText("&Modifier keys:");
			
			this.modifierEditor= new Text(composite, SWT.BORDER);
			this.modifierEditor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		this.modifierEditor.addKeyListener(new KeyListener() {
			
			@Override
			public void keyPressed(final KeyEvent e) {
				e.doit= false;
				final InfoHoverDescriptor descriptor= getSelecteddescriptor();
				if (descriptor == null) {
					return;
				}
				if (e.keyCode > 0 && e.character == 0) {
					descriptor.stateMask= ((e.stateMask | e.keyCode) & (SWT.CTRL | SWT.ALT | SWT.SHIFT | SWT.COMMAND));
				}
				else if (e.keyCode == SWT.DEL | e.keyCode == SWT.BS) {
					descriptor.stateMask= 0;
				}
				else {
					return;
				}
				AdvancedInfoHoverConfigurationBlock.this.modifierEditor.setText(MessageUtil.getModifierString(descriptor.getStateMask()));
				AdvancedInfoHoverConfigurationBlock.this.hoverTableViewer.refresh(descriptor);
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
			}
			
		});
		
		LayoutUtil.addSmallFiller(composite, true);
		
		this.descriptors= this.registry.loadCurrent();
		this.hoverTableViewer.setInput(this.descriptors);
		this.hoverTableViewer.getTable().setSelection(0);
		handleHoverListSelection();
	}
	
	
	private InfoHoverDescriptor getSelecteddescriptor() {
		return (InfoHoverDescriptor) ((IStructuredSelection) this.hoverTableViewer.getSelection()).getFirstElement();
	}
	
	private void handleHoverListSelection() {
		final InfoHoverDescriptor descriptor= getSelecteddescriptor();
		if (descriptor == null) {
			this.modifierEditor.setText(""); //$NON-NLS-1$
			this.modifierEditor.setEditable(false);
		}
		else {
			this.modifierEditor.setText(MessageUtil.getModifierString(descriptor.stateMask));
			this.modifierEditor.setEnabled(descriptor.isEnabled);
		}
	}
	
	
	@Override
	protected void updateControls() {
		super.updateControls();
		final List<InfoHoverDescriptor> updated= this.registry.applyPreferences(this, this.descriptors);
		this.descriptors.clear();
		this.descriptors.addAll(updated);
		this.hoverTableViewer.refresh();
	}
	
	@Override
	protected void updatePreferences() {
		setPrefValues(this.registry.toPreferencesMap(this.descriptors));
		super.updatePreferences();
	}
	
}
