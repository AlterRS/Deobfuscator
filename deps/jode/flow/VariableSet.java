/* VariableSet Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: VariableSet.java.in,v 4.1.2.1 2002/05/28 17:34:09 hoenicke Exp $
 */

package jode.flow;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import jode.decompiler.LocalInfo;


/**
 * This class represents a set of Variables, which are mainly used in the in/out
 * sets of StructuredBlock. The type of the Variables is LocalInfo.
 * <p>
 * <p/>
 * It defines some Helper-Function, like intersecting, merging, union and
 * difference.
 * <p>
 * <p/>
 * Note that a variable set can contain LocalInfos that use the same slot, but
 * are different.
 */
public final class VariableSet extends AbstractSet implements Cloneable {
	LocalInfo[] locals;
	int count;

	/**
	 * Creates a new empty variable set
	 */
	public VariableSet() {
		locals = null;
		count = 0;
	}

	/**
	 * Creates a new pre initialized variable set
	 */
	public VariableSet(LocalInfo[] locals) {
		count = locals.length;
		this.locals = locals;
	}

	public final void grow(int size) {
		if (locals != null) {
			size += count;
			if (size > locals.length) {
				int nextSize = locals.length * 2;
				// GlobalOptions.err.println("wanted: "+size+" next: "+nextSize);
				LocalInfo[] newLocals = new LocalInfo[nextSize > size ? nextSize
						: size];
				System.arraycopy(locals, 0, newLocals, 0, count);
				locals = newLocals;
			}
		} else if (size > 0)
			locals = new LocalInfo[size];
	}

	/**
	 * Adds a local info to this variable set.
	 */
	public boolean add(Object li) {
		if (contains(li))
			return false;
		grow(1);
		locals[count++] = (LocalInfo) li;
		return true;
	}

	/**
	 * Checks if the variable set contains the given local info.
	 */
	public boolean contains(Object li) {
		li = ((LocalInfo) li).getLocalInfo();
		for (int i = 0; i < count; i++)
			if (locals[i].getLocalInfo() == li)
				return true;
		return false;
	}

	/**
	 * Checks if the variable set contains a local with the given name.
	 */
	public final boolean containsSlot(int slot) {
		return findSlot(slot) != null;
	}

	/**
	 * Checks if the variable set contains a local with the given name.
	 */
	public LocalInfo findLocal(String name) {
		for (int i = 0; i < count; i++)
			if (locals[i].getName().equals(name))
				return locals[i];
		return null;
	}

	/**
	 * Checks if the variable set contains a local with the given slot.
	 */
	public LocalInfo findSlot(int slot) {
		for (int i = 0; i < count; i++)
			if (locals[i].getSlot() == slot)
				return locals[i];
		return null;
	}

	/**
	 * Removes a local info from this variable set.
	 */
	public boolean remove(Object li) {
		li = ((LocalInfo) li).getLocalInfo();
		for (int i = 0; i < count; i++) {
			if (locals[i].getLocalInfo() == li) {
				locals[i] = locals[--count];
				locals[count] = null;
				return true;
			}
		}
		return false;
	}

	public int size() {
		return count;
	}

	public Iterator iterator() {
		return new Iterator() {
			int pos = 0;

			public boolean hasNext() {
				return pos < count;
			}

			public Object next() {
				return locals[pos++];
			}

			public void remove() {
				if (pos < count)
					System.arraycopy(locals, pos, locals, pos - 1, count - pos);
				count--;
				pos--;
				locals[count] = null;
			}
		};
	}

	/**
	 * Removes everything from this variable set.
	 */
	public void clear() {
		locals = null;
		count = 0;
	}

	public Object clone() {
		try {
			VariableSet other = (VariableSet) super.clone();
			if (count > 0) {
				other.locals = new LocalInfo[count];
				System.arraycopy(locals, 0, other.locals, 0, count);
			}
			return other;
		} catch (CloneNotSupportedException ex) {
			throw new jode.AssertError("Clone?");
		}
	}

	/**
	 * Intersects the current VariableSet with another and returns the
	 * intersection. The existing VariableSet are not changed.
	 * 
	 * @param vs
	 *            the other variable set.
	 */
	public VariableSet intersect(VariableSet vs) {
		VariableSet intersection = new VariableSet();
		intersection.grow(Math.min(count, vs.count));
		big_loop: for (int i = 0; i < count; i++) {
			LocalInfo li = locals[i];
			int slot = li.getSlot();
			if (vs.containsSlot(slot) && !intersection.containsSlot(slot))
				intersection.locals[intersection.count++] = li.getLocalInfo();
		}
		return intersection;
	}

	/**
	 * Add the variables in gen to the current set, unless there are variables
	 * in kill using the same slot.
	 * 
	 * @param gen
	 *            The gen set.
	 * @param kill
	 *            The kill set.
	 */
	public void mergeGenKill(Collection gen, SlotSet kill) {
		grow(gen.size());
		big_loop: for (Iterator i = gen.iterator(); i.hasNext();) {
			LocalInfo li2 = (LocalInfo) i.next();
			if (!kill.containsSlot(li2.getSlot()))
				add(li2.getLocalInfo());
		}
	}
}
