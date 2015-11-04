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

package de.walware.ecommons.ltk.internal.ui.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.masterdetail.MasterDetailObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.databinding.IntegerValidator;
import de.walware.ecommons.databinding.jface.DataBindingSupport;
import de.walware.ecommons.preferences.core.Preference;
import de.walware.ecommons.preferences.core.Preference.BooleanPref;
import de.walware.ecommons.preferences.core.Preference.IntPref;
import de.walware.ecommons.preferences.ui.ColorSelectorObservableValue;
import de.walware.ecommons.preferences.ui.ConfigurationBlock;
import de.walware.ecommons.preferences.ui.ConfigurationBlockPreferencePage;
import de.walware.ecommons.preferences.ui.ManagedConfigurationBlock;
import de.walware.ecommons.preferences.ui.RGBPref;
import de.walware.ecommons.text.ui.settings.DecorationPreferences;
import de.walware.ecommons.ui.util.LayoutUtil;

import de.walware.ecommons.ltk.ui.LTKUIPreferences;


public class EditorsPreferencePage extends ConfigurationBlockPreferencePage {
	
	
	public EditorsPreferencePage() {
	}
	
	
	@Override
	protected ConfigurationBlock createConfigurationBlock() {
		return new EditorsConfigurationBlock(createStatusChangedListener());
	}
	
}


class EditorsConfigurationBlock extends ManagedConfigurationBlock {
	
	private static class AppearanceColorsItem {
		
		final String name;
		final RGBPref pref;
		
		AppearanceColorsItem(final String label, final RGBPref pref) {
			this.name= label;
			this.pref= pref;
		}
		
	}
	
	
	private ListViewer colorList;
	private ColorSelector colorEditor;
	
	private BooleanPref matchingBracketsPref;
	private Button matchingBracketsControl;
	
	private IntPref contentAssistDelayPref;
	private Text contentAssistDelayControl;
	
	
	public EditorsConfigurationBlock(final IStatusChangeListener statusListener) {
		super(null, statusListener);
	}
	
	
	@Override
	protected void createBlockArea(final Composite pageComposite) {
		final Map<Preference<?>, String> prefs= new HashMap<>();
		
		final List<AppearanceColorsItem> colors= new ArrayList<>();
		
		// Matching Bracket
		final DecorationPreferences decoPrefs= LTKUIPreferences.getEditorDecorationPreferences();
		this.matchingBracketsPref= decoPrefs.getMatchingBracketsEnabled();
		prefs.put(this.matchingBracketsPref, null);
		{	final AppearanceColorsItem color= new AppearanceColorsItem(
					Messages.Editors_MatchingBracketsHighlightColor,
					decoPrefs.getMatchingBracketsColor() );
			colors.add(color);
			prefs.put(color.pref, null);
		}
		
		// Assist
		{	final AppearanceColorsItem color= new AppearanceColorsItem(
					Messages.Editors_CodeAssistParametersForegrondColor,
					LTKUIPreferences.CONTEXT_INFO_FOREGROUND_COLOR_PREF );
			colors.add(color);
			prefs.put(color.pref, LTKUIPreferences.ASSIST_GROUP_ID);
		}
		{	final AppearanceColorsItem color= new AppearanceColorsItem(
					Messages.Editors_CodeAssistParametersBackgroundColor,
					LTKUIPreferences.CONTEXT_INFO_BACKGROUND_COLOR_PREF );
			colors.add(color);
			prefs.put(color.pref, LTKUIPreferences.ASSIST_GROUP_ID);
		}
		
//		{	final AppearanceColorsItem color= new AppearanceColorsItem(
//					Messages.Editors_CodeAssistReplacementForegroundColor,
//					assistPreferences.getReplacementForegroundPref() );
//			colors.add(color);
//			prefs.put(color.pref, assistPreferences.getGroupId());
//		}
//		{	final AppearanceColorsItem color= new AppearanceColorsItem(
//					Messages.Editors_CodeAssistReplacementBackgroundColor,
//					ContentAssistPreference.REPLACEMENT_BACKGROUND );
//			colors.add(color);
//			prefs.put(color.pref, ContentAssistPreference.GROUP_ID);
//		}
		
		this.contentAssistDelayPref= LTKUIPreferences.CONTENT_ASSIST_DELAY_PREF;
		prefs.put(this.contentAssistDelayPref, LTKUIPreferences.ASSIST_GROUP_ID);
		
		// Register preferences
		setupPreferenceManager(prefs);
		
		// Controls
		addLinkHeader(pageComposite, Messages.Editors_link);
		{	final Composite group= createAppearanceSection(pageComposite);
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		LayoutUtil.addSmallFiller(pageComposite, false);
		{	final Composite group= createAssistSection(pageComposite);
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		// Binding
		this.colorList.setInput(colors.toArray(new AppearanceColorsItem[colors.size()]));
		this.colorList.setSelection(new StructuredSelection(colors.get(0)));
		initBindings();
		updateControls();
	}
	
	private Composite createAppearanceSection(final Composite parent) {
		final Group group= new Group(parent, SWT.NONE);
		group.setText(Messages.Editors_Appearance);
		group.setLayout(LayoutUtil.createGroupGrid(2));
		
		this.matchingBracketsControl= new Button(group, SWT.CHECK | SWT.LEFT);
		this.matchingBracketsControl.setText(Messages.Editors_HighlightMatchingBrackets);
		this.matchingBracketsControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		LayoutUtil.addSmallFiller(group, false);
		final Composite colorComposite;
		{	colorComposite= new Composite(group, SWT.NONE);
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
			colorComposite.setLayoutData(gd);
			colorComposite.setLayout(LayoutUtil.createCompositeGrid(2));
			final Label label= new Label(colorComposite, SWT.LEFT);
			label.setText(Messages.Editors_AppearanceColors);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		}
		{	this.colorList= new ListViewer(colorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
			this.colorList.setContentProvider(new ArrayContentProvider());
			this.colorList.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					final AppearanceColorsItem item= (AppearanceColorsItem) element;
					return item.name;
				}
			});
			this.colorList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		}
		{	final Composite colorOptions= new Composite(colorComposite, SWT.NONE);
			colorOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			colorOptions.setLayout(LayoutUtil.createCompositeGrid(2));
			
			final Label label= new Label(colorOptions, SWT.LEFT);
			label.setText(Messages.Editors_Color);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			this.colorEditor= new ColorSelector(colorOptions);
			this.colorEditor.getButton().setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		}
		
		return group;
	}
	
	private Composite createAssistSection(final Composite parent) {
		final Group group= new Group(parent, SWT.NONE);
		group.setText(Messages.Editors_CodeAssist);
		group.setLayout(LayoutUtil.createGroupGrid(2));
		
		{	final Label label= new Label(group, SWT.LEFT);
			label.setText(Messages.Editors_CodeAssist_AutoTriggerDelay_label);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			this.contentAssistDelayControl= new Text(group, SWT.SINGLE | SWT.BORDER);
			final GridData gd= new GridData(SWT.LEFT, SWT.CENTER, true, false);
			gd.widthHint= LayoutUtil.hintWidth(this.contentAssistDelayControl, 4);
			this.contentAssistDelayControl.setLayoutData(gd);
		}
		return group;
	}
	
	@Override
	protected void addBindings(final DataBindingSupport db) {
		db.getContext().bindValue(SWTObservables.observeSelection(this.matchingBracketsControl),
				createObservable(this.matchingBracketsPref),
				null, null);
		final IObservableValue colorItem= ViewersObservables.observeSingleSelection(this.colorList);
		db.getContext().bindValue(new ColorSelectorObservableValue(this.colorEditor),
				MasterDetailObservables.detailValue(colorItem, new IObservableFactory() {
					@Override
					public IObservable createObservable(final Object target) {
						final AppearanceColorsItem item= (AppearanceColorsItem) target;
						return EditorsConfigurationBlock.this.createObservable(item.pref);
					}
				}, RGB.class),
				null, null);
		db.getContext().bindValue(SWTObservables.observeText(this.contentAssistDelayControl, SWT.Modify),
				createObservable(this.contentAssistDelayPref),
				new UpdateValueStrategy().setAfterGetValidator(
						new IntegerValidator(10, 2000, Messages.Editors_CodeAssist_AutoTriggerDelay_error_message) ),
				null );
	}
	
}
