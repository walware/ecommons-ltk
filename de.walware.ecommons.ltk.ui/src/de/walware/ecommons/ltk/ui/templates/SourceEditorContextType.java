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

package de.walware.ecommons.ltk.ui.templates;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.IWorkspaceSourceUnit;
import de.walware.ecommons.ltk.internal.ui.TemplatesMessages;


public class SourceEditorContextType extends TemplateContextType {
	
	
	/**
	 * Resolver that resolves to the variable defined in the context.
	 */
	protected static class CodeTemplatesVariableResolver extends TemplateVariableResolver {
		
		public CodeTemplatesVariableResolver(final String type, final String description) {
			super(type, description);
		}
		
		@Override
		protected String resolve(final TemplateContext context) {
			return context.getVariable(getType());
		}
		
	}
	
	protected static class InitialSelectionStart extends TemplateVariableResolver {
		
		public InitialSelectionStart() {
			super(SELECT_START_VARIABLE, TemplatesMessages.Templates_Variable_SelectionBegin_description);  
		}
		
		@Override
		public void resolve(final TemplateVariable variable, final TemplateContext context) {
			variable.setValue(""); //$NON-NLS-1$
			variable.setUnambiguous(true);
		}
	}
	
	protected static class InitialSelectionEnd extends TemplateVariableResolver {
		
		public InitialSelectionEnd() {
			super(SELECT_END_VARIABLE, TemplatesMessages.Templates_Variable_SelectionEnd_description);  
		}
		
		@Override
		public void resolve(final TemplateVariable variable, final TemplateContext context) {
			variable.setValue(""); //$NON-NLS-1$
			variable.setUnambiguous(true);
		}
		
	}
	
	
	/**
	 * Resolver for file name.
	 */
	protected static class File extends SimpleTemplateVariableResolver {
		
		public File() {
			super("file_name", TemplatesMessages.Templates_Variable_File_description);  //$NON-NLS-1$
		}
		
		@Override
		protected String resolve(final TemplateContext context) {
			if (context instanceof IWorkbenchTemplateContext) {
				final ISourceUnit su = ((IWorkbenchTemplateContext) context).getSourceUnit();
				if (su != null) {
					final IElementName elementName = su.getElementName();
					if (elementName != null) {
						return elementName.getSegmentName();
					}
				}
			}
			return ""; //$NON-NLS-1$
		}
	}
	
	/**
	 * Resolver for project name.
	 */
	protected static class Project extends SimpleTemplateVariableResolver {
		
		public Project() {
			super("enclosing_project", TemplatesMessages.Templates_Variable_EnclosingProject_description);  //$NON-NLS-1$
		}
		
		@Override
		protected String resolve(final TemplateContext context) {
			if (context instanceof IWorkbenchTemplateContext) {
				final ISourceUnit su = ((IWorkbenchTemplateContext) context).getSourceUnit();
				if (su instanceof IWorkspaceSourceUnit) {
					return ((IWorkspaceSourceUnit) su).getResource().getProject().getName();
				}
			}
			return ""; //$NON-NLS-1$
		}
	}
	
	/**
	 * Resolver for ToDo-tags. Intend to extend and overwrite {@link #getTag(ISourceUnit)}
	 */
	protected static class Todo extends SimpleTemplateVariableResolver {
		
		public Todo() {
			super("todo", TemplatesMessages.Templates_Variable_ToDo_description);  //$NON-NLS-1$
		}
		
		@Override
		protected String resolve(final TemplateContext context) {
			if (context instanceof IWorkbenchTemplateContext) {
				final String tag = getTag(((IWorkbenchTemplateContext) context).getSourceUnit());
				if (tag != null) {
					return tag;
				}
			}
			return "TODO"; //$NON-NLS-1$
		}
		
		protected String getTag(final ISourceUnit su) {
			return null;
		}
		
	}
	
	private static class MyLineSelection extends GlobalTemplateVariables.LineSelection {
		
		@Override
		protected String resolve(final TemplateContext context) {
			String value = super.resolve(context);
			final int length = value.length();
			if (length > 0 && context instanceof DocumentTemplateContext) {
				final char c = value.charAt(length-1);
				if (c != '\n' && c != '\r') {
					value += TextUtilities.getDefaultLineDelimiter(((DocumentTemplateContext) context).getDocument());
				}
			}
			return value;
		}
		
	}
	
	
	public static final String FILENAME_VARIABLE = "file_name"; //$NON-NLS-1$
	public static final String SELECT_START_VARIABLE = "selection_begin"; //$NON-NLS-1$
	public static final String SELECT_END_VARIABLE = "selection_end"; ////$NON-NLS-1$
	
	
	public SourceEditorContextType(final String id, final String name) {
		super(id, name);
	}
	
	public SourceEditorContextType(final String id) {
		super(id);
	}
	
	public SourceEditorContextType() {
		super();
	}
	
	
	protected void addCommonVariables() {
		addResolver(new GlobalTemplateVariables.Dollar());
		addResolver(new GlobalTemplateVariables.Date());
		addResolver(new GlobalTemplateVariables.Year());
		addResolver(new GlobalTemplateVariables.Time());
		addResolver(new GlobalTemplateVariables.User());
		addResolver(new Project());
	}
	
	protected void addEditorVariables() {
		addResolver(new GlobalTemplateVariables.Cursor());
		addResolver(new GlobalTemplateVariables.WordSelection());
		addResolver(new MyLineSelection());
	}
	
	protected void addSourceUnitGenerationVariables() {
		addResolver(new File());
		addResolver(new InitialSelectionStart());
		addResolver(new InitialSelectionEnd());
	}
	
	
	@Override
	public void resolve(final TemplateBuffer buffer, final TemplateContext context) throws MalformedTreeException, BadLocationException {
		Assert.isNotNull(context);
		final TemplateVariable[] variables= buffer.getVariables();
		
		final IDocument document= new Document(buffer.getString());
		final List<TextEdit> positions = TemplatesUtil.variablesToPositions(variables);
		final List<TextEdit> edits= new ArrayList<TextEdit>(5);
		
		
		// iterate over all variables and try to resolve them
		for (int i= 0; i != variables.length; i++) {
			final TemplateVariable variable= variables[i];
			
			if (variable.isUnambiguous()) {
				continue;
			}
			
			// remember old values
			final int[] oldOffsets= variable.getOffsets();
			final int oldLength= variable.getLength();
			final String oldValue= variable.getDefaultValue();
			
			final String type= variable.getType();
			TemplateVariableResolver resolver = getResolver(type);
			if (resolver == null) {
				resolver = new TemplateVariableResolver();
				resolver.setType(type);
			}
			
			resolver.resolve(variable, context);
			
			final String value= variable.getDefaultValue();
			final String[] ln = document.getLegalLineDelimiters();
			final boolean multiLine = (TextUtilities.indexOf(ln, value, 0)[0] != -1);
			
			if (!oldValue.equals(value)) {
				// update buffer to reflect new value
				for (int k= 0; k != oldOffsets.length; k++) {
					String thisValue = value;
					if (multiLine) {
						final String indent = TemplatesUtil.searchIndentation(document, oldOffsets[k]);
						if (indent.length() > 0) {
							final StringBuilder temp = new StringBuilder(thisValue);
							int offset = 0;
							while (true) {
								final int[] search = TextUtilities.indexOf(ln, temp.toString(), offset);
								if (search[0] == -1) {
									break;
								}
								offset = search[0]+ln[search[1]].length();
								temp.insert(offset, indent);
								offset += indent.length();
							}
							thisValue = temp.toString();
						}
					}
					edits.add(new ReplaceEdit(oldOffsets[k], oldLength, thisValue));
				}
			}
		}
		
		final MultiTextEdit edit= new MultiTextEdit(0, document.getLength());
		edit.addChildren(positions.toArray(new TextEdit[positions.size()]));
		edit.addChildren(edits.toArray(new TextEdit[edits.size()]));
		edit.apply(document, TextEdit.UPDATE_REGIONS);
		
		TemplatesUtil.positionsToVariables(positions, variables);
		
		buffer.setContent(document.get(), variables);
	}
	
	
	@Override
	public int hashCode() {
		return getId().hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SourceEditorContextType)) {
			return false;
		}
		final SourceEditorContextType other = (SourceEditorContextType) obj;
		return getId().equals(other.getId());
	}
	
}
