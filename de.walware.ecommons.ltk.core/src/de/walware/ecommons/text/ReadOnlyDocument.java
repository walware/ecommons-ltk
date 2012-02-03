/*******************************************************************************
 * Copyright (c) 2011-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ITextStore;


/**
 * Read-only document.
 */
public class ReadOnlyDocument extends AbstractDocument {
	
	
	private static class StringTextStore implements ITextStore {
		
		private final String fContent;
		
		/**
		 * Creates a new string text store with the given content.
		 *
		 * @param content the content
		 */
		public StringTextStore(final String content) {
			if (content == null) {
				throw new NullPointerException();
			}
			fContent = content;
		}
		
		@Override
		public char get(final int offset) {
			return fContent.charAt(offset);
		}
		
		@Override
		public String get(final int offset, final int length) {
			return fContent.substring(offset, offset + length);
		}
		
		@Override
		public int getLength() {
			return fContent.length();
		}
		
		@Override
		public void replace(final int offset, final int length, final String text) {
		}
		
		@Override
		public void set(final String text) {
		}
		
	}
	
	
	/**
	 * Creates a new read-only document with the given content.
	 *
	 * @param content the content
	 * @param lineDelimiters the line delimiters
	 */
	public ReadOnlyDocument(final String content, final long timestamp) {
		super();
		setTextStore(new StringTextStore(content));
		setLineTracker(new DefaultLineTracker());
		completeInitialization();
		super.set(content, timestamp);
	}
	
	
	@Override
	public void set(final String text, final long modificationStamp) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void replace(final int pos, final int length, final String text) throws BadLocationException {
		throw new UnsupportedOperationException();
	}
	
}
