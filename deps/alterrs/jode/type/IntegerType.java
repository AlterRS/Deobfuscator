/* IntegerType Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: IntegerType.java,v 1.7.4.1 2002/05/28 17:34:22 hoenicke Exp $
 */

package alterrs.jode.type;

import alterrs.jode.GlobalOptions;

/**
 * This is a type class for 16 bit integral types. There are seven different
 * types, namely <code>int, char, short, byte, boolean,
 * const short, const byte</code> abbreviated <code>I, C, S, B, Z, cS,
 * cB</code>. <code>cB</code> and <code>cS</code> specify constant ints whose
 * value is in byte resp. short range. They may be converted to B resp. S, but
 * sometimes need an explicit cast.
 * 
 * @author Jochen Hoenicke
 */
public class IntegerType extends Type {

	/*
	 * Order does matter: First type that is possible (and hinted) will be
	 * taken.
	 */
	public static final int IT_Z = 0x01;
	public static final int IT_I = 0x02;
	public static final int IT_C = 0x04;
	public static final int IT_S = 0x08;
	public static final int IT_B = 0x10;
	public static final int IT_cS = 0x20;
	public static final int IT_cB = 0x40;
	private static final int NUM_TYPES = 7;

	private static final int[] subTypes = {
	/* Z */IT_Z,
	/* I */IT_I | IT_C | IT_S | IT_B/* |IT_cS|IT_cB */, /* C */IT_C,
	/* S */IT_S | IT_B/* |IT_cS|IT_cB */, /* B */IT_B/* |IT_cB */,
	/* cS */IT_cS | IT_cB, /* cB */IT_cB };
	private static final int[] superTypes = {
	/* Z */IT_Z,
	/* I */IT_I, /* C */IT_I | IT_C,
	/* S */IT_I | IT_S, /* B */IT_I | IT_S | IT_B,
	/* cS */IT_I | IT_C | IT_S | IT_cS, /* cB */
			IT_I | IT_C | IT_S | IT_B | IT_cS | IT_cB };
	private static final Type[] simpleTypes = { new IntegerType(IT_Z),
			new IntegerType(IT_I), new IntegerType(IT_C),
			new IntegerType(IT_S), new IntegerType(IT_B),
			new IntegerType(IT_cS), new IntegerType(IT_cB) };
	private static final String[] typeNames = { "Z", "I", "C", "S", "B", "s",
			"b" };

	int possTypes;
	int hintTypes;

	/**
	 * Create a new type with the given type.
	 */
	public IntegerType(int types) {
		this(types, types);
	}

	public IntegerType(int types, int hints) {
		super(TC_INTEGER);
		possTypes = types;
		hintTypes = hints;
	}

	public Type getHint() {
		int hint = possTypes & hintTypes;
		if (hint == 0)
			hint = possTypes;
		int i = 0;
		while ((hint & 1) == 0) {
			hint >>= 1;
			i++;
		}
		return simpleTypes[i];
	}

	public Type getCanonic() {
		int types = possTypes;
		int i = 0;
		while ((types >>= 1) != 0) {
			i++;
		}
		return simpleTypes[i];
	}

	private static int getSubTypes(int types) {
		int result = 0;
		for (int i = 0; i < NUM_TYPES; i++) {
			if (((1 << i) & types) != 0)
				result |= subTypes[i];
		}
		return result;
	}

	private static int getSuperTypes(int types) {
		int result = 0;
		for (int i = 0; i < NUM_TYPES; i++) {
			if (((1 << i) & types) != 0)
				result |= superTypes[i];
		}
		return result;
	}

	public Type getSubType() {
		return new IntegerType(getSubTypes(possTypes), getSubTypes(hintTypes));
	}

	public Type getSuperType() {
		/* Don't upgrade hints */
		return new IntegerType(getSuperTypes(possTypes), hintTypes);
	}

	/**
	 * Checks if this type represents a valid type instead of a list of minimum
	 * types.
	 */
	public boolean isValidType() {
		return true;
	}

	/**
	 * Check if this and &lt;unknown -- type&rt; are not disjunct.
	 * 
	 * @param type
	 *            a simple type; this mustn't be a range type.
	 * @return true if this is the case.
	 */
	public boolean isOfType(Type type) {
		return (type.typecode == TC_INTEGER && (((IntegerType) type).possTypes & possTypes) != 0);
	}

	public String getDefaultName() {
		switch (((IntegerType) getHint()).possTypes) {
		case IT_Z:
			return "bool";
		case IT_C:
			return "c";
		case IT_B:
		case IT_S:
		case IT_I:
			return "i";
		default:
			throw new alterrs.jode.AssertError(
					"Local can't be of constant type!");
		}
	}

	public Object getDefaultValue() {
		return new Integer(0);
	}

	public String getTypeSignature() {
		switch (((IntegerType) getHint()).possTypes) {
		case IT_Z:
			return "Z";
		case IT_C:
			return "C";
		case IT_B:
			return "B";
		case IT_S:
			return "S";
		case IT_I:
		default:
			return "I";
		}
	}

	public Class getTypeClass() {
		switch (((IntegerType) getHint()).possTypes) {
		case IT_Z:
			return Boolean.TYPE;
		case IT_C:
			return Character.TYPE;
		case IT_B:
			return Byte.TYPE;
		case IT_S:
			return Short.TYPE;
		case IT_I:
		default:
			return Integer.TYPE;
		}
	}

	public String toString() {
		if (possTypes == hintTypes) {
			switch (possTypes) {
			case IT_Z:
				return "boolean";
			case IT_C:
				return "char";
			case IT_B:
				return "byte";
			case IT_S:
				return "short";
			case IT_I:
				return "int";
			}
		}
		StringBuffer sb = new StringBuffer("{");
		for (int i = 0; i < NUM_TYPES; i++) {
			if (((1 << i) & possTypes) != 0)
				sb.append(typeNames[i]);
		}
		if (possTypes != hintTypes) {
			sb.append(":");
			for (int i = 0; i < NUM_TYPES; i++) {
				if (((1 << i) & hintTypes) != 0)
					sb.append(typeNames[i]);
			}
		}
		sb.append("}");
		return sb.toString();
	}

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
		if (type == tUnknown)
			return this;

		int mergeTypes;
		int mergeHints = 0;
		if (type.typecode != TC_INTEGER)
			mergeTypes = 0;
		else {
			IntegerType other = (IntegerType) type;
			mergeTypes = possTypes & other.possTypes;
			mergeHints = hintTypes & other.hintTypes;

			if (mergeTypes == possTypes && mergeHints == hintTypes)
				return this;
			if (mergeTypes == other.possTypes && mergeHints == other.hintTypes)
				return other;
		}
		Type result = mergeTypes == 0 ? tError : new IntegerType(mergeTypes,
				mergeHints);
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0) {
			GlobalOptions.err.println("intersecting " + this + " and " + type
					+ " to " + result);
		}
		return result;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof IntegerType) {
			IntegerType other = (IntegerType) o;
			return other.possTypes == possTypes && other.hintTypes == hintTypes;
		}
		return false;
	}
}
