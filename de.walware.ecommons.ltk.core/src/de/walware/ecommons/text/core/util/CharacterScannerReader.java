/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.text.core.util;

import org.eclipse.jface.text.IDocument;

import de.walware.ecommons.text.core.rules.BufferedDocumentScanner;


public class CharacterScannerReader {
	
	
	private final BufferedDocumentScanner scanner;
	
	private int offset;
	
	
	public CharacterScannerReader(final BufferedDocumentScanner scanner) {
		this.scanner= scanner;
	}
	
	
	public final BufferedDocumentScanner getScanner() {
		return this.scanner;
	}
	
	public final int read() {
		final int c= this.scanner.read();
		if (c >= 0) {
			this.offset++;
		}
		return c;
	}
	
	public final boolean read(final char c1) {
		final int c= this.scanner.read();
		if (c == c1) {
			this.offset++;
			return true;
		}
		if (c >= 0) {
			this.scanner.unread();
		}
		return false;
	}
	
	public final boolean read(final char c1, final char c2) {
		int c= this.scanner.read();
		if (c == c1) {
			c = this.scanner.read();
			if (c == c2) {
				this.offset+= 2;
				return true;
			}
			this.scanner.unread();
		}
		if (c >= 0) {
			this.scanner.unread();
		}
		return false;
	}
	
	public final boolean read(final char c1, final char c2, final char c3) {
		int c= this.scanner.read();
		if (c == c1) {
			c = this.scanner.read();
			if (c == c2) {
				c = this.scanner.read();
				if (c == c3) {
					this.offset+= 3;
					return true;
				}
				this.scanner.unread();
			}
			this.scanner.unread();
		}
		if (c >= 0) {
			this.scanner.unread();
		}
		return false;
	}
	
	public final boolean read2(final char[] cSeq) {
		for (int i= 1; i < cSeq.length; i++) {
			final int c= this.scanner.read();
			if (c != cSeq[i]) {
				unreadRaw((c >= 0) ? i : (i - 1));
				return false;
			}
		}
		this.offset+= cSeq.length - 1;
		return true;
	}
	
	
	public final boolean readConsuming(final char c1, final char c2) {
		int c= this.scanner.read();
		if (c == c1) {
			c= this.scanner.read();
			if (c == c2) {
				this.offset+= 2;
				return true;
			}
		}
		if (c >= 0) {
			this.scanner.unread();
		}
		return false;
	}
	
	public final boolean readConsuming(final char[] seq) {
		final int n= seq.length;
		for (int i= 0; i < n; i++) {
			final int c= this.scanner.read();
			if (c != seq[i]) {
				this.offset+= i;
				if (c >= 0) {
					this.scanner.unread();
				}
				return false;
			}
		}
		this.offset+= n;
		return true;
	}
	
	public final boolean readConsuming2(final char[] cSeq) {
		final int n= cSeq.length;
		for (int i= 1; i < n; i++) {
			final int c= this.scanner.read();
			if (c != cSeq[i]) {
				this.offset+= i - 1;
				if (c >= 0) {
					this.scanner.unread();
				}
				return false;
			}
		}
		this.offset+= n-1;
		return true;
	}
	
	public final int readConsumingWhitespace() {
		int readed= 0;
		while (true) {
			final int c= this.scanner.read();
			if (c != ' ' && c != '\t') {
				if (c >= 0) {
					this.scanner.unread();
				}
				this.offset+= readed;
				return readed;
			}
			readed++;
		}
	}
	
	
	public final boolean readTemp(final char c1) {
		final int c= this.scanner.read();
		if (c >= 0) {
			this.scanner.unread();
		}
		return (c == c1);
	}
	
	public final boolean readTemp(final char c1, final char c2) {
		int c= this.scanner.read();
		if (c == c1) {
			c= this.scanner.read();
			if (c == c2) {
				return true;
			}
			this.scanner.unread();
		}
		if (c >= 0) {
			this.scanner.unread();
		}
		return false;
	}
	
	
	public final void unread() {
		this.offset--;
		this.scanner.unread();
	}
	
	public final void unread(int count) {
		this.offset-= count;
		while (count-- > 0) {
			this.scanner.unread();
		}
	}
	
	
	public final int readRaw() {
		return this.scanner.read();
	}
	
	public final void unreadRaw(int count) {
		while (count-- > 0) {
			this.scanner.unread();
		}
	}
	
	
	public int getOffset() {
		return this.offset;
	}
	
	public void setRange(final IDocument document, final int beginOffset, final int length) {
		this.scanner.setRange(document, beginOffset, length);
		this.offset= beginOffset;
	}
	
}
