/* InnerClassInfo Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: InnerClassInfo.java,v 1.2.2.1 2002/05/28 17:34:00 hoenicke Exp $
 */

package alterrs.jode.bytecode;

/**
 * A simple class containing the info about an inner class.
 */
public class InnerClassInfo {
	public String inner, outer;
	public String name;
	public int modifiers;

	public InnerClassInfo(String inner, String outer, String name, int modif) {
		this.inner = inner;
		this.outer = outer;
		this.name = name;
		this.modifiers = modif;
	}

	public String toString() {
		return "InnerClassInfo[" + inner + "," + outer + "," + name + ","
				+ java.lang.reflect.Modifier.toString(modifiers) + "]";
	}
}
