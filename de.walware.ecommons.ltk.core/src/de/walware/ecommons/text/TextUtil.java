/*=============================================================================#
 # Copyright (c) 2007-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import de.walware.ecommons.text.core.IPartitionConstraint;


/**
 * Text utilities, in addition to {@link TextUtilities} of JFace.
 */
public class TextUtil {
	
	public static final Pattern LINE_DELIMITER_PATTERN = Pattern.compile("\\r[\\n]?|\\n"); //$NON-NLS-1$
	
	private static final IScopeContext PLATFORM_SCOPE = InstanceScope.INSTANCE;
	
	
	private static class PositionComparator implements Comparator<Position> {
		
		@Override
		public int compare(final Position o1, final Position o2) {
			final int diff = o1.offset - o2.offset;
			if (diff != 0) {
				return diff;
			}
			return o1.length - o2.length;
		}
		
	}
	
	public static final Comparator<Position> POSITION_COMPARATOR = new PositionComparator();
	
	
	/**
	 * Returns the default line delimiter of the Eclipse platform (workbench)
	 * 
	 * @return the line delimiter string
	 */
	public static final String getPlatformLineDelimiter() {
		final String lineDelimiter = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null,
				new IScopeContext[] { PLATFORM_SCOPE });
		if (lineDelimiter != null) {
			return lineDelimiter;
		}
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the default line delimiter for the specified project
	 * 
	 * If it cannot find a project specific setting, it returns the
	 * {@link #getPlatformLineDelimiter()}
	 * 
	 * @param project the project handle, may be <code>null</code>
	 * @return the line delimiter string
	 */
	public static String getLineDelimiter(final IProject project) {
		if (project != null) {
			final String lineSeparator = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null,
					new IScopeContext[] { new ProjectScope(project.getProject()), PLATFORM_SCOPE });
			if (lineSeparator != null) {
				return lineSeparator;
			}
		}
		return getPlatformLineDelimiter();
	}
	
	
	/**
	 * Return the length of the overlapping length of two regions.
	 * If they don't overlap, it return the negative distance of the regions.
	 */
	public static final int overlaps(final int reg1Start, final int reg1End, final int reg2Start, final int reg2End) {
		if (reg1Start <= reg2Start) {
			if (reg2End < reg1End) {
				return reg2End-reg2Start;
			}
			return reg1End-reg2Start;
		}
		else {
			if (reg1End < reg2End) {
				return reg1End-reg1Start;
			}
			return reg2End-reg1Start;
		}
	}
	
	/**
	 * Return the distance of two regions
	 */
	public final int distance(final int reg1Start, final int reg1End, final int reg2Start, final int reg2End) {
		if (reg2Start > reg1End) {
			return reg2Start-reg1End;
		}
		if (reg1Start > reg2End) {
			return reg1Start-reg2End;
		}
		return 0;
	}
	
	/**
	 * Return the distance of a point to the region.
	 */
	public static final int distance(final IRegion region, final int pointOffset) {
		int regPointOffset = region.getOffset();
		if (pointOffset < regPointOffset) {
			return regPointOffset-pointOffset;
		}
		regPointOffset += region.getLength();
		if (pointOffset > regPointOffset) {
			return pointOffset-regPointOffset;
		}
		return 0;
	}
	
	public static ArrayList<String> toLines(final String text) {
		final ArrayList<String> lines = new ArrayList<>(2 + text.length() / 30);
		TextUtil.addLines(text, lines);
		return lines;
	}
	
	/**
	 * Adds text of lines of a string without its line delimiters to the list.
	 * 
	 * @param text the text
	 * @param lines list the lines are added to
	 */
	public static void addLines(final String text, final List<String> lines) {
		final int n = text.length();
		int i = 0;
		int lineStart = 0;
		while (i < n) {
			switch (text.charAt(i)) {
			case '\r':
				lines.add(text.substring(lineStart, i));
				i++;
				if (i < n && text.charAt(i) == '\n') {
					i++;
				}
				lineStart = i;
				continue;
			case '\n':
				lines.add(text.substring(lineStart, i));
				i++;
				if (i < n && text.charAt(i) == '\r') {
					i++;
				}
				lineStart = i;
				continue;
			default:
				i++;
				continue;
			}
		}
		if (lineStart < n) {
			lines.add(text.substring(lineStart, n));
		}
	}
	
	/**
	 * Adds text of lines of a document without its line delimiters to the list.
	 * 
	 * The first line begins at <code>offset</code>, the last line ends at <code>offset+length</code>.
	 * The positions must not be inside a line delimiter (if it consists of multiple chars).
	 * 
	 * @param document the document
	 * @param offset the offset of region to include
	 * @param length the length of region to include
	 * @param lines list the lines are added to
	 * @throws BadLocationException
	 */
	public static final void addLines(final IDocument document, final int offset, final int length,
			final ArrayList<String> lines) throws BadLocationException {
		final int startLine = document.getLineOfOffset(offset);
		final int endLine = document.getLineOfOffset(offset+length);
		lines.ensureCapacity(lines.size() + endLine-startLine+1);
		
		IRegion lineInfo;
		if (startLine > endLine) {
			throw new IllegalArgumentException();
		}
		if (startLine == endLine) {
			lineInfo = document.getLineInformation(endLine);
			lines.add(document.get(offset, length));
			return;
		}
		else {
			lineInfo = document.getLineInformation(startLine);
			lines.add(document.get(offset, Math.max(0, lineInfo.getOffset()+lineInfo.getLength()-offset)));
			for (int line = startLine+1; line < endLine; line++) {
				lineInfo = document.getLineInformation(line);
				lines.add(document.get(lineInfo.getOffset(), lineInfo.getLength()));
			}
			lineInfo = document.getLineInformation(endLine);
			if (offset+length > lineInfo.getOffset()) {
				lines.add(document.get(lineInfo.getOffset(), offset+length-lineInfo.getOffset()));
			}
		}
	}
	
	/**
	 * Computes the region of full lines containing the two specified positions 
	 * (e.g. begin and end offset of the editor selection).
	 * 
	 * If the second position is in column 0 and in another line than the first position,
	 * the line of second position is not included in the region. The last line contains
	 * the line delimiter, if exists (not if EOF).
	 * 
	 * @param document the document
	 * @param position1 first position
	 * @param position2 second position >= position1
	 * @return a region for the block
	 * @throws BadLocationException
	 */
	public static final IRegion getBlock(final IDocument document, final int position1, final int position2) throws BadLocationException {
		final int line1 = document.getLineOfOffset(position1);
		int line2 = document.getLineOfOffset(position2);
		if (line1 < line2 && document.getLineOffset(line2) == position2) {
			line2--;
		}
		final int start = document.getLineOffset(line1);
		final int length = document.getLineOffset(line2)+document.getLineLength(line2)-start;
		return new Region(start, length);
	}
	
	public static final IRegion expand(final IRegion region1, final IRegion region2) {
		if (region2 == null) {
			return region1;
		}
		final int offset = Math.min(region1.getOffset(), region2.getOffset());
		return new Region(offset, Math.max(
				region1.getOffset()+region1.getLength(), region2.getOffset()+region2.getLength())
						- offset);
	}
	
	public static final int getColumn(final IDocument document, final int offset, int line, int tabWidth)
			throws BadLocationException {
		if (offset > document.getLength()) {
			return -1;
		}
		if (line < 0) {
			line = document.getLineOfOffset(offset);
		}
		if (tabWidth <= 0) {
			tabWidth = 8;
		}
		int currentColumn = 0;
		int currentOffset = document.getLineOffset(line);
		while (currentOffset < offset) {
			final char c = document.getChar(currentOffset++);
			switch (c) {
			case '\n':
			case '\r':
				return -1;
			case '\t':
				currentColumn += tabWidth - (currentColumn % tabWidth);
				continue;
			default:
				currentColumn++;
				continue;
			}
		}
		return currentColumn;
	}
	
	public static final int getColumn(final String text, final int offset, int tabWidth) {
		if (offset > text.length()) {
			return -1;
		}
		if (tabWidth <= 0) {
			tabWidth = 8;
		}
		int currentColumn = 0;
		int currentOffset = 0;
		while (currentOffset < offset) {
			final char c = text.charAt(currentOffset++);
			switch (c) {
			case '\n':
			case '\r':
				return -1;
			case '\t':
				currentColumn += tabWidth - (currentColumn % tabWidth);
				continue;
			default:
				currentColumn++;
				continue;
			}
		}
		return currentColumn;
	}
	
	
	public static final int countBackward(final IDocument document, int offset, final char c)
			throws BadLocationException {
		int count= 0;
		while (offset > 0 && document.getChar(--offset) == c) {
			count++;
		}
		return count;
	}
	
	public static final int countForward(final IDocument document, int offset, final char c)
			throws BadLocationException {
		int count= 0;
		final int length= document.getLength();
		while (offset < length && document.getChar(offset++) == c) {
			count++;
		}
		return count;
	}
	
	
	public static List<IRegion> getMatchingRegions(final AbstractDocument document,
			final String partitioning, final IPartitionConstraint contraint,
			final IRegion region, final boolean extend) throws BadLocationException, BadPartitioningException {
		final List<IRegion> regions = new ArrayList<>();
		
		final int regionEnd = region.getOffset() + region.getLength();
		int validBegin = -1;
		int offset = region.getOffset();
		
		if (extend && offset > 0) {
			final ITypedRegion partition = document.getPartition(partitioning, offset - 1, false);
			if (contraint.matches(partition.getType())) {
				offset = partition.getOffset();
				do {
					final ITypedRegion prevPartition = document.getPartition(partitioning, offset - 1, false);
					if (!contraint.matches(prevPartition.getType())) {
						break;
					}
					offset = prevPartition.getOffset();
				} while (offset > 0);
				validBegin = offset;
			}
			offset = partition.getOffset() + partition.getLength();
		}
		
		do {
			final ITypedRegion partition = document.getPartition(partitioning, offset, false);
			if (validBegin < 0) {
				if (contraint.matches(partition.getType())) {
					validBegin = partition.getOffset();
				}
			}
			else { // (validBegin >= 0)
				if (!contraint.matches(partition.getType())) {
					regions.add(new Region(validBegin, offset - validBegin));
					validBegin = -1;
				}
			}
			offset = partition.getOffset() + partition.getLength();
		} while (offset < regionEnd);
		
		if (validBegin >= 0) {
			if (extend) {
				do {
					final ITypedRegion partition = document.getPartition(partitioning, offset, false);
					if (!contraint.matches(partition.getType())) {
						break;
					}
					offset = partition.getOffset() + partition.getLength();
				} while (offset < document.getLength());
			}
			regions.add(new Region(validBegin, offset - validBegin));
		}
		
		return regions;
	}
	
}
