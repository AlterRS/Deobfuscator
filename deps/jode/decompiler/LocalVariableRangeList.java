/* LocalVariableRangeList Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: LocalVariableRangeList.java,v 4.10.4.1 2002/05/28 17:34:03 hoenicke Exp $
 */

package jode.decompiler;

import jode.GlobalOptions;
import jode.type.Type;

public class LocalVariableRangeList {

	LocalVarEntry list = null;

	LocalVariableRangeList() {
	}

	private void add(LocalVarEntry li) {
		LocalVarEntry prev = null;
		LocalVarEntry next = list;
		while (next != null && next.endAddr < li.startAddr) {
			prev = next;
			next = next.next;
		}
		/*
		 * prev.endAddr < li.startAddr <= next.endAddr
		 */
		if (next != null && li.endAddr >= next.startAddr) {
			if (next.type.equals(li.type) && next.name.equals(li.name)) {
				/*
				 * Same type, same name and overlapping range. This is the same
				 * local: extend next to the common range and don't add li.
				 */
				next.startAddr = Math.min(next.startAddr, li.startAddr);
				next.endAddr = Math.max(next.endAddr, li.endAddr);
				return;
			}
			GlobalOptions.err.println("warning: non disjoint locals");
		}
		li.next = next;
		if (prev == null)
			list = li;
		else
			prev.next = li;
	}

	private LocalVarEntry find(int addr) {
		LocalVarEntry li = list;
		while (li != null && li.endAddr < addr)
			li = li.next;
		if (li == null || li.startAddr > addr) {
			return null;
		}
		return li;
	}

	public void addLocal(int startAddr, int endAddr, String name, Type type) {
		LocalVarEntry li = new LocalVarEntry(startAddr, endAddr, name, type);
		add(li);
	}

	public LocalVarEntry getInfo(int addr) {
		return find(addr);
	}
}
