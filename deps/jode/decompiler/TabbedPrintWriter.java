/* TabbedPrintWriter Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: TabbedPrintWriter.java,v 4.20.2.4 2002/05/28 17:34:03 hoenicke Exp $
 */

package jode.decompiler;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

import jode.bytecode.ClassInfo;
import jode.bytecode.InnerClassInfo;
import jode.type.ArrayType;
import jode.type.ClassInterfacesType;
import jode.type.NullType;
import jode.type.Type;


public class TabbedPrintWriter {
	/* The indentation size. */
	private int indentsize;
	/* The size of a tab, MAXINT if we shouldn't use tabs at all. */
	private int tabWidth;
	private int lineWidth;
	private int currentIndent = 0;
	private String indentStr = "";
	private PrintWriter pw;
	private ImportHandler imports;
	private Stack scopes = new Stack();

	private StringBuffer currentLine;
	private BreakPoint currentBP;
	public final static int EXPL_PAREN = 0;
	public final static int NO_PAREN = 1;
	public final static int IMPL_PAREN = 2;
	public final static int DONT_BREAK = 3;

	/**
	 * Convert the numeric indentation to a string.
	 */
	protected String makeIndentStr(int indent) {
		String tabSpaceString = /* (tab x 20) . (space x 20) */
		"\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t                    ";
		if (indent < 0)
			return "NEGATIVEINDENT" + indent;

		int tabs = indent / tabWidth;
		indent -= tabs * tabWidth;
		if (tabs <= 20 && indent <= 20) {
			/* The fast way. */
			return tabSpaceString.substring(20 - tabs, 20 + indent);
		} else {
			/* the not so fast way */
			StringBuffer sb = new StringBuffer(tabs + indent);
			while (tabs > 20) {
				sb.append(tabSpaceString.substring(0, 20));
				tabs -= 20;
			}
			sb.append(tabSpaceString.substring(0, tabs));
			while (indent > 20) {
				sb.append(tabSpaceString.substring(20));
				indent -= 20;
			}
			sb.append(tabSpaceString.substring(40 - indent));
			return sb.toString();
		}
	}

	class BreakPoint {
		int options;
		int breakPenalty;
		int breakPos;
		int startPos;
		BreakPoint parentBP;
		Vector childBPs;
		int nesting = 0;
		int endPos;
		int whatBreak = 0;

		public BreakPoint(BreakPoint parent, int position) {
			this.breakPos = position;
			this.parentBP = parent;
			this.options = DONT_BREAK;
			this.breakPenalty = 0;
			this.startPos = -1;
			this.endPos = -1;
			this.whatBreak = 0;
			this.childBPs = null;
		}

		public void startOp(int opts, int penalty, int pos) {
			if (startPos != -1)
				throw new InternalError("missing breakOp");
			startPos = pos;
			options = opts;
			breakPenalty = penalty;
			childBPs = new Vector();
			breakOp(pos);
		}

		public void breakOp(int pos) {
			childBPs.addElement(new BreakPoint(this, pos));
		}

		public void endOp(int pos) {
			endPos = pos;
			if (childBPs.size() == 1) {
				/*
				 * There is no breakpoint in this op, replace this with our
				 * child, if possible.
				 */
				BreakPoint child = (BreakPoint) childBPs.elementAt(0);
				options = Math.min(options, child.options);
				startPos = child.startPos;
				endPos = child.endPos;
				breakPenalty = child.breakPenalty;
				childBPs = child.childBPs;
			}
		}

		public void dump(String line) {
			if (startPos == -1) {
				pw.print(line);
			} else {
				pw.print(line.substring(0, startPos));
				dumpRegion(line);
				pw.print(line.substring(endPos));
			}
		}

		public void dumpRegion(String line) {
			String parens = "{\010{}\010}<\010<>\010>[\010[]\010]`\010`'\010'"
					.substring(options * 6, options * 6 + 6);
			pw.print(parens.substring(0, 3));
			Enumeration enum_ = childBPs.elements();
			int cur = startPos;
			BreakPoint child = (BreakPoint) enum_.nextElement();
			if (child.startPos >= 0) {
				pw.print(line.substring(cur, child.startPos));
				child.dumpRegion(line);
				cur = child.endPos;
			}
			while (enum_.hasMoreElements()) {
				child = (BreakPoint) enum_.nextElement();
				pw.print(line.substring(cur, child.breakPos));
				pw.print("!\010!" + breakPenalty);
				cur = child.breakPos;
				if (child.startPos >= 0) {
					pw.print(line.substring(child.breakPos, child.startPos));
					child.dumpRegion(line);
					cur = child.endPos;
				}
			}
			pw.print(line.substring(cur, endPos));
			pw.print(parens.substring(3));
		}

		public void printLines(int indent, String line) {
			if (startPos == -1) {
				pw.print(line);
			} else {
				pw.print(line.substring(0, startPos));
				printRegion(indent + startPos, line);
				pw.print(line.substring(endPos));
			}
		}

		public void printRegion(int indent, String line) {
			if (options == IMPL_PAREN) {
				pw.print("(");
				indent++;
			}

			Enumeration enum_ = childBPs.elements();
			int cur = startPos;
			BreakPoint child = (BreakPoint) enum_.nextElement();
			if (child.startPos >= 0) {
				pw.print(line.substring(cur, child.startPos));
				child.printRegion(indent + child.startPos - cur, line);
				cur = child.endPos;
			}
			if (options == NO_PAREN)
				indent += indentsize;
			String indentStr = makeIndentStr(indent);
			while (enum_.hasMoreElements()) {
				child = (BreakPoint) enum_.nextElement();
				pw.print(line.substring(cur, child.breakPos));
				pw.println();
				pw.print(indentStr);
				cur = child.breakPos;
				if (cur < endPos && line.charAt(cur) == ' ')
					cur++;
				if (child.startPos >= 0) {
					pw.print(line.substring(cur, child.startPos));
					child.printRegion(indent + child.startPos - cur, line);
					cur = child.endPos;
				}
			}
			pw.print(line.substring(cur, endPos));
			if (options == IMPL_PAREN)
				pw.print(")");
		}

		public BreakPoint commitMinPenalty(int space, int lastSpace,
				int minPenalty) {
			if (startPos == -1 || lastSpace > endPos - startPos
					|| minPenalty == 10 * (endPos - startPos - lastSpace)) {
				/* We don't have to break anything */
				startPos = -1;
				childBPs = null;
				return this;
			}

			int size = childBPs.size();
			if (size > 1 && options != DONT_BREAK) {
				/* penalty if we are breaking the line here. */
				int breakPen = getBreakPenalty(space, lastSpace, minPenalty + 1);
				// pw.print("commit[bp="+breakPen+";"+minPenalty+";"
				// +space+","+lastSpace+"]");
				if (minPenalty == breakPen) {
					commitBreakPenalty(space, lastSpace, breakPen);
					return this;
				}
			}

			/* penalty if we are breaking only one child */
			for (int i = 0; i < size; i++) {
				BreakPoint child = (BreakPoint) childBPs.elementAt(i);
				int front = child.startPos - startPos;
				int tail = endPos - child.endPos;
				int needPenalty = minPenalty - (i < size - 1 ? 1 : 0);
				if (needPenalty == child.getMinPenalty(space - front, lastSpace
						- front - tail, needPenalty + 1)) {
					child = child.commitMinPenalty(space - front, lastSpace
							- front - tail, needPenalty);
					child.breakPos = breakPos;
					return child;
				}
			}
			pw.println("XXXXXXXXXXX CAN'T COMMIT");
			startPos = -1;
			childBPs = null;
			return this;
		}

		public int getMinPenalty(int space, int lastSpace, int minPenalty) {
			// pw.print("getMinPenalty["+startPos+","+endPos+"]("+space+","+lastSpace+","+minPenalty+") ");
			if (10 * -lastSpace >= minPenalty) {
				// pw.println("= minPenalty");
				return minPenalty;
			}

			if (startPos == -1)
				return 10 * -lastSpace;

			if (lastSpace > endPos - startPos) {
				// pw.println("= NULL");
				return 0;
			}

			if (minPenalty <= 1) {
				// pw.println("= ONE");
				return minPenalty;
			}

			if (minPenalty > 10 * (endPos - startPos - lastSpace))
				minPenalty = 10 * (endPos - startPos - lastSpace);

			// pw.print("[mp="+minPenalty+"]");

			int size = childBPs.size();
			if (size == 0)
				return minPenalty;

			if (size > 1 && options != DONT_BREAK) {
				/* penalty if we are breaking at this level. */
				minPenalty = getBreakPenalty(space, lastSpace, minPenalty);
				// pw.print("[bp="+minPenalty+"]");
			}

			/* penalty if we are breaking only one child */
			for (int i = 0; i < size; i++) {
				BreakPoint child = (BreakPoint) childBPs.elementAt(i);
				int front = child.startPos - startPos;
				int tail = endPos - child.endPos;
				int penalty = (i < size - 1 ? 1 : 0);
				minPenalty = penalty
						+ child.getMinPenalty(space - front, lastSpace - front
								- tail, minPenalty - penalty);
			}
			// pw.println("= "+minPenalty);
			return minPenalty;
		}

		public void commitBreakPenalty(int space, int lastSpace, int minPenalty) {
			// pw.println("commitBreakPenalty: "+startPos+","+endPos+";"
			// +space+","+lastSpace+";"+minPenalty);

			if (options == IMPL_PAREN) {
				space--;
				lastSpace -= 2;
			}

			Enumeration enum_ = childBPs.elements();
			childBPs = new Vector();
			int currInd = 0;
			BreakPoint lastChild, nextChild;
			boolean indentNext = options == NO_PAREN;
			for (lastChild = (BreakPoint) enum_.nextElement(); enum_
					.hasMoreElements(); lastChild = nextChild) {
				nextChild = (BreakPoint) enum_.nextElement();
				int childStart = lastChild.breakPos;
				int childEnd = nextChild.breakPos;

				if (currInd > 0) {
					currInd += childEnd - childStart;
					if (currInd <= space)
						continue;
				}
				if (childStart < endPos
						&& currentLine.charAt(childStart) == ' ')
					childStart++;

				if (childEnd - childStart > space) {
					int front = lastChild.startPos - childStart;
					int tail = childEnd - lastChild.endPos;
					int childPenalty = lastChild.getMinPenalty(space - front,
							space - front - tail, minPenalty);
					currInd = 0;
					childBPs.addElement(lastChild.commitMinPenalty(space
							- front, space - front - tail, childPenalty));
				} else {
					lastChild.startPos = -1;
					lastChild.childBPs = null;
					childBPs.addElement(lastChild);
					currInd = childEnd - childStart;
				}

				if (indentNext) {
					space -= indentsize;
					lastSpace -= indentsize;
					indentNext = false;
				}
			}
			int childStart = lastChild.breakPos;
			if (currInd > 0 && currInd + endPos - childStart <= lastSpace)
				return;

			if (childStart < endPos && currentLine.charAt(childStart) == ' ')
				childStart++;
			if (endPos - childStart > lastSpace) {
				int front = lastChild.startPos - childStart;
				int tail = endPos - lastChild.endPos;
				int childPenalty = lastChild.getMinPenalty(space - front,
						lastSpace - front - tail, minPenalty + 1);
				childBPs.addElement(lastChild.commitMinPenalty(space - front,
						lastSpace - front - tail, childPenalty));
			} else {
				lastChild.startPos = -1;
				lastChild.childBPs = null;
				childBPs.addElement(lastChild);
			}
		}

		public int getBreakPenalty(int space, int lastSpace, int minPenalty) {
			int penalty = breakPenalty;
			int currInd = 0;
			if (options == IMPL_PAREN) {
				space--;
				lastSpace -= 2;
			}
			if (space < 0)
				return minPenalty;
			Enumeration enum_ = childBPs.elements();
			BreakPoint lastChild, nextChild;
			boolean indentNext = options == NO_PAREN;
			for (lastChild = (BreakPoint) enum_.nextElement(); enum_
					.hasMoreElements(); lastChild = nextChild) {
				nextChild = (BreakPoint) enum_.nextElement();
				int childStart = lastChild.breakPos;
				int childEnd = nextChild.breakPos;

				if (currInd > 0) {
					currInd += childEnd - childStart;
					if (currInd <= space)
						continue;

					penalty++;
					if (indentNext) {
						space -= indentsize;
						lastSpace -= indentsize;
						indentNext = false;
					}
				}

				if (childStart < endPos
						&& currentLine.charAt(childStart) == ' ')
					childStart++;

				if (childEnd - childStart > space) {
					int front = lastChild.startPos - childStart;
					int tail = childEnd - lastChild.endPos;
					penalty += 1 + lastChild.getMinPenalty(space - front, space
							- front - tail, minPenalty - penalty - 1);

					if (indentNext) {
						space -= indentsize;
						lastSpace -= indentsize;
						indentNext = false;
					}
					currInd = 0;
				} else
					currInd = childEnd - childStart;

				if (penalty >= minPenalty)
					return minPenalty;
			}
			int childStart = lastChild.breakPos;
			if (currInd > 0) {
				if (currInd + endPos - childStart <= lastSpace)
					return penalty;

				penalty++;
				if (indentNext) {
					space -= indentsize;
					lastSpace -= indentsize;
					indentNext = false;
				}
			}
			if (childStart < endPos && currentLine.charAt(childStart) == ' ')
				childStart++;
			if (endPos - childStart > lastSpace) {
				int front = lastChild.startPos - childStart;
				int tail = endPos - lastChild.endPos;
				penalty += lastChild.getMinPenalty(space - front, lastSpace
						- front - tail, minPenalty - penalty);
			}
			if (penalty < minPenalty)
				return penalty;
			return minPenalty;
		}
	}

	public TabbedPrintWriter(OutputStream os, ImportHandler imports,
			boolean autoFlush) {
		pw = new PrintWriter(os, autoFlush);
		this.imports = imports;
		init();
	}

	public TabbedPrintWriter(Writer os, ImportHandler imports, boolean autoFlush) {
		pw = new PrintWriter(os, autoFlush);
		this.imports = imports;
		init();
	}

	public TabbedPrintWriter(OutputStream os, ImportHandler imports) {
		this(os, imports, true);
	}

	public TabbedPrintWriter(Writer os, ImportHandler imports) {
		this(os, imports, true);
	}

	public TabbedPrintWriter(OutputStream os) {
		this(os, null);
	}

	public TabbedPrintWriter(Writer os) {
		this(os, null);
	}

	public void init() {
		this.indentsize = (Options.outputStyle & Options.TAB_SIZE_MASK);
		this.tabWidth = 8;
		this.lineWidth = 79;
		currentLine = new StringBuffer();
		currentBP = new BreakPoint(null, 0);
		currentBP.startOp(DONT_BREAK, 1, 0);
	}

	public void tab() {
		currentIndent += indentsize;
		indentStr = makeIndentStr(currentIndent);
	}

	public void untab() {
		currentIndent -= indentsize;
		indentStr = makeIndentStr(currentIndent);
	}

	public void startOp(int options, int penalty) {
		currentBP = (BreakPoint) currentBP.childBPs.lastElement();
		currentBP.startOp(options, penalty, currentLine.length());
	}

	public void breakOp() {
		int pos = currentLine.length();
		if (pos > currentBP.startPos && currentLine.charAt(pos - 1) == ' ')
			pos--;
		currentBP.breakOp(pos);
	}

	public void endOp() {
		currentBP.endOp(currentLine.length());
		currentBP = currentBP.parentBP;
		if (currentBP == null)
			throw new NullPointerException();
	}

	public Object saveOps() {
		Stack state = new Stack();
		int pos = currentLine.length();
		while (currentBP.parentBP != null) {
			state.push(new Integer(currentBP.breakPenalty));
			/* We don't want parentheses or unconventional line breaking */
			currentBP.options = DONT_BREAK;
			currentBP.endPos = pos;
			currentBP = currentBP.parentBP;
		}
		return state;
	}

	public void restoreOps(Object s) {
		Stack state = (Stack) s;
		while (!state.isEmpty()) {
			int penalty = ((Integer) state.pop()).intValue();
			startOp(DONT_BREAK, penalty);
		}
	}

	public void println(String str) {
		print(str);
		println();
	}

	public void println() {
		currentBP.endPos = currentLine.length();

		// pw.print(indentStr);
		// currentBP.dump(currentLine.toString());
		// pw.println();

		int lw = lineWidth - currentIndent;
		int minPenalty = currentBP.getMinPenalty(lw, lw, Integer.MAX_VALUE / 2);
		currentBP = currentBP.commitMinPenalty(lw, lw, minPenalty);

		// pw.print(indentStr);
		// currentBP.dump(currentLine.toString());
		// pw.println();
		pw.print(indentStr);
		currentBP.printLines(currentIndent, currentLine.toString());
		pw.println();

		currentLine.setLength(0);
		currentBP = new BreakPoint(null, 0);
		currentBP.startOp(DONT_BREAK, 1, 0);
	}

	public void print(String str) {
		currentLine.append(str);
	}

	public void printType(Type type) {
		print(getTypeString(type));
	}

	public void pushScope(Scope scope) {
		scopes.push(scope);
	}

	public void popScope() {
		scopes.pop();
	}

	/**
	 * Checks if the name in inScope conflicts with an identifier in a higher
	 * scope.
	 */
	public boolean conflicts(String name, Scope inScope, int context) {
		int dot = name.indexOf('.');
		if (dot >= 0)
			name = name.substring(0, dot);
		int count = scopes.size();
		for (int ptr = count; ptr-- > 0;) {
			Scope scope = (Scope) scopes.elementAt(ptr);
			if (scope == inScope)
				return false;
			if (scope.conflicts(name, context)) {
				return true;
			}
		}
		return false;
	}

	public Scope getScope(Object obj, int scopeType) {
		int count = scopes.size();
		for (int ptr = count; ptr-- > 0;) {
			Scope scope = (Scope) scopes.elementAt(ptr);
			if (scope.isScopeOf(obj, scopeType))
				return scope;
		}
		return null;
	}

	public String getInnerClassString(ClassInfo info, int scopeType) {
		InnerClassInfo[] outers = info.getOuterClasses();
		if (outers == null)
			return null;
		for (int i = 0; i < outers.length; i++) {
			if (outers[i].name == null || outers[i].outer == null)
				return null;
			Scope scope = getScope(ClassInfo.forName(outers[i].outer),
					Scope.CLASSSCOPE);
			if (scope != null && !conflicts(outers[i].name, scope, scopeType)) {
				StringBuffer sb = new StringBuffer(outers[i].name);
				for (int j = i; j-- > 0;) {
					sb.append('.').append(outers[j].name);
				}
				return sb.toString();
			}
		}
		String name = getClassString(
				ClassInfo.forName(outers[outers.length - 1].outer), scopeType);
		StringBuffer sb = new StringBuffer(name);
		for (int j = outers.length; j-- > 0;)
			sb.append('.').append(outers[j].name);
		return sb.toString();
	}

	public String getAnonymousClassString(ClassInfo info, int scopeType) {
		InnerClassInfo[] outers = info.getOuterClasses();
		if (outers == null)
			return null;
		for (int i = 0; i < outers.length; i++) {
			if (outers[i].name == null)
				return "ANONYMOUS CLASS " + info.getName();
			Scope scope = getScope(info, Scope.METHODSCOPE);
			if (scope != null && !conflicts(outers[i].name, scope, scopeType)) {
				StringBuffer sb = new StringBuffer(outers[i].name);
				for (int j = i; j-- > 0;) {
					sb.append('.').append(outers[j].name);
				}
				return sb.toString();
			} else if (outers[i].outer == null) {
				StringBuffer sb;
				if (scope != null)
					sb = new StringBuffer("NAME CONFLICT ");
				else
					sb = new StringBuffer("UNREACHABLE ");

				sb.append(outers[i].name);
				for (int j = i; j-- > 0;) {
					sb.append('.').append(outers[j].name);
				}
				return sb.toString();
			}
		}
		String name = getClassString(
				ClassInfo.forName(outers[outers.length - 1].outer), scopeType);
		StringBuffer sb = new StringBuffer(name);
		for (int j = outers.length; j-- > 0;)
			sb.append('.').append(outers[j].name);
		return sb.toString();
	}

	public String getClassString(ClassInfo clazz, int scopeType) {
		String name = clazz.getName();
		if (name.indexOf('$') >= 0) {
			if ((Options.options & Options.OPTION_INNER) != 0) {
				String innerClassName = getInnerClassString(clazz, scopeType);
				if (innerClassName != null)
					return innerClassName;
			}
			if ((Options.options & Options.OPTION_ANON) != 0) {
				String innerClassName = getAnonymousClassString(clazz,
						scopeType);
				if (innerClassName != null)
					return innerClassName;
			}
		}
		if (imports != null) {
			String importedName = imports.getClassString(clazz);
			if (!conflicts(importedName, null, scopeType))
				return importedName;
		}
		if (conflicts(name, null, Scope.AMBIGUOUSNAME))
			return "PKGNAMECONFLICT " + name;
		return name;
	}

	public String getTypeString(Type type) {
		if (type instanceof ArrayType)
			return getTypeString(((ArrayType) type).getElementType()) + "[]";
		else if (type instanceof ClassInterfacesType) {
			ClassInfo clazz = ((ClassInterfacesType) type).getClassInfo();
			return getClassString(clazz, Scope.CLASSNAME);
		} else if (type instanceof NullType)
			return "Object";
		else
			return type.toString();
	}

	/**
	 * Print a opening brace with the current indentation style. Called at the
	 * end of the line of the instance that opens the brace. It doesn't do a tab
	 * stop after opening the brace.
	 */
	public void openBrace() {
		if ((Options.outputStyle & Options.BRACE_AT_EOL) != 0) {
			print(currentLine.length() > 0 ? " {" : "{");
			println();
		} else {
			if (currentLine.length() > 0)
				println();
			if ((Options.outputStyle & Options.BRACE_FLUSH_LEFT) == 0
					&& currentIndent > 0)
				tab();
			println("{");
		}
	}

	public void openBraceClass() {
		if (currentLine.length() > 0) {
			if ((Options.outputStyle & Options.BRACE_AT_EOL) != 0)
				print(" ");
			else
				println();
		}
		println("{");
	}

	/**
	 * Print a opening brace with the current indentation style. Called at the
	 * end the line of a method declaration.
	 */
	public void openBraceNoIndent() {
		if ((Options.outputStyle & Options.BRACE_AT_EOL) != 0) {
			print(currentLine.length() > 0 ? " {" : "{");
			println();
		} else {
			if (currentLine.length() > 0)
				println();
			println("{");
		}
	}

	/**
	 * Print a opening brace with the current indentation style. Called at the
	 * end of the line of the instance that opens the brace. It doesn't do a tab
	 * stop after opening the brace.
	 */
	public void openBraceNoSpace() {
		if ((Options.outputStyle & Options.BRACE_AT_EOL) != 0)
			println("{");
		else {
			if (currentLine.length() > 0)
				println();
			if ((Options.outputStyle & Options.BRACE_FLUSH_LEFT) == 0
					&& currentIndent > 0)
				tab();
			println("{");
		}
	}

	public void closeBraceContinue() {
		if ((Options.outputStyle & Options.BRACE_AT_EOL) != 0)
			print("} ");
		else {
			println("}");
			if ((Options.outputStyle & Options.BRACE_FLUSH_LEFT) == 0
					&& currentIndent > 0)
				untab();
		}
	}

	public void closeBraceClass() {
		print("}");
	}

	public void closeBrace() {
		if ((Options.outputStyle & Options.BRACE_AT_EOL) != 0)
			println("}");
		else {
			println("}");
			if ((Options.outputStyle & Options.BRACE_FLUSH_LEFT) == 0
					&& currentIndent > 0)
				untab();
		}
	}

	public void closeBraceNoIndent() {
		println("}");
	}

	public void flush() {
		pw.flush();
	}

	public void close() {
		pw.close();
	}
}
