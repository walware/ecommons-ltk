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

package de.walware.ecommons.ltk.core;

import de.walware.ecommons.text.ILineInformation;
import de.walware.ecommons.text.LineInformationCreator;


/**
 * Source code with time stamp.
 */
public class SourceContent {
	
	
	private static final LineInformationCreator LINES_CREATOR= new LineInformationCreator();
	
	
	private final long stamp;
	
	private final String text;
	
	private final int offset;
	private volatile ILineInformation lines;
	
	
	public SourceContent(final long stamp, final String text) {
		this(stamp, text, 0, null);
	}
	
	public SourceContent(final long stamp, final String text, final int offset) {
		this(stamp, text, offset, null);
	}
	
	public SourceContent(final long stamp, final String text, final int offset,
			final ILineInformation lines) {
		this.stamp= stamp;
		this.text= text;
		this.offset= offset;
		this.lines= lines;
	}
	
	
	public final long getStamp() {
		return this.stamp;
	}
	
	public final String getText() {
		return this.text;
	}
	
	/**
	 * Returns offset of begin of the {@link #getText() text} in complete source.
	 * 
	 * @return begin offset
	 */
	public final int getBeginOffset() {
		return this.offset;
	}
	
	/**
	 * Returns offset of end of the {@link #getText() text} in complete source.
	 * 
	 * @return end offset (exclusive)
	 */
	public final int getEndOffset() {
		return this.offset + this.text.length();
	}
	
	public final ILineInformation getLines() {
		if (this.lines == null) {
			synchronized (LINES_CREATOR) {
				if (this.lines == null) {
					this.lines= LINES_CREATOR.create(this.text);
				}
			}
		}
		return this.lines;
	}
	
	
	@Override
	public String toString() {
		return getText();
	}
	
}
