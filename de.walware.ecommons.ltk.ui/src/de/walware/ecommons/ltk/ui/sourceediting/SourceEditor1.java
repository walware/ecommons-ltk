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

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.templates.ITemplatesPage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.FastList;
import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.SettingsChangeNotifier;
import de.walware.ecommons.text.ICharPairMatcher;
import de.walware.ecommons.text.TextUtil;
import de.walware.ecommons.text.core.sections.IDocContentSections;
import de.walware.ecommons.text.ui.TextHandlerUtil;
import de.walware.ecommons.ui.ISettingsChangedHandler;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.IDocumentModelProvider;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.ast.IAstNode;
import de.walware.ecommons.ltk.core.IModelTypeDescriptor;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceStructElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.ui.ElementInfoController;
import de.walware.ecommons.ltk.ui.IModelElementInputProvider;
import de.walware.ecommons.ltk.ui.ISelectionWithElementInfoListener;
import de.walware.ecommons.ltk.ui.LTKInputData;
import de.walware.ecommons.ltk.ui.LTKUI;
import de.walware.ecommons.ltk.ui.PostSelectionCancelExtension;
import de.walware.ecommons.ltk.ui.PostSelectionWithElementInfoController;
import de.walware.ecommons.ltk.ui.PostSelectionWithElementInfoController.IgnoreActivation;
import de.walware.ecommons.ltk.ui.sourceediting.actions.DeleteNextWordHandler;
import de.walware.ecommons.ltk.ui.sourceediting.actions.DeletePreviousWordHandler;
import de.walware.ecommons.ltk.ui.sourceediting.actions.GotoMatchingBracketHandler;
import de.walware.ecommons.ltk.ui.sourceediting.actions.GotoNextWordHandler;
import de.walware.ecommons.ltk.ui.sourceediting.actions.GotoPreviousWordHandler;
import de.walware.ecommons.ltk.ui.sourceediting.actions.SelectNextWordHandler;
import de.walware.ecommons.ltk.ui.sourceediting.actions.SelectPreviousWordHandler;
import de.walware.ecommons.ltk.ui.sourceediting.actions.ToggleCommentHandler;


/**
 * Abstract LTK based source editor.
 */
public abstract class SourceEditor1 extends TextEditor implements ISourceEditor,
			SettingsChangeNotifier.ChangeListener, IPreferenceChangeListener,
			IShowInSource, IShowInTargetList {
	
	
	public static final String ACTION_ID_TOGGLE_COMMENT = "de.walware.statet.ui.actions.ToggleComment"; //$NON-NLS-1$
	
	protected static final ImList<String> ACTION_SET_CONTEXT_IDS= ImCollections.newIdentityList(
			"de.walware.ecommons.ltk.contexts.EditSource1MenuSet" ); //$NON-NLS-1$
	
	private static final ImList<String> CONTEXT_IDS= ImCollections.addElement(
			ACTION_SET_CONTEXT_IDS,
			"de.walware.ecommons.text.contexts.TextEditor" ); //$NON-NLS-1$
	
	
/*- Static utility methods --------------------------------------------------*/
	
	@Deprecated
	protected static IProjectNature getProject(final IEditorInput input, final String projectNatureId) {
		if (input != null && input instanceof IFileEditorInput) {
			final IProject project = ((IFileEditorInput) input).getFile().getProject();
			try {
				if (project != null && project.hasNature(projectNatureId)) {
					return project.getNature(projectNatureId);
				}
			}
			catch (final CoreException ignore) {}
		}
		return null;
	}
	
	/**
	 * Returns the lock object for the given annotation model.
	 * 
	 * @param annotationModel the annotation model
	 * @return the annotation model's lock object
	 */
	protected static final Object getLockObject(final IAnnotationModel annotationModel) {
		if (annotationModel instanceof ISynchronizable) {
			final Object lock = ((ISynchronizable) annotationModel).getLockObject();
			if (lock != null) {
				return lock;
			}
		}
		return annotationModel;
	}
	
	
/*- Inner classes -----------------------------------------------------------*/
	
	protected class PostSelectionEditorCancel extends PostSelectionCancelExtension {
		
		public PostSelectionEditorCancel() {
		}
		
		@Override
		public void init() {
			final ISourceViewer viewer = getSourceViewer();
			if (viewer != null) {
				viewer.addTextInputListener(this);
				viewer.getDocument().addDocumentListener(this);
			}
		}
		
		@Override
		public void dispose() {
			final ISourceViewer viewer = getSourceViewer();
			if (viewer != null) {
				viewer.removeTextInputListener(this);
				final IDocument document = viewer.getDocument();
				if (document != null) {
					document.removeDocumentListener(this);
				}
			}
		}
	}
	
	private class EffectSynchonizer implements ITextEditToolSynchronizer, ILinkedModeListener {
		
		private EffectSynchonizer() {
		}
		
		@Override
		public void install(final LinkedModeModel model) {
			fEffectSynchonizerCounter++;
			if (fMarkOccurrencesProvider != null) {
				fMarkOccurrencesProvider.uninstall();
			}
			model.addLinkingListener(this);
		}
		
		@Override
		public void left(final LinkedModeModel model, final int flags) {
			fEffectSynchonizerCounter--;
			updateMarkOccurrencesEnablement();
		}
		
		@Override
		public void resume(final LinkedModeModel model, final int flags) {
		}
		
		@Override
		public void suspend(final LinkedModeModel model) {
		}
		
	}
	
	
/*- Fields -----------------------------------------------------------------*/
	
	private final IContentType contentType;
	private final IModelTypeDescriptor modelType;
	
	private SourceEditorViewerConfigurator fConfigurator;
	private boolean fLazySetup;
	private ISourceUnit fSourceUnit;
	private ElementInfoController fModelProvider;
	private PostSelectionWithElementInfoController fModelPostSelection;
	protected volatile Point fCurrentSelection;
	
	/** The outline page of this editor */
	private SourceEditor1OutlinePage fOutlinePage;
	
	/** The templates page of this editor */
	private ITemplatesPage fTemplatesPage;
	
	private StructureSelectionHistory fSelectionHistory;
	private Preference<Boolean> fFoldingEnablement;
	private ProjectionSupport fFoldingSupport;
	private ISourceEditorAddon fFoldingProvider;
	private FoldingActionGroup fFoldingActionGroup;
	private Preference<Boolean> fMarkOccurrencesEnablement;
	private ISourceEditorAddon fMarkOccurrencesProvider;
	
	private EffectSynchonizer fEffectSynchronizer;
	private int fEffectSynchonizerCounter;
	
	private final FastList<IUpdate> fContentUpdateables = new FastList<>(IUpdate.class);
	private final FastList<IHandler2> fStateUpdateables = new FastList<>(IHandler2.class);
	
	private boolean fInputChange;
	private int fInputUpdate = Integer.MAX_VALUE;
	
	private ImageDescriptor fImageDescriptor;
	
	
/*- Contructors ------------------------------------------------------------*/
	
	public SourceEditor1(final IContentType contentType) {
		super();
		this.contentType= contentType;
		this.modelType= LTK.getExtContentTypeManager().getModelTypeForContentType(contentType.getId());
	}
	
	
/*- Methods ----------------------------------------------------------------*/
	
	@Override
	public IContentType getContentType() {
		return this.contentType;
	}
	
	/**
	 * Returns the model type of source units of the editor.
	 * The value must not change for an editor instance and all source units
	 * in the editor must be of the same type.
	 * 
	 * @return id of the model type
	 */
	public String getModelTypeId() {
		return this.modelType.getId();
	}
	
	@Override
	protected void initializeEditor() {
		fConfigurator = createConfiguration();
		super.initializeEditor();
		setCompatibilityMode(false);
		final SourceEditorViewerConfiguration configuration = fConfigurator.getSourceViewerConfiguration();
		setPreferenceStore(configuration.getPreferences());
		setSourceViewerConfiguration(configuration);
		if (configuration.isSmartInsertSupported()) {
			configureInsertMode(SMART_INSERT, true);
		}
		
		PreferencesUtil.getSettingsChangeNotifier().addChangeListener(this);
	}
	
	protected abstract SourceEditorViewerConfigurator createConfiguration();
	
	protected SourceEditorViewerConfigurator createInfoConfigurator() {
		return null;
	}
	
	
	protected void enableStructuralFeatures(final IModelManager modelManager,
			final Preference<Boolean> codeFoldingEnablement,
			final Preference<Boolean> markOccurrencesEnablement) {
		fModelProvider = new ElementInfoController(modelManager, LTK.EDITOR_CONTEXT);
		fFoldingEnablement = codeFoldingEnablement;
		fMarkOccurrencesEnablement = markOccurrencesEnablement;
	}
	
	/**
	 * Overwrites the default title image (editor icon) during the initialization of the editor
	 * input.
	 * 
	 * The image is created and disposed automatically.
	 * 
	 * For example, it can be used to overwrite the default image using the image descriptor of 
	 * the editor input in {@link #setDocumentProvider(IEditorInput)}
	 * 
	 * @param descriptor the image description of the icon or <code>null</code>
	 */
	protected void overwriteTitleImage(final ImageDescriptor descriptor) {
		if (fImageDescriptor == descriptor || (fImageDescriptor == null && descriptor == null) ) {
			return;
		}
		if (fImageDescriptor != null) {
			JFaceResources.getResources().destroyImage(fImageDescriptor);
		}
		fImageDescriptor = descriptor;
		if (fImageDescriptor != null) {
			super.setTitleImage(JFaceResources.getResources().createImage(descriptor));
		}
	}
	
	@Override
	protected void setTitleImage(final Image titleImage) {
		if (fImageDescriptor == null) {
			super.setTitleImage(titleImage);
		}
	}
	
	@Override
	protected void initializeKeyBindingScopes() {
		setContexts(CONTEXT_IDS);
	}
	
	protected void setContexts(final Collection<String> ids) {
		setKeyBindingScopes(ids.toArray(new String[ids.size()]));
	}
	
	@Override
	protected String[] collectContextMenuPreferencePages() {
		final List<String> list = new ArrayList<>();
		collectContextMenuPreferencePages(list);
		list.addAll(Arrays.asList(super.collectContextMenuPreferencePages()));
		return list.toArray(new String[list.size()]);
	}
	
	protected void collectContextMenuPreferencePages(final List<String> pageIds) {
	}
	
	
	@Override
	protected void doSetInput(final IEditorInput input) throws CoreException {
		if (fModelProvider != null && fSourceUnit != null) {
			fModelProvider.setInput(null);
		}
		
		// project has changed
		final ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer != null) {
			fConfigurator.unconfigureTarget();
		}
		else {
			fLazySetup = true;
		}
		
		fInputChange = true;
		fInputUpdate = 1;
		super.doSetInput(input);
		// setup in 
		//   1) setDocumentProvider -> setupConfiguration(..., input)
		//   2) handleInsertModeChanged -> setupConfiguration(..., input, SourceViewer)
		fInputChange = false;
		fInputUpdate = Integer.MAX_VALUE;
		
		initSmartInsert();
		
		if (input != null && fOutlinePage != null) {
			updateOutlinePageInput(fOutlinePage);
		}
	}
	
	private void initSmartInsert() {
		final SourceEditorViewerConfiguration config = fConfigurator.getSourceViewerConfiguration();
		if (config.isSmartInsertSupported()) {
			if (config.isSmartInsertByDefault()) {
				setInsertMode(SMART_INSERT);
			}
			else {
				setInsertMode(INSERT);
			}
		}
	}
	
	@Override
	protected void setPartName(final String partName) {
		super.setPartName(partName);
		
		// see doSetInput
		if (fInputChange) {
			if (fInputUpdate != 1) {
				return;
			}
			fInputUpdate = 2;
			final IEditorInput input = getEditorInput();
			setupConfiguration(input);
		}
	}
	
	@Override
	protected void handleInsertModeChanged() {
		// see doSetInput
		if (fInputChange && !fLazySetup) {
			if (fInputUpdate != 2) {
				return;
			}
			fInputUpdate = 3;
			final IEditorInput input = getEditorInput();
			final ISourceViewer sourceViewer = getSourceViewer();
			if (input != null && sourceViewer != null) {
				setupConfiguration(input, sourceViewer);
				fConfigurator.configureTarget();
			}
			fInputChange = false;
		}
		
		super.handleInsertModeChanged();
	}
	
	/**
	 * Subclasses should setup the SourceViewerConfiguration.
	 */
	protected void setupConfiguration(final IEditorInput newInput) {
		final IDocumentProvider documentProvider = getDocumentProvider();
		if (documentProvider instanceof IDocumentModelProvider) {
			fSourceUnit = ((IDocumentModelProvider) documentProvider).getWorkingCopy(newInput);
			if (fModelProvider != null) {
				fModelProvider.setInput(fSourceUnit);
			}
		}
	}
	
	/**
	 * Subclasses should setup the SourceViewerConfiguration.
	 */
	protected void setupConfiguration(final IEditorInput newInput, final ISourceViewer sourceViewer) {
		updateStateDependentActions();
	}
	
	@Override
	public ISourceUnit getSourceUnit() {
		return fSourceUnit;
	}
	
	
	@Override
	public SourceViewer getViewer() {
		return (SourceViewer) super.getSourceViewer();
	}
	
	@Override
	public IDocContentSections getDocumentContentInfo() {
		return fConfigurator.getDocumentContentInfo();
	}
	
	@Override
	public IWorkbenchPart getWorkbenchPart() {
		return this;
	}
	
	@Override
	public IServiceLocator getServiceLocator() {
		return getSite();
	}
	
	@Override
	public boolean isEditable(final boolean validate) {
		if (validate) {
			return SourceEditor1.this.validateEditorInputState();
		}
		return SourceEditor1.this.isEditorInputModifiable();
	}
	
	public IModelElementInputProvider getModelInputProvider() {
		return fModelProvider;
	}
	
	public void addPostSelectionWithElementInfoListener(final ISelectionWithElementInfoListener listener) {
		if (fModelPostSelection != null) {
			fModelPostSelection.addListener(listener);
		}
	}
	
	public void removePostSelectionWithElementInfoListener(final ISelectionWithElementInfoListener listener) {
		if (fModelPostSelection != null) {
			fModelPostSelection.removeListener(listener);
		}
	}
	
	
	@Override
	public void createPartControl(final Composite parent) {
		super.createPartControl(parent);
		
		if (fModelProvider != null) {
			fModelPostSelection = new PostSelectionWithElementInfoController(fModelProvider,
					(IPostSelectionProvider) getSelectionProvider(), new PostSelectionEditorCancel());
			fModelPostSelection.addListener(new ISelectionWithElementInfoListener() {
				@Override
				public void inputChanged() {
				}
				@Override
				public void stateChanged(final LTKInputData state) {
					final IRegion toHighlight = getRangeToHighlight(state);
					if (toHighlight != null) {
						setHighlightRange(toHighlight.getOffset(), toHighlight.getLength(), false);
					}
					else {
						resetHighlightRange();
					}
				}
			});
		}
		if (fFoldingEnablement != null) {
			final ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
			
			fFoldingSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
			final SourceEditorViewerConfigurator config = createInfoConfigurator();
			if (config != null) {
				final IInformationControlCreator presentationCreator = new IInformationControlCreator() {
					@Override
					public IInformationControl createInformationControl(final Shell parent) {
						return new SourceViewerInformationControl(parent, createInfoConfigurator(), getOrientation());
					}
				};
				fFoldingSupport.setHoverControlCreator(new IInformationControlCreator() {
					@Override
					public IInformationControl createInformationControl(final Shell parent) {
						return new SourceViewerInformationControl(parent, createInfoConfigurator(), getOrientation(), presentationCreator);
					}
				});
				fFoldingSupport.setInformationPresenterControlCreator(presentationCreator);
			}
			fFoldingSupport.install();
			viewer.addProjectionListener(new IProjectionListener() {
				@Override
				public void projectionEnabled() {
					installFoldingProvider();
				}
				@Override
				public void projectionDisabled() {
					uninstallFoldingProvider();
				}
			});
			PreferencesUtil.getInstancePrefs().addPreferenceNodeListener(
					fFoldingEnablement.getQualifier(), this);
			updateFoldingEnablement();
		}
		if (fMarkOccurrencesEnablement != null) {
			PreferencesUtil.getInstancePrefs().addPreferenceNodeListener(
					fMarkOccurrencesEnablement.getQualifier(), this);
			updateMarkOccurrencesEnablement();
		}
		
		if (fLazySetup) {
			fLazySetup = false;
			setupConfiguration(getEditorInput(), getSourceViewer());
			fConfigurator.setTarget(this);
		}
	}
	
	@Override
	protected ISourceViewer createSourceViewer(final Composite parent, final IVerticalRuler ruler, final int styles) {
		fAnnotationAccess = getAnnotationAccess();
		fOverviewRuler = createOverviewRuler(getSharedColors());
		
		final ISourceViewer viewer = new SourceEditorViewer(parent,
				ruler, getOverviewRuler(), isOverviewRulerVisible(), styles,
				getSourceViewerFlags() );
		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		
		return viewer;
	}
	
	protected int getSourceViewerFlags() {
		return 0;
	}
	
	protected IRegion getRangeToReveal(final ISourceUnitModelInfo modelInfo, final ISourceStructElement element) {
		return null;
	}
	
	protected IRegion getRangeToHighlight(final LTKInputData state) {
		final ISourceUnitModelInfo info = state.getInputInfo();
		if (info == null) {
			return null;
		}
		
		final IRegion region= getRangeToHighlight(info, state.getModelSelection());
		if (region != null) {
			return region;
		}
		
		final IAstNode root = info.getAst().root;
		TRY_AST: if (root != null) {
			final ITextSelection selection = (ITextSelection) state.getSelection();
			final int n = root.getChildCount();
			for (int i = 0; i < n; i++) {
				final IAstNode child = root.getChild(i);
				if (selection.getOffset() >= child.getOffset()) {
					if (selection.getOffset()+selection.getLength() <= child.getEndOffset()) {
						return child;
					}
				}
				else {
					break TRY_AST;
				}
			}
		}
		return null;
	}
	
	protected IRegion getRangeToHighlight(final ISourceUnitModelInfo info, ISourceStructElement element) {
		while (element != null) {
			switch (element.getElementType() & IModelElement.MASK_C1) {
			case IModelElement.C1_CLASS:
			case IModelElement.C1_METHOD:
				return TextUtil.expand(element.getSourceRange(), element.getDocumentationRange());
			case IModelElement.C1_SOURCE:
				if ((element.getElementType() & IModelElement.MASK_C2) == IModelElement.C2_SOURCE_CHUNK) {
					return TextUtil.expand(element.getSourceRange(), element.getDocumentationRange());
				}
				return null;
			case IModelElement.C1_VARIABLE:
				if ((element.getSourceParent().getElementType() & IModelElement.MASK_C2) == IModelElement.C2_SOURCE_FILE) {
					return TextUtil.expand(element.getSourceRange(), element.getDocumentationRange());
				}
				//$FALL-THROUGH$
			default:
				element = element.getSourceParent();
				continue;
			}
		}
		return null;
	}
	
	
	protected ISourceEditorAddon createCodeFoldingProvider() {
		return null;
	}
	
	private void installFoldingProvider() {
		uninstallFoldingProvider();
		fFoldingProvider = createCodeFoldingProvider();
		if (fFoldingProvider != null) {
			fFoldingProvider.install(this);
		}
	}
	
	private void uninstallFoldingProvider() {
		if (fFoldingProvider != null) {
			fFoldingProvider.uninstall();
			fFoldingProvider = null;
		}
	}
	
	private void updateFoldingEnablement() {
		if (fFoldingEnablement != null) {
			UIAccess.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					final Boolean enable = PreferencesUtil.getInstancePrefs().getPreferenceValue(fFoldingEnablement);
					final ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
					if (enable != null && UIAccess.isOkToUse(viewer)) {
						if (enable != viewer.isProjectionMode()) {
							viewer.doOperation(ProjectionViewer.TOGGLE);
						}
					}
				}
			});
		}
	}
	
	
	protected ISourceEditorAddon createMarkOccurrencesProvider() {
		return null;
	}
	
	private void uninstallMarkOccurrencesProvider() {
		if (fMarkOccurrencesProvider != null) {
			fMarkOccurrencesProvider.uninstall();
			fMarkOccurrencesProvider = null;
		}
	}
	
	private void updateMarkOccurrencesEnablement() {
		if (fMarkOccurrencesEnablement != null) {
			UIAccess.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					final Boolean enable = PreferencesUtil.getInstancePrefs().getPreferenceValue(fMarkOccurrencesEnablement);
					if (enable) {
						if (fMarkOccurrencesProvider == null) {
							fMarkOccurrencesProvider = createMarkOccurrencesProvider();
						}
						if (fMarkOccurrencesProvider != null && fEffectSynchonizerCounter == 0) {
							fMarkOccurrencesProvider.install(SourceEditor1.this);
						}
					}
					else {
						uninstallMarkOccurrencesProvider();
					}
				}
			});
		}
	}
	
	
	@Override
	protected void configureSourceViewerDecorationSupport(final SourceViewerDecorationSupport support) {
		super.configureSourceViewerDecorationSupport(support);
		fConfigurator.configureSourceViewerDecorationSupport(support);
	}
	
	@Override
	protected void createActions() {
		super.createActions();
		final IHandlerService handlerService = (IHandlerService) getServiceLocator().getService(IHandlerService.class);
		final StyledText textWidget = getViewer().getTextWidget();
		
		{	final IHandler2 handler = new GotoNextWordHandler(this);
			setAction(ITextEditorActionDefinitionIds.WORD_NEXT, null);
			TextHandlerUtil.disable(textWidget, ITextEditorActionDefinitionIds.WORD_NEXT);
			handlerService.activateHandler(ITextEditorActionDefinitionIds.WORD_NEXT, handler);
		}
		{	final IHandler2 handler = new GotoPreviousWordHandler(this);
			setAction(ITextEditorActionDefinitionIds.WORD_PREVIOUS, null);
			TextHandlerUtil.disable(textWidget, ITextEditorActionDefinitionIds.WORD_NEXT);
			handlerService.activateHandler(ITextEditorActionDefinitionIds.WORD_PREVIOUS, handler);
		}
		{	final IHandler2 handler = new SelectNextWordHandler(this);
			setAction(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, null);
			TextHandlerUtil.disable(textWidget, ITextEditorActionDefinitionIds.SELECT_WORD_NEXT);
			handlerService.activateHandler(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, handler);
		}
		{	final IHandler2 handler = new SelectPreviousWordHandler(this);
			setAction(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, null);
			TextHandlerUtil.disable(textWidget, ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS);
			handlerService.activateHandler(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, handler);
		}
		{	final IHandler2 handler = new DeleteNextWordHandler(this);
			setAction(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, null);
			TextHandlerUtil.disable(textWidget, ITextEditorActionDefinitionIds.DELETE_NEXT_WORD);
			handlerService.activateHandler(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, handler);
			markAsStateDependentHandler(handler, true);
		}
		{	final IHandler2 handler = new DeletePreviousWordHandler(this);
			setAction(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, null);
			TextHandlerUtil.disable(textWidget, ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD);
			handlerService.activateHandler(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, handler);
			markAsStateDependentHandler(handler, true);
		}
		
		final ICharPairMatcher matcher = fConfigurator.getSourceViewerConfiguration().getPairMatcher();
		if (matcher != null) {
			handlerService.activateHandler(ISourceEditorCommandIds.GOTO_MATCHING_BRACKET,
					new GotoMatchingBracketHandler(matcher, this));
		}
		
		{	final IHandler2 handler = createToggleCommentHandler();
			if (handler != null) {
				handlerService.activateHandler(ISourceEditorCommandIds.TOGGLE_COMMENT, handler);
			}
		}
		{	final IHandler2 handler = createCorrectIndentHandler();
			if (handler != null) {
				handlerService.activateHandler(LTKUI.CORRECT_INDENT_COMMAND_ID, handler);
			}
		}
		
		if (fFoldingEnablement != null) {
			fFoldingActionGroup = createFoldingActionGroup();
		}
		if (fModelProvider != null) {
			fSelectionHistory = new StructureSelectionHistory(this);
			handlerService.activateHandler(ISourceEditorCommandIds.SELECT_ENCLOSING,
					new StructureSelectHandler.Enclosing(this, fSelectionHistory));
			handlerService.activateHandler(ISourceEditorCommandIds.SELECT_PREVIOUS,
					new StructureSelectHandler.Previous(this, fSelectionHistory));
			handlerService.activateHandler(ISourceEditorCommandIds.SELECT_NEXT,
					new StructureSelectHandler.Next(this, fSelectionHistory));
			final StructureSelectionHistoryBackHandler backHandler = new StructureSelectionHistoryBackHandler(this, fSelectionHistory);
			handlerService.activateHandler(ISourceEditorCommandIds.SELECT_LAST, backHandler);
			fSelectionHistory.addUpdateListener(backHandler);
		}
		
		//WorkbenchHelp.setHelp(action, IJavaHelpContextIds.TOGGLE_COMMENT_ACTION);
	}
	
	protected FoldingActionGroup createFoldingActionGroup() {
		return new FoldingActionGroup(this, (ProjectionViewer) getSourceViewer());
	}
	
	protected IHandler2 createToggleCommentHandler() {
		final IHandler2 commentHandler = new ToggleCommentHandler(this);
		markAsStateDependentHandler(commentHandler, true);
		return commentHandler;
	}
	
	protected IHandler2 createCorrectIndentHandler() {
		return null;
	}
	
	protected void markAsContentDependentHandler(final IUpdate handler, final boolean mark) {
		if (mark) {
			fContentUpdateables.add(handler);
		}
		else {
			fContentUpdateables.remove(handler);
		}
	}
	
	protected void markAsStateDependentHandler(final IHandler2 handler, final boolean mark) {
		if (mark) {
			fStateUpdateables.add(handler);
		}
		else {
			fStateUpdateables.remove(handler);
		}
	}
	
	@Override
	protected void updateContentDependentActions() {
		super.updateContentDependentActions();
		final IUpdate[] listeners = fContentUpdateables.toArray();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].update();
		}
	}
	
	@Override
	protected void updateStateDependentActions() {
		super.updateStateDependentActions();
		final IHandler2[] listeners = fStateUpdateables.toArray();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].setEnabled(this);
		}
	}
	
	@Override
	protected void rulerContextMenuAboutToShow(final IMenuManager menu) {
		super.rulerContextMenuAboutToShow(menu);
		if (fFoldingActionGroup != null) {
			final IMenuManager foldingMenu = new MenuManager(EditingMessages.CodeFolding_label, "projection"); //$NON-NLS-1$
			menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, foldingMenu);
			fFoldingActionGroup.fillMenu(foldingMenu);
		}
	}
	
	
	@Override
	public ITextEditToolSynchronizer getTextEditToolSynchronizer() {
		if (fEffectSynchronizer == null) {
			fEffectSynchronizer = new EffectSynchonizer();
		}
		return fEffectSynchronizer;
	}
	
	@Override
	public Object getAdapter(final Class required) {
		if (required == ISourceEditor.class) {
			return this;
		}
		if (required == ISourceViewer.class) {
			return getSourceViewer();
		}
		
		if (required == IContentType.class) {
			return this.contentType;
		}
		
		if (required == IContentOutlinePage.class) {
			if (fOutlinePage == null) {
				fOutlinePage = createOutlinePage();
				if (fOutlinePage != null) {
					updateOutlinePageInput(fOutlinePage);
				}
			}
			return fOutlinePage;
		}
		if (required == ITemplatesPage.class) {
			if (fTemplatesPage == null) {
				fTemplatesPage = createTemplatesPage();
			}
			return fTemplatesPage;
		}
		if (fFoldingSupport != null) {
			final Object adapter = fFoldingSupport.getAdapter(getSourceViewer(), required);
			if (adapter != null) {
				return adapter;
			}
		}
		
		return super.getAdapter(required);
	}
	
	
	@Override
	public void settingsChanged(final Set<String> groupIds) {
		final Map<String, Object> options = new HashMap<>();
		UIAccess.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				handleSettingsChanged(groupIds, options);
			}
		});
	}
	
	/**
	 * @see ISettingsChangedHandler#handleSettingsChanged(Set, Map)
	 */
	protected void handleSettingsChanged(final Set<String> groupIds, final Map<String, Object> options) {
		if (fConfigurator != null) {
			fConfigurator.handleSettingsChanged(groupIds, options);
		}
	}
	
	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		if (fFoldingEnablement != null && event.getKey().equals(fFoldingEnablement.getKey())) {
			updateFoldingEnablement();
		}
		if (fMarkOccurrencesEnablement != null && event.getKey().equals(fMarkOccurrencesEnablement.getKey())) {
			updateMarkOccurrencesEnablement();
		}
	}
	
	protected void updateIndentSettings() {
		updateIndentPrefixes();
	}
	
	
	@Override
	protected void handleCursorPositionChanged() {
		fCurrentSelection = getSourceViewer().getSelectedRange();
		super.handleCursorPositionChanged();
	}
	
	
	protected SourceEditor1OutlinePage createOutlinePage() {
		return null;
	}
	
	protected void updateOutlinePageInput(final SourceEditor1OutlinePage page) {
	}
	
	void handleOutlinePageClosed() {
		if (fOutlinePage != null) {
			fOutlinePage = null;
			resetHighlightRange();
		}
	}
	
	protected ITemplatesPage createTemplatesPage() {
		return null;
	}
	
	
	@Override
	// inject annotation painter workaround
	protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(final ISourceViewer viewer) {
		if (fSourceViewerDecorationSupport == null) {
			fSourceViewerDecorationSupport= new de.walware.epatches.ui.SourceViewerDecorationSupport(viewer, getOverviewRuler(), getAnnotationAccess(), getSharedColors());
			configureSourceViewerDecorationSupport(fSourceViewerDecorationSupport);
		}
		return fSourceViewerDecorationSupport;
	}
	
	@Override
	public void selectAndReveal(final int start, final int length) {
		selectAndReveal(start, length, start, length);
	}
	
	@Override
	protected void selectAndReveal(final int selectionStart, final int selectionLength, final int revealStart, final int revealLength) {
		if (fModelPostSelection != null) {
			fModelPostSelection.setUpdateOnSelection(true);
			try {
				super.selectAndReveal(selectionStart, selectionLength, revealStart, revealLength);
			}
			finally {
				fModelPostSelection.setUpdateOnSelection(false);
			}
		}
		else {
			super.selectAndReveal(selectionStart, selectionLength, revealStart, revealLength);
		}
	}
	
	public void setSelection(final ISelection selection, final ISelectionWithElementInfoListener listener) {
		if (fModelPostSelection != null && listener != null) {
			final IgnoreActivation activation = fModelPostSelection.ignoreNext(listener);
			doSetSelection(selection);
			activation.deleteNext();
		}
		else {
			doSetSelection(selection);
		}
	}
	
	@Override
	protected void doSetSelection(final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structured= (IStructuredSelection) selection;
			if (!structured.isEmpty()) {
				final Object first= structured.getFirstElement();
				IRegion region= null;
				if (first instanceof ISourceStructElement) {
					final ISourceStructElement sourceElement= (ISourceStructElement) first;
					region= sourceElement.getNameSourceRange();
					if (region == null) {
						region= sourceElement.getSourceRange();
						if (region != null) {
							region= new Region(region.getOffset(), 0);
						}
					}
					
					final ISourceUnit sourceUnit= sourceElement.getSourceUnit();
					final ISourceUnitModelInfo modelInfo= sourceUnit.getModelInfo(getModelTypeId(), 0, null);
					if (modelInfo != null) {
						final IRegion toReveal= getRangeToReveal(modelInfo, sourceElement);
						if (toReveal != null) {
							final SourceViewer viewer= getViewer();
							if (viewer instanceof ITextViewerExtension5) {
								((ITextViewerExtension5) viewer).exposeModelRange(toReveal);
							}
							getViewer().revealRange(toReveal.getOffset(), toReveal.getLength());
						}
						final IRegion toHighlight= getRangeToHighlight(modelInfo, sourceElement);
						if (toHighlight != null) {
							setHighlightRange(toHighlight.getOffset(), toHighlight.getLength(), true);
						}
					}
				}
				if (region == null && first instanceof IRegion) {
					region= (IRegion) first;
				}
				if (region != null) {
					selectAndReveal(region.getOffset(), region.getLength());
					return;
				}
			}
		}
		super.doSetSelection(selection);
	}
	
	
	@Override
	public void dispose() {
		if (fModelProvider != null) {
			fModelProvider.setInput(null);
			fModelProvider.dispose();
		}
		
		PreferencesUtil.getSettingsChangeNotifier().removeChangeListener(this);
		if (fModelPostSelection != null) {
			fModelPostSelection.dispose();
		}
		if (fFoldingEnablement != null) {
			PreferencesUtil.getInstancePrefs().removePreferenceNodeListener(
					fFoldingEnablement.getQualifier(), this);
			uninstallFoldingProvider();
		}
		if (fMarkOccurrencesEnablement != null) {
			PreferencesUtil.getInstancePrefs().removePreferenceNodeListener(
					fMarkOccurrencesEnablement.getQualifier(), this);
			uninstallMarkOccurrencesProvider();
		}
		
		super.dispose();
		
		if (fImageDescriptor != null) {
			JFaceResources.getResources().destroyImage(fImageDescriptor);
			fImageDescriptor = null;
		}
		
		fSourceUnit = null;
		fModelProvider = null;
		fModelPostSelection = null;
	}
	
	@Override
	public ShowInContext getShowInContext() {
		final Point selectionPoint = fCurrentSelection;
		final ISourceViewer sourceViewer = getSourceViewer();
		final ISourceUnit unit = getSourceUnit();
		ISelection selection = null;
		if (selectionPoint != null && unit != null && sourceViewer != null) {
			selection = new LTKInputData(unit, getSelectionProvider());
		}
		return new ShowInContext(getEditorInput(), selection);
	}
	
	@Override
	public String[] getShowInTargetIds() {
		return new String[] { IPageLayout.ID_PROJECT_EXPLORER };
	}
	
}
