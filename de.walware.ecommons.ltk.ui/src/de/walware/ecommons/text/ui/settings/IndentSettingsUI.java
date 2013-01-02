/*******************************************************************************
 * Copyright (c) 2012-2013 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui.settings;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.jface.databinding.swt.SWTObservables;
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
	
	
	private Text fTabSize;
	private ComboViewer fIndentPolicy;
	private Button fConserveIndent;
	private Label fIndentSpaceCountLabel;
	private Text fIndentSpaceCount;
	private Button fReplaceOtherTabs;
	private Text fLineWidthControl;
	
	
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
			fIndentPolicy = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
			fIndentPolicy.setContentProvider(new ArrayContentProvider());
			final IndentationType[] items = new IndentationType[] { IndentationType.TAB, IndentationType.SPACES };
			final String[] itemLabels = new String[] { Messages.CodeStyle_Indent_Type_UseTabs_name, Messages.CodeStyle_Indent_Type_UseSpaces_name };
			fIndentPolicy.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					final IndentationType t = (IndentationType) element;
					switch (t) {
					case TAB:
						return itemLabels[0];
					case SPACES:
						return itemLabels[1];
					}
					return null;
				}
			});
			fIndentPolicy.setInput(items);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(fIndentPolicy.getCombo(), itemLabels);
			fIndentPolicy.getCombo().setLayoutData(gd);
			fIndentPolicy.setSelection(new StructuredSelection(IndentationType.TAB));
		}
		
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText(Messages.CodeStyle_TabSize_label);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			fTabSize = new Text(composite, SWT.RIGHT | SWT.BORDER | SWT.SINGLE);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(fTabSize, 2);
			fTabSize.setLayoutData(gd);
		}
		{	final Label label = fIndentSpaceCountLabel = new Label(composite, SWT.NONE);
			label.setText(Messages.CodeStyle_Indent_NumOfSpaces_label);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			fIndentSpaceCount = new Text(composite, SWT.RIGHT | SWT.SINGLE | SWT.BORDER);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(fIndentSpaceCount, 2);
			fIndentSpaceCount.setLayoutData(gd);
		}
		
		{	fConserveIndent = new Button(composite, SWT.CHECK);
			fConserveIndent.setText(Messages.CodeStyle_Indent_ConserveExisting_label);
			fConserveIndent.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		}
		{	fReplaceOtherTabs = new Button(composite, SWT.CHECK);
			fReplaceOtherTabs.setText(Messages.CodeStyle_Indent_ReplaceOtherTabs_label);
			fReplaceOtherTabs.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		}
	}
	
	public void addLineWidth(final Composite composite) {
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText(Messages.CodeStyle_LineWidth_label);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			fLineWidthControl = new Text(composite, SWT.RIGHT | SWT.SINGLE | SWT.BORDER);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(fLineWidthControl, 4);
			fLineWidthControl.setLayoutData(gd);
		}
	}
	
	public void addBindings(final DataBindingSupport db, final Object model) {
		db.getContext().bindValue(SWTObservables.observeText(fTabSize, SWT.Modify),
				BeansObservables.observeValue(db.getRealm(), model, IIndentSettings.TAB_SIZE_PROP),
				new UpdateValueStrategy().setAfterGetValidator(new IntegerValidator(1, 32, Messages.CodeStyle_TabSize_error_message)),
				null);
		
		final IObservableValue indentObservable = ViewersObservables.observeSingleSelection(fIndentPolicy);
		indentObservable.setValue(null);
		indentObservable.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				final IndentationType t = (IndentationType) event.diff.getNewValue();
				fIndentSpaceCountLabel.setEnabled(t == IndentationType.SPACES);
				fIndentSpaceCount.setEnabled(t == IndentationType.SPACES);
			}
		});
		db.getContext().bindValue(indentObservable, BeansObservables.observeValue(db.getRealm(), model, IIndentSettings.INDENT_DEFAULT_TYPE_PROP),
				null, null );
		db.getContext().bindValue(SWTObservables.observeText(fIndentSpaceCount, SWT.Modify),
				BeansObservables.observeValue(db.getRealm(), model, IIndentSettings.INDENT_SPACES_COUNT_PROP),
				new UpdateValueStrategy().setAfterGetValidator(new IntegerValidator(1, 32, Messages.CodeStyle_Indent_NumOfSpaces_error_message)),
				null );
		db.getContext().bindValue(SWTObservables.observeSelection(fReplaceOtherTabs),
				BeansObservables.observeValue(db.getRealm(), model, IIndentSettings.REPLACE_TABS_WITH_SPACES_PROP),
				null, null );
		db.getContext().bindValue(SWTObservables.observeSelection(fConserveIndent),
				BeansObservables.observeValue(db.getRealm(), model, IIndentSettings.REPLACE_CONSERVATIVE_PROP),
				null, null );
		
		if (fLineWidthControl != null) {
			db.getContext().bindValue(SWTObservables.observeText(fLineWidthControl, SWT.Modify),
					BeansObservables.observeValue(db.getRealm(), model, IIndentSettings.WRAP_LINE_WIDTH_PROP),
					new UpdateValueStrategy().setAfterGetValidator(new IntegerValidator(40, 400, Messages.CodeStyle_LineWidth_error_message)),
					null );
		}
	}
	
}
