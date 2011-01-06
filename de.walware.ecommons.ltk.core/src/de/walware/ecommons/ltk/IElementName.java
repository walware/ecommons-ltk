/*******************************************************************************
 * Copyright (c) 2008-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;


/**
 * Name of an element like an {@link IModelElement}.
 * <p>
 * A element name consists of one or multiple segments. Each segment is of a special
 * type (to differ different way to identify the element). The type is model type dependent.
 * The segment has raw segment name and a name which can be used to display it (including
 * all following segments).
 */
public interface IElementName {
	
	/**
	 * Returns the type the name is specified.
	 * 
	 * @return a model dependent type constant
	 */
	public int getType();
	
	/**
	 * Returns the raw name of the segment.
	 * 
	 * @return the name or <code>null</code>
	 */
	public String getSegmentName();
	
	/**
	 * Returns a default text representation.
	 * 
	 * @return the name
	 */
	public String getDisplayName();
	
	/**
	 * Returns the next child segment, if exists.
	 * 
	 * @return the next segment or <code>null</code>
	 */
	public IElementName getNextSegment();
	
}
