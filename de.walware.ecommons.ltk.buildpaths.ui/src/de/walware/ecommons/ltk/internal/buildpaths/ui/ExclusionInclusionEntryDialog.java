/*=============================================================================#
 # Copyright (c) 2010-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.buildpaths.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;

import de.walware.ecommons.ui.components.StatusInfo;
import de.walware.ecommons.ui.dialogs.ExtStatusDialog;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.MessageUtil;
import de.walware.ecommons.ui.viewers.TypedViewerFilter;

import de.walware.ecommons.ltk.buildpaths.core.IBuildpathAttribute;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathListElement;
import de.walware.ecommons.ltk.buildpaths.ui.BuildpathsUIDescription;


public class ExclusionInclusionEntryDialog extends ExtStatusDialog {
	
	
	private static final ImList<Class<?>> ACCEPTED_CLASSES= ImCollections.<Class<?>>newList(
			IFolder.class, IFile.class );
	
	
	static List<IPath> chooseExclusionPattern(final Shell shell, final IContainer sourceFolder,
			final String title, final String message,
			final IPath initialPath, final boolean multiSelection) {
		final ViewerFilter filter= new TypedViewerFilter(ACCEPTED_CLASSES);
		
		final ILabelProvider lp= new WorkbenchLabelProvider();
		final ITreeContentProvider cp= new WorkbenchContentProvider();
		
		IResource initialElement= null;
		if (initialPath != null) {
			IContainer curr= sourceFolder;
			final int nSegments= initialPath.segmentCount();
			for (int i= 0; i < nSegments; i++) {
				final IResource elem= curr.findMember(initialPath.segment(i));
				if (elem != null) {
					initialElement= elem;
				}
				if (elem instanceof IContainer) {
					curr= (IContainer) elem;
				}
				else {
					break;
				}
			}
		}
		
		final ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(shell, lp, cp);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setAllowMultiple(multiSelection);
		dialog.setInput(sourceFolder);
		dialog.setInitialSelection(initialElement);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.setHelpAvailable(false);
		
		if (dialog.open() == Window.OK) {
			final Object[] objects= dialog.getResult();
			final int existingSegments= sourceFolder.getFullPath().segmentCount();
			
			final List<IPath> result= new ArrayList<>(objects.length);
			for (int i= 0; i < objects.length; i++) {
				final IResource currRes= (IResource) objects[i];
				IPath path= currRes.getFullPath().removeFirstSegments(existingSegments).makeRelative();
				if (currRes instanceof IContainer) {
					path= path.addTrailingSeparator();
				}
				result.add(path);
			}
			return result;
		}
		return null;
	}
	
	
	private final BuildpathsUIDescription uiDescription;
	
	private final BuildpathListElement element;
	private final String attributeKey;
	
	private IContainer elementSourceFolder;
	
	private final IPath originalPattern;
	private final List<IPath> existingPatterns;
	private IPath pattern;
	
	private Text patternControl;
	
	private final StatusInfo patternStatus;
	
	
	public ExclusionInclusionEntryDialog(final Shell parent,
			final BuildpathListElement element, final String attributeKey,
			final IPath patternToEdit, final List<IPath> existingPatterns,
			final BuildpathsUIDescription uiDescription) {
		super(parent);
		
		this.uiDescription= uiDescription;
		
		this.element= element;
		this.attributeKey= attributeKey;
		
		this.originalPattern= patternToEdit;
		this.existingPatterns= existingPatterns;
		
		{	final IWorkspaceRoot root= this.element.getProject().getWorkspace().getRoot();
			final IResource res= root.findMember(this.element.getPath());
			if (res instanceof IContainer) {
				this.elementSourceFolder= (IContainer) res;
			}
		}
		
		final String title;
		switch (attributeKey) {
		case IBuildpathAttribute.FILTER_INCLUSIONS:
			title= (patternToEdit == null) ?
					Messages.ExclusionInclusion_EntryDialog_Include_Add_title :
					Messages.ExclusionInclusion_EntryDialog_Include_Edit_title;
			break;
		case IBuildpathAttribute.FILTER_EXCLUSIONS:
			title= (patternToEdit == null) ?
					Messages.ExclusionInclusion_EntryDialog_Exclude_Add_title :
					Messages.ExclusionInclusion_EntryDialog_Exclude_Edit_title;
			break;
		default:
			throw new IllegalArgumentException(attributeKey);
		}
		setTitle(title);
		
		this.patternStatus= new StatusInfo();
	}
	
	
	@Override
	public void create() {
		super.create();
		
		updateTargets();
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area= new Composite(parent, SWT.NONE);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		area.setLayout(LayoutUtil.createDialogGrid(2));
		
		final String descriptionText, labelText;
		switch (this.attributeKey) {
		case IBuildpathAttribute.FILTER_INCLUSIONS:
			descriptionText= NLS.bind(Messages.ExclusionInclusion_EntryDialog_Exclude_description,
					this.uiDescription.getDefaultExt(this.element) );
			labelText= NLS.bind(Messages.ExclusionInclusion_EntryDialog_Exclude_Pattern_label,
					MessageUtil.processPath(this.element.getPath().toString()) );
			break;
		case IBuildpathAttribute.FILTER_EXCLUSIONS:
			descriptionText= NLS.bind(Messages.ExclusionInclusion_EntryDialog_Include_description,
					this.uiDescription.getDefaultExt(this.element) );
			labelText= NLS.bind(Messages.ExclusionInclusion_EntryDialog_Include_Pattern_label,
					MessageUtil.processPath(this.element.getPath().toString()) );
			break;
		default:
			throw new IllegalStateException();
		}
		
		{	final Label description= new Label(area, SWT.WRAP);
			description.setText(descriptionText);
			
			final GridData gd= new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
			gd.horizontalSpan= 2;
			gd.widthHint= convertWidthInCharsToPixels(80);
			description.setLayoutData(gd);
		}
		
		final Composite composite= area;
		{	final Label label= new Label(area, SWT.NONE);
			label.setText(labelText);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		}
		{	final Text text= new Text(area, SWT.BORDER);
		
			final GridData gd= new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.widthHint= LayoutUtil.hintWidth(text, 60);
			text.setLayoutData(gd);
			this.patternControl= text;
		}
		{	final Button button= new Button(area, SWT.PUSH);
			button.setText(Messages.ExclusionInclusion_EntryDialog_Choose_label);
			button.setEnabled(this.elementSourceFolder != null);
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final IPath pattern= doBrowsePattern();
					if (pattern != null) {
						ExclusionInclusionEntryDialog.this.patternControl.setText(pattern.toString());
						update();
					}
				}
			});
			
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, false, false);
			gd.widthHint= LayoutUtil.hintWidth(button);
			button.setLayoutData(gd);
		}
		
		applyDialogFont(composite);
		return composite;
	}
	
	private IPath doBrowsePattern() {
		final String title, message;
		switch (this.attributeKey) {
		case IBuildpathAttribute.FILTER_INCLUSIONS:
			title= Messages.ExclusionInclusion_Choose_Include_title;
			message= Messages.ExclusionInclusion_Choose_Include_Single_description;
			break;
		case IBuildpathAttribute.FILTER_EXCLUSIONS:
			title= Messages.ExclusionInclusion_Choose_Exclude_title;
			message= Messages.ExclusionInclusion_Choose_Exclude_Single_description;
			break;
		default:
			throw new IllegalStateException();
		}
		
		final IPath initialPath= new Path(this.patternControl.getText());
		
		final List<IPath> patterns= chooseExclusionPattern(getShell(), this.elementSourceFolder,
				title, message, initialPath, false );
		if (patterns == null) {
			return null;
		}
		return patterns.get(0);
	}
	
	protected void updateTargets() {
		this.patternControl.setText((this.originalPattern != null) ?
				this.originalPattern.toString() : "" ); //$NON-NLS-1$
	}
	
	protected void update() {
		validate();
		updateStatus(this.patternStatus);
	}
	
	protected void validate() {
		final String pattern= this.patternControl.getText().trim();
		if (pattern.length() == 0) {
			this.patternStatus.setError(Messages.ExclusionInclusion_EntryDialog_error_Empty_message);
			return;
		}
		final IPath path= new Path(pattern);
		if (path.isAbsolute() || path.getDevice() != null) {
			this.patternStatus.setError(Messages.ExclusionInclusion_EntryDialog_error_NotRelative_message);
			return;
		}
		if (!path.equals(this.originalPattern) && this.existingPatterns.contains(path)) {
			this.patternStatus.setError(Messages.ExclusionInclusion_EntryDialog_error_AlreadyExists_message);
			return;
		}
		
		this.pattern= path;
		this.patternStatus.setOK();
	}
	
	public IPath getPattern() {
		return this.pattern;
	}
	
}
