/* LocalVariableTable Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: LocalVariableTable.java,v 4.14.2.1 2002/05/28 17:34:03 hoenicke Exp $
 */

package alterrs.jode.decompiler;

import alterrs.jode.bytecode.LocalVariableInfo;
import alterrs.jode.type.Type;

public class LocalVariableTable {
	LocalVariableRangeList[] locals;

	public LocalVariableTable(int maxLocals, LocalVariableInfo[] lvt) {
		locals = new LocalVariableRangeList[maxLocals];
		for (int i = 0; i < maxLocals; i++)
			locals[i] = new LocalVariableRangeList();

		for (int i = 0; i < lvt.length; i++)
			locals[lvt[i].slot].addLocal(lvt[i].start.getAddr(),
					lvt[i].end.getAddr(), lvt[i].name, Type.tType(lvt[i].type));
	}

	public LocalVarEntry getLocal(int slot, int addr)
			throws ArrayIndexOutOfBoundsException {
		return locals[slot].getInfo(addr);
	}
}
