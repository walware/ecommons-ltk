/*=============================================================================#
 # Copyright (c) 2000-2014 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiEditorInput;

import de.walware.ecommons.ltk.ISourceUnitManager;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * A number of routines for working with editors.
 */
public class EditorUtil {
	
	
	/**
	 * Returns an array of all editors that have an unsaved content. If the identical content is
	 * presented in more than one editor, only one of those editor parts is part of the result.
	 * @param skipNonResourceEditors if <code>true</code>, editors whose inputs do not adapt to {@link IResource}
	 * are not saved
	 *
	 * @return an array of dirty editor parts
	 */
	public static List<IEditorPart> getDirtyEditors(final boolean skipNonResourceEditors) {
		final Set inputs = new HashSet();
		final List<IEditorPart> result = new ArrayList<IEditorPart>(0);
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			final IWorkbenchPage[] pages= windows[i].getPages();
			for (int x = 0; x < pages.length; x++) {
				final IEditorPart[] editors= pages[x].getDirtyEditors();
				for (int z = 0; z < editors.length; z++) {
					final IEditorPart ep = editors[z];
					final IEditorInput input = ep.getEditorInput();
					if (inputs.add(input)) {
						if (!skipNonResourceEditors || isResourceEditorInput(input)) {
							result.add(ep);
						}
					}
				}
			}
		}
		return result;
	}
	
	private static boolean isResourceEditorInput(final IEditorInput input) {
		if (input instanceof MultiEditorInput) {
			final IEditorInput[] inputs= ((MultiEditorInput) input).getInput();
			for (int i= 0; i < inputs.length; i++) {
				if (inputs[i].getAdapter(IResource.class) != null) {
					return true;
				}
			}
		} 
		else if (input.getAdapter(IResource.class) != null) {
			return true;
		}
		return false;
	}
	
	public static boolean isModelTypeEditorInput(final IEditorInput input, final String modelTypeId) {
		final Object ifile = input.getAdapter(IFile.class);
		final ISourceUnitManager suManager = LTK.getSourceUnitManager();
		if (ifile != null) {
			final ISourceUnit su = suManager.getSourceUnit(modelTypeId, LTK.PERSISTENCE_CONTEXT, ifile, false, null);
			if (su != null) {
				su.disconnect(null);
				return true;
			}
			return false;
		}
		else if (input instanceof IURIEditorInput) {
			final IFileStore store;
			try {
				store = EFS.getStore(((IURIEditorInput) input).getURI());
			}
			catch (final CoreException e) {
				return false;
			}
			final ISourceUnit su = suManager.getSourceUnit(modelTypeId, LTK.EDITOR_CONTEXT, store, false, null);
			if (su != null) {
				su.disconnect(null);
				return true;
			}
			return false;
		}
		return false;
	}
	
	/**
	 * Creates a region describing the text block (complete lines) of the selection.
	 * 
	 * @param document The document
	 * @param offset offset of the selection
	 * @param length length of the selection
	 * @return the region describing the text block comprising the given selection
	 * @throws BadLocationException 
	 */
	public static IRegion getTextBlockFromSelection(final IDocument document,
			int offset, final int length) throws BadLocationException {
		final int endOffset= offset + length;
		final int firstLine= document.getLineOfOffset(offset);
		int lastLine= document.getLineOfOffset(endOffset);
		offset= document.getLineOffset(firstLine);
		int lastLineOffset = document.getLineOffset(lastLine);
		if (firstLine != lastLine && lastLineOffset == endOffset) {
			lastLine--;
			lastLineOffset= document.getLineOffset(lastLine);
		}
		return new Region(offset, lastLineOffset + document.getLineLength(lastLine) - offset);
	}
	
}
