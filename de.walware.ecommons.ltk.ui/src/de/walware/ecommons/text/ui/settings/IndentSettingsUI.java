/*=============================================================================#
 # Copyright (c) 2012-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.ui.settings;

import java.util.EnumMap;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.walware.ecommons.databinding.IntegerValidator;
import de.walware.ecommons.databinding.jface.DataBindingSupport;
import de.walware.ecommons.text.IIndentSettings;
import de.walware.ecommons.text.IIndentSettings.IndentationType;
import de.walware.ecommons.text.internal.ui.Messages;
import de.walware.ecommons.ui.util.LayoutUtil;


public class IndentSettingsUI {
	
	private final static EnumMap<IndentationType, String> INDENT_NAMES= new EnumMap<>(IndentationType.class);
	static {
		INDENT_NAMES.put(IndentationType.TAB, Messages.CodeStyle_Indent_Type_UseTabs_name);
		INDENT_NAMES.put(IndentationType.SPACES, Messages.CodeStyle_Indent_Type_UseSpaces_name);
	};
	
	
	private Text tabSizeControl;
	private ComboViewer indentPolicyControl;
	private Button conserveIndentControl;
	private Label indentSpaceCountLabel;
	private Text indentSpaceCountControl;
	private Button replaceOtherTabsControl;
	private Text lineWidthControl;
	
	
	public String getGroupLabel() {
		return Messages.CodeStyle_Indent_group;
	}
	
	public String getLevelUnitLabel() {
		return Messages.CodeStyle_Indent_Levels_label;
	}
	
	public void createControls(final Composite composite) {
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText(Messages.CodeStyle_Indent_Type_label);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			this.indentPolicyControl = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			this.indentPolicyControl.setContentProvider(new ArrayContentProvider());
			final IndentationType[] items= getAvailableIndentationTypes();
			this.indentPolicyControl.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					final IndentationType t = (IndentationType) element;
					return INDENT_NAMES.get(t);
				}
			});
			this.indentPolicyControl.setInput(items);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(this.indentPolicyControl.getCombo(), INDENT_NAMES.values());
			this.indentPolicyControl.getCombo().setLayoutData(gd);
			this.indentPolicyControl.setSelection(new StructuredSelection(IndentationType.TAB));
		}
		
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText(Messages.CodeStyle_TabSize_label);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			this.tabSizeControl = new Text(composite, SWT.RIGHT | SWT.BORDER | SWT.SINGLE);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(this.tabSizeControl, 2);
			this.tabSizeControl.setLayoutData(gd);
		}
		{	final Label label = this.indentSpaceCountLabel = new Label(composite, SWT.NONE);
			label.setText(Messages.CodeStyle_Indent_NumOfSpaces_label);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			this.indentSpaceCountControl = new Text(composite, SWT.RIGHT | SWT.SINGLE | SWT.BORDER);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(this.indentSpaceCountControl, 2);
			this.indentSpaceCountControl.setLayoutData(gd);
		}
		
		{	this.conserveIndentControl = new Button(composite, SWT.CHECK);
			this.conserveIndentControl.setText(Messages.CodeStyle_Indent_ConserveExisting_label);
			this.conserveIndentControl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		}
		{	this.replaceOtherTabsControl = new Button(composite, SWT.CHECK);
			this.replaceOtherTabsControl.setText(Messages.CodeStyle_Indent_ReplaceOtherTabs_label);
			this.replaceOtherTabsControl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		}
	}
	
	public Text getTabSizeControl() {
		return this.tabSizeControl;
	}
	
	public void addLineWidth(final Composite composite) {
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText(Messages.CodeStyle_LineWidth_label);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			this.lineWidthControl = new Text(composite, SWT.RIGHT | SWT.SINGLE | SWT.BORDER);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(this.lineWidthControl, 4);
			this.lineWidthControl.setLayoutData(gd);
		}
	}
	
	protected IndentationType[] getAvailableIndentationTypes() {
		return new IndentationType[] { IndentationType.TAB, IndentationType.SPACES };
	}
	
	public void addBindings(final DataBindingSupport db, final Object model) {
		db.getContext().bindValue(
				WidgetProperties.text(SWT.Modify).observe(this.tabSizeControl),
				BeanProperties.value(IIndentSettings.TAB_SIZE_PROP)
						.observe(db.getRealm(), model ),
				new UpdateValueStrategy().setAfterGetValidator(new IntegerValidator(1, 32, Messages.CodeStyle_TabSize_error_message)),
				null);
		
		final IObservableValue indentObservable = ViewersObservables.observeSingleSelection(this.indentPolicyControl);
		indentObservable.setValue(null);
		indentObservable.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				final IndentationType t = (IndentationType) event.diff.getNewValue();
				IndentSettingsUI.this.indentSpaceCountLabel.setEnabled(t == IndentationType.SPACES);
				IndentSettingsUI.this.indentSpaceCountControl.setEnabled(t == IndentationType.SPACES);
			}
		});
		db.getContext().bindValue(indentObservable,
				BeanProperties.value(IIndentSettings.INDENT_DEFAULT_TYPE_PROP)
						.observe(db.getRealm(), model ),
				null, null );
		db.getContext().bindValue(
				WidgetProperties.text(SWT.Modify).observe(this.indentSpaceCountControl),
				BeanProperties.value(IIndentSettings.INDENT_SPACES_COUNT_PROP)
						.observe(db.getRealm(), model ),
				new UpdateValueStrategy().setAfterGetValidator(new IntegerValidator(1, 32, Messages.CodeStyle_Indent_NumOfSpaces_error_message)),
				null );
		db.getContext().bindValue(
				WidgetProperties.selection().observe(this.replaceOtherTabsControl),
				BeanProperties.value(IIndentSettings.REPLACE_TABS_WITH_SPACES_PROP)
						.observe(db.getRealm(), model ),
				null, null );
		db.getContext().bindValue(
				WidgetProperties.selection().observe(this.conserveIndentControl),
				BeanProperties.value(IIndentSettings.REPLACE_CONSERVATIVE_PROP)
						.observe(db.getRealm(), model ),
				null, null );
		
		if (this.lineWidthControl != null) {
			db.getContext().bindValue(
					WidgetProperties.text(SWT.Modify).observe(this.lineWidthControl),
					BeanProperties.value(IIndentSettings.WRAP_LINE_WIDTH_PROP)
							.observe(db.getRealm(), model ),
					new UpdateValueStrategy().setAfterGetValidator(new IntegerValidator(40, 400, Messages.CodeStyle_LineWidth_error_message)),
					null );
		}
	}
	
}
