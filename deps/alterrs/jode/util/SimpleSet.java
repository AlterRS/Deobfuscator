/* SimpleSet Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: SimpleSet.java.in,v 1.1.2.1 2002/05/28 17:34:24 hoenicke Exp $
 */

package alterrs.jode.util;

import java.util.AbstractSet;
import java.util.Iterator;

public class SimpleSet extends AbstractSet implements Cloneable {
	Object[] elementObjects;
	int count = 0;

	public SimpleSet() {
		this(2);
	}

	public SimpleSet(int initialSize) {
		elementObjects = new Object[initialSize];
	}

	public int size() {
		return count;
	}

	public boolean add(Object element) {
		if (element == null)
			throw new NullPointerException();

		for (int i = 0; i < count; i++) {
			if (element.equals(elementObjects[i]))
				return false;
		}

		if (count == elementObjects.length) {
			Object[] newArray = new Object[(count + 1) * 3 / 2];
			System.arraycopy(elementObjects, 0, newArray, 0, count);
			elementObjects = newArray;
		}
		elementObjects[count++] = element;
		return true;
	}

	public Object clone() {
		try {
			SimpleSet other = (SimpleSet) super.clone();
			other.elementObjects = (Object[]) elementObjects.clone();
			return other;
		} catch (CloneNotSupportedException ex) {
			throw new alterrs.jode.AssertError("Clone?");
		}
	}

	public Iterator iterator() {
		return new Iterator() {
			int pos = 0;

			public boolean hasNext() {
				return pos < count;
			}

			public Object next() {
				return elementObjects[pos++];
			}

			public void remove() {
				if (pos < count)
					System.arraycopy(elementObjects, pos, elementObjects,
							pos - 1, count - pos);
				count--;
				pos--;
			}
		};
	}
}
