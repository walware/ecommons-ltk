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

package de.walware.ecommons.tasklist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;

import de.walware.ecommons.text.core.ILineInformation;


public class TaskMarkerHandler {
	
	
	private final String markerId;
	
	private Pattern taskTagPattern;
	private Map<String, TaskPriority> taskTagMap;
	
	private IResource resource;
	
	
	public TaskMarkerHandler(final String markerId) {
		this.markerId= markerId;
	}
	
	
	public void setup(final IResource resource) {
		this.resource= resource;
	}
	
	public void addTaskMarker(final String message, final int offset, int lineNumber, final String match) 
		throws CoreException {
		
		final TaskPriority prio= this.taskTagMap.get(match);
		
		final IMarker marker= this.resource.createMarker(this.markerId);
		
		marker.setAttribute(IMarker.MESSAGE, message);
		marker.setAttribute(IMarker.PRIORITY, prio.getMarkerPriority());
		if (lineNumber == -1) {
			lineNumber= 1;
		}
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		if (offset != -1) {
			marker.setAttribute(IMarker.CHAR_START, offset);
			marker.setAttribute(IMarker.CHAR_END, offset+message.length());
		}
		marker.setAttribute(IMarker.USER_EDITABLE, false);
	}
	
	public void removeTaskMarkers() throws CoreException {
		this.resource.deleteMarkers(this.markerId, false, IResource.DEPTH_INFINITE);
	}
	
	protected void initTaskPattern(final List<TaskTag> taskTags) {
		this.taskTagPattern= null;
		this.taskTagMap= null;
		
		if (taskTags.isEmpty()) {
			return;
		}
		
		this.taskTagMap= new HashMap<>(taskTags.size());
		final String separatorRegex= "[^\\p{L}\\p{N}]"; //$NON-NLS-1$
		final StringBuilder regex= new StringBuilder(separatorRegex);
		regex.append('(');
		for (final TaskTag taskTag : taskTags) {
			regex.append(Pattern.quote(taskTag.getKeyword()));
			regex.append('|'); 
			this.taskTagMap.put(taskTag.getKeyword(), taskTag.getPriority());
		}
		regex.setCharAt(regex.length()-1, ')');
		regex.append("(?:\\z|").append(separatorRegex).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		this.taskTagPattern= Pattern.compile(regex.toString());
	}
	
	public void checkForTasks(final String content, final int offset, final ILineInformation lines)
			throws CoreException, BadLocationException {
		if (this.taskTagPattern != null) {
			final Matcher matcher= this.taskTagPattern.matcher(content);
			if (matcher.find()) {
				final int start= matcher.start(1);
				final String text= new String(content.substring(start));
				addTaskMarker(text, offset+start, lines.getLineOfOffset(offset)+1, matcher.group(1));
			}
		}
	}
	
}
