/*=============================================================================#
 # Copyright (c) 2015-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.workbench.ui.WorkbenchUIUtil;

import de.walware.ecommons.ltk.core.ElementSet;
import de.walware.ecommons.ltk.core.model.ISourceStructElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditorAssociated;
import de.walware.ecommons.ltk.ui.util.LTKSelectionUtil;


public abstract class AbstractSourceDocumentHandler<TSourceUnit extends ISourceUnit>
		extends AbstractHandler {
	
	
	protected class ExecData {
		
		private final IWorkbenchPart activePart;
		private final ISourceEditor sourceEditor;
		private final ImList<? extends TSourceUnit> sourceUnits;
		
		final ITextSelection textSelection;
		
		final ElementSet elementSelection;
		
		
		public ExecData(final IWorkbenchPart activePart, final ISourceEditor sourceEditor,
				final ImList<? extends TSourceUnit> sourceUnits,
				final ITextSelection textSelection, final ElementSet elementSet) {
			this.activePart= activePart;
			this.sourceEditor= sourceEditor;
			this.sourceUnits= sourceUnits;
			
			this.textSelection= textSelection;
			this.elementSelection= elementSet;
		}
		
		public IWorkbenchPart getActivePart() {
			return this.activePart;
		}
		
		public ISourceEditor getSourceEditor() {
			return this.sourceEditor;
		}
		
		public ImList<? extends TSourceUnit> getSourceUnits() {
			return this.sourceUnits;
		}
		
		public ITextSelection getTextSelection() {
			return this.textSelection;
		}
		
		public ElementSet getElementSelection() {
			return this.elementSelection;
		}
		
	}
	
	protected static final byte DIRECT= 0;
	protected static final byte SHOW_BUSY= 1;
	protected static final byte BACKGROUND= 2;
	
	
	public AbstractSourceDocumentHandler() {
	}
	
	
	protected abstract String getTaskLabel();
	
	protected abstract boolean isEditTask();
	
	
	@Override
	public void setEnabled(Object evaluationContext) {
		final IWorkbenchPart activePart= WorkbenchUIUtil.getActivePart(evaluationContext);
		final ISelection selection= WorkbenchUIUtil.getCurrentSelection(evaluationContext);
		final ISourceEditor sourceEditor= getSourceEditor(activePart);
		
		if (sourceEditor != null && selection instanceof ITextSelection) {
			final ISourceUnit sourceUnit= sourceEditor.getSourceUnit();
			setBaseEnabled(sourceUnit != null &&
					(!isEditTask() || sourceEditor.isEditable(false) ));
			return;
		}
		
		if (selection instanceof IStructuredSelection) {
			final ISourceStructElement[] selectedElements= LTKSelectionUtil
					.getSelectedSourceStructElements((IStructuredSelection) selection);
			setBaseEnabled(selectedElements != null && selectedElements.length > 0
					&& checkSourceUnits(selectedElements) );
			return;
		}
		
		setBaseEnabled(false);
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbenchPart activePart= WorkbenchUIUtil.getActivePart(event.getApplicationContext());
		final ISelection selection= WorkbenchUIUtil.getCurrentSelection(event.getApplicationContext());
		final ISourceEditor sourceEditor= getSourceEditor(activePart);
		
		try {
			final IProgressMonitor monitor= new NullProgressMonitor();
			if (sourceEditor != null && selection instanceof ITextSelection) {
				final ISourceUnit sourceUnit= sourceEditor.getSourceUnit();
				if (sourceUnit != null && isSourceUnitSupported(sourceUnit)) {
					if (!isEditTask() || sourceEditor.isEditable(true)) {
						final ExecData data= createExecData(event, activePart, sourceEditor,
								ImCollections.newList((TSourceUnit) sourceUnit),
								(ITextSelection) selection, null, monitor );
						if (data != null) {
							execute(data);
							return null;
						}
					}
				}
				return null;
			}
			if (selection instanceof IStructuredSelection) {
				final ISourceStructElement[] selectedElements= LTKSelectionUtil
						.getSelectedSourceStructElements((IStructuredSelection) selection);
				if (selectedElements != null && selectedElements.length > 0) {
					final ImList<TSourceUnit> sourceUnits= createSourceUnits(selectedElements, monitor);
					if (sourceUnits != null) {
						final ExecData data= createExecData(event, activePart, sourceEditor,
								sourceUnits, null, selectedElements, monitor );
						if (data != null) {
							execute(data);
							return null;
						}
					}
				}
				return null;
			}
			return null;
		}
		catch (final Exception e) {
			throw new ExecutionException(
					NLS.bind(EditingMessages.GenericAction_error_message, getTaskLabel()),
					e );
		}
	}
	
	private ISourceEditor getSourceEditor(final IWorkbenchPart part) {
		if (part instanceof ISourceEditor) {
			return (ISourceEditor) part;
		}
		{	final ISourceEditor editor= (ISourceEditor) part.getAdapter(ISourceEditor.class);
			if (editor != null) {
				return editor;
			}
		}
		{	final ISourceEditorAssociated editorAssociated= (ISourceEditorAssociated) part
					.getAdapter(ISourceEditorAssociated.class);
			if (editorAssociated != null) {
				return editorAssociated.getSourceEditor();
			}
		}
		return null;
	}
	
	private boolean checkSourceUnits(final ISourceStructElement[] selectedElements) {
		ISourceUnit lastSourceUnit= null;
		List<ISourceUnit> sourceUnits= null;
		for (int i= 0; i < selectedElements.length; i++) {
			final ISourceUnit sourceUnit= selectedElements[i].getSourceUnit();
			if (sourceUnit == null) {
				return false;
			}
			if (sourceUnit.equals(lastSourceUnit)
					|| (sourceUnits != null && sourceUnits.contains(sourceUnit)) ) {
				continue;
			}
			if (!isSourceUnitSupported(sourceUnit)
					|| (isEditTask() && !sourceUnit.checkState(false, null)) ) {
				return false;
			}
			if (lastSourceUnit == null) {
				lastSourceUnit= sourceUnit;
			}
			else {
				if (sourceUnits == null) {
					if (!isMultiSourceUnitsSupported()) {
						return false;
					}
					sourceUnits= new ArrayList<>();
					sourceUnits.add(lastSourceUnit);
				}
				sourceUnits.add(sourceUnit);
				lastSourceUnit= sourceUnit;
			}
		}
		return true;
	}
	
	private ImList<TSourceUnit> createSourceUnits(final ISourceStructElement[] selectedElements,
			final IProgressMonitor monitor) {
		TSourceUnit lastSourceUnit= null;
		List<TSourceUnit> sourceUnits= null;
		for (int i= 0; i < selectedElements.length; i++) {
			final ISourceUnit sourceUnit= selectedElements[i].getSourceUnit();
			if (sourceUnit == null) {
				return null;
			}
			if (sourceUnit.equals(lastSourceUnit)
					|| (sourceUnits != null && sourceUnits.contains(sourceUnit)) ) {
				continue;
			}
			if (!isSourceUnitSupported(sourceUnit)
					|| (isEditTask() && !sourceUnit.checkState(true, monitor)) ) {
				return null;
			}
			if (lastSourceUnit == null) {
				lastSourceUnit= (TSourceUnit) sourceUnit;
			}
			else {
				if (sourceUnits == null) {
					if (!isMultiSourceUnitsSupported()) {
						return null;
					}
					sourceUnits= new ArrayList<>();
					sourceUnits.add(lastSourceUnit);
				}
				sourceUnits.add((TSourceUnit) sourceUnit);
				lastSourceUnit= (TSourceUnit) sourceUnit;
			}
		}
		return (sourceUnits == null) ?
				ImCollections.newList(lastSourceUnit) :
				ImCollections.toList(sourceUnits);
	}
	
	private void execute(final ExecData data) throws Exception {
		final AtomicReference<Exception> error= new AtomicReference<>();
		switch (getExecMode(data)) {
		case BACKGROUND:
			try {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor monitor)
							throws InvocationTargetException, InterruptedException {
						try {
							AbstractSourceDocumentHandler.this.doExecute(data, monitor);
						}
						catch (final Exception e) {
							throw new InvocationTargetException(e);
						}
					}
				});
			}
			catch (final InvocationTargetException e) {
				error.set((Exception) e.getTargetException());
			}
			catch (final InterruptedException e) {}
			break;
		case SHOW_BUSY:
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				@Override
				public void run() {
					try {
						AbstractSourceDocumentHandler.this.doExecute(data, new NullProgressMonitor());
					}
					catch (final Exception e) {
						error.set(e);
					}
				}
			});
			break;
		default:
			doExecute(data, new NullProgressMonitor());
			return;
		}
		
		if (error.get() != null) {
			throw error.get();
		}
	}
	
	
	protected abstract boolean isSourceUnitSupported(final ISourceUnit sourceUnit);
	
	protected boolean isMultiSourceUnitsSupported() {
		return false;
	}
	
	protected ExecData createExecData(final ExecutionEvent event, final IWorkbenchPart activePart,
			final ISourceEditor sourceEditor, final ImList<? extends TSourceUnit> sourceUnits,
			final ITextSelection textSelection, final ISourceStructElement[] selectedElements,
			final IProgressMonitor monitor) throws Exception {
		return new ExecData(activePart, sourceEditor, sourceUnits, textSelection,
				(selectedElements != null) ? new ElementSet(selectedElements) : null );
	}
	
	protected byte getExecMode(final ExecData data) {
		return (isEditTask()) ? SHOW_BUSY : DIRECT;
	}
	
	protected abstract void doExecute(ExecData data,
			IProgressMonitor monitor) throws Exception;
	
}
