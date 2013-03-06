/* ArrayEnum Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ArrayEnum.java,v 1.3.4.1 2002/05/28 17:34:24 hoenicke Exp $
 */

package alterrs.jode.util;

public class ArrayEnum implements java.util.Enumeration {
	int index = 0;
	int size;
	Object[] array;

	public ArrayEnum(int size, Object[] array) {
		this.size = size;
		this.array = array;
	}

	public boolean hasMoreElements() {
		return index < size;
	}

	public Object nextElement() {
		return array[index++];
	}
}
