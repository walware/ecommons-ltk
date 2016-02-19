/*=============================================================================#
 # Copyright (c) 2014-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.util;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

import de.walware.ecommons.ltk.ast.IAstNode;
import de.walware.ecommons.ltk.ast.ICommonAstVisitor;


public class AstPrinter implements ICommonAstVisitor {
	
	
	protected final Writer writer;
	
	private String text;
	
	private int level;
	
	private final int maxFragmentSize= 25;
	
	
	public AstPrinter(final Writer writer) {
		this.writer= writer;
	}
	
	
	public Writer getWriter() {
		return this.writer;
	}
	
	public void print(final IAstNode node, final String text) throws IOException {
		try {
			this.level= 0;
			this.text= text;
			
			node.accept(this);
		}
		catch (final InvocationTargetException e) {
			throw (IOException) e.getCause();
		}
		finally {
			this.text= null;
		}
	}
	
	@Override
	public void visit(final IAstNode node) throws InvocationTargetException {
		try {
			printIdent(this.level);
			this.writer.append('[');
			this.writer.append(Integer.toString(node.getOffset()));
			this.writer.append(", "); //$NON-NLS-1$
			this.writer.append(Integer.toString(node.getEndOffset()));
			this.writer.append(") "); //$NON-NLS-1$
			printNodeInfo(node);
			printFragment(node.getOffset(), node.getEndOffset());
			this.writer.append('\n');
			
			this.level++;
			node.acceptInChildren(this);
			this.level--;
		}
		catch (final IOException e) {
			throw new InvocationTargetException(e);
		}
	}
	
	protected void printIdent(final int depth) throws IOException {
		for (int i= 0; i < depth; i++) {
			this.writer.write("    "); //$NON-NLS-1$
		}
	}
	
	protected void printNodeInfo(final IAstNode node) throws IOException {
		this.writer.append(node.getClass().getSimpleName());
	}
	
	protected void printFragment(final int beginOffset, final int endOffset) throws IOException {
		if (this.text != null && this.maxFragmentSize > 0) {
			this.writer.write(": "); //$NON-NLS-1$
			int l= endOffset - beginOffset;
			if (l <= this.maxFragmentSize) {
				writeEncoded(this.text, beginOffset, l);
			}
			else if (this.maxFragmentSize < 13) {
				writeEncoded(this.text, beginOffset, this.maxFragmentSize - 3);
				this.writer.write(" ... "); //$NON-NLS-1$
			}
			else {
				l= (this.maxFragmentSize - 3) / 2;
				writeEncoded(this.text, beginOffset, l);
				this.writer.write(" ... "); //$NON-NLS-1$
				writeEncoded(this.text, endOffset - l, l);
			}
		}
	}
	
	private void writeEncoded(final String s, final int begin, final int length) throws IOException {
		final int end= begin + length;
		for (int i= begin; i < end; i++) {
			if (i < 0) {
				this.writer.write("<E: out of bounds>");
				i= 0;
			}
			else if (i >= s.length()) {
				this.writer.write("<E: out of bounds>");
				return;
			}
			final int c= s.charAt(i);
			if (c < 0x10) {
				this.writer.write("<0x0"); //$NON-NLS-1$
				this.writer.write(Integer.toHexString(c));
				this.writer.write('>');
			}
			else if (c < 0x20) {
				this.writer.write("<0x"); //$NON-NLS-1$
				this.writer.write(Integer.toHexString(c));
				this.writer.write('>');
			}
			else {
				this.writer.write(c);
			}
		}
	}
	
}
