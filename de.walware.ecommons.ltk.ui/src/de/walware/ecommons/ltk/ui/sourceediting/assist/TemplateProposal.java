/*******************************************************************************
 * Copyright (c) 2005-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.util.Comparator;

import com.ibm.icu.text.Collator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.InclusivePositionUpdater;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ICommonStatusConstants;
import de.walware.ecommons.text.ui.DefaultBrowserInformationInput;
import de.walware.ecommons.text.ui.PositionBasedCompletionProposal;
import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.sourceediting.ITextEditToolSynchronizer;
import de.walware.ecommons.ltk.ui.templates.IWorkbenchTemplateContext;


/**
 * Like default {@link org.eclipse.jface.text.templates.TemplateProposal}, but
 * <ul>
 *   <li>supports {@link ITextEditToolSynchronizer}</li>
 * </ul>
 */
public class TemplateProposal implements IAssistCompletionProposal,
		ICompletionProposalExtension, ICompletionProposalExtension3, ICompletionProposalExtension4,
		ICompletionProposalExtension5, ICompletionProposalExtension6 {
	
	public static class TemplateComparator implements Comparator<TemplateProposal> {
		
		private final Collator fgCollator = Collator.getInstance();
		
		@Override
		public int compare(final TemplateProposal arg0, final TemplateProposal arg1) {
			final int result = fgCollator.compare(arg0.getTemplate().getName(), arg1.getTemplate().getName());
			if (result != 0) {
				return result;
			}
			return fgCollator.compare(arg0.getDisplayString(), arg1.getDisplayString());
		}
		
	}
	
	
	private final Template fTemplate;
	private final TemplateContext fContext;
	private final int fRelevance;
	private final Image fImage;
	private StyledString fDisplayString;
	private IRegion fRegion;
	
	private IRegion fSelectionToSet; // initialized by apply()
	private InclusivePositionUpdater fUpdater;
	private String fId;
	
	
	public TemplateProposal(final Template template, final TemplateContext context,
			final IRegion region, final Image image, final int relevance) {
		assert (template != null);
		assert (context != null);
		assert (region != null);
		
		fTemplate = template;
		fContext = context;
		fImage = image;
		fRegion = region;
		
		fDisplayString = null;
		
		fRelevance = relevance;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void selected(final ITextViewer textViewer, final boolean smartToggle) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unselected(final ITextViewer textViewer) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
		try {
			final int replaceOffset = getReplaceOffset();
			if (offset >= replaceOffset) {
				final String content = document.get(replaceOffset, offset - replaceOffset);
				return fTemplate.getName().regionMatches(true, 0, content, 0, content.length());
			}
		} catch (final BadLocationException e) {
			// concurrent modification - ignore
		}
		return false;
	}
	
	
	protected TemplateContext getContext() {
		return fContext;
	}
	
	protected Template getTemplate() {
		return fTemplate;
	}
	
	@Override
	public boolean isValidFor(final IDocument document, final int offset) {
		// not called anymore
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public char[] getTriggerCharacters() {
		// no triggers
		return new char[0];
	}
	
	/**
	 * Returns the relevance.
	 *
	 * @return the relevance
	 */
	@Override
	public int getRelevance() {
		return fRelevance;
	}
	
	@Override
	public String getSortingString() {
		return fTemplate.getName();
	}
	
	@Override
	public boolean isAutoInsertable() {
		return fTemplate.isAutoInsertable();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayString() {
		return getStyledDisplayString().getString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public StyledString getStyledDisplayString() {
		if (fDisplayString == null) {
			final StyledString s = new StyledString(fTemplate.getName());
			s.append(" – " + fTemplate.getDescription(), StyledString.QUALIFIER_STYLER);
			fDisplayString = s;
		}
		return fDisplayString;
	}
	
	@Override
	public Image getImage() {
		return fImage;
	}
	
	@Override
	public IInformationControlCreator getInformationControlCreator() {
		return null;
	}
	
	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}
	
	@Override
	public Object getAdditionalProposalInfo(final IProgressMonitor monitor) {
		try {
			final TemplateContext context = getContext();
			context.setReadOnly(true);
			if (context instanceof IWorkbenchTemplateContext) {
				return new DefaultBrowserInformationInput(
						null, getDisplayString(), ((IWorkbenchTemplateContext) context).evaluateInfo(getTemplate()), 
						DefaultBrowserInformationInput.FORMAT_SOURCE_INPUT);
			}
				
			final TemplateBuffer templateBuffer = context.evaluate(getTemplate());
			if (templateBuffer != null) {
				return new DefaultBrowserInformationInput(
						null, getDisplayString(), templateBuffer.toString(), 
						DefaultBrowserInformationInput.FORMAT_SOURCE_INPUT);
			}
		}
		catch (final TemplateException e) { }
		catch (final BadLocationException e) { }
		return null;
	}
	
	@Override
	public void apply(final IDocument document) {
		// not called anymore
	}
	
	@Override
	public void apply(final IDocument document, final char trigger, final int offset) {
		// not called anymore
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void apply(final ITextViewer viewer, final char trigger, final int stateMask, final int offset) {
		final IDocument document = viewer.getDocument();
		final Position regionPosition = new Position(fRegion.getOffset(), fRegion.getLength());
		final Position offsetPosition = new Position(offset, 0);
		try {
			document.addPosition(regionPosition);
			document.addPosition(offsetPosition);
			fContext.setReadOnly(false);
			TemplateBuffer templateBuffer;
			try {
				templateBuffer = fContext.evaluate(fTemplate);
			}
			catch (final TemplateException e1) {
				fSelectionToSet = new Region(fRegion.getOffset(), fRegion.getLength());
				return;
			}
			
			fRegion = new Region(regionPosition.getOffset(), regionPosition.getLength());
			final int start = getReplaceOffset();
			final int end = Math.max(getReplaceEndOffset(), offsetPosition.getOffset());
			
			// insert template string
			final String templateString = templateBuffer.getString();
			document.replace(start, end - start, templateString);
			
			// translate positions
			final LinkedModeModel model = new LinkedModeModel();
			final TemplateVariable[] variables = templateBuffer.getVariables();
			boolean hasPositions = false;
			for (int i = 0; i != variables.length; i++) {
				final TemplateVariable variable = variables[i];
				
				if (variable.isUnambiguous()) {
					continue;
				}
				
				final LinkedPositionGroup group = new LinkedPositionGroup();
				
				final int[] offsets = variable.getOffsets();
				final int length = variable.getLength();
				
				final String[] values = variable.getValues();
				final ICompletionProposal[] proposals = new ICompletionProposal[values.length];
				for (int j = 0; j < values.length; j++) {
					ensurePositionCategoryInstalled(document, model);
					final Position pos = new Position(offsets[0] + start, length);
					document.addPosition(getCategory(), pos);
					proposals[j] = new PositionBasedCompletionProposal(values[j], pos, length);
				}
				
				for (int j = 0; j < offsets.length; j++) {
					if (j == 0 && proposals.length > 1) {
						group.addPosition(new ProposalPosition(document, offsets[j] + start, length, proposals));
					} else {
						group.addPosition(new LinkedPosition(document, offsets[j] + start, length));
					}
				}
				
				model.addGroup(group);
				hasPositions = true;
			}
			
			if (hasPositions) {
				model.forceInstall();
				
				if (fContext instanceof IWorkbenchTemplateContext) {
					final ISourceEditor editor = ((IWorkbenchTemplateContext) fContext).getEditor();
					final ITextEditToolSynchronizer synch = editor.getTextEditToolSynchronizer();
					if (synch != null) {
						synch.install(model);
					}
				}
				
				final LinkedModeUI ui = new LinkedModeUI(model, viewer);
				ui.setExitPosition(viewer, getCaretOffset(templateBuffer) + start, 0, Integer.MAX_VALUE);
				ui.enter();
				
				fSelectionToSet = ui.getSelectedRegion();
			} else {
				ensurePositionCategoryRemoved(document);
				fSelectionToSet = new Region(getCaretOffset(templateBuffer) + start, 0);
			}
			
		}
		catch (final BadLocationException e) {
			handleError(e);
		}
		catch (final BadPositionCategoryException e) {
			handleError(e);
		}
		finally {
			document.removePosition(regionPosition);
			document.removePosition(offsetPosition);
		}
	}
	
	
	private void handleError(final Exception e) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID,
				ICommonStatusConstants.INTERNAL_TEMPLATE, "Template Evaluation Error", e));
		fSelectionToSet = fRegion;
	}
	
	private String getCategory() {
		return "TemplateProposalCategory_" + toString(); //$NON-NLS-1$
	}
	
	private void ensurePositionCategoryInstalled(final IDocument document, final LinkedModeModel model) {
		if (!document.containsPositionCategory(getCategory())) {
			document.addPositionCategory(getCategory());
			fUpdater = new InclusivePositionUpdater(getCategory());
			document.addPositionUpdater(fUpdater);
			
			model.addLinkingListener(new ILinkedModeListener() {
				
				@Override
				public void left(final LinkedModeModel environment, final int flags) {
					ensurePositionCategoryRemoved(document);
				}
				
				@Override
				public void suspend(final LinkedModeModel environment) {}
				@Override
				public void resume(final LinkedModeModel environment, final int flags) {}
			});
		}
	}
	
	private void ensurePositionCategoryRemoved(final IDocument document) {
		if (document.containsPositionCategory(getCategory())) {
			try {
				document.removePositionCategory(getCategory());
			} catch (final BadPositionCategoryException e) {
				// ignore
			}
			document.removePositionUpdater(fUpdater);
		}
	}
	
	private int getCaretOffset(final TemplateBuffer buffer) {
		final TemplateVariable[] variables = buffer.getVariables();
		for (int i = 0; i != variables.length; i++) {
			final TemplateVariable variable = variables[i];
			if (variable.getType().equals(GlobalTemplateVariables.Cursor.NAME)) {
				return variable.getOffsets()[0];
			}
		}
		return buffer.getString().length();
	}
	
	/**
	 * Returns the offset of the range in the document that will be replaced by
	 * applying this template.
	 * 
	 * @return the offset of the range in the document that will be replaced by
	 *     applying this template
	 */
	protected final int getReplaceOffset() {
		int start;
		if (fContext instanceof DocumentTemplateContext) {
			final DocumentTemplateContext docContext = (DocumentTemplateContext) fContext;
			start = docContext.getStart();
		} else {
			start = fRegion.getOffset();
		}
		return start;
	}
	
	/**
	 * Returns the end offset of the range in the document that will be replaced
	 * by applying this template.
	 * 
	 * @return the end offset of the range in the document that will be replaced
	 *     by applying this template
	 */
	protected final int getReplaceEndOffset() {
		int end;
		if (fContext instanceof DocumentTemplateContext) {
			final DocumentTemplateContext docContext = (DocumentTemplateContext) fContext;
			end = docContext.getEnd();
		} else {
			end = fRegion.getOffset() + fRegion.getLength();
		}
		return end;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public CharSequence getPrefixCompletionText(final IDocument document, final int completionOffset) {
		return fTemplate.getName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPrefixCompletionStart(final IDocument document, final int completionOffset) {
		return getReplaceOffset();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Point getSelection(final IDocument document) {
		if (fSelectionToSet != null) {
			return new Point(fSelectionToSet.getOffset(), fSelectionToSet.getLength());
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getContextInformationPosition() {
		return fRegion.getOffset();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
	
}
