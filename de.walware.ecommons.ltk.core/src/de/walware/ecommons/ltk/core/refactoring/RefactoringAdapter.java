/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CopyArguments;
import org.eclipse.ltk.core.refactoring.participants.CopyParticipant;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.internal.core.refactoring.Resources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;

import de.walware.ecommons.collections.ImCollections;
import de.walware.ecommons.io.FileUtil;
import de.walware.ecommons.text.BasicHeuristicTokenScanner;
import de.walware.ecommons.text.TextUtil;

import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.LTKUtil;
import de.walware.ecommons.ltk.core.ElementSet;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceElement;
import de.walware.ecommons.ltk.core.model.ISourceStructElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.IWorkspaceSourceUnit;
import de.walware.ecommons.ltk.core.util.ElementComparator;
import de.walware.ecommons.ltk.internal.core.refactoring.Messages;


/**
 * Provides common functions for refacotring. 
 * Can be extended to adapt to language specific peculiarity.
 */
public abstract class RefactoringAdapter {
	
	
	private static final Comparator<IModelElement> MODELELEMENT_SORTER = new ElementComparator();
	
	
	private final String fModelTypeId;
	
	
	public RefactoringAdapter(final String modelTypeId) {
		fModelTypeId = modelTypeId;
	}
	
	
	public Comparator<IModelElement> getModelElementComparator() {
		return MODELELEMENT_SORTER;
	}
	
	public String getModelTypeId() {
		return fModelTypeId;
	}
	
	public boolean isSupportedModelType(final String typeId) {
		return (fModelTypeId == typeId);
	}
	
	public abstract String getPluginIdentifier();
	
	public abstract boolean isCommentContent(final ITypedRegion partition);
	
	public abstract BasicHeuristicTokenScanner getScanner(final ISourceUnit su);
	
	/**
	 * - Sort elements
	 * - Removes nested children.
	 * 
	 * @param elements must be sorted by unit and order
	 * @return
	 */
	public ISourceStructElement[] checkElements(final ISourceStructElement[] elements) {
		if (elements.length <= 1) {
			return elements;
		}
		Arrays.sort(elements, getModelElementComparator());
		ISourceStructElement last = elements[0];
		ISourceUnit unitOfLast = last.getSourceUnit();
		int endOfLast = last.getSourceRange().getOffset()+last.getSourceRange().getLength();
		final List<ISourceStructElement> checked = new ArrayList<>(elements.length);
		for (final ISourceStructElement element : elements) {
			final ISourceUnit unit = element.getSourceUnit();
			final int end = last.getSourceRange().getOffset()+last.getSourceRange().getLength();
			if (unit != unitOfLast) {
				checked.add(element);
				last = element;
				unitOfLast = unit;
				endOfLast = end;
				continue;
			}
			if (end > endOfLast) {
				checked.add(element);
				last = element;
				endOfLast = end;
				continue;
			}
			// is child, ignore
			continue;
		}
		return checked.toArray(new ISourceStructElement[checked.size()]);
	}
	
	public IRegion getContinuousSourceRange(final ISourceStructElement[] elements) {
		if (elements == null || elements.length == 0) {
			return null;
		}
		final ISourceUnit su = elements[0].getSourceUnit();
		if (su == null) {
			return null;
		}
		final AbstractDocument doc = su.getDocument(null);
		if (doc == null) {
			return null;
		}
		
		// check if no other code is between the elements
		// and create one single range including comments at line end
		try {
			final BasicHeuristicTokenScanner scanner = getScanner(su);
			scanner.configure(doc);
			final int start = elements[0].getSourceRange().getOffset();
			int end = elements[0].getSourceRange().getOffset() + elements[0].getSourceRange().getLength();
			
			for (int i = 1; i < elements.length; i++) {
				if (elements[i].getSourceUnit() != su) {
					return null;
				}
				final int elementStart = elements[i].getSourceRange().getOffset();
				final int elementEnd = elementStart + elements[i].getSourceRange().getLength();
				if (elementEnd <= end) {
					continue;
				}
				int match;
				while (end < elementStart &&
						(match = scanner.findAnyNonBlankForward(end, elementStart, true)) >= 0) {
					final ITypedRegion partition = doc.getPartition(scanner.getDocumentPartitioning(), match, false);
					if (isCommentContent(partition)) {
						end = partition.getOffset() + partition.getLength();
					}
					else {
						return null;
					}
				}
				end = elementEnd;
			}
			final IRegion lastLine = doc.getLineInformationOfOffset(end);
			final int match = scanner.findAnyNonBlankForward(end, lastLine.getOffset()+lastLine.getLength(), true);
			if (match >= 0) {
				final ITypedRegion partition = doc.getPartition(scanner.getDocumentPartitioning(), match, false);
				if (isCommentContent(partition)) {
					end = partition.getOffset() + partition.getLength();
				}
			}
			return new Region(start, end-start);
		}
		catch (final BadPartitioningException e) {
		}
		catch (final BadLocationException e) {
		}
		return null;
	}
	
	public String getSourceCodeStringedTogether(final ISourceStructElement[] sourceElements,
			final IProgressMonitor monitor) throws CoreException {
		return getSourceCodeStringedTogether(new ElementSet((Object[]) sourceElements), monitor);
	}
	
	public String getSourceCodeStringedTogether(final ElementSet sourceElements,
			final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, sourceElements.getElementCount() * 2);
		ISourceUnit lastUnit = null;
		BasicHeuristicTokenScanner scanner = null;
		try {
			sourceElements.removeElementsWithAncestorsOnList();
			Collections.sort(sourceElements.getModelElements(), getModelElementComparator());
			final String lineDelimiter = TextUtil.getPlatformLineDelimiter();
			
			AbstractDocument doc = null;
			final List<IModelElement> modelElements = sourceElements.getModelElements();
			int todo = modelElements.size();
			
			final StringBuilder sb = new StringBuilder(todo*100);
			final List<String> codeFragments = new ArrayList<>();
			for (final IModelElement element : modelElements) {
				final ISourceUnit su = LTKUtil.getSourceUnit(element);
				if (su != lastUnit) {
					if (lastUnit != null) {
						progress.setWorkRemaining(todo*2);
						lastUnit.disconnect(progress.newChild(1));
						lastUnit = null;
					}
					su.connect(progress.newChild(1));
					lastUnit = su;
					scanner = getScanner(su);
					doc = su.getDocument(monitor);
				}
				getSourceCode((ISourceElement) element, doc, scanner, codeFragments);
				for (final String s : codeFragments) {
					sb.append(s);
					sb.append(lineDelimiter);
				}
				codeFragments.clear();
				
				todo--;
			}
			return sb.toString();
		}
		catch (final BadLocationException e) {
			throw new CoreException(failDocAnalyzation(e));
		}
		catch (final BadPartitioningException e) {
			throw new CoreException(failDocAnalyzation(e));
		}
		finally {
			if (lastUnit != null) {
				progress.setWorkRemaining(1);
				lastUnit.disconnect(progress.newChild(1));
				lastUnit = null;
			}
		}
	}
	
	protected void getSourceCode(final ISourceElement element, final AbstractDocument doc,
			final BasicHeuristicTokenScanner scanner, final List<String> codeFragments) 
			throws BadLocationException, BadPartitioningException {
		final IRegion range = expandElementRange(element, doc, scanner);
		if (range != null && range.getLength() > 0) {
			codeFragments.add(doc.get(range.getOffset(), range.getLength()));
		}
	}
	
	public IRegion expandElementRange(final ISourceElement element, final AbstractDocument document,
			final BasicHeuristicTokenScanner scanner) 
			throws BadLocationException, BadPartitioningException {
		final IRegion sourceRange = element.getSourceRange();
		int start = sourceRange.getOffset();
		int end = start + sourceRange.getLength();
		
		final IRegion docRange = element.getDocumentationRange();
		if (docRange != null) {
			if (docRange.getOffset() < start) {
				start = docRange.getOffset();
			}
			if (docRange.getOffset()+docRange.getLength() > end) {
				end = docRange.getOffset()+docRange.getLength();
			}
		}
		
		return expandSourceRange(start, end, document, scanner);
	}
	
	protected IRegion expandSourceRange(final int start, int end, final AbstractDocument doc,
			final BasicHeuristicTokenScanner scanner) 
			throws BadLocationException, BadPartitioningException {
		scanner.configure(doc);
		
		IRegion lastLineInfo;
		int match;
		lastLineInfo = doc.getLineInformationOfOffset(end);
		match = scanner.findAnyNonBlankForward(end, lastLineInfo.getOffset()+lastLineInfo.getLength(), true);
		if (match >= 0) {
			final ITypedRegion partition = doc.getPartition(scanner.getDocumentPartitioning(),
					match, false );
			if (isCommentContent(partition)) {
				end = partition.getOffset() + partition.getLength();
			}
		}
		final int checkLine = doc.getLineOfOffset(end)+1;
		if (checkLine < doc.getNumberOfLines()) {
			final IRegion checkLineInfo = doc.getLineInformation(checkLine);
			match = scanner.findAnyNonBlankForward(
					end, checkLineInfo.getOffset()+checkLineInfo.getLength(), true);
			if (match < 0) {
				end = checkLineInfo.getOffset()+checkLineInfo.getLength();
			}
		}
		
		return new Region(start, end-start);
	}
	
	public IRegion expandWhitespaceBlock(final AbstractDocument document, final IRegion region,
			final BasicHeuristicTokenScanner scanner) throws BadLocationException {
		scanner.configure(document);
		final int firstLine = document.getLineOfOffset(region.getOffset());
		int lastLine = document.getLineOfOffset(region.getOffset()+region.getLength());
		if (lastLine > firstLine && document.getLineOffset(lastLine) == region.getOffset()+region.getLength()) {
			lastLine--;
		}
		int result;
		final int min = document.getLineOffset(firstLine);
		final int max = document.getLineOffset(lastLine)+document.getLineLength(lastLine);
		result = scanner.findAnyNonBlankForward(region.getOffset()+region.getLength(), max, true);
		final int end = (result >= 0) ? result : max;
		result = scanner.findAnyNonBlankBackward(region.getOffset(), min, true);
		if (result >= 0) {
			return new Region(result+1, end-(result+1));
		}
		else {
			return new Region(min, end-min);
		}
	}
	
	
	public boolean canDelete(final ElementSet elements) {
		if (elements.getInitialObjects().size() == 0) {
			return false;
		}
		if (!elements.isOK()) {
			return false;
		}
		for (final IModelElement element : elements.getModelElements()) {
			if (!canDelete(element)) {
				return false;
			}
		}
		for (final IResource element : elements.getResources()) {
			if (!canDelete(element)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canDelete(final IModelElement element) {
		if (!element.exists()) {
			return false;
		}
//		if ((element.getElementType() & IModelElement.MASK_C1) == IModelElement.PROJECT) {
//			return false;
//		}
		if (!isSupportedModelType(element.getModelTypeId())) {
			return false;
		}
		if (element.isReadOnly()) {
			return false;
		}
		return true;
	}
	
	public boolean canDelete(final IResource resource) {
		if (!resource.exists() || resource.isPhantom()) {
			return false;
		}
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT) {
			return false;
		}
		if (resource.getParent() != null) {
			final ResourceAttributes attributes = resource.getParent().getResourceAttributes();
			if (attributes != null && attributes.isReadOnly()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canInsert(final ElementSet elements, final RefactoringDestination to) {
		if (to.getInitialObjects().get(0) instanceof ISourceElement) {
			return canInsert(elements, (ISourceElement) to.getInitialObjects().get(0), to.getPosition());
		}
		return false;
	}
	
	protected boolean canInsert(final ElementSet elements, final ISourceElement to,
			final RefactoringDestination.Position pos) {
		if (elements.getInitialObjects().size() == 0) {
			return false;
		}
		if (!elements.isOK()) {
			return false;
		}
		if (!canInsertTo(to)) {
			return false;
		}
		for (final IModelElement element : elements.getModelElements()) {
			if (!canInsert(element)) {
				return false;
			}
		}
		for (final IResource element : elements.getResources()) {
//			if (!canInsert(element, parent) {
				return false;
//			}
		}
		return !elements.includes(to);
	}
	
	protected boolean canInsert(final IModelElement element) {
		if (!element.exists()) {
			return false;
		}
		if (!isSupportedModelType(element.getModelTypeId())) {
			return false;
		}
		return true;
	}
	
	public boolean canInsertTo(final RefactoringDestination destination) {
		if (destination.getModelElements().size() != 1) {
			return false;
		}
		return canInsertTo(destination.getModelElements().get(0));
	}
	
	protected boolean canInsertTo(final IModelElement element) {
		if (!element.exists()) {
			return false;
		}
		if (!isSupportedModelType(element.getModelTypeId())) {
			return false;
		}
		if (element.isReadOnly()) {
			return false;
		}
		return true;
	}
	
	
	public void checkInitialToModify(final RefactoringStatus result, final ElementSet elements) {
		final Set<IResource> resources = new HashSet<>();
		resources.addAll(elements.getResources());
		for(final IModelElement element : elements.getModelElements()) {
			final ISourceUnit su = LTKUtil.getSourceUnit(element);
			if (su instanceof IWorkspaceSourceUnit) {
				resources.add(((IWorkspaceSourceUnit) su).getResource());
				continue;
			}
			result.addFatalError(Messages.Check_ElementNotInWS_message);
			return;
		}
		result.merge(RefactoringStatus.create(
				Resources.checkInSync(resources.toArray(new IResource[resources.size()]))
				));
	}
	
	public void checkFinalToModify(final RefactoringStatus result, final ElementSet elements, final IProgressMonitor monitor) {
		final Set<IResource> resources = new HashSet<>();
		resources.addAll(elements.getResources());
		for(final IModelElement element : elements.getModelElements()) {
			final ISourceUnit su = LTKUtil.getSourceUnit(element);
			if (su instanceof IWorkspaceSourceUnit) {
				resources.add(((IWorkspaceSourceUnit) su).getResource());
				continue;
			}
			result.addFatalError(Messages.Check_ElementNotInWS_message);
			return;
		}
		final IResource[] array = resources.toArray(new IResource[resources.size()]);
		result.merge(RefactoringStatus.create(Resources.checkInSync(array)));
		result.merge(RefactoringStatus.create(Resources.makeCommittable(array, IWorkspace.VALIDATE_PROMPT)));
	}
	
	public void checkFinalToDelete(final RefactoringStatus result, final ElementSet elements) throws CoreException {
		for (final IModelElement element : elements.getModelElements()) {
			checkFinalToDelete(result, element);
		}
		for (final IResource element : elements.getResources()) {
			checkFinalToDelete(result, element);
		}
	}
	
	public void checkFinalToDelete(final RefactoringStatus result, final IResource element) throws CoreException {
		if (element.getType() == IResource.FILE) {
			warnIfDirty(result, (IFile) element);
			return;
		}
		else {
			element.accept(new IResourceVisitor() {
				@Override
				public boolean visit(final IResource visitedResource) throws CoreException {
					if (visitedResource instanceof IFile) {
						warnIfDirty(result, (IFile) visitedResource);
					}
					return true;
				}
			}, IResource.DEPTH_INFINITE, false);
		}
	}
	
	public void checkFinalToDelete(final RefactoringStatus result, final IModelElement element) throws CoreException {
		if ((element.getElementType() & IModelElement.MASK_C2) == IModelElement.C2_SOURCE_FILE) {
			if (element instanceof IWorkspaceSourceUnit) {
				checkFinalToDelete(result, ((IWorkspaceSourceUnit) element).getResource());
			}
		}
		else if ((element.getElementType() & IModelElement.MASK_C1) == IModelElement.C1_BUNDLE
				&& element instanceof ISourceStructElement) {
			final List<? extends IModelElement> children = ((ISourceStructElement) element).getSourceChildren(null);
			for (final IModelElement child : children) {
				checkFinalToDelete(result, child);
			}
		}
	}
	
	public void warnIfDirty(final RefactoringStatus result, final IFile file) {
		if (file == null || !file.exists()) {
			return;
		}
		final ITextFileBuffer buffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
		if (buffer != null && buffer.isDirty()) {
			if (buffer.isStateValidated() && buffer.isSynchronized()) {
				result.addWarning(NLS.bind(
					Messages.Check_FileUnsavedChanges_message,
					FileUtil.getFileUtil(file).getLabel()) );
			} else {
				result.addFatalError(NLS.bind(
					Messages.Check_FileUnsavedChanges_message, 
					FileUtil.getFileUtil(file).getLabel()) );
			}
		}
	}
	
	public boolean confirmDeleteOfReadOnlyElements(final ElementSet elements, final Object queries) throws CoreException {
		// TODO add query support
		return hasReadOnlyElements(elements);
	}
	
	public boolean hasReadOnlyElements(final ElementSet elements) throws CoreException {
		for (final IResource element : elements.getResources()) {
			if (hasReadOnlyElements(element)) {
				return true;
			}
		}
		for (final IModelElement element : elements.getModelElements()) {
			if (hasReadOnlyElements(element)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasReadOnlyElements(final IResource element) throws CoreException {
		if (isReadOnly(element)) {
			return true;
		}
		if (element instanceof IContainer) {
			final IResource[] members = ((IContainer) element).members(false);
			for (final IResource member : members) {
				if (hasReadOnlyElements(member)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean hasReadOnlyElements(final IModelElement element) throws CoreException {
		final ISourceUnit su = LTKUtil.getSourceUnit(element);
		IResource resource = null;
		if (su instanceof IWorkspaceSourceUnit) {
			resource = ((IWorkspaceSourceUnit) su).getResource();
		}
		if (resource == null) {
			resource = (IResource) element.getAdapter(IResource.class);
		}
		if (resource != null) {
			return hasReadOnlyElements(resource);
		}
		return false;
	}
	
	public boolean isReadOnly(final IResource element) {
		final ResourceAttributes attributes = element.getResourceAttributes();
		if (attributes != null) {
			return attributes.isReadOnly();
		}
		return false;
	}
	
	
	public void addParticipantsToDelete(final ElementSet elementsToDelete,
			final List<RefactoringParticipant> list,
			final RefactoringStatus status, final RefactoringProcessor processor, 
			final SharableParticipants shared)
			throws CoreException {
		final String[] natures = ElementSet.getAffectedProjectNatures(elementsToDelete);
		final DeleteArguments arguments = new DeleteArguments();
		for (final IResource resource : elementsToDelete.getResources()) {
			final DeleteParticipant[] deletes = ParticipantManager.loadDeleteParticipants(status, 
				processor, resource, 
				arguments, natures, shared);
			list.addAll(Arrays.asList(deletes));
		}
		for (final IResource resource : elementsToDelete.getResourcesOwnedByElements()) {
			final DeleteParticipant[] deletes = ParticipantManager.loadDeleteParticipants(status, 
				processor, resource, 
				arguments, natures, shared);
			list.addAll(Arrays.asList(deletes));
		}
		for (final IModelElement element : elementsToDelete.getModelElements()) {
			final DeleteParticipant[] deletes = ParticipantManager.loadDeleteParticipants(status, 
				processor, element, 
				arguments, natures, shared);
			list.addAll(Arrays.asList(deletes));
		}
	}
	
	public void addParticipantsToMove(final ElementSet elementsToMove,
			final RefactoringDestination destination,
			final List<RefactoringParticipant> list,
			final RefactoringStatus status, final RefactoringProcessor processor, 
			final SharableParticipants shared, final ReorgExecutionLog executionLog)
			throws CoreException {
		final String[] natures = ElementSet.getAffectedProjectNatures(
				ImCollections.newList(elementsToMove, destination) );
		final MoveArguments mArguments = new MoveArguments(destination.getModelElements().get(0),
				false );
//		for (final IResource resource : elementsToCopy.getResources()) {
//			final MoveParticipant[] deletes = ParticipantManager.loadMoveParticipants(status, 
//					processor, resource, arguments, natures, shared );
//			list.addAll(Arrays.asList(deletes));
//		}
//		for (final IResource resource : elementsToCopy.getResourcesOwnedByElements()) {
//			final MoveParticipant[] deletes = ParticipantManager.loadMoveParticipants(status, 
//					processor, resource, arguments, natures, shared );
//			list.addAll(Arrays.asList(deletes));
//		}
		for (final IModelElement element : elementsToMove.getModelElements()) {
			final MoveParticipant[] deletes = ParticipantManager.loadMoveParticipants(status,
					processor, element, mArguments, natures, shared );
			list.addAll(Arrays.asList(deletes));
		}
	}
	
	public void addParticipantsToCopy(final ElementSet elementsToCopy,
			final RefactoringDestination destination,
			final List<RefactoringParticipant> list,
			final RefactoringStatus status, final RefactoringProcessor processor, 
			final SharableParticipants shared, final ReorgExecutionLog executionLog)
			throws CoreException {
		final String[] natures = ElementSet.getAffectedProjectNatures(
				ImCollections.newList(elementsToCopy, destination) );
		final CopyArguments mArguments = new CopyArguments(destination.getModelElements().get(0),
				executionLog );
//		for (final IResource resource : elementsToCopy.getResources()) {
//			final CopyParticipant[] deletes = ParticipantManager.loadCopyParticipants(status,
//					processor, resource, arguments, natures, shared );
//			list.addAll(Arrays.asList(deletes));
//		}
//		for (final IResource resource : elementsToCopy.getResourcesOwnedByElements()) {
//			final CopyParticipant[] deletes = ParticipantManager.loadCopyParticipants(status,
//					processor, resource, arguments, natures, shared);
//			list.addAll(Arrays.asList(deletes));
//		}
		for (final IModelElement element : elementsToCopy.getModelElements()) {
			final CopyParticipant[] deletes = ParticipantManager.loadCopyParticipants(status,
					processor, element,
					mArguments, natures, shared);
			list.addAll(Arrays.asList(deletes));
		}
	}
	
	public void buildDeltaToDelete(final ElementSet elements,
			final IResourceChangeDescriptionFactory resourceDelta) {
		for (final IResource resource : elements.getResources()) {
			resourceDelta.delete(resource);
		}
		for (final IResource resource : elements.getResourcesOwnedByElements()) {
			resourceDelta.delete(resource);
		}
		for (final IFile file : elements.getFilesContainingElements()) {
			resourceDelta.change(file);
		}
	}
	
	public void buildDeltaToModify(final ElementSet elements,
			final IResourceChangeDescriptionFactory resourceDelta) {
		for (final IResource resource : elements.getResources()) {
			if (resource instanceof IFile) {
				resourceDelta.change((IFile) resource);
			}
		}
		for (final IResource resource : elements.getResourcesOwnedByElements()) {
			if (resource instanceof IFile) {
				resourceDelta.change((IFile) resource);
			}
		}
		for (final IFile file : elements.getFilesContainingElements()) {
			resourceDelta.change(file);
		}
	}
	
	/**
	 * @param changeName the name of the change
	 * @param resources the resources to delete
	 * @param manager the text change manager
	 * @return the created change
	 * @throws CoreException 
	 */
	public Change createChangeToDelete(final String changeName,
			final ElementSet elementsToDelete,
			final TextChangeManager manager, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 1);
		final CompositeChange result = new CompositeChange(changeName);
		
		addChangesToDelete(result, elementsToDelete, manager, progress.newChild(1));
		
		result.addAll(manager.getAllChanges());
		return result;
	}
	
	public Change createChangeToMove(final String changeName, 
			final ElementSet elementsToMove, final RefactoringDestination destination,
			final TextChangeManager manager, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 3);
		final CompositeChange result = new CompositeChange(changeName);
		
		final String code = getSourceCodeStringedTogether(elementsToMove, progress.newChild(1));
		
		addChangesToDelete(result, elementsToMove, manager, progress.newChild(1));
		addChangesToInsert(result, code, destination, manager, progress.newChild(1));
		
		result.addAll(manager.getAllChanges());
		return result;
	}
	
	public Change createChangeToCopy(final String changeName, 
			final ElementSet elementsToMove, final RefactoringDestination destination,
			final TextChangeManager manager, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 2);
		final CompositeChange result = new CompositeChange(changeName);
		
		final String code = getSourceCodeStringedTogether(elementsToMove, progress.newChild(1));
		
		addChangesToInsert(result, code, destination, manager, progress.newChild(1));
		
		result.addAll(manager.getAllChanges());
		return result;
	}
	
	public Change createChangeToInsert(final String changeName,
			final String code, final RefactoringDestination destination,
			final TextChangeManager manager,
			final IProgressMonitor monitor) throws CoreException {
		assert (changeName != null && code != null && destination != null && manager != null);
		final SubMonitor progress = SubMonitor.convert(monitor);
		final CompositeChange result = new CompositeChange(changeName);
		
		addChangesToInsert(result, code, destination, manager, progress);
		
		result.addAll(manager.getAllChanges());
		return result;
	}
	
	protected void addChangesToDelete(final CompositeChange result, 
			final ElementSet elements,
			final TextChangeManager manager, final SubMonitor progress) throws CoreException {
		for (final IResource resource : elements.getResources()) {
			result.add(createChangeToDelete(elements, resource));
		}
		final Map<ISourceUnit, List<IModelElement>> suSubChanges = new HashMap<>();
		for (final IModelElement element : elements.getModelElements()) {
			final IResource resource = elements.getOwningResource(element);
			if (resource != null) {
				result.add(createChangeToDelete(elements, resource));
			}
			else {
				final ISourceUnit su = LTKUtil.getSourceUnit(element);
				List<IModelElement> list = suSubChanges.get(su);
				if (list == null) {
					list = new ArrayList<>(1);
					suSubChanges.put(su, list);
				}
				list.add(element);
			}
		}
		if (!suSubChanges.isEmpty()) {
			progress.setWorkRemaining(suSubChanges.size()*3);
			for (final Map.Entry<ISourceUnit, List<IModelElement>> suChanges : suSubChanges.entrySet()) {
				createChangeToDelete(elements, suChanges.getKey(), suChanges.getValue(), manager, progress);
			}
		}
	}
	
	private void createChangeToDelete(final ElementSet elements,
			final ISourceUnit su, final List<IModelElement> elementsInUnit,
			final TextChangeManager manager, final SubMonitor progress) throws CoreException {
		if (!(su instanceof IWorkspaceSourceUnit)
				|| ((IWorkspaceSourceUnit) su).getResource().getType() != IResource.FILE ) {
			throw new IllegalArgumentException();
		}
		su.connect(progress.newChild(1));
		try {
			final MultiTextEdit rootEdit = getRootEdit(manager, su);
			
			final BasicHeuristicTokenScanner scanner = getScanner(su);
			final AbstractDocument document = su.getDocument(null);
			for (final IModelElement element : elementsInUnit) {
				final ISourceElement member = (ISourceElement) element;
				final IRegion sourceRange = expandElementRange(member, document, scanner);
				final DeleteEdit edit = new DeleteEdit(sourceRange.getOffset(), sourceRange.getLength());
				rootEdit.addChild(edit);
			}
			progress.worked(1);
		}
		catch (final BadLocationException e) {
			throw new CoreException(failCreation(e));
		}
		catch (final BadPartitioningException e) {
			throw new CoreException(failCreation(e));
		}
		finally {
			su.disconnect(progress.newChild(1));
		}
	}
	
	protected Change createChangeToDelete(final ElementSet elements, final IModelElement element) throws CoreException {
		final IResource resource = elements.getOwningResource(element);
		if (resource != null) {
			return createChangeToDelete(elements, resource);
		}
		throw new IllegalStateException(); 
	}
	
	protected Change createChangeToDelete(final ElementSet elements, final IResource resource) {
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT) {
			throw new IllegalStateException();
		}
		return new DeleteResourceChange(resource.getFullPath(), true);
	}
	
	protected Change createChangeToDelete(final ElementSet elements, final ISourceUnit su) throws CoreException {
		if (su instanceof IWorkspaceSourceUnit) {
			return createChangeToDelete(elements, ((IWorkspaceSourceUnit) su).getResource());
		}
		throw new IllegalStateException();
	}
	
	
	protected void addChangesToInsert(final CompositeChange result, final String code,
			final RefactoringDestination destination,
			final TextChangeManager manager, final SubMonitor progress) throws CoreException {
		final ISourceElement element = (ISourceElement) destination.getModelElements().get(0);
		final ISourceUnit su = LTKUtil.getSourceUnit(element);
		
		progress.setWorkRemaining(3);
		createChangeToInsert(su, code, element, destination, manager, progress);
	}
	
	private void createChangeToInsert(final ISourceUnit su,
			final String code,
			final ISourceElement desElement, final RefactoringDestination destination,
			final TextChangeManager manager, final SubMonitor progress) throws CoreException {
		if (!(su instanceof IWorkspaceSourceUnit)
				|| ((IWorkspaceSourceUnit) su).getResource().getType() != IResource.FILE ) {
			throw new IllegalArgumentException();
		}
		su.connect(progress.newChild(1));
		try {
			final AbstractDocument document = su.getDocument(progress.newChild(1));
			final BasicHeuristicTokenScanner scanner = getScanner(su);
			
			final int offset;
			if (destination.getPosition() == RefactoringDestination.Position.AT) {
				offset = destination.getOffset();
			}
			else {
				offset = getInsertionOffset(document, desElement, destination.getPosition(), scanner);
			}
			
			final MultiTextEdit rootEdit = getRootEdit(manager, su);
			
			final InsertEdit edit = new InsertEdit(offset, code);
			rootEdit.addChild(edit);
			((SourceUnitChange) manager.get(su)).setInsertPosition(new Position(edit.getOffset()));
			
			progress.worked(1);
		}
		catch (final BadLocationException e) {
			throw new CoreException(failCreation(e));
		}
		catch (final BadPartitioningException e) {
			throw new CoreException(failCreation(e));
		}
		finally {
			su.disconnect(progress.newChild(1));
		}
	}
	
	protected int getInsertionOffset(final AbstractDocument document, final ISourceElement element,
			final RefactoringDestination.Position pos,
			final BasicHeuristicTokenScanner scanner) throws BadLocationException, BadPartitioningException {
		final IRegion range = expandElementRange(element, document, scanner);
		if (pos == RefactoringDestination.Position.ABOVE) {
			final int offset = range.getOffset();
			
			return offset;
		}
		else {
			int offset = range.getOffset()+range.getLength();
			
			final int line = document.getLineOfOffset(offset);
			final IRegion lineInformation = document.getLineInformation(line);
			if (offset == lineInformation.getOffset() + lineInformation.getLength()) {
				offset += document.getLineDelimiter(line).length();
			}
			return offset;
		}
	}
	
	private MultiTextEdit getRootEdit(final TextChangeManager manager, final ISourceUnit su) {
		final TextFileChange textFileChange = manager.get(su);
		if (su.getWorkingContext() == LTK.EDITOR_CONTEXT) {
			textFileChange.setSaveMode(TextFileChange.LEAVE_DIRTY);
		}
		if (textFileChange.getEdit() == null) {
			textFileChange.setEdit(new MultiTextEdit());
		}
		return (MultiTextEdit) textFileChange.getEdit();
	}
	
	
	protected IStatus failDocAnalyzation(final Throwable e) {
		return new Status(IStatus.ERROR, LTK.PLUGIN_ID, Messages.Common_error_AnalyzingSourceDocument_message, e);
	}
	
	protected IStatus failCreation(final Throwable e) {
		return new Status(IStatus.ERROR, LTK.PLUGIN_ID, Messages.Common_error_CreatingElementChange_message, e);
	}
	
}
