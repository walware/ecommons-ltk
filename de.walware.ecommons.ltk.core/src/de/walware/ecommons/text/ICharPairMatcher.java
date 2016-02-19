/*=============================================================================#
 # Copyright (c) 2012-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ICharacterPairMatcher;


/**
 * Extended version of {@link ICharacterPairMatcher} providing
 * {@link #match(IDocument, int, boolean)}
 */
public interface ICharPairMatcher extends ICharacterPairMatcher {
	
	
	@Override
	IRegion match(IDocument document, int offset);
	
	/**
	 * Starting at the given offset, the matcher chooses a character close to this offset.
	 * The matcher then searches for the matching peer character of the chosen character
	 * and if it finds one, returns the minimal region of the document that contains both
	 * characters.
	 * 
	 * @param document the document
	 * @param offset the start offset
	 * @param auto if the character can be selected automatically
	 *     before or after the given offset
	 * @return the minimal region containing the peer characters, if a pair was found;
	 *     a region with length -1 if start character but not its peer was found;
	 *     otherwise <code>null</code>
	 */
	IRegion match(IDocument document, int offset, boolean auto);
	
	
}
