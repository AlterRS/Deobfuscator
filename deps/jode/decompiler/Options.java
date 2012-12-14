/* Options Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: Options.java,v 4.2.2.2 2002/05/28 17:34:03 hoenicke Exp $
 */

package jode.decompiler;

import jode.bytecode.ClassInfo;
import jode.bytecode.InnerClassInfo;

public class Options {
	public static final int TAB_SIZE_MASK = 0x0f;
	public static final int BRACE_AT_EOL = 0x10;
	public static final int BRACE_FLUSH_LEFT = 0x20;
	public static final int GNU_SPACING = 0x40;
	public static final int SUN_STYLE = 0x14;
	public static final int GNU_STYLE = 0x42;
	public static final int PASCAL_STYLE = 0x24;

	public static final int OPTION_LVT = 0x0001;
	public static final int OPTION_INNER = 0x0002;
	public static final int OPTION_ANON = 0x0004;
	public static final int OPTION_PUSH = 0x0008;
	public static final int OPTION_PRETTY = 0x0010;
	public static final int OPTION_DECRYPT = 0x0020;
	public static final int OPTION_ONETIME = 0x0040;
	public static final int OPTION_IMMEDIATE = 0x0080;
	public static final int OPTION_VERIFY = 0x0100;
	public static final int OPTION_CONTRAFO = 0x0200;

	public static int options = OPTION_LVT | OPTION_INNER | OPTION_ANON
			| OPTION_PRETTY | OPTION_DECRYPT
			| /* OPTION_VERIFY | */OPTION_CONTRAFO;

	public static int outputStyle = SUN_STYLE;

	public final static boolean doAnonymous() {
		return (options & OPTION_ANON) != 0;
	}

	public final static boolean doInner() {
		return (options & OPTION_INNER) != 0;
	}

	public static boolean skipClass(ClassInfo clazz) {
		InnerClassInfo[] outers = clazz.getOuterClasses();
		if (outers != null) {
			if (outers[0].outer == null) {
				return doAnonymous();
			} else {
				return doInner();
			}
		}
		return false;
	}
}
