/*******************************************************************************
 * Copyright (c) 2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - copy from JDT
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

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
import org.eclipse.swt.layout.GridLayout;
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
		
		public boolean isGrayed(final Object element) {
			return false;
		}
		
		public boolean isChecked(final Object element) {
			final InfoHoverDescriptor descriptor = (InfoHoverDescriptor) element;
			return descriptor.fIsEnabled;
		}
		
		public void checkStateChanged(final CheckStateChangedEvent event) {
			final InfoHoverDescriptor descriptor = (InfoHoverDescriptor) event.getElement();
			descriptor.fIsEnabled = event.getChecked();
			handleHoverListSelection();
		}
		
	}
	
	
	private final InfoHoverRegistry fRegistry;
	
	private List<InfoHoverDescriptor> fDescriptors;
	
	private CheckboxTableViewer fHoverTableViewer;
	
	private Text fModifierEditor;
	
	
	public AdvancedInfoHoverConfigurationBlock(final InfoHoverRegistry registry,
			final IStatusChangeListener statusListener) {
		super(null, statusListener);
		fRegistry = registry;
	}
	
	
	@Override
	protected void createBlockArea(final Composite pageComposite) {
		final Map<Preference, String> prefs = new HashMap<Preference, String>();
		prefs.put(fRegistry.getPrefSeparateSettings(), fRegistry.getSettingsGroupId());
		setupPreferenceManager(prefs);
		
		final Composite composite = new Composite(pageComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 2));
		
		final CheckboxTableComposite tableComposite = new CheckboxTableComposite(composite,
				SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		fHoverTableViewer = tableComposite.viewer;
		tableComposite.table.setHeaderVisible(true);
		tableComposite.table.setLinesVisible(true);
		
		{	final TableViewerColumn column = tableComposite.addColumn("Hover Type",
					SWT.LEFT, new ColumnWeightData(1) );
			column.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final InfoHoverDescriptor descriptor = (InfoHoverDescriptor) cell.getElement();
					cell.setText(descriptor.getName());
				}
			});
		}
		{	final TableViewerColumn column = tableComposite.addColumn("Modifier Keys",
					SWT.LEFT, new ColumnWeightData(1) );
			column.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final InfoHoverDescriptor descriptor = (InfoHoverDescriptor) cell.getElement();
					cell.setText(MessageUtil.getModifierString(descriptor.getStateMask()));
				}
			});
		}
		
		fHoverTableViewer.setUseHashlookup(true);
		fHoverTableViewer.setContentProvider(new ArrayContentProvider());
		fHoverTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				handleHoverListSelection();
			}
		});
		final CheckProvider checkProvider = new CheckProvider();
		fHoverTableViewer.setCheckStateProvider(checkProvider);
		fHoverTableViewer.addCheckStateListener(checkProvider);
		
		// Text field for modifier string
		{	final Label label = new Label(composite, SWT.LEFT);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			label.setText("&Modifier keys:");
			
			fModifierEditor = new Text(composite, SWT.BORDER);
			fModifierEditor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		fModifierEditor.addKeyListener(new KeyListener() {
			
			public void keyPressed(final KeyEvent e) {
				e.doit = false;
				final InfoHoverDescriptor descriptor = getSelecteddescriptor();
				if (descriptor == null) {
					return;
				}
				if (e.keyCode > 0 && e.character == 0) {
					descriptor.fStateMask = ((e.stateMask | e.keyCode) & (SWT.CTRL | SWT.ALT | SWT.SHIFT | SWT.COMMAND));
				}
				else if (e.keyCode == SWT.DEL | e.keyCode == SWT.BS) {
					descriptor.fStateMask = 0;
				}
				else {
					return;
				}
				fModifierEditor.setText(MessageUtil.getModifierString(descriptor.getStateMask()));
				fHoverTableViewer.refresh(descriptor);
			}
			
			public void keyReleased(final KeyEvent e) {
			}
			
		});
		
		LayoutUtil.addSmallFiller(composite, true);
		
		fDescriptors = fRegistry.loadCurrent();
		fHoverTableViewer.setInput(fDescriptors);
		fHoverTableViewer.getTable().setSelection(0);
		handleHoverListSelection();
	}
	
	
	private InfoHoverDescriptor getSelecteddescriptor() {
		return (InfoHoverDescriptor) ((IStructuredSelection) fHoverTableViewer.getSelection()).getFirstElement();
	}
	
	private void handleHoverListSelection() {
		final InfoHoverDescriptor descriptor = getSelecteddescriptor();
		if (descriptor == null) {
			fModifierEditor.setText(""); //$NON-NLS-1$
			fModifierEditor.setEditable(false);
		}
		else {
			fModifierEditor.setText(MessageUtil.getModifierString(descriptor.fStateMask));
			fModifierEditor.setEnabled(descriptor.fIsEnabled);
		}
	}
	
	
	@Override
	protected void updateControls() {
		super.updateControls();
		final List<InfoHoverDescriptor> updated = fRegistry.applyPreferences(this, fDescriptors);
		fDescriptors.clear();
		fDescriptors.addAll(updated);
		fHoverTableViewer.refresh();
	}
	
	@Override
	protected void updatePreferences() {
		final Map<Preference, Object> map = fRegistry.toPreferencesMap(fDescriptors);
		for (final Map.Entry<Preference, Object> entry : map.entrySet()) {
			setPrefValue(entry.getKey(), entry.getValue());
		}
		super.updatePreferences();
	}
	
}
