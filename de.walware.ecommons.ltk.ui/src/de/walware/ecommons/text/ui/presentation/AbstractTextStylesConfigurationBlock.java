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

package de.walware.ecommons.text.ui.presentation;

import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.masterdetail.MasterDetailObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.preferences.core.Preference;
import de.walware.ecommons.preferences.ui.ColorSelectorObservableValue;
import de.walware.ecommons.preferences.ui.OverlayStoreConfigurationBlock;
import de.walware.ecommons.preferences.ui.OverlayStorePreference;
import de.walware.ecommons.preferences.ui.PreferenceStoreBeanWrapper;
import de.walware.ecommons.preferences.ui.RGBPref;
import de.walware.ecommons.text.internal.ui.Messages;
import de.walware.ecommons.text.ui.TextViewerEditorColorUpdater;
import de.walware.ecommons.text.ui.TextViewerJFaceUpdater;
import de.walware.ecommons.text.ui.presentation.AbstractTextStylesConfigurationBlock.SyntaxNode.UseStyle;
import de.walware.ecommons.text.ui.settings.TextStyleManager;
import de.walware.ecommons.ui.ColorManager;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.MessageUtil;
import de.walware.ecommons.ui.util.PixelConverter;
import de.walware.ecommons.ui.util.UIAccess;
import de.walware.ecommons.ui.util.ViewerUtil;
import de.walware.ecommons.ui.util.ViewerUtil.Node;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfiguration;


/**
 * Common UI to configure the text style of syntax tokens (tree, options, preview).
 */
public abstract class AbstractTextStylesConfigurationBlock extends OverlayStoreConfigurationBlock {
	// even in text, it currently depends on ltk SourceEditorViewerConfiguration;
	
	
	/**
	 * Generic node of the tree.
	 * 
	 * Note: getter and setters in all nodes for easy DataBinding.
	 */
	protected static abstract class SyntaxNode extends Node {
		
		
		public static class UseStyle {
			
			private final String label;
			private final String refRootKey;
			
			public UseStyle(final String refRootKey, final String label) {
				super();
				this.refRootKey= refRootKey;
				this.label= label;
			}
			
			public String getLabel() {
				return this.label;
			}
			
			public String getRefRootKey() {
				return this.refRootKey;
			}
			
		}
		
		public static UseStyle createUseCustomStyle() {
			return new UseStyle("", Messages.SyntaxColoring_Use_CustomStyle_label); //$NON-NLS-1$
		}
		
		public static UseStyle createUseNoExtraStyle(final String parentKey) {
			return new UseStyle(parentKey, Messages.SyntaxColoring_Use_NoExtraStyle_label);
		}
		
		public static UseStyle createUseOtherStyle(final String otherKey, final String otherLabel) {
			return new UseStyle(otherKey, NLS.bind(Messages.SyntaxColoring_Use_OtherStyle_label, otherLabel));
		}
		
		public static UseStyle createUseOtherStyle(final String otherKey, final String cat, final String otherLabel) {
			return new UseStyle(otherKey, NLS.bind(Messages.SyntaxColoring_Use_OtherStyleOf_label, otherLabel, cat));
		}
		
		
		protected static class PrefProperty {
			
			public final Preference<?> pref;
			public final String prop;
			
			public PrefProperty(final Preference<?> pref, final String prop) {
				this.pref= pref;
				this.prop= prop;
			}
		}
		
		
		public static final String PROP_USE= "useStyle"; //$NON-NLS-1$
		public static final String PROP_COLOR= "color"; //$NON-NLS-1$
		public static final String PROP_BOLD= "bold"; //$NON-NLS-1$
		public static final String PROP_ITALIC= "italic"; //$NON-NLS-1$
		public static final String PROP_STRIKETHROUGH= "strikethrough"; //$NON-NLS-1$
		public static final String PROP_UNDERLINE= "underline"; //$NON-NLS-1$
		
		
		private final String description;
		
		private final ImList<UseStyle> availableStyles;
		
		private IPreferenceStore preferenceStore;
		
		private ImList<PrefProperty> preferences= ImCollections.emptyList();
		
		private PreferenceStoreBeanWrapper beanSupport;
		
		
		private SyntaxNode(final String name, final String description,
				final SyntaxNode[] children,
				final ImList<UseStyle> availableStyles) {
			super(name, children);
			assert (availableStyles != null);
			
			this.description= description;
			
			this.availableStyles= availableStyles;
		}
		
		protected void setPreferences(final List<PrefProperty> prefs) {
			this.preferences= ImCollections.toList(prefs);
		}
		
		
		@Override
		public SyntaxNode[] getChildren() {
			return (SyntaxNode[]) super.getChildren();
		}
		
		public String getDescription() {
			return this.description;
		}
		
		
		protected void gatherPreferenceKeys(final List<OverlayStorePreference> keys) {
			for (final PrefProperty pref : this.preferences) {
				keys.add(OverlayStorePreference.create(pref.pref));
			}
		}
		
		protected void connectPreferenceStore(final IPreferenceStore store) {
			this.preferenceStore= store;
			this.beanSupport= new PreferenceStoreBeanWrapper(store, this);
			for (final PrefProperty pref : this.preferences) {
				this.beanSupport.addPreference(pref.prop, pref.pref);
			}
		}
		
		protected IPreferenceStore getPreferenceStore() {
			return this.preferenceStore;
		}
		
		
		/*-- Bean-Support --*/
		
		public void addPropertyChangeListener(final PropertyChangeListener listener) {
			this.beanSupport.addPropertyChangeListener(listener);
		}
		
		public void addPropertyChangeListener(final String propertyName,
				final PropertyChangeListener listener) {
			this.beanSupport.addPropertyChangeListener(propertyName, listener);
		}
		
		public void removePropertyChangeListener(final PropertyChangeListener listener) {
			this.beanSupport.removePropertyChangeListener(listener);
		}
		
		public void removePropertyChangeListener(final String propertyName,
				final PropertyChangeListener listener) {
			this.beanSupport.removePropertyChangeListener(propertyName, listener);
		}
		
		
		/*-- Property-Access --*/
		public ImList<UseStyle> getAvailableUseStyles() {
			return this.availableStyles;
		}
		public UseStyle getUseStyle() {
			return (!this.availableStyles.isEmpty()) ? this.availableStyles.get(0) : null;
		}
		protected UseStyle getUseStyle(final String value) {
			for (final UseStyle style : this.availableStyles) {
				if (style.getRefRootKey().equals(value)) {
					return style;
				}
			}
			return this.availableStyles.get(0);
		}
		public void setUseStyle(final UseStyle useStyle) {
		}
		
		public RGB getColor() {
			return null;
		}
		public void setColor(final RGB color) {
		}
		
		public boolean isBold() {
			return false;
		}
		public void setBold(final boolean enabled) {
		}
		
		public boolean isItalic() {
			return false;
		}
		public void setItalic(final boolean enabled) {
		}
		
		public boolean isStrikethrough() {
			return false;
		}
		public void setStrikethrough(final boolean enabled) {
		}
		
		public boolean isUnderline() {
			return false;
		}
		public void setUnderline(final boolean enabled) {
		}
		
	}
	
	/**
	 * Category Node without syntax style.
	 */
	protected static class CategoryNode extends SyntaxNode {
		
		public CategoryNode(final String name, final SyntaxNode[] children) {
			super(name, null, children, ImCollections.<UseStyle>emptyList());
		}
		
		
	}
	
	/**
	 * Style Node with syntax style, connected to overlay-preferencestory.
	 */
	protected static class StyleNode extends SyntaxNode {
		
		public class UseStylePref extends Preference<UseStyle> {
			UseStylePref(final String qualifier, final String key) {
				super(qualifier, key);
			}
			@Override
			public Class<UseStyle> getUsageType() {
				return UseStyle.class;
			}
			@Override
			public UseStyle store2Usage(final String storeValue) {
				return getUseStyle(storeValue);
			}
			@Override
			public String usage2Store(final UseStyle usageValue) {
				return usageValue.getRefRootKey();
			}
		}
		
		private final String rootKey;
		
		
		public StyleNode(final String name, final String description, final String rootKey,
				final UseStyle[] availableStyles, final SyntaxNode[] children) {
			super(name, description, children, ImCollections.newList(availableStyles));
			this.rootKey= rootKey;
			
			final List<PrefProperty> prefs= new ArrayList<>();
			if (getAvailableUseStyles().size() > 1) {
				prefs.add(new PrefProperty(new UseStylePref(null, getUseKey()), PROP_USE));
			}
			prefs.add(new PrefProperty(new RGBPref(null, getColorKey()), PROP_COLOR));
			prefs.add(new PrefProperty(new Preference.BooleanPref(null, getBoldKey()), PROP_BOLD));
			prefs.add(new PrefProperty(new Preference.BooleanPref(null, getItalicKey()), PROP_ITALIC));
			prefs.add(new PrefProperty(new Preference.BooleanPref(null, getUnderlineKey()), PROP_UNDERLINE));
			prefs.add(new PrefProperty(new Preference.BooleanPref(null, getStrikethroughKey()), PROP_STRIKETHROUGH));
			setPreferences(prefs);
		}
		
		
		private String getUseKey() {
			return this.rootKey + ITextPresentationConstants.TEXTSTYLE_USE_SUFFIX;
		}
		private String getColorKey() {
			return this.rootKey + ITextPresentationConstants.TEXTSTYLE_COLOR_SUFFIX;
		}
		private String getBoldKey() {
			return this.rootKey + ITextPresentationConstants.TEXTSTYLE_BOLD_SUFFIX;
		}
		private String getItalicKey() {
			return this.rootKey + ITextPresentationConstants.TEXTSTYLE_ITALIC_SUFFIX;
		}
		private String getUnderlineKey() {
			return this.rootKey + ITextPresentationConstants.TEXTSTYLE_UNDERLINE_SUFFIX;
		}
		private String getStrikethroughKey() {
			return this.rootKey + ITextPresentationConstants.TEXTSTYLE_STRIKETHROUGH_SUFFIX;
		}
		
		
		/*-- Property-Access --*/
		@Override
		public void setUseStyle(final UseStyle useStyle) {
			if (useStyle != null) {
				getPreferenceStore().setValue(getUseKey(), useStyle.getRefRootKey());
			}
		}
		@Override
		public UseStyle getUseStyle() {
			return getUseStyle(getPreferenceStore().getString(getUseKey()));
		}
		
		@Override
		public RGB getColor() {
			return PreferenceConverter.getColor(getPreferenceStore(), getColorKey());
		}
		@Override
		public void setColor(final RGB color) {
			PreferenceConverter.setValue(getPreferenceStore(), getColorKey(), color);
		}
		
		@Override
		public boolean isBold() {
			return getPreferenceStore().getBoolean(getBoldKey());
		}
		@Override
		public void setBold(final boolean enabled) {
			getPreferenceStore().setValue(getBoldKey(), enabled);
		}
		
		@Override
		public boolean isItalic() {
			return getPreferenceStore().getBoolean(getItalicKey());
		}
		@Override
		public void setItalic(final boolean enabled) {
			getPreferenceStore().setValue(getItalicKey(), enabled);
		}
		
		@Override
		public boolean isStrikethrough() {
			return getPreferenceStore().getBoolean(getStrikethroughKey());
		}
		@Override
		public void setStrikethrough(final boolean enabled) {
			getPreferenceStore().setValue(getStrikethroughKey(), enabled);
		}
		
		@Override
		public boolean isUnderline() {
			return getPreferenceStore().getBoolean(getUnderlineKey());
		}
		@Override
		public void setUnderline(final boolean enabled) {
			getPreferenceStore().setValue(getUnderlineKey(), enabled);
		}
		
	}
	
	protected static class BackgroundNode extends SyntaxNode {
		
		
		private final String rootKey;
		
		
		public BackgroundNode(final String name, final String description, final String rootKey) {
			super(name, description, null, ImCollections.newList(createUseCustomStyle()));
			this.rootKey= rootKey;
			
			final List<PrefProperty> prefs= new ArrayList<>();
			prefs.add(new PrefProperty(new RGBPref(null, getColorKey()), PROP_COLOR));
			setPreferences(prefs);
		}
		
		
		private String getColorKey() {
			return this.rootKey + ITextPresentationConstants.TEXTSTYLE_COLOR_SUFFIX;
		}
		
		@Override
		public RGB getColor() {
			return PreferenceConverter.getColor(getPreferenceStore(), getColorKey());
		}
		@Override
		public void setColor(final RGB color) {
			PreferenceConverter.setValue(getPreferenceStore(), getColorKey(), color);
		}
		
	}
	
	
	private static class SyntaxNodeLabelProvider extends CellLabelProvider {
		
		@Override
		public boolean useNativeToolTip(final Object object) {
			return true;
		}
		@Override
		public String getToolTipText(final Object element) {
			return ((SyntaxNode) element).getDescription();
		}
		
		@Override
		public void update(final ViewerCell cell) {
			cell.setText(((Node) cell.getElement()).getName());
		}
		
	}
	
	private static class UseStyleLabelProvider extends LabelProvider {
		
		@Override
		public String getText(final Object element) {
			final UseStyle style= (UseStyle) element;
			return style.getLabel();
		}
		
	}
	
	
	private static void collectKeys(final List<OverlayStorePreference> keys, final SyntaxNode[] nodes) {
		for (final SyntaxNode node : nodes) {
			node.gatherPreferenceKeys(keys);
			final SyntaxNode[] children= node.getChildren();
			if (children != null) {
				collectKeys(keys, children);
			}
		}
	}
	
	private static void connectStore(final SyntaxNode[] nodes, final IPreferenceStore store) {
		for (final SyntaxNode node: nodes) {
			node.connectPreferenceStore(store);
			final SyntaxNode[] children= node.getChildren();
			if (children != null) {
				connectStore(children, store);
			}
		}
	}
	
	
	private SyntaxNode[] rootNodes;
	private DataBindingContext dbc;
	
	private TreeViewer selectionViewer;
	
	private Set<String> groupIds;
	
	private ComboViewer useControl;
	private Label colorLabel;
	private ColorSelector colorEditor;
	private Button boldCheckbox;
	private Button italicCheckbox;
	private Button strikethroughCheckbox;
	private Button underlineCheckbox;
	
	private ColorManager colorManager;
	private TextStyleManager textStyles;
	
	protected SourceViewer previewViewer;
	private SourceEditorViewerConfiguration configuration;
	
	
	public AbstractTextStylesConfigurationBlock() {
	}
	
	
	protected abstract SyntaxNode[] createItems();
	protected abstract String getSettingsGroup();
	
	
	@Override
	protected Set<String> getChangedGroups() {
		return this.groupIds;
	}
	
	protected String getLinkMessage() {
		return Messages.SyntaxColoring_link;
	}
	
	/**
	 * If {@link TextAttribute}s (underline, strikethrough) is supported
	 */
	protected boolean isTextAttributesSupported() {
		return true;
	}
	
	@Override
	protected void createBlockArea(final Composite pageComposite) {
		// Prepare model
		this.rootNodes= createItems();
		this.groupIds= new HashSet<>();
		this.groupIds.add(getSettingsGroup());
		final List<OverlayStorePreference> keys= new ArrayList<>();
		collectKeys(keys, this.rootNodes);
		setupOverlayStore(keys.toArray(new OverlayStorePreference[keys.size()]));
		connectStore(this.rootNodes, this.fOverlayStore);
		
		{	final String message= getLinkMessage();
			if (message != null) {
				addLinkHeader(pageComposite, Messages.SyntaxColoring_link);
			}
		}
		
		final Composite composite= new Composite(pageComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(LayoutUtil.createCompositeGrid(1));
		
		// Tree / Options
		{	final Label label= new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			label.setText(Messages.SyntaxColoring_List_label);
		}
		
		final Composite configComposite= new Composite(composite, SWT.NONE);
		configComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		configComposite.setLayout(LayoutUtil.createCompositeGrid(2));
		{	final Control selectionControl= createTreeViewer(configComposite);
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, false, true);
			final Point size= ViewerUtil.calculateTreeSizeHint(this.selectionViewer.getControl(), this.rootNodes, 9);
			gd.widthHint= size.x;
			gd.heightHint= size.y;
			selectionControl.setLayoutData(gd);
		}
		{	final Control optionControl= createOptionsControl(configComposite);
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalIndent= 5;
	//		gd.horizontalIndent= LayoutUtil.defaultSmallIndent();
			optionControl.setLayoutData(gd);
		}
		
		// Previewer
		{	final Label label= new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			label.setText(Messages.SyntaxColoring_Preview);
		}
		{	final Control previewerControl= createPreviewer(composite);
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan= 2;
			final PixelConverter conv= new PixelConverter(previewerControl);
			gd.widthHint= conv.convertWidthInCharsToPixels(20);
			gd.heightHint= conv.convertHeightInCharsToPixels(5);
			previewerControl.setLayoutData(gd);
		}
		
		initFields();
		initBindings();
		
		UIAccess.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (UIAccess.isOkToUse(AbstractTextStylesConfigurationBlock.this.selectionViewer)) {
					AbstractTextStylesConfigurationBlock.this.selectionViewer.setSelection(new StructuredSelection(AbstractTextStylesConfigurationBlock.this.rootNodes[0]));
				}
			}
		});
	}
	
	
	public Control createTreeViewer(final Composite parent) {
		this.selectionViewer= new TreeViewer(parent, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		this.selectionViewer.setContentProvider(new ViewerUtil.NodeContentProvider());
		this.selectionViewer.setLabelProvider(new SyntaxNodeLabelProvider());
		ColumnViewerToolTipSupport.enableFor(this.selectionViewer);
		
		ViewerUtil.addDoubleClickExpansion(this.selectionViewer);
		
		return this.selectionViewer.getControl();
	}
	
	private Control createOptionsControl(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(LayoutUtil.createCompositeGrid(2));
		{	this.useControl= new ComboViewer(composite, SWT.READ_ONLY | SWT.DROP_DOWN);
			this.useControl.setLabelProvider(new UseStyleLabelProvider());
			final GridData gd= new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
			gd.widthHint= LayoutUtil.hintWidth(this.useControl.getCombo(), new String[] {
							"XXXXXXXXXXXXXXX", //$NON-NLS-1$
							Messages.SyntaxColoring_Use_CustomStyle_label,
							Messages.SyntaxColoring_Use_NoExtraStyle_label }); 
			this.useControl.getControl().setLayoutData(gd);
		}
		final int indent= LayoutUtil.defaultSmallIndent();
		{	final Label label= new Label(composite, SWT.NONE);
			final GridData gd= new GridData(SWT.FILL, SWT.CENTER, false, false);
			gd.horizontalIndent= indent;
			label.setLayoutData(gd);
			label.setText(Messages.SyntaxColoring_Color);
			this.colorLabel= label;
			
			this.colorEditor= new ColorSelector(composite);
			final Button foregroundColorButton= this.colorEditor.getButton();
			foregroundColorButton.setLayoutData(
					new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING) );
		}
		{	this.boldCheckbox= new Button(composite, SWT.CHECK);
			final GridData gd= new GridData(SWT.FILL, SWT.CENTER, false, false);
			gd.horizontalIndent= indent;
			this.boldCheckbox.setLayoutData(gd);
			this.boldCheckbox.setText(Messages.SyntaxColoring_Bold);
		}
		{	this.italicCheckbox= new Button(composite, SWT.CHECK);
			final GridData gd= new GridData(SWT.FILL, SWT.CENTER, false, false);
			gd.horizontalIndent= indent;
			this.italicCheckbox.setLayoutData(gd);
			this.italicCheckbox.setText(Messages.SyntaxColoring_Italic);
		}
		{	this.underlineCheckbox= new Button(composite, SWT.CHECK);
			final GridData gd= new GridData(SWT.FILL, SWT.CENTER, false, false);
			gd.horizontalIndent= indent;
			this.underlineCheckbox.setLayoutData(gd);
			this.underlineCheckbox.setText(Messages.SyntaxColoring_Underline);
		}
		{	this.strikethroughCheckbox= new Button(composite, SWT.CHECK);
			final GridData gd= new GridData(SWT.FILL, SWT.CENTER, false, false);
			gd.horizontalIndent= indent;
			this.strikethroughCheckbox.setLayoutData(gd);
			this.strikethroughCheckbox.setText(Messages.SyntaxColoring_Strikethrough);
		}
		
		return composite;
	}
	
	private Control createPreviewer(final Composite parent) {
		this.colorManager= new ColorManager();
		
		final IPreferenceStore store= new ChainedPreferenceStore(new IPreferenceStore[] {
				this.fOverlayStore, EditorsUI.getPreferenceStore() });
		this.previewViewer= new SourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		this.previewViewer.setEditable(false);
		this.configuration= getSourceViewerConfiguration(this.colorManager, store);
		this.previewViewer.configure(this.configuration);
		new TextViewerJFaceUpdater(this.previewViewer, store);
		new TextViewerEditorColorUpdater(this.previewViewer, store);
		
		final String content= loadPreviewContentFromFile(getPreviewFileName());
		final IDocument document= new Document(content);
		getDocumentSetupParticipant().setup(document);
		this.previewViewer.setDocument(document);
		
		return this.previewViewer.getControl();
	}
	
	protected abstract String getPreviewFileName();
	
	protected SourceEditorViewerConfiguration getSourceViewerConfiguration(
			final ColorManager colorManager, final IPreferenceStore preferenceStore) {
		this.textStyles= new TextStyleManager(colorManager, preferenceStore, getSettingsGroup());
		return getSourceEditorViewerConfiguration(preferenceStore, this.textStyles);
	}
	
	protected abstract SourceEditorViewerConfiguration getSourceEditorViewerConfiguration(
			IPreferenceStore preferenceStore, TextStyleManager textStyles);
	
	protected abstract IDocumentSetupParticipant getDocumentSetupParticipant();
	
	private String loadPreviewContentFromFile(final String filename) {
		String line;
		final String separator= "\n"; //$NON-NLS-1$
		final StringBuffer buffer= new StringBuffer(512);
		BufferedReader reader= null;
		try {
			reader= new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			while ((line= reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(separator);
			}
		}
		catch (final IOException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, -1,
					NLS.bind("An error occurred when loading the preview code from ''{0}''.", filename), e));
		}
		finally {
			if (reader != null) {
				try { reader.close(); } catch (final IOException e) {}
			}
		}
		return buffer.toString();
	}
	
	
	public void initFields() {
		this.selectionViewer.setInput(this.rootNodes);
	}
	
	private void initBindings() {
		final Realm realm= Realm.getDefault();
		this.dbc= new DataBindingContext(realm);
		
		// Observe changes in selection.
		final IObservableValue selection= ViewersObservables.observeSingleSelection(this.selectionViewer);
		selection.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				final SyntaxNode newNode= (SyntaxNode) event.diff.getNewValue();
				if (newNode != null) {
					updateEnablement(newNode, newNode.getUseStyle());
				}
			}
		});
		// Bind use style selection
		final IObservableList list= MasterDetailObservables.detailList(
				BeanProperties.value("availableUseStyles", List.class).observeDetail(selection),
				new IObservableFactory() {
					@Override
					public IObservable createObservable(final Object target) {
						return Observables.staticObservableList(realm, (List) target);
					}
				}, null);
		this.useControl.setContentProvider(new ObservableListContentProvider());
		this.useControl.setInput(list);
		final IObservableValue useStyle= BeanProperties.value(SyntaxNode.PROP_USE, UseStyle.class)
				.observeDetail(selection);
		useStyle.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				final IStructuredSelection selection= (IStructuredSelection) AbstractTextStylesConfigurationBlock.this
						.selectionViewer.getSelection();
				final UseStyle newUse= (UseStyle) event.diff.getNewValue();
				updateEnablement((SyntaxNode) selection.getFirstElement(), newUse);
			}
		});
		this.dbc.bindValue(ViewersObservables.observeSingleSelection(this.useControl),
				useStyle,
				null, null);
		// Bind option widgets to the properties of the current selection.
		this.dbc.bindValue(new ColorSelectorObservableValue(this.colorEditor),
				BeanProperties.value(SyntaxNode.PROP_COLOR, RGB.class).observeDetail(selection),
				null, null);
		this.dbc.bindValue(
				WidgetProperties.selection().observe(this.boldCheckbox),
				BeanProperties.value(SyntaxNode.PROP_BOLD, boolean.class)
						.observeDetail(selection),
				null, null);
		this.dbc.bindValue(
				WidgetProperties.selection().observe(this.italicCheckbox),
				BeanProperties.value(SyntaxNode.PROP_ITALIC, boolean.class)
						.observeDetail(selection),
				null, null);
		if (isTextAttributesSupported()) {
			this.dbc.bindValue(
					WidgetProperties.selection().observe(this.strikethroughCheckbox),
					BeanProperties.value(SyntaxNode.PROP_STRIKETHROUGH, boolean.class)
							.observeDetail(selection),
					null, null);
			this.dbc.bindValue(
					WidgetProperties.selection().observe(this.underlineCheckbox),
					BeanProperties.value(SyntaxNode.PROP_UNDERLINE, boolean.class)
							.observeDetail(selection),
					null, null);
		}
		else {
			this.strikethroughCheckbox.setVisible(false);
			this.underlineCheckbox.setVisible(false);
		}
	}
	
	private void updateEnablement(final SyntaxNode node, final UseStyle useStyle) {
		int showOptions;
		boolean enableOptions;
		if (node instanceof StyleNode) {
			this.useControl.getControl().setEnabled(node.getAvailableUseStyles().size() > 1);
			showOptions= 2;
			enableOptions= (useStyle != null && useStyle.getRefRootKey().equals("")); //$NON-NLS-1$
		}
		else if (node instanceof BackgroundNode) {
			this.useControl.getControl().setEnabled(false);
			showOptions= 1;
			enableOptions= true;
		}
		else {
			this.useControl.getControl().setEnabled(false);
			showOptions= 0;
			enableOptions= false;
		}
		
		this.colorLabel.setVisible(showOptions >= 1);
		this.colorEditor.getButton().setVisible(showOptions >= 1);
		this.colorLabel.setEnabled(enableOptions);
		this.colorEditor.setEnabled(enableOptions);
		
		this.boldCheckbox.setVisible(showOptions >= 2);
		this.boldCheckbox.setEnabled(enableOptions);
		this.italicCheckbox.setVisible(showOptions >= 2);
		this.italicCheckbox.setEnabled(enableOptions);
		if (isTextAttributesSupported()) {
			this.strikethroughCheckbox.setVisible(showOptions >= 2);
			this.strikethroughCheckbox.setEnabled(enableOptions);
			this.underlineCheckbox.setVisible(showOptions >= 2);
			this.underlineCheckbox.setEnabled(enableOptions);
		}
	}
	
	@Override
	protected void handlePropertyChange() {
		if (UIAccess.isOkToUse(this.previewViewer)) {
			final Map<String, Object> options= new HashMap<>();
			if (this.textStyles != null) {
				this.textStyles.handleSettingsChanged(this.groupIds, options);
			}
			this.configuration.handleSettingsChanged(this.groupIds, options);
			this.previewViewer.invalidateTextPresentation();
		}
	}
	
	@Override
	public void dispose() {
		if (this.dbc != null) {
			this.dbc.dispose();
			this.dbc= null;
		}
		if (this.colorManager != null) {
			this.colorManager.dispose();
			this.colorManager= null;
		}
		super.dispose();
	}
	
	protected String addListToTooltip(final String tooltip, final String[] listItems) {
		final StringBuilder description= new StringBuilder(tooltip);
		final int end= Math.min(20, listItems.length);
		for (int i= 0; i < end; i++) {
			description.append("\n    ");  //$NON-NLS-1$
			description.append(listItems[i]);
		}
		if (end < listItems.length) {
			description.append("\n    ... (" + listItems.length + ')'); //$NON-NLS-1$
		}
		return MessageUtil.escapeForTooltip(description);
	}
	
	protected String addExtraStyleNoteToTooltip(final String tooltip) {
		return NLS.bind(tooltip, Messages.SyntaxColoring_MindExtraStyle_tooltip);
	}
	
}
