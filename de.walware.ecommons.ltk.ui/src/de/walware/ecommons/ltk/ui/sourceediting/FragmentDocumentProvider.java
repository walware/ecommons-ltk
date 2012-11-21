package de.walware.ecommons.ltk.ui.sourceediting;

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
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.LTK;


public class FragmentDocumentProvider extends AbstractDocumentProvider
		implements IDocumentModelProvider {
	
	
	public class SourceElementInfo extends ElementInfo {
		
		public ISourceUnit fWorkingCopy;
		
		public SourceElementInfo(final IDocument document, final IAnnotationModel model) {
			super(document, model);
		}
		
	}
	
	
	private final String fModelTypeId;
	
	private final PartitionerDocumentSetupParticipant fDocumentSetupParticipant;
	
	
	public FragmentDocumentProvider(final String modelTypeId,
			final PartitionerDocumentSetupParticipant documentSetupParticipant) {
		fModelTypeId = modelTypeId;
		fDocumentSetupParticipant = documentSetupParticipant;
	}
	
	
	@Override
	protected ElementInfo createElementInfo(final Object element) throws CoreException {
		ISourceUnit su = null;
		AbstractDocument document = null;
		if (element instanceof ISourceFragmentEditorInput) {
			final ISourceFragmentEditorInput fragmentInput = ((ISourceFragmentEditorInput) element);
			
			final IProgressMonitor monitor = getProgressMonitor();
			final SubMonitor progress = SubMonitor.convert(monitor, 2);
			try {
				su = LTK.getSourceUnitManager().getSourceUnit(fModelTypeId, LTK.EDITOR_CONTEXT,
						fragmentInput.getSourceFragment(), true, progress.newChild(1));
				document = su.getDocument(progress.newChild(1));
			}
			catch (final Exception e) {}
			finally {
				if (monitor != null) {
					monitor.done();
				}
			}
		}
		if (su == null) {
			document = createDocument((su != null) ? su : element);
		}
		if (document != null) {
			setupDocument(document);
			final SourceElementInfo info = new SourceElementInfo(document, createAnnotationModel(element));
			info.fWorkingCopy = su;
			return info;
		}
		return null;
	}
	
	@Override
	protected AbstractDocument createDocument(final Object element) throws CoreException {
		if (element instanceof ISourceFragmentEditorInput) {
			final ISourceFragment fragment = ((ISourceFragmentEditorInput) element).getSourceFragment();
			return fragment.getDocument();
		}
		return null;
	}
	
	protected void setupDocument(final AbstractDocument document) {
		if (document.getDocumentPartitioner(fDocumentSetupParticipant.getPartitioningId()) == null) {
			fDocumentSetupParticipant.setup(document);
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
		final SourceElementInfo info = (SourceElementInfo) elementInfo;
		if (info.fWorkingCopy != null) {
			final IProgressMonitor monitor = getProgressMonitor();
			final SubMonitor progress = SubMonitor.convert(monitor, 1);
			try {
				info.fWorkingCopy.disconnect(progress.newChild(1));
			}
			finally {
				info.fWorkingCopy = null;
				if (monitor != null) {
					monitor.done();
				}
			}
		}
		super.disposeElementInfo(element, elementInfo);
	}
	
	@Override
	public ISourceUnit getWorkingCopy(final Object element) {
		final SourceElementInfo info = (SourceElementInfo) getElementInfo(element);
		if (info != null) {
			return info.fWorkingCopy;
		}
		return null;
	}
	
}
