/* ArrayType Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ArrayType.java,v 4.19.2.3 2002/05/28 17:34:21 hoenicke Exp $
 */

package alterrs.jode.type;

import java.util.Vector;

import alterrs.jode.bytecode.ClassInfo;

/**
 * This type represents an array type.
 * 
 * @author Jochen Hoenicke
 */
public class ArrayType extends ReferenceType {
	// The interfaces that an array implements:
	final static ClassInfo[] arrayIfaces = {
			// Make sure to list all interfaces, even if some interface
			// implements another (or change code in getGeneralizedType().
			ClassInfo.forName("java.lang.Cloneable"),
			ClassInfo.forName("java.io.Serializable") };

	Type elementType;

	ArrayType(Type elementType) {
		super(TC_ARRAY);
		this.elementType = elementType;
	}

	public Type getElementType() {
		return elementType;
	}

	public Type getSuperType() {
		if (elementType instanceof IntegerType)
			return tRange(tObject, this);
		else
			return tRange(tObject,
					(ReferenceType) tArray(elementType.getSuperType()));
	}

	public Type getSubType() {
		if (elementType instanceof IntegerType)
			return this;
		else
			return tArray(elementType.getSubType());
	}

	public Type getHint() {
		return tArray(elementType.getHint());
	}

	public Type getCanonic() {
		return tArray(elementType.getCanonic());
	}

	/**
	 * Create the type corresponding to the range from bottomType to this.
	 * 
	 * @param bottomType
	 *            the start point of the range
	 * @return the range type, or tError if not possible.
	 */
	public Type createRangeType(ReferenceType bottom) {
		/*
		 * tArray(y), tArray(x) -> tArray( y.intersection(x) ) obj , tArray(x)
		 * -> <obj, tArray(x)> iff tArray extends and implements obj
		 */
		if (bottom.getTypeCode() == TC_ARRAY)
			return tArray(elementType
					.intersection(((ArrayType) bottom).elementType));

		if (bottom.getTypeCode() == TC_CLASS) {
			ClassInterfacesType bottomCIT = (ClassInterfacesType) bottom;
			if (bottomCIT.clazz == null
					&& implementsAllIfaces(null, arrayIfaces, bottomCIT.ifaces))
				return tRange(bottomCIT, this);
		}
		return tError;
	}

	/**
	 * Returns the common sub type of this and type.
	 * 
	 * @param type
	 *            the other type.
	 * @return the common sub type.
	 */
	public Type getSpecializedType(Type type) {
		/*
		 * tArray(x), iface -> tArray(x) iff tArray implements iface tArray(x),
		 * tArray(y) -> tArray(x.intersection(y)) tArray(x), other -> tError
		 */
		if (type.getTypeCode() == TC_RANGE) {
			type = ((RangeType) type).getBottom();
		}
		if (type == tNull)
			return this;
		if (type.getTypeCode() == TC_ARRAY) {
			Type elType = elementType
					.intersection(((ArrayType) type).elementType);
			return elType != tError ? tArray(elType) : tError;
		}
		if (type.getTypeCode() == TC_CLASS) {
			ClassInterfacesType other = (ClassInterfacesType) type;
			if (other.clazz == null
					&& implementsAllIfaces(null, arrayIfaces, other.ifaces))
				return this;
		}
		return tError;
	}

	/**
	 * Returns the common super type of this and type.
	 * 
	 * @param type
	 *            the other type.
	 * @return the common super type.
	 */
	public Type getGeneralizedType(Type type) {
		/*
		 * tArray(x), tNull -> tArray(x) tArray(x), tClass(y) -> common ifaces
		 * of tArray and tClass tArray(x), tArray(y) ->
		 * tArray(x.intersection(y)) or tObject tArray(x), other -> tError
		 */
		if (type.getTypeCode() == TC_RANGE) {
			type = ((RangeType) type).getTop();
		}
		if (type == tNull)
			return this;
		if (type.getTypeCode() == TC_ARRAY) {
			Type elType = elementType
					.intersection(((ArrayType) type).elementType);
			if (elType != tError)
				return tArray(elType);
			return ClassInterfacesType.create(null, arrayIfaces);
		}
		if (type.getTypeCode() == TC_CLASS) {
			ClassInterfacesType other = (ClassInterfacesType) type;
			if (implementsAllIfaces(other.clazz, other.ifaces, arrayIfaces))
				return ClassInterfacesType.create(null, arrayIfaces);
			if (other.clazz == null
					&& implementsAllIfaces(null, arrayIfaces, other.ifaces))
				return other;

			/*
			 * Now the more complicated part: find all interfaces, that are
			 * implemented by one interface or class in each group.
			 * 
			 * First get all interfaces of this.clazz and this.ifaces.
			 */
			Vector newIfaces = new Vector();
			iface_loop: for (int i = 0; i < arrayIfaces.length; i++) {
				/*
				 * Now consider each array interface. If any clazz or interface
				 * in other implements it, add it to the newIfaces vector.
				 */
				if (other.clazz != null
						&& arrayIfaces[i].implementedBy(other.clazz)) {
					newIfaces.addElement(arrayIfaces[i]);
					continue iface_loop;
				}
				for (int j = 0; j < other.ifaces.length; j++) {
					if (arrayIfaces[i].implementedBy(other.ifaces[j])) {
						newIfaces.addElement(arrayIfaces[i]);
						continue iface_loop;
					}
				}
			}
			ClassInfo[] ifaceArray = new ClassInfo[newIfaces.size()];
			newIfaces.copyInto(ifaceArray);
			return ClassInterfacesType.create(null, ifaceArray);
		}
		return tError;
	}

	/**
	 * Checks if we need to cast to a middle type, before we can cast from
	 * fromType to this type.
	 * 
	 * @return the middle type, or null if it is not necessary.
	 */
	public Type getCastHelper(Type fromType) {
		Type hintType = fromType.getHint();
		switch (hintType.getTypeCode()) {
		case TC_ARRAY:
			if (!elementType.isClassType()
					|| !((ArrayType) hintType).elementType.isClassType())
				return tObject;
			Type middleType = elementType
					.getCastHelper(((ArrayType) hintType).elementType);
			if (middleType != null)
				return tArray(middleType);
			return null;
		case TC_CLASS:
			ClassInterfacesType hint = (ClassInterfacesType) hintType;
			if (hint.clazz == null
					&& implementsAllIfaces(null, arrayIfaces, hint.ifaces))
				return null;
			return tObject;
		case TC_UNKNOWN:
			return null;
		}
		return tObject;
	}

	/**
	 * Checks if this type represents a valid type instead of a list of minimum
	 * types.
	 */
	public boolean isValidType() {
		return elementType.isValidType();
	}

	public boolean isClassType() {
		return true;
	}

	public String getTypeSignature() {
		return "[" + elementType.getTypeSignature();
	}

	public Class getTypeClass() throws ClassNotFoundException {
		return Class.forName("[" + elementType.getTypeSignature());
	}

	public String toString() {
		return elementType.toString() + "[]";
	}

	private static String pluralize(String singular) {
		return singular
				+ ((singular.endsWith("s") || singular.endsWith("x")
						|| singular.endsWith("sh") || singular.endsWith("ch")) ? "es"
						: "s");
	}

	public String getDefaultName() {
		if (elementType instanceof ArrayType)
			return elementType.getDefaultName();
		return pluralize(elementType.getDefaultName());
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof ArrayType) {
			ArrayType type = (ArrayType) o;
			return type.elementType.equals(elementType);
		}
		return false;
	}
}
