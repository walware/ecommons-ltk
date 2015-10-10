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

package de.walware.ecommons.text;

import java.util.Arrays;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.osgi.util.NLS;

import de.walware.ecommons.text.IIndentSettings.IndentationType;
import de.walware.ecommons.text.core.ITextRegion;


/**
 * Util to compute and edit line indentations
 */
public class IndentUtil {
	
	public static final int COLUMN_IDX= 0;
	public static final int OFFSET_IDX= 1;
	
	public static final char[] repeat(final char c, final int n) {
		final char[] chars= new char[n];
		Arrays.fill(chars, c);
		return chars;
	}
	
	
	public interface ILineIndent extends ITextRegion {
		
		int getIndentOffset();
		
		int getIndentColumn();
		
		boolean isBlank();
		
	}
	
	
	private static abstract class LineIndent implements ILineIndent {
		
		
		private static final class Blank extends LineIndent {
			
			public Blank(final int lineOffset, final int indentOffset, final int indentColumn) {
				super(lineOffset, indentOffset, indentColumn);
			}
			
			@Override
			public boolean isBlank() {
				return true;
			}
			
		}
		
		private static final class NonBlank extends LineIndent {
			
			public NonBlank(final int lineOffset, final int indentOffset, final int indentColumn) {
				super(lineOffset, indentOffset, indentColumn);
			}
			
			@Override
			public boolean isBlank() {
				return false;
			}
			
		}
		
		
		private final int lineOffset;
		
		private final int indentOffset;
		
		private final int indentColumn;
		
		
		public LineIndent(final int lineOffset, final int indentOffset, final int indentColumn) {
			this.lineOffset= lineOffset;
			this.indentOffset= indentOffset;
			this.indentColumn= indentColumn;
		}
		
		
		@Override
		public int getOffset() {
			return this.lineOffset;
		}
		
		@Override
		public int getEndOffset() {
			return this.indentOffset;
		}
		
		@Override
		public int getLength() {
			return this.indentOffset - this.lineOffset;
		}
		
		@Override
		public int getIndentOffset() {
			return this.indentOffset;
		}
		
		@Override
		public int getIndentColumn() {
			return this.indentColumn;
		}
		
		@Override
		public abstract boolean isBlank();
		
	}
	
	
	public static abstract class IndentEditAction {
		
		private int indentColumn;
		
		public IndentEditAction() {
		}
		public IndentEditAction(final int indentColumn) {
			this.indentColumn= indentColumn;
		}
		public int getIndentColumn(final int line, final int lineOffset)
				throws BadLocationException {
			return this.indentColumn;
		}
		public abstract void doEdit(int line, int lineOffset, int length, StringBuilder text)
				throws BadLocationException;
	}
	
	
	private static interface EditStrategy {
		
		public void editInIndent(int firstLine, int lastLine, IndentEditAction action)
				throws BadLocationException;
		
		public void changeIndent(final int firstLine, final int lastLine, IndentEditAction action)
				throws BadLocationException;
		
		public String copyLineIndent(int line) 
				throws BadLocationException;
		
	}
	
	private class ConserveStrategy implements EditStrategy {
		
		@Override
		public void editInIndent(final int firstLine, final int lastLine, final IndentEditAction action)
				throws BadLocationException {
			final StringBuilder replacement= new StringBuilder(20);
			ITER_LINES : for (int line= firstLine; line <= lastLine; line++) {
				final IRegion lineInfo= IndentUtil.this.document.getLineInformation(line);
				final int indentColumn= action.getIndentColumn(line, lineInfo.getOffset());
				if (indentColumn < 0) {
					continue ITER_LINES;
				}
				if (indentColumn > 0) {
					replacement.setLength(0);
					int indentation= 0;
					int offset= lineInfo.getOffset();
					boolean changed= false;
					
					ITER_CHARS : while (indentation < indentColumn) {
						final int c= getDocumentChar(offset);
						int tabStart, tabEnd, spaceCount;
						switch (c) {
						case ' ':
							indentation++;
							offset++;
							replacement.append(' ');
							continue ITER_CHARS;
						case '\t':
							tabStart= (indentation/IndentUtil.this.tabWidth) * IndentUtil.this.tabWidth;
							tabEnd= tabStart + IndentUtil.this.tabWidth;
							if (tabEnd > indentColumn) {
								spaceCount= tabEnd - indentation;
								replacement.append(repeat(' ', spaceCount));
								changed= true;
							}
							else {
								replacement.append('\t');
							}
							indentation= tabEnd;
							offset++;
							continue ITER_CHARS;
						case '\r':
						case '\n':
						case -1:
							tabStart= (indentation/IndentUtil.this.tabWidth) * IndentUtil.this.tabWidth;
							tabEnd= tabStart + IndentUtil.this.tabWidth;
							if (IndentUtil.this.tabAsDefault && (tabEnd <= indentColumn)) {
								spaceCount= indentation-tabStart;
								replacement.delete(replacement.length()-spaceCount, replacement.length());
								replacement.append('\t');
								indentation= tabEnd;
								changed= true;
							}
							else {
								spaceCount= indentColumn-indentation;
								replacement.append(repeat(' ', spaceCount));
								indentation+= spaceCount;
								changed= true;
							}
							continue ITER_CHARS;
						default:
							throw new IllegalArgumentException(createNoIndentationCharMessage(c));
						}
					}
					if (changed) {
						action.doEdit(line, lineInfo.getOffset(), offset-lineInfo.getOffset(), replacement);
						continue ITER_LINES;
					}
				}
				action.doEdit(line, lineInfo.getOffset(), 0, null);
				continue ITER_LINES;
			}
		}
		
		@Override
		public void changeIndent(final int firstLine, final int lastLine, final IndentEditAction action)
				throws BadLocationException {
			final StringBuilder replacement= new StringBuilder(20);
			ITER_LINES : for (int line= firstLine; line <= lastLine; line++) {
				final IRegion lineInfo= IndentUtil.this.document.getLineInformation(line);
				final int indentColumn= action.getIndentColumn(line, lineInfo.getOffset());
				if (indentColumn < 0) {
					continue ITER_LINES;
				}
				replacement.setLength(0);
				int column= 0;
				int offset= lineInfo.getOffset();
				
				ITER_CHARS : while (column < indentColumn) {
					final int c= getDocumentChar(offset);
					int tabStart, tabEnd, spaceCount;
					switch (c) {
					case ' ':
						column++;
						offset++;
						replacement.append(' ');
						continue ITER_CHARS;
					case '\t':
						tabStart= (column/IndentUtil.this.tabWidth) * IndentUtil.this.tabWidth;
						tabEnd= tabStart + IndentUtil.this.tabWidth;
						if (tabEnd > indentColumn) {
							spaceCount= indentColumn - column;
							replacement.append(repeat(' ', spaceCount));
							column= indentColumn;
						}
						else {
							replacement.append('\t');
							column= tabEnd;
						}
						offset++;
						continue ITER_CHARS;
					default:
						break ITER_CHARS;
					}
				}
				ITER_CHARS : while (true) {
					final int c= getDocumentChar(offset);
					if (c != ' ' && c != '\t') {
						break ITER_CHARS;
					}
					offset++;
				}
				if (column < indentColumn) {
					appendIndent(replacement, column, indentColumn);
				}
				
				action.doEdit(line, lineInfo.getOffset(), offset-lineInfo.getOffset(), replacement);
				continue ITER_LINES;
			}
		}
		
		@Override
		public String copyLineIndent(final int line) throws BadLocationException {
			final IRegion lineInfo= IndentUtil.this.document.getLineInformation(line);
			int offset= lineInfo.getOffset();
			ITERATE_CHAR : while (true) {
				final int c= getDocumentChar(offset++);
				switch (c) {
				case ' ':
					continue ITERATE_CHAR;
				case '\t':
					continue ITERATE_CHAR;
				default:
					--offset;
					break ITERATE_CHAR;
				}
			}
			return IndentUtil.this.document.get(lineInfo.getOffset(), offset-lineInfo.getOffset());
		}
		
	}
	
	private class CorrectStrategy implements EditStrategy {
		
		@Override
		public void editInIndent(final int firstLine, final int lastLine, final IndentEditAction action)
				throws BadLocationException {
			final StringBuilder replacement= new StringBuilder(20);
			ITER_LINES : for (int line= firstLine; line <= lastLine; line++) {
				final int lineOffset= IndentUtil.this.document.getLineOffset(line);
				final int indentColumn= action.getIndentColumn(line, lineOffset);
				if (indentColumn < 0) {
					continue ITER_LINES;
				}
				final int[] current= getLineIndent(line, true);
				replacement.setLength(0);
				appendIndent(replacement, indentColumn);
				if (current[COLUMN_IDX] >= 0) {
					appendSpaces(replacement, current[COLUMN_IDX]-indentColumn);
				}
				action.doEdit(line, lineOffset, current[OFFSET_IDX]-lineOffset, replacement);
				continue ITER_LINES;
			}
		}
		
		@Override
		public void changeIndent(final int firstLine, final int lastLine, final IndentEditAction action)
				throws BadLocationException {
			final StringBuilder replacement= new StringBuilder(20);
			ITER_LINES : for (int line= firstLine; line <= lastLine; line++) {
				final int lineOffset= IndentUtil.this.document.getLineOffset(line);
				final int indentColumn= action.getIndentColumn(line, lineOffset);
				if (indentColumn < 0) {
					continue ITER_LINES;
				}
				final int[] current= getLineIndent(line, false);
				replacement.setLength(0);
				appendIndent(replacement, indentColumn);
				action.doEdit(line, lineOffset, current[OFFSET_IDX]-lineOffset, replacement);
				continue ITER_LINES;
			}
			
		}
		
		@Override
		public String copyLineIndent(final int line) throws BadLocationException {
			final IRegion lineInfo= IndentUtil.this.document.getLineInformation(line);
			int column= 0;
			int offset= lineInfo.getOffset();
			ITERATE_CHAR : while (true) {
				final int c= getDocumentChar(offset++);
				switch (c) {
				case ' ':
					column++;
					continue ITERATE_CHAR;
				case '\t':
					column+= IndentUtil.this.tabWidth - (column % IndentUtil.this.tabWidth);
					continue ITERATE_CHAR;
				default:
					break ITERATE_CHAR;
				}
			}
			return createIndentString(column);
		}
		
	}
	
	
	private final IDocument document;
	private final int tabWidth;
	private final boolean tabAsDefault;
	private final int numOfSpaces;
	private final EditStrategy editStrategy;
	
	
//	public IndentUtil(final IDocument document, final booeditStrategy,
//			final boolean tabsAsDefault, final int tabWidth, final int numOfSpaces) {
//		fDocument= document;
//		switch (editStrategy) {
//		case CONSERVE_STRATEGY:
//			fConservative= new ConserveStrategy();
//			break;
//		case CORRECT_STRATEGY:
//			fConservative= new CorrectStrategy();
//			break;
//		}
//		fTabAsDefault= tabsAsDefault;
//		fTabWidth= tabWidth;
//		fNumOfSpaces= numOfSpaces;
//	}
//	
	public IndentUtil(final IDocument document, final IIndentSettings settings) {
		this.document= document;
		this.editStrategy= (settings.getReplaceConservative()) ?
				new ConserveStrategy() : new CorrectStrategy();
		this.tabAsDefault= (settings.getIndentDefaultType() == IndentationType.TAB);
		this.tabWidth= settings.getTabSize();
		this.numOfSpaces= settings.getIndentSpacesCount();
	}
	
	
	public final IDocument getDocument() {
		return this.document;
	}
	
	public final int getTabWidth() {
		return this.tabWidth;
	}
	
	/**
	 * Return the indentation indentColumn of the specified line.
	 * 
	 * @param line line to check
	 * @param markBlankLine if true, empty lines have are marked with a indentColumn of -1
	 * @return column and offset of line indent
	 * @throws BadLocationException
	 */
	@Deprecated
	public int[] getLineIndent(final int line, final boolean markBlankLine) throws BadLocationException {
		final IRegion lineInfo= this.document.getLineInformation(line);
		int column= 0;
		int offset= lineInfo.getOffset();
		ITERATE_CHAR : while (true) {
			final int c= getDocumentChar(offset++);
			switch (c) {
			case ' ':
				column++;
				continue ITERATE_CHAR;
			case '\t':
				column+= this.tabWidth - (column % this.tabWidth);
				continue ITERATE_CHAR;
			case '\r':
			case '\n':
			case -1:
				if (markBlankLine) {
					return new int[] { -1, --offset };
				}
				//$FALL-THROUGH$
			default:
				return new int[] { column, --offset };
			}
		}
	}
	
	/**
	 * Creates a line indentation string with same depth as the given line.
	 * The exact string depends on the configured strategy.
	 * @param line the line number
	 * @return line indentation string
	 * @throws BadLocationException
	 */
	public String copyLineIndent(final int line) throws BadLocationException {
		return this.editStrategy.copyLineIndent(line);
	}
	
//	public int getIndentationindentColumn(String chars) throws BadLocationException {
//		
//		int indentation= 0;
//		ITER_CHARS : for (int i= 0; i < chars.length(); i++) {
//			char c= fDocument.getChar(i);
//			switch (c) {
//			case ' ':
//				indentation++;
//				continue ITER_CHARS;
//			case '\t':
//				indentation+= fTabWidth;
//				continue ITER_CHARS;
//			default:
//				throw new IllegalArgumentException("No indentation char: '"+c+"'."); //$NON-NLS-1$ //$NON-NLS-2$
//			}
//		}
//		return indentation;
//	}
	
	/**
	 * Returns the common (min) indentation indentColumn of all lines. Empty lines are ignored.
	 * @param startLine line index of first line
	 * @param endLine line index of last line
	 * @return
	 * @throws BadLocationException
	 */
	public int getMultilineIndentColumn(final int startLine, final int endLine) throws BadLocationException {
		int indentation= Integer.MAX_VALUE;
		for (int line= startLine; line <= endLine; line++) {
			final int[] lineIndent= getLineIndent(line, true);
			if (lineIndent[COLUMN_IDX] >= 0) {
				indentation= Math.min(indentation, lineIndent[COLUMN_IDX]);
			}
		}
		if (indentation == Integer.MAX_VALUE) {
			indentation= 0;
		}
		return indentation;
	}
	
	/**
	 * Prepares the indentation of the line, so you can insert text at the
	 * given indentation indentColumn.
	 * 
	 * @param line line index
	 * @return the returned object of your action
	 * @throws BadLocationException
	 */
	public void editInIndent(final int firstLine, final int lastLine, final IndentEditAction action)
			throws BadLocationException {
		this.editStrategy.editInIndent(firstLine, lastLine, action);
	}
	
	public void changeIndent(final int firstLine, final int lastLine, final IndentEditAction action)
			throws BadLocationException {
		this.editStrategy.changeIndent(firstLine, lastLine, action);
	}
	
	
	/**
	 * Returns the indentation of the specified line.
	 * 
	 * @param line line to check
	 * @return the line indent
	 */
	public final ILineIndent getIndent(final CharSequence line) {
		int offset= 0;
		int column= 0;
		ITER_CHARS : for (; offset < line.length(); offset++) {
			final char c= line.charAt(offset);
			switch (c) {
			case ' ':
				column++;
				continue ITER_CHARS;
			case '\t':
				column+= this.tabWidth - (column % this.tabWidth);
				continue ITER_CHARS;
			case '\r':
			case '\n':
				break ITER_CHARS;
			default:
				return new LineIndent.NonBlank(0, offset, column);
			}
		}
		return new LineIndent.Blank(0, offset, column);
	}
	
	/**
	 * Returns the column of the specified offset.
	 * 
	 * Linebreak are not specially handled.
	 * 
	 * @param offset index in string
	 * @return char column
	 */
	public final int getColumn(final CharSequence line, final int offset) {
		int checkOffset= 0;
		int column= 0;
		ITER_CHARS : while (checkOffset < offset) {
			switch (line.charAt(checkOffset++)) {
			case '\t':
				column+= this.tabWidth - (column % this.tabWidth);
				continue ITER_CHARS;
			default:
				column++;
				continue ITER_CHARS;
			}
		}
		return column;
	}
	
	/**
	 * Returns the column of the specified offset.
	 * 
	 * Linebreak are not specially handled.
	 * 
	 * @param line text
	 * @param offset index in text
	 * @param startColumn column of the text
	 * @return char column
	 */
	public final int getColumn(final CharSequence line, final int offset, final int startColumn) {
		int checkOffset= 0;
		int column= startColumn;
		ITER_CHARS : while (checkOffset < offset) {
			switch (line.charAt(checkOffset++)) {
			case '\t':
				column+= this.tabWidth - (column % this.tabWidth);
				continue ITER_CHARS;
			default:
				column++;
				continue ITER_CHARS;
			}
		}
		return column;
	}
	
	/**
	 * Returns the char offset in indentation with column ≥ the specified column.
	 * 
	 * @param line text to check
	 * @param column indentColumn to search for
	 * @return the offset
	 */
	public final int getIndentedOffsetAt(final CharSequence line, final int column) {
		int currentOffset= 0;
		int currentColumn= 0;
		ITER_CHARS : for (; currentOffset < line.length() && currentColumn < column; currentOffset++) {
			final char c= line.charAt(currentOffset);
			switch (c) {
			case ' ':
				currentColumn++;
				continue ITER_CHARS;
			case '\t':
				currentColumn+= this.tabWidth - (currentColumn % this.tabWidth);
				continue ITER_CHARS;
			default:
				throw new IllegalArgumentException(createNoIndentationCharMessage(c));
			}
		}
		return currentOffset;
	}
	
	
	/**
	 * Returns the indentation of the specified line.
	 * 
	 * @param lineNum the index of the line to check
	 * @return the line indent
	 * @throws BadLocationException 
	 */
	public final ILineIndent getIndent(final int lineNum) throws BadLocationException {
		int offset= this.document.getLineOffset(lineNum);
		int column= 0;
		final int bound= this.document.getLength();
		ITER_CHARS : for (; offset < bound; offset++) {
			final char c= this.document.getChar(offset);
			switch (c) {
			case ' ':
				column++;
				continue ITER_CHARS;
			case '\t':
				column+= this.tabWidth - (column % this.tabWidth);
				continue ITER_CHARS;
			case '\r':
			case '\n':
				break ITER_CHARS;
			default:
				return new LineIndent.NonBlank(0, offset, column);
			}
		}
		return new LineIndent.Blank(0, offset, column);
	}
	
	/**
	 * Returns the column of the specified offset.
	 * 
	 * Linebreaks are not specially handled.
	 * 
	 * @param offset offset in document
	 * @return the column
	 * @throws BadLocationException
	 */
	public final int getColumn(final int offset) throws BadLocationException {
		return getColumn(this.document.getLineOfOffset(offset), offset);
	}
	
	/**
	 * Returns the column of the specified offset.
	 * 
	 * Linebreaks are not specially handled.
	 * 
	 * @param lineNum the index of the line to check
	 * @param offset the offset in document
	 * @return the column
	 * @throws BadLocationException
	 */
	public final int getColumn(final int lineNum, final int offset) throws BadLocationException {
		int checkOffset= this.document.getLineOffset(lineNum);
		int column= 0;
		ITER_CHARS : while (checkOffset < offset) {
			switch (this.document.getChar(checkOffset++)) {
			case '\t':
				column+= this.tabWidth - (column % this.tabWidth);
				continue ITER_CHARS;
			default:
				column++;
				continue ITER_CHARS;
			}
		}
		return column;
	}
	
	/**
	 * Returns the document offset in indentation with column ≥ the specified column.
	 * 
	 * @param lineNum the index of the line to check
	 * @param column the column
	 * @return the offset of the column
	 * @throws BadLocationException
	 * @throws IllegalArgumentException
	 */
	public final int getIndentedOffsetAt(final int lineNum, final int column)
			throws BadLocationException {
		final IRegion lineInfo= this.document.getLineInformation(lineNum);
		int currentOffset= lineInfo.getOffset();
		int currentColumn= 0;
		ITER_CHARS : while (currentColumn < column) {
			final char c= this.document.getChar(currentOffset++);
			switch (c) {
			case ' ':
				currentColumn++;
				continue ITER_CHARS;
			case '\t':
				currentColumn+= this.tabWidth - (currentColumn % this.tabWidth);
				continue ITER_CHARS;
			default:
				throw new IllegalArgumentException(createNoIndentationCharMessage(c));
			}
		}
		return currentOffset;
	}
	
	/**
	 * Returns the last document offset with column ≤ the specified column.
	 * 
	 * @param lineNum the index of the line to check
	 * @param column the column
	 * @return the offset of the column or -1 if line is shorter
	 * @throws BadLocationException
	 */
	public final int getOffsetAtMax(final int lineNum, final int column)
			throws BadLocationException {
		final IRegion lineInfo= this.document.getLineInformation(lineNum);
		int offset= lineInfo.getOffset();
		final int bound= lineInfo.getLength();
		int current= 0;
		ITER_CHARS : while (current < column) {
			if (offset >= bound) {
				return -1;
			}
			switch (this.document.getChar(offset++)) {
			case '\t':
				current+= this.tabWidth - (current % this.tabWidth);
				continue ITER_CHARS;
			default:
				current++;
				continue ITER_CHARS;
			}
		}
		return (current > column) ? offset - 1 : offset;
	}
	
	
	/**
	 * Returns the configured width of a default indentation.
	 * 
	 * @return number of visual char columns
	 */
	public int getLevelColumns() {
		if (this.tabAsDefault) {
			return this.tabWidth;
		}
		else {
			return this.numOfSpaces;
		}
	}
	
	/**
	 * Computes the indentation column adding the specified levels to the current indentColumn.
	 * 
	 * @param currentColumn indentColumn in visual char columns
	 * @param levels number of indentation levels
	 * @return indentColumn in visual char columns
	 */
	public int getNextLevelColumn(final int currentColumn, final int levels) {
		final int columns= getLevelColumns();
		return ((currentColumn / columns + levels) * columns);
	}
	
	/**
	 * Creates a string for indentation of specified indentColumn (respects the preferences).
	 * @param indentColumn
	 * @return
	 */
	public String createIndentString(final int indentColumn) {
		if (this.tabAsDefault) {
			return new StringBuilder(indentColumn)
					.append(repeat('\t', indentColumn / this.tabWidth))
					.append(repeat(' ', indentColumn % this.tabWidth))
					.toString();
		}
		else {
			return new String(repeat(' ', indentColumn));
		}
	}
	
	public final String createIndentCompletionString(final int currentColumn) {
		if (this.tabAsDefault) {
			return "\t"; //$NON-NLS-1$
		}
		else {
			final int rest= currentColumn % this.numOfSpaces;
			return new String(repeat(' ', this.numOfSpaces - rest));
		}
	}
	
	public final String createTabCompletionString(final int currentColumn) {
		if (this.tabAsDefault) {
			return "\t"; //$NON-NLS-1$
		}
		else {
			return createTabSpacesCompletionString(currentColumn);
		}
	}
	
	public final String createTabSpacesCompletionString(final int currentColumn) {
		final int rest= currentColumn % this.tabWidth;
		return new String(repeat(' ', this.tabWidth - rest));
	}
	
	
	protected final int getDocumentChar(final int idx) throws BadLocationException {
		if (idx >= 0 && idx < this.document.getLength()) {
			return this.document.getChar(idx);
		}
		if (idx == -1 || idx == this.document.getLength()) {
			return -1;
		}
		throw new BadLocationException();
	}
	
	public final void appendIndent(final StringBuilder sb, final int indentColumn) {
		if (this.tabAsDefault) {
			sb.append(repeat('\t', indentColumn / this.tabWidth));
			sb.append(repeat(' ', indentColumn % this.tabWidth));
		}
		else {
			sb.append(repeat(' ', indentColumn));
		}
	}
	
	public final void appendIndent(final StringBuilder sb, final int currentColumn, final int indentColumn) {
		if (this.tabAsDefault) {
			final int tabDiff= (indentColumn / this.tabWidth) - (currentColumn / this.tabWidth) ;
			if (tabDiff > 0) {
				final int spaces= currentColumn % this.tabWidth;
				if (spaces > 0) {
					sb.append(repeat(' ', this.tabWidth - spaces));
					sb.append(repeat('\t', tabDiff - 1));
				}
				else {
					sb.append(repeat('\t', tabDiff));
				}
				sb.append(repeat(' ', indentColumn % this.tabWidth));
			}
			else {
				sb.append(repeat(' ', indentColumn - currentColumn));
			}
		}
		else {
			sb.append(repeat(' ', indentColumn));
		}
	}
	
//	protected final void appendIndentCompletion(final StringBuilder s, final int currentColumn) {
//		if (fTabAsDefault) {
//			s.append('\t');
//		}
//		else {
//			s.append(repeat(' ', fNumOfSpaces-(currentColumn % fNumOfSpaces)));
//		}
//	}
	
	protected final void appendSpaces(final StringBuilder s, final int num) {
		s.append(repeat(' ', num));
	}
	
	private String createNoIndentationCharMessage(final int c) {
		return NLS.bind("No indentation char: ''{0}''.", ((char) c)); //$NON-NLS-1$
	}
	
}
