/* SlotSet Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: SlotSet.java.in,v 4.1.2.1 2002/05/28 17:34:09 hoenicke Exp $
 */

package alterrs.jode.flow;

import java.util.AbstractSet;
import java.util.Iterator;

import alterrs.jode.decompiler.LocalInfo;

/**
 * This class represents a set of local info, all having different slots. It is
 * used for representing the in sets of flow block.
 * <p>
 * <p/>
 * Its add method will automatically merge any localinfo that have the same slot
 * and is in the method.
 * <p>
 */
public final class SlotSet extends AbstractSet implements Cloneable {
	LocalInfo[] locals;
	int count;

	/**
	 * Creates a new empty variable set
	 */
	public SlotSet() {
		locals = null;
		count = 0;
	}

	/**
	 * Creates a new pre initialized variable set
	 */
	public SlotSet(LocalInfo[] locals) {
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
	public boolean add(Object o) {
		LocalInfo li = (LocalInfo) o;
		LocalInfo contained = findSlot(li.getSlot());
		if (contained != null) {
			li.combineWith(contained);
			return false;
		} else {
			grow(1);
			locals[count++] = li;
			return true;
		}
	}

	public final boolean contains(Object o) {
		return containsSlot(((LocalInfo) o).getSlot());
	}

	/**
	 * Checks if the variable set contains a local with the given name.
	 */
	public final boolean containsSlot(int slot) {
		return findSlot(slot) != null;
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
	 * Removes a slot from this variable set.
	 */
	public boolean remove(Object li) {
		int slot = ((LocalInfo) li).getSlot();
		for (int i = 0; i < count; i++) {
			if (locals[i].getSlot() == slot) {
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
			SlotSet other = (SlotSet) super.clone();
			if (count > 0) {
				other.locals = new LocalInfo[count];
				System.arraycopy(locals, 0, other.locals, 0, count);
			}
			return other;
		} catch (CloneNotSupportedException ex) {
			throw new alterrs.jode.AssertError("Clone?");
		}
	}

	/**
	 * Merges this SlotSet with another. For all slots occuring in both variable
	 * sets, all corresponding LocalInfos are merged. The sets are not changed
	 * (use addAll for this).
	 * 
	 * @param vs
	 *            the other variable set.
	 * @return The merged variables.
	 */
	public void merge(VariableSet vs) {
		for (int i = 0; i < count; i++) {
			LocalInfo li = locals[i];
			int slot = li.getSlot();
			for (int j = 0; j < vs.count; j++) {
				if (li.getSlot() == vs.locals[j].getSlot())
					li.combineWith(vs.locals[j]);
			}
		}
	}

	/**
	 * Add the slots in kill to the current set, unless there are already in
	 * this set. This differs from addAll, in the fact that it doesn't combine
	 * the locals.
	 * 
	 * @param kill
	 *            The other kill set.
	 */
	public void mergeKill(SlotSet kill) {
		grow(kill.size());
		big_loop: for (Iterator i = kill.iterator(); i.hasNext();) {
			LocalInfo li2 = (LocalInfo) i.next();
			if (!containsSlot(li2.getSlot()))
				add(li2.getLocalInfo());
		}
	}
}
