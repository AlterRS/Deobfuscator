/* ReferenceType Copyright (C) 1999-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: ReferenceType.java,v 1.5.4.1 2002/05/28 17:34:22 hoenicke Exp $
 */

package jode.type;

import jode.GlobalOptions;
import jode.bytecode.ClassInfo;

/**
 * This is an abstrace super class of all reference types. Reference types are
 * ClassInterfacesType, ArrayType and NullType.
 * <p>
 * <p/>
 * To do intersection on range types, the reference types need three more
 * operations: specialization, generalization and createRange.
 * <p>
 * <p/>
 * specialization chooses all common sub type of two types. It is used to find
 * the bottom of the intersected interval.
 * <p>
 * <p/>
 * generalization chooses the common super type of two types. It is used to find
 * the top of the intersected interval.
 * <p>
 * <p/>
 * When the new interval is created with <code>createRangeType</code> the bottom
 * and top are adjusted so that they only consists of possible types. It then
 * decides, if it needs a range type, or if the reference types already
 * represents all types.
 * 
 * @author Jochen Hoenicke
 */
public abstract class ReferenceType extends Type {
	public ReferenceType(int typecode) {
		super(typecode);
	}

	/**
	 * Returns the specialized type set of this and type. The result should be a
	 * type set, so that every type, extends all types in type and this, iff it
	 * extends all types in the resulting type set.
	 * 
	 * @param type
	 *            the other type.
	 * @return the specialized type.
	 */
	public abstract Type getSpecializedType(Type type);

	/**
	 * Returns the generalized type set of this and type. The result should be a
	 * type set, so that every type, is extended/implemented by one type in this
	 * and one type in <code>type</code>, iff it is extended/implemented by one
	 * type in the resulting type set.
	 * 
	 * @param type
	 *            the other type.
	 * @return the generalized type
	 */
	public abstract Type getGeneralizedType(Type type);

	/**
	 * Creates a range type set of this and bottom. The resulting type set
	 * contains all types, that extend all types in bottom and are extended by
	 * at least one type in this. <br>
	 * Note that a RangeType will do this, but we normalize the bottom and top
	 * set.
	 * 
	 * @param bottom
	 *            the bottom type.
	 * @return the range type set.
	 */
	public abstract Type createRangeType(ReferenceType bottom);

	/**
	 * Tells if all otherIfaces, are implemented by at least one ifaces or by
	 * clazz.
	 * <p/>
	 * This is a useful function for generalizing/specializing interface types
	 * or arrays.
	 * 
	 * @param clazz
	 *            The clazz, can be null.
	 * @param ifaces
	 *            The ifaces.
	 * @param otherifaces
	 *            The other ifaces, that must be implemented.
	 * @return true, if all otherIfaces are implemented.
	 */
	protected static boolean implementsAllIfaces(ClassInfo clazz,
			ClassInfo[] ifaces, ClassInfo[] otherIfaces) {
		big: for (int i = 0; i < otherIfaces.length; i++) {
			ClassInfo iface = otherIfaces[i];
			if (clazz != null && iface.implementedBy(clazz))
				continue big;
			for (int j = 0; j < ifaces.length; j++) {
				if (iface.implementedBy(ifaces[j]))
					continue big;
			}
			return false;
		}
		return true;
	}

	public Type getSuperType() {
		return (this == tObject) ? tObject : tRange(tObject, this);
	}

	public abstract Type getSubType();

	/**
	 * Intersect this type with another type and return the new type.
	 * 
	 * @param type
	 *            the other type.
	 * @return the intersection, or tError, if a type conflict happens.
	 */
	public Type intersection(Type type) {
		if (type == tError)
			return type;
		if (type == Type.tUnknown)
			return this;

		Type newBottom = getSpecializedType(type);
		Type newTop = getGeneralizedType(type);
		Type result;
		if (newTop.equals(newBottom))
			result = newTop;
		else if (newTop instanceof ReferenceType
				&& newBottom instanceof ReferenceType)
			result = ((ReferenceType) newTop)
					.createRangeType((ReferenceType) newBottom);
		else
			result = tError;

		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0) {
			GlobalOptions.err.println("intersecting " + this + " and " + type
					+ " to " + result);
		}
		return result;
	}
}
