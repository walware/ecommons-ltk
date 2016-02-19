/*=============================================================================#
 # Copyright (c) 2010-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;

import de.walware.ecommons.text.ISourceFragment;
import de.walware.ecommons.text.PartitionerDocumentSetupParticipant;

import de.walware.ecommons.ltk.IDocumentModelProvider;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.core.model.ISourceUnit;


public class FragmentDocumentProvider extends AbstractDocumentProvider
		implements IDocumentModelProvider {
	
	
	public class SourceElementInfo extends ElementInfo {
		
		private ISourceUnit workingCopy;
		
		public SourceElementInfo(final IDocument document, final IAnnotationModel model) {
			super(document, model);
		}
		
	}
	
	
	private final String modelTypeId;
	
	private final IDocumentSetupParticipant documentSetupParticipant;
	
	
	public FragmentDocumentProvider(final String modelTypeId,
			final PartitionerDocumentSetupParticipant documentSetupParticipant) {
		if (modelTypeId == null) {
			throw new NullPointerException("modelTypeId"); //$NON-NLS-1$
		}
		this.modelTypeId= modelTypeId;
		this.documentSetupParticipant= documentSetupParticipant;
	}
	
	
	@Override
	protected ElementInfo createElementInfo(final Object element) throws CoreException {
		ISourceUnit su= null;
		AbstractDocument document= null;
		if (element instanceof ISourceFragmentEditorInput) {
			final ISourceFragmentEditorInput fragmentInput= ((ISourceFragmentEditorInput) element);
			
			final IProgressMonitor monitor= getProgressMonitor();
			final SubMonitor progress= SubMonitor.convert(monitor, 2);
			try {
				su= LTK.getSourceUnitManager().getSourceUnit(this.modelTypeId, LTK.EDITOR_CONTEXT,
						fragmentInput.getSourceFragment(), true, progress.newChild(1));
				document= su.getDocument(progress.newChild(1));
			}
			catch (final Exception e) {}
			finally {
				if (monitor != null) {
					monitor.done();
				}
			}
		}
		if (document == null) {
			document= createDocument((su != null) ? su : element);
		}
		if (document != null) {
			setupDocument(document);
			final SourceElementInfo info= new SourceElementInfo(document, createAnnotationModel(element));
			info.workingCopy= su;
			return info;
		}
		return null;
	}
	
	@Override
	protected AbstractDocument createDocument(final Object element) throws CoreException {
		if (element instanceof ISourceFragmentEditorInput) {
			final ISourceFragment fragment= ((ISourceFragmentEditorInput) element).getSourceFragment();
			return fragment.getDocument();
		}
		return null;
	}
	
	protected void setupDocument(final AbstractDocument document) {
		if (this.documentSetupParticipant != null) {
			this.documentSetupParticipant.setup(document);
		}
	}
	
	@Override
	protected IAnnotationModel createAnnotationModel(final Object element) throws CoreException {
		return new AnnotationModel();
	}
	
	@Override
	protected IRunnableContext getOperationRunner(final IProgressMonitor monitor) {
		return null;
	}
	
	@Override
	protected void doSaveDocument(final IProgressMonitor monitor, final Object element,
			final IDocument document, final boolean overwrite) throws CoreException {
	}
	
	@Override
	protected void disposeElementInfo(final Object element, final ElementInfo elementInfo) {
		final SourceElementInfo info= (SourceElementInfo) elementInfo;
		if (info.workingCopy != null) {
			final IProgressMonitor monitor= getProgressMonitor();
			final SubMonitor progress= SubMonitor.convert(monitor, 1);
			try {
				info.workingCopy.disconnect(progress.newChild(1));
			}
			finally {
				info.workingCopy= null;
				if (monitor != null) {
					monitor.done();
				}
			}
		}
		super.disposeElementInfo(element, elementInfo);
	}
	
	@Override
	public ISourceUnit getWorkingCopy(final Object element) {
		final SourceElementInfo info= (SourceElementInfo) getElementInfo(element);
		if (info != null) {
			return info.workingCopy;
		}
		return null;
	}
	
}
