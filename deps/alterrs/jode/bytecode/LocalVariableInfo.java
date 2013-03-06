/* LocalVariableInfo Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: LocalVariableInfo.java,v 1.1.4.1 2002/05/28 17:34:00 hoenicke Exp $
 */

package alterrs.jode.bytecode;

/**
 * A simple class containing the info of the LocalVariableTable
 */
public class LocalVariableInfo {
	public Instruction start, end;
	public String name, type;
	public int slot;
}
