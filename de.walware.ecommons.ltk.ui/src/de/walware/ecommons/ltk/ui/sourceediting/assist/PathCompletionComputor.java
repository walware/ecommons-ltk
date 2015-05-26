/*=============================================================================#
 # Copyright (c) 2007-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import java.io.File;
import java.util.Arrays;

import com.ibm.icu.text.Collator;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.lang.SystemUtils;
import de.walware.ecommons.runtime.core.utils.PathUtils;
import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


/**
 * Content assist processor for completion of path for local file system resources.
 */
public abstract class PathCompletionComputor implements IContentAssistComputer {
	
	
	protected class ResourceProposal extends CompletionProposalWithOverwrite implements ICompletionProposalExtension3 {
		
		private final IFileStore fileStore;
		private final boolean isDirectory;
		/** The parent in the workspace, if in workspace */
		private final IContainer workspaceRef;
		
		private final String name;
		
		/** Final completion string */
		private String completion;
		
		private IRegion selectionToSet;
		
		
		/**
		 * Creates a new completion proposal for a resource
		 * 
		 * @param offset the offset in the document where to insert the completion
		 * @param fileStore the EFS resource handle
		 * @param explicitName optional explicit name used instead of the name of the fileStore
		 * @param prefix optional prefix to prefix before the name
		 * @param workspaceRef the workspace resource handle, if the resource is in the workspace
		 */
		public ResourceProposal(final AssistInvocationContext context, 
				final int offset, final IFileStore fileStore, final String explicitName, final String prefix, final IContainer workspaceRef) {
			super(context, offset);
			this.fileStore= fileStore;
			this.isDirectory= this.fileStore.fetchInfo().isDirectory();
			this.workspaceRef= workspaceRef;
			final StringBuilder name= new StringBuilder((explicitName != null) ? explicitName : this.fileStore.getName());
			if (prefix != null) {
				name.insert(0, prefix);
			}
			if (this.isDirectory) {
				name.append(PathCompletionComputor.this.fileSeparator);
			}
			this.name= name.toString();
		}
		
		
		@Override
		protected String getPluginId() {
			return PathCompletionComputor.this.getPluginId();
		}
		
		@Override
		public int getRelevance() {
			return 40;
		}
		
		@Override
		public String getSortingString() {
			return this.name;
		}
		
		@Override
		public boolean isAutoInsertable() {
			return false;
		}
		
		private void createCompletion(final IDocument document) {
			if (this.completion == null) {
				try {
					this.completion= checkPathCompletion(document, getReplacementOffset(), this.name);
				}
				catch (final BadLocationException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, -1,
							"An error occurred while creating the final path completion.", e), StatusManager.LOG);
				}
			}
		}
		
		@Override
		protected int computeReplacementLength(final int replacementOffset, final Point selection, final int caretOffset, final boolean overwrite) throws BadLocationException {
			int end= Math.max(caretOffset, selection.x + selection.y);
			if (overwrite) {
				final IDocument document= getInvocationContext().getSourceViewer().getDocument();
				final int length= document.getLength();
				while (end < length) {
					final char c= document.getChar(end);
					if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
						end++;
					}
					else {
						break;
					}
				}
				if (end >= length) {
					end= length;
				}
			}
			return (end - replacementOffset);
		}
		
		/**
		 * @{inheritDoc}
		 */
		@Override
		public Image getImage() {
			Image image= null;
			if (this.workspaceRef != null) {
				final IResource member= this.workspaceRef.findMember(this.fileStore.getName(), true);
				if (member != null) {
					image= LTKUIPlugin.getDefault().getWorkbenchLabelProvider().getImage(member);
				}
			}
			if (image == null) {
				image= PlatformUI.getWorkbench().getSharedImages().getImage(
					this.isDirectory ? ISharedImages.IMG_OBJ_FOLDER : ISharedImages.IMG_OBJ_FILE);
			}
			return image;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDisplayString() {
			return this.name;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getAdditionalProposalInfo() {
			return null;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public IContextInformation getContextInformation() {
			return null;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public IInformationControlCreator getInformationControlCreator() {
			return null;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean validate(final IDocument document, final int offset, final DocumentEvent event) {
			final int replacementOffset= getReplacementOffset();
			if (offset < replacementOffset) {
				return false;
			}
			try {
				final String startsWith= document.get(replacementOffset, offset-replacementOffset);
				return this.name.regionMatches(true, 0, startsWith, 0, startsWith.length());
			}
			catch (final BadLocationException e) {
				return false;
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void doApply(final char trigger, final int stateMask, final int caretOffset, final int replacementOffset, final int replacementLength)
				throws BadLocationException {
			final AssistInvocationContext context= getInvocationContext();
			final SourceViewer viewer= context.getSourceViewer();
			final IDocument document= viewer.getDocument();
//			final Point selectedRange= viewer.getSelectedRange();
			createCompletion(document);
			final Position newSelectionOffset= new Position(replacementOffset + replacementLength, 0);
			try {
				document.addPosition(newSelectionOffset);
				document.replace(replacementOffset, newSelectionOffset.getOffset() - replacementOffset, this.completion);
				this.selectionToSet= new Region(newSelectionOffset.getOffset(), 0);
			}
			catch (final BadLocationException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, -1,
						"An error occurred while inserting the path completion.", e), StatusManager.SHOW | StatusManager.LOG);
				return;
			}
			finally {
				document.removePosition(newSelectionOffset);
			}
			if (this.isDirectory) {
				reinvokeAssist(viewer);
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Point getSelection(final IDocument document) {
			if (this.selectionToSet != null) {
				return new Point(this.selectionToSet.getOffset(), this.selectionToSet.getLength());
			}
			return null;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getPrefixCompletionStart(final IDocument document, final int completionOffset) {
			return getReplacementOffset();
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public CharSequence getPrefixCompletionText(final IDocument document, final int completionOffset) {
			createCompletion(document);
			return this.completion;
		}
		
	}
	
	
	private char fileSeparator;
	private char fileSeparatorBackup;
	
	private boolean isWindows;
	
	private ISourceEditor editor;
	
	
	public PathCompletionComputor() {
	}
	
	
	public abstract String getPluginId();
	
	protected ISourceEditor getEditor() {
		return this.editor;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionStarted(final ISourceEditor editor, final ContentAssist assist) {
		this.editor= editor;
		this.isWindows= getIsWindows();
		this.fileSeparator= getDefaultFileSeparator();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionEnded() {
		this.editor= null;
	}
	
	protected boolean getIsWindows() {
		return SystemUtils.isOSWindows(System.getProperty(SystemUtils.OS_NAME_KEY));
	}
	
	protected final boolean isWindows() {
		return this.isWindows;
	}
	
	protected char getDefaultFileSeparator() {
		return (isWindows()) ? '\\' : '/';
	}
	
	protected char getSegmentSeparator() {
		return this.fileSeparator;
	}
	
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '/', '\\', ':' };
	}
	
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IStatus computeCompletionProposals(final AssistInvocationContext context, final int mode,
			final AssistProposalCollector<IAssistCompletionProposal> proposals, final IProgressMonitor monitor) {
		try {
			final int offset= context.getInvocationOffset();
			final IRegion partition= getContentRange(context, mode);
			if (partition == null) {
				return null;
			}
			
			String prefix= checkPrefix(context.getSourceViewer().getDocument().get(
					partition.getOffset(), offset - partition.getOffset() ));
			
			if (prefix == null) {
				return null;
			}
			
			boolean needSeparatorBeforeStart= false; // including virtual separator
			String segmentPrefix= ""; //$NON-NLS-1$
			IFileStore baseStore= null;
			
			if (prefix.length() > 0 && prefix.charAt(prefix.length() - 1) == '.') {
				// prevent that path segments '.' and '..' at end are resolved by Path#canonicalize
				if (prefix.equals(".") || prefix.endsWith("/.") || (isWindows() && prefix.endsWith("\\."))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					prefix= prefix.substring(0, prefix.length() - 1);
					segmentPrefix= "."; //$NON-NLS-1$
				}
				else if (prefix.equals("..") || prefix.endsWith("/..") || (isWindows() && prefix.endsWith("\\.."))) { // //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					prefix= prefix.substring(0, prefix.length() - 2);
					segmentPrefix= ".."; //$NON-NLS-1$
				}
			}
			IPath path= createPath(prefix);
			if (path == null) {
				return null;
			}
			if (path.segmentCount() == 0) {
				if (isWindows() && path.getDevice() != null && !path.isRoot()) { // C: -> C:/
					path= path.addTrailingSeparator();
					needSeparatorBeforeStart= true;
				}
			}
			else if (// path.segmentCount() > 0 &&
					segmentPrefix.isEmpty() && !path.hasTrailingSeparator()) {
				segmentPrefix= path.lastSegment();
				path= path.removeLastSegments(1);
			}
			// on Windows, path starting with path separator are relative to the device of current directory
			if (path.isAbsolute() && isWindows() && path.getDevice() == null && !path.isUNC()) { 
				final IPath basePath= getRelativeBasePath();
				if (basePath != null) {
					path= path.setDevice(basePath.getDevice());
				}
			}
			
			baseStore= resolveStore(path);
			
			updatePathSeparator(prefix);
			
			String completionPrefix= (needSeparatorBeforeStart) ? Character.toString(this.fileSeparator) : null;
			
			if (baseStore == null || !baseStore.fetchInfo().exists()) {
				if (path != null) {
					return tryAlternative(context, proposals, path, offset - segmentPrefix.length(), 
							segmentPrefix, completionPrefix );
				}
				return null;
			}
			
			doAddChildren(context, proposals, baseStore, offset - segmentPrefix.length(), segmentPrefix,
					completionPrefix );
			if (!segmentPrefix.isEmpty() && !segmentPrefix.equals(".")) { //$NON-NLS-1$
				baseStore= baseStore.getChild(segmentPrefix);
				if (baseStore.fetchInfo().exists()) {
					final StringBuilder prefixBuilder= new StringBuilder();
					if (completionPrefix != null) {
						prefixBuilder.append(completionPrefix);
					}
					prefixBuilder.append(baseStore.getName());
					prefixBuilder.append(this.fileSeparator);
					completionPrefix= prefixBuilder.toString();
					doAddChildren(context, proposals, baseStore, offset - segmentPrefix.length(),
							"", completionPrefix ); //$NON-NLS-1$
				}
			}
			return Status.OK_STATUS;
		}
		catch (final BadLocationException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, -1,
					"An error occurred while preparing path completions.", e), StatusManager.LOG);
		}
		catch (final CoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, -1,
					"An error occurred while preparing path completions.", e), StatusManager.LOG);
		}
		restorePathSeparator();
		return null;
	}
	
	/**
	 * @param prefix to check
	 * @return the prefix, if valid, otherwise <code>null</code>
	 */
	protected String checkPrefix(final String prefix) {
		final char[] breakingChars= "\n\r+<>|?*\"".toCharArray(); //$NON-NLS-1$
		for (int i= 0; i < breakingChars.length; i++) {
			if (prefix.indexOf(breakingChars[i]) >= 0) {
				return null;
			}
		}
		return prefix;
	}
	
	private IPath createPath(String s) {
		if (isWindows() && File.separatorChar == '/') {
			s= s.replace('\\', '/');
		}
		return PathUtils.check(new Path(s));
	}
	
	private void updatePathSeparator(final String prefix) {
		final int lastBack= prefix.lastIndexOf('\\');
		final int lastForw= prefix.lastIndexOf('/');
		if (lastBack > lastForw) {
			this.fileSeparatorBackup= this.fileSeparator;
			this.fileSeparator= '\\';
		}
		else if (lastForw > lastBack) {
			this.fileSeparatorBackup= this.fileSeparator;
			this.fileSeparator= '/';
		}
		// else -1 == -1
	}
	
	private void restorePathSeparator() {
		if (this.fileSeparatorBackup != 0) {
			this.fileSeparator= this.fileSeparatorBackup;
			this.fileSeparatorBackup= 0;
		}
	}
	
	protected void doAddChildren(final AssistInvocationContext context, final AssistProposalCollector<IAssistCompletionProposal> proposals,
			final IFileStore baseStore,
			final int startOffset, final String segmentPrefix, final String completionPrefix) throws CoreException {
		final IContainer[] workspaceRefs= ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(baseStore.toURI());
		final IContainer workspaceRef= (workspaceRefs.length > 0) ? workspaceRefs[0] : null;
		final String[] names= baseStore.childNames(EFS.NONE, new NullProgressMonitor());
		Arrays.sort(names, Collator.getInstance());
		for (final String name : names) {
			if (segmentPrefix.isEmpty()
					|| name.regionMatches(true, 0, segmentPrefix, 0, segmentPrefix.length()) ) {
				proposals.add(new ResourceProposal(context, startOffset,
						baseStore.getChild(name), null, completionPrefix,
						workspaceRef ));
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IStatus computeContextInformation(final AssistInvocationContext context,
			final AssistProposalCollector<IAssistInformationProposal> proposals, final IProgressMonitor monitor) {
		return null;
	}
	
	
	protected abstract IRegion getContentRange(AssistInvocationContext context, int mode)
			throws BadLocationException;
	
	protected IPath getRelativeBasePath() {
		return null;
	}
	
	protected IFileStore getRelativeBaseStore() {
		return null;
	}
	
	protected IFileStore resolveStore(IPath path) throws CoreException {
		if (!path.isAbsolute()) {
			if (!isWindows() && path.getDevice() == null && "~".equals(path.segment(0))) { //$NON-NLS-1$
				final IPath homePath= new Path(System.getProperty(SystemUtils.USER_HOME_KEY));
				path= PathUtils.check(homePath.append(path.removeFirstSegments(1)));
			}
			else {
				final IFileStore base= getRelativeBaseStore();
				if (base != null) {
					return base.getFileStore(path);
				}
				return null;
			}
		}
		return EFS.getStore(URIUtil.toURI(path));
	}
	
	protected IStatus tryAlternative(final AssistInvocationContext context, final AssistProposalCollector<IAssistCompletionProposal> proposals,
			final IPath path, final int startOffset,
			final String segmentPrefix, final String completionPrefix) throws CoreException {
		return null;
	}
	
	/**
	 * Final check of completion string. 
	 * 
	 * E.g. to escape special chars.
	 * 
	 * @param document
	 * @param completionOffset
	 * @param completion
	 * 
	 * @return the checked completion string
	 * @throws BadLocationException
	 */
	protected String checkPathCompletion(final IDocument document, final int completionOffset, final String completion) 
			throws BadLocationException {
		return completion;
	}
	
}
