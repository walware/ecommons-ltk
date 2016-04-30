/*=============================================================================#
 # Copyright (c) 2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.buildpaths.core;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.walware.jcommons.collections.ImCollections;
import de.walware.jcommons.collections.ImList;


public class BuildpathsUtils {
	
	
	public static final boolean equalPattern(final IPath pattern1, final IPath pattern2) {
		return (pattern1 == pattern2)
				|| (pattern1 != null && pattern1.equals(pattern2)
						&& pattern1.hasTrailingSeparator() == pattern2.hasTrailingSeparator() );
	}
	
	public static boolean equalPatterns(final List<IPath> patterns1, final List<IPath> patterns2) {
		if (patterns1 == patterns2) {
			return true;
		}
		if (patterns1 != null && patterns2 != null) {
			final int length= patterns1.size();
			if (patterns2.size() == length) {
				for (int i= 0; i < length; i++) {
					if (!equalPattern(patterns1.get(i), patterns2.get(i))) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	
	public static ImList<IPath> decodePatterns(final String sequence) {
		if (sequence != null) {
			if (sequence.isEmpty()) {
				return ImCollections.emptyList();
			}
			final String[] patterns= sequence.split("\\|");
			final IPath[] paths= new IPath[patterns.length];
			int index= 0;
			for (int j= 0; j < patterns.length; j++) {
				final String pattern= patterns[j];
				if (pattern.isEmpty()) {
					continue; // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=105581
				}
				paths[index++]= new Path(new String(pattern));
			}
			return ImCollections.newList(paths, 0, index);
		}
		return null;
	}
	
	public static String encodePatterns(final ImList<IPath> patterns) {
		if (patterns != null) {
			if (patterns.isEmpty()) {
				return ""; //$NON-NLS-1$
			}
			if (patterns.size() == 1) {
				return patterns.get(0).toPortableString();
			}
			final Iterator<IPath> iter= patterns.iterator();
			final StringBuilder sb= new StringBuilder(iter.next().toPortableString());
			while (iter.hasNext()) {
				sb.append('|');
				sb.append(iter.next().toPortableString());
			}
			
			return sb.toString();
		}
		return null;
	}
	
	
	public final static boolean isExcluded(final IResource resource,
			final IBuildpathElement element) {
		final IPath path= resource.getFullPath();
		final int resourceType= resource.getType();
		final ImList<String> inclusionPatterns= ((BuildpathElement) element).getFullInclusionPatterns();
		final ImList<String> exclusionPatterns= ((BuildpathElement) element).getFullExclusionPatterns();
		return isExcluded(path.toString(), (resourceType == IResource.FOLDER || resourceType == IResource.PROJECT),
				inclusionPatterns, exclusionPatterns );
	}
	
	public final static boolean isExcluded(String path, final boolean isFolderPath,
			final ImList<String> inclusionPatterns, final ImList<String> exclusionPatterns) {
		if (inclusionPatterns == null && exclusionPatterns == null) {
			return false;
		}
		
		CHECK_INCLUSION: if (inclusionPatterns != null) {
			for (final String pattern : exclusionPatterns) {
				String folderPattern= pattern;
				if (isFolderPath) {
					final int lastSlash= pattern.lastIndexOf('/');
					if (lastSlash != -1 && lastSlash != pattern.length() - 1) { // trailing slash -> adds '**' for free (see http://ant.apache.org/manual/dirtasks.html)
						final int star= pattern.indexOf('*', lastSlash);
						if ((star == -1
								|| star >= pattern.length() - 1
								|| pattern.charAt(star + 1) != '*')) {
							folderPattern= pattern.substring(0, lastSlash);
						}
					}
				}
				if (matchPath(folderPattern, path, true, '/')) {
					break CHECK_INCLUSION;
				}
			}
			return true; // never included
		}
		if (isFolderPath) {
			path= path + "/*";
		}
		if (exclusionPatterns != null) {
			for (final String pattern : exclusionPatterns) {
				if (matchPath(pattern, path, true, '/')) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Answers true if the pattern matches the filepath using the pathSepatator, false otherwise.
	 *
	 * Path char[] pattern matching, accepting wild-cards '**', '*' and '?' (using Ant directory tasks
	 * conventions, also see "http://jakarta.apache.org/ant/manual/dirtasks.html#defaultexcludes").
	 * Path pattern matching is enhancing regular pattern matching in supporting extra rule where '**' represent
	 * any folder combination.
	 * Special rule:
	 * - foo\  is equivalent to foo\**
	 * When not case sensitive, the pattern is assumed to already be lowercased, the
	 * name will be lowercased character per character as comparing.
	 *
	 * @param pattern the given pattern
	 * @param filepath the given path
	 * @param isCaseSensitive to find out whether or not the matching should be case sensitive
	 * @param pathSeparator the given path separator
	 * @return true if the pattern matches the filepath using the pathSepatator, false otherwise
	 */
	public static final boolean matchPath(final String pattern, final String filepath,
		final boolean isCaseSensitive, final char pathSeparator) {
		if (filepath == null) {
			return false; // null name cannot match
		}
		if (pattern == null) {
			return true; // null pattern is equivalent to '*'
		}
		
		// offsets inside pattern
		int pSegmentStart= (pattern.charAt(0) == pathSeparator) ? 1 : 0;
		final int pLength= pattern.length();
		int pSegmentEnd= pattern.indexOf(pathSeparator, pSegmentStart + 1);
		if (pSegmentEnd < 0) {
			pSegmentEnd= pLength;
		}
		
		// special case: pattern foo\ is equivalent to foo\**
		final boolean freeTrailingDoubleStar= (pattern.charAt(pLength - 1) == pathSeparator);
		
		// offsets inside filepath
		int fSegmentStart;
		final int fLength= filepath.length();
		if (filepath.charAt(0) != pathSeparator){
			fSegmentStart= 0;
		}
		else {
			fSegmentStart= 1;
		}
		if (fSegmentStart != pSegmentStart) {
			return false; // both must start with a separator or none.
		}
		int fSegmentEnd= filepath.indexOf(pathSeparator, fSegmentStart + 1);
		if (fSegmentEnd < 0) {
			fSegmentEnd= fLength;
		}
		
		// first segments
		while (pSegmentStart < pLength
			&& !(pSegmentEnd == pLength && freeTrailingDoubleStar
					|| (pSegmentEnd == pSegmentStart + 2
							&& pattern.charAt(pSegmentStart) == '*'
							&& pattern.charAt(pSegmentStart + 1) == '*' ))) {
			
			if (fSegmentStart >= fLength) {
				return false;
			}
			if (!match(pattern, pSegmentStart, pSegmentEnd,
							filepath, fSegmentStart, fSegmentEnd,
							isCaseSensitive )) {
				return false;
			}
			
			// jump to next segment
			pSegmentEnd= pattern.indexOf(pathSeparator, pSegmentStart= pSegmentEnd + 1);
			// skip separator
			if (pSegmentEnd < 0) {
				pSegmentEnd= pLength;
			}
			
			fSegmentEnd= filepath.indexOf(pathSeparator, fSegmentStart= fSegmentEnd + 1);
			// skip separator
			if (fSegmentEnd < 0) {
				fSegmentEnd= fLength;
			}
		}
		
		/* check sequence of doubleStar+segment */
		int pSegmentRestart;
		if ((pSegmentStart >= pLength && freeTrailingDoubleStar)
				|| (pSegmentEnd == pSegmentStart + 2
					&& pattern.charAt(pSegmentStart) == '*'
					&& pattern.charAt(pSegmentStart + 1) == '*') ) {
			pSegmentEnd= pattern.indexOf(pathSeparator, pSegmentStart= pSegmentEnd + 1);
			// skip separator
			if (pSegmentEnd < 0) {
				pSegmentEnd= pLength;
			}
			pSegmentRestart= pSegmentStart;
		}
		else {
			if (pSegmentStart >= pLength) {
				return fSegmentStart >= fLength; // true if filepath is done too.
			}
			pSegmentRestart= 0; // force fSegmentStart check
		}
		int fSegmentRestart= fSegmentStart;
		CHECK_SEGMENT : while (fSegmentStart < fLength) {
			if (pSegmentStart >= pLength) {
				if (freeTrailingDoubleStar) {
					return true;
				}
				// mismatch - restart current path segment
				pSegmentEnd= pattern.indexOf(pathSeparator, pSegmentStart= pSegmentRestart);
				if (pSegmentEnd < 0) {
					pSegmentEnd= pLength;
				}
				
				fSegmentRestart= filepath.indexOf(pathSeparator, fSegmentRestart + 1);
				// skip separator
				if (fSegmentRestart < 0) {
					fSegmentRestart= fLength;
				}
				else {
					fSegmentRestart++;
				}
				fSegmentEnd= filepath.indexOf(pathSeparator, fSegmentStart= fSegmentRestart);
				if (fSegmentEnd < 0) {
					fSegmentEnd= fLength;
				}
				continue CHECK_SEGMENT;
			}
			
			/* path segment is ending */
			if (pSegmentEnd == pSegmentStart + 2
					&& pattern.charAt(pSegmentStart) == '*'
					&& pattern.charAt(pSegmentStart + 1) == '*') {
				pSegmentEnd= pattern.indexOf(pathSeparator, pSegmentStart= pSegmentEnd + 1);
				// skip separator
				if (pSegmentEnd < 0) {
					pSegmentEnd= pLength;
				}
				pSegmentRestart= pSegmentStart;
				fSegmentRestart= fSegmentStart;
				if (pSegmentStart >= pLength) {
					return true;
				}
				continue CHECK_SEGMENT;
			}
			/* chech current path segment */
			if (!match(pattern, pSegmentStart, pSegmentEnd,
								filepath, fSegmentStart, fSegmentEnd,
								isCaseSensitive )) {
				// mismatch - restart current path segment
				pSegmentEnd= pattern.indexOf(pathSeparator, pSegmentStart= pSegmentRestart);
				if (pSegmentEnd < 0) {
					pSegmentEnd= pLength;
				}
				
				fSegmentRestart= filepath.indexOf(pathSeparator, fSegmentRestart + 1);
				// skip separator
				if (fSegmentRestart < 0) {
					fSegmentRestart= fLength;
				}
				else {
					fSegmentRestart++;
				}
				fSegmentEnd= filepath.indexOf(pathSeparator, fSegmentStart= fSegmentRestart);
				if (fSegmentEnd < 0) {
					fSegmentEnd= fLength;
				}
				continue CHECK_SEGMENT;
			}
			// jump to next segment
			pSegmentEnd= pattern.indexOf(pathSeparator, pSegmentStart= pSegmentEnd + 1);
			// skip separator
			if (pSegmentEnd < 0) {
				pSegmentEnd= pLength;
			}
			
			fSegmentEnd= filepath.indexOf(pathSeparator, fSegmentStart= fSegmentEnd + 1);
			// skip separator
			if (fSegmentEnd < 0) {
				fSegmentEnd= fLength;
			}
		}
		
		return ((pSegmentRestart >= pSegmentEnd)
				|| (fSegmentStart >= fLength && pSegmentStart >= pLength)
				|| (pSegmentStart == pLength - 2
						&& pattern.charAt(pSegmentStart) == '*'
						&& pattern.charAt(pSegmentStart + 1) == '*')
				|| (pSegmentStart == pLength && freeTrailingDoubleStar) );
	}
	
	/**
	 * Answers true if a sub-pattern matches the subpart of the given name, false otherwise.
	 * char[] pattern matching, accepting wild-cards '*' and '?'. Can match only subset of name/pattern.
	 * end positions are non-inclusive.
	 * The subpattern is defined by the patternStart and pattternEnd positions.
	 * When not case sensitive, the pattern is assumed to already be lowercased, the
	 * name will be lowercased character per character as comparing.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    pattern= { '?', 'b', '*' }
	 *    patternStart= 1
	 *    patternEnd= 3
	 *    name= { 'a', 'b', 'c' , 'd' }
	 *    nameStart= 1
	 *    nameEnd= 4
	 *    isCaseSensitive= true
	 *    result => true
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    pattern= { '?', 'b', '*' }
	 *    patternStart= 1
	 *    patternEnd= 2
	 *    name= { 'a', 'b', 'c' , 'd' }
	 *    nameStart= 1
	 *    nameEnd= 4
	 *    isCaseSensitive= true
	 *    result => false
	 * </pre>
	 * </li>
	 * </ol>
	 * 
	 * @param pattern the given pattern
	 * @param patternStart the given pattern start
	 * @param patternEnd the given pattern end
	 * @param name the given name
	 * @param nameStart the given name start
	 * @param nameEnd the given name end
	 * @param isCaseSensitive flag to know if the matching should be case sensitive
	 * @return true if a sub-pattern matches the subpart of the given name, false otherwise
	 */
	public static final boolean match(
			final String pattern,
			final int patternStart, int patternEnd,
			final String name, final int nameStart, int nameEnd,
			final boolean isCaseSensitive) {
		if (name == null) {
			return false; // null name cannot match
		}
		if (pattern == null) {
			return true; // null pattern is equivalent to '*'
		}
		int iPattern= patternStart;
		int iName= nameStart;
		
		if (patternEnd < 0) {
			patternEnd= pattern.length();
		}
		if (nameEnd < 0) {
			nameEnd= name.length();
		}
		
		/* check first segment */
		char patternChar= 0;
		while (true) {
			if (iPattern == patternEnd) {
				if (iName == nameEnd) {
					return true; // the chars match
				}
				return false; // pattern has ended but not the name, no match
			}
			if ((patternChar= pattern.charAt(iPattern)) == '*') {
				break;
			}
			if (iName == nameEnd) {
				return false; // name has ended but not the pattern
			}
			if (patternChar != ((isCaseSensitive) ?
							name.charAt(iName) :
							Character.toLowerCase(name.charAt(iName)) )
					&& patternChar != '?') {
				return false;
			}
			iName++;
			iPattern++;
		}
		/* check sequence of star+segment */
		int segmentStart;
		if (patternChar == '*') {
			segmentStart= ++iPattern; // skip star
		}
		else {
			segmentStart= 0; // force iName check
		}
		int prefixStart= iName;
		CHECK_SEGMENT : while (iName < nameEnd) {
			if (iPattern == patternEnd) {
				iPattern= segmentStart; // mismatch - restart current segment
				iName= ++prefixStart;
				continue CHECK_SEGMENT;
			}
			/* segment is ending */
			if ((patternChar= pattern.charAt(iPattern)) == '*') {
				segmentStart= ++iPattern; // skip start
				if (segmentStart == patternEnd) {
					return true;
				}
				prefixStart= iName;
				continue CHECK_SEGMENT;
			}
			/* check current name character */
			if (patternChar != ((isCaseSensitive) ?
							name.charAt(iName) :
							Character.toLowerCase(name.charAt(iName)) )
					&& patternChar != '?') {
				iPattern= segmentStart; // mismatch - restart current segment
				iName= ++prefixStart;
				continue CHECK_SEGMENT;
			}
			iName++;
			iPattern++;
		}
		
		return ((segmentStart == patternEnd)
				|| (iName == nameEnd && iPattern == patternEnd)
				|| (iPattern == patternEnd - 1 && pattern.charAt(iPattern) == '*') );
	}
	
}
