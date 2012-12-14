/* Type Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: Type.java.in,v 4.1.2.1 2002/05/28 17:34:22 hoenicke Exp $
 */

package jode.type;

import java.util.Iterator;

import jode.AssertError;
import jode.GlobalOptions;
import jode.bytecode.ClassInfo;
import jode.util.UnifyHash;


/**
 * This is my type class. It differs from java.lang.class, in that it represents
 * a set of types. Since most times this set is infinite, it needs a special
 * representation. <br>
 * <p/>
 * The main operation on a type sets are tSuperType, tSubType and intersection.
 * 
 * @author Jochen Hoenicke
 */
public class Type {
	public static final int TC_BOOLEAN = 0;
	public static final int TC_BYTE = 1;
	public static final int TC_CHAR = 2;
	public static final int TC_SHORT = 3;
	public static final int TC_INT = 4;
	public static final int TC_LONG = 5;
	public static final int TC_FLOAT = 6;
	public static final int TC_DOUBLE = 7;
	public static final int TC_NULL = 8;
	public static final int TC_ARRAY = 9;
	public static final int TC_CLASS = 10;
	public static final int TC_VOID = 11;
	public static final int TC_METHOD = 12;
	public static final int TC_ERROR = 13;
	public static final int TC_UNKNOWN = 101;
	public static final int TC_RANGE = 103;
	public static final int TC_INTEGER = 107;

	private static final UnifyHash classHash = new UnifyHash();
	private static final UnifyHash arrayHash = new UnifyHash();
	private static final UnifyHash methodHash = new UnifyHash();

	/**
	 * This type represents the singleton set containing the boolean type.
	 */
	public static final Type tBoolean = new IntegerType(IntegerType.IT_Z);
	/**
	 * This type represents the singleton set containing the byte type.
	 */
	public static final Type tByte = new IntegerType(IntegerType.IT_B);
	/**
	 * This type represents the singleton set containing the char type.
	 */
	public static final Type tChar = new IntegerType(IntegerType.IT_C);
	/**
	 * This type represents the singleton set containing the short type.
	 */
	public static final Type tShort = new IntegerType(IntegerType.IT_S);
	/**
	 * This type represents the singleton set containing the int type.
	 */
	public static final Type tInt = new IntegerType(IntegerType.IT_I);
	/**
	 * This type represents the singleton set containing the long type.
	 */
	public static final Type tLong = new Type(TC_LONG);
	/**
	 * This type represents the singleton set containing the float type.
	 */
	public static final Type tFloat = new Type(TC_FLOAT);
	/**
	 * This type represents the singleton set containing the double type.
	 */
	public static final Type tDouble = new Type(TC_DOUBLE);
	/**
	 * This type represents the void type. It is really not a type at all.
	 */
	public static final Type tVoid = new Type(TC_VOID);
	/**
	 * This type represents the empty set, and probably means, that something
	 * has gone wrong.
	 */
	public static final Type tError = new Type(TC_ERROR);
	/**
	 * This type represents the set of all possible types.
	 */
	public static final Type tUnknown = new Type(TC_UNKNOWN);
	/**
	 * This type represents the set of all integer types, up to 32 bit.
	 */
	public static final Type tUInt = new IntegerType(IntegerType.IT_I
			| IntegerType.IT_B | IntegerType.IT_C | IntegerType.IT_S);
	/**
	 * This type represents the set of the boolean and int type.
	 */
	public static final Type tBoolInt = new IntegerType(IntegerType.IT_I
			| IntegerType.IT_Z);
	/**
	 * This type represents the set of boolean and all integer types, up to 32
	 * bit.
	 */
	public static final Type tBoolUInt = new IntegerType(IntegerType.IT_I
			| IntegerType.IT_B | IntegerType.IT_C | IntegerType.IT_S
			| IntegerType.IT_Z);
	/**
	 * This type represents the set of the boolean and byte type.
	 */
	public static final Type tBoolByte = new IntegerType(IntegerType.IT_B
			| IntegerType.IT_Z);
	/**
	 * This type represents the singleton set containing
	 * <code>java.lang.Object</code>.
	 */
	public static final ClassInterfacesType tObject = tClass("java.lang.Object");
	/**
	 * This type represents the singleton set containing the special null type
	 * (the type of null).
	 */
	public static final ReferenceType tNull = new NullType();
	/**
	 * This type represents the set of all reference types, including class
	 * types, array types, interface types and the null type.
	 */
	public static final Type tUObject = tRange(tObject, tNull);
	/**
	 * This type represents the singleton set containing
	 * <code>java.lang.String</code>.
	 */
	public static final Type tString = tClass("java.lang.String");
	/**
	 * This type represents the singleton set containing
	 * <code>java.lang.StringBuffer</code>.
	 */
	public static final Type tStringBuffer = tClass("java.lang.StringBuffer");
	/**
	 * This type represents the singleton set containing
	 * <code>java.lang.Class</code>.
	 */
	public static final Type tJavaLangClass = tClass("java.lang.Class");

	/**
	 * Generate the singleton set of the type represented by the given string.
	 * 
	 * @param type
	 *            the type signature (or method signature).
	 * @return a singleton set containing the given type.
	 */
	public static final Type tType(String type) {
		if (type == null || type.length() == 0)
			return tError;
		switch (type.charAt(0)) {
		case 'Z':
			return tBoolean;
		case 'B':
			return tByte;
		case 'C':
			return tChar;
		case 'S':
			return tShort;
		case 'I':
			return tInt;
		case 'F':
			return tFloat;
		case 'J':
			return tLong;
		case 'D':
			return tDouble;
		case 'V':
			return tVoid;
		case '[':
			return tArray(tType(type.substring(1)));
		case 'L':
			int index = type.indexOf(';');
			if (index != type.length() - 1)
				return tError;
			return tClass(type.substring(1, index));
		case '(':
			return tMethod(type);
		}
		throw new AssertError("Unknown type signature: " + type);
	}

	/**
	 * Generate the singleton set of the type represented by the given class
	 * name.
	 * 
	 * @param clazzname
	 *            the full qualified name of the class. The packages may be
	 *            separated by `.' or `/'.
	 * @return a singleton set containing the given type.
	 */
	public static final ClassInterfacesType tClass(String clazzname) {
		return tClass(ClassInfo.forName(clazzname.replace('/', '.')));
	}

	/**
	 * Generate the singleton set of the type represented by the given class
	 * info.
	 * 
	 * @param clazzinfo
	 *            the jode.bytecode.ClassInfo.
	 * @return a singleton set containing the given type.
	 */
	public static final ClassInterfacesType tClass(ClassInfo clazzinfo) {
		int hash = clazzinfo.hashCode();
		Iterator iter = classHash.iterateHashCode(hash);
		while (iter.hasNext()) {
			ClassInterfacesType type = (ClassInterfacesType) iter.next();
			if (type.getClassInfo() == clazzinfo)
				return type;
		}
		ClassInterfacesType type = new ClassInterfacesType(clazzinfo);
		classHash.put(hash, type);
		return type;
	}

	/**
	 * Generate/look up the set of the array type whose element types are in the
	 * given type set.
	 * 
	 * @param type
	 *            the element types (which may be the empty set tError).
	 * @return the set of array types (which may be the empty set tError).
	 */
	public static final Type tArray(Type type) {
		if (type == tError)
			return type;

		int hash = type.hashCode();
		Iterator iter = arrayHash.iterateHashCode(hash);
		while (iter.hasNext()) {
			ArrayType arrType = (ArrayType) iter.next();
			if (arrType.getElementType().equals(type))
				return arrType;
		}
		ArrayType arrType = new ArrayType(type);
		arrayHash.put(hash, arrType);
		return arrType;
	}

	/**
	 * Generate/look up the method type for the given signature
	 * 
	 * @param signature
	 *            the method decriptor.
	 * @return a method type (a singleton set).
	 */
	public static MethodType tMethod(String signature) {
		int hash = signature.hashCode();
		Iterator iter = methodHash.iterateHashCode(hash);
		while (iter.hasNext()) {
			MethodType methodType = (MethodType) iter.next();
			if (methodType.getTypeSignature().equals(signature))
				return methodType;
		}
		MethodType methodType = new MethodType(signature);
		methodHash.put(hash, methodType);
		return methodType;
	}

	/**
	 * Generate the range type from bottom to top. This should represent all
	 * reference types, that can be casted to bottom by a widening cast and
	 * where top can be casted to. You should not use this method directly; use
	 * tSubType, tSuperType and intersection instead, which is more general.
	 * 
	 * @param bottom
	 *            the bottom type.
	 * @param top
	 *            the top type.
	 * @return the range type.
	 */
	public static final Type tRange(ReferenceType bottom, ReferenceType top) {
		return new RangeType(bottom, top);
	}

	/**
	 * Generate the set of types, to which one of the types in type can be
	 * casted to by a widening cast. The following holds:
	 * <ul>
	 * <li>tSuperType(tObject) = tObject</li>
	 * <li>tSuperType(tError) = tError</li>
	 * <li>type.intersection(tSuperType(type)).equals(type) (this means type is
	 * a subset of tSuperType(type).</li>
	 * <li>tSuperType(tNull) = tUObject</li>
	 * <li>tSuperType(tChar) = {tChar, tInt }</li>
	 * </ul>
	 * 
	 * @param type
	 *            a set of types.
	 * @return the super types of type.
	 */
	public static Type tSuperType(Type type) {
		return type.getSuperType();
	}

	/**
	 * Generate the set of types, which can be casted to one of the types in
	 * type by a widening cast. The following holds:
	 * <ul>
	 * <li>tSubType(tObject) = tUObject</li>
	 * <li>tSubType(tError) = tError</li>
	 * <li>type.intersection(tSubType(type)).equals(type) (this means type is a
	 * subset of tSubType(type).</li>
	 * <li>tSuperType(tSubType(type)) is a subset of type</li>
	 * <li>tSubType(tSuperType(type)) is a subset of type</li>
	 * <li>tSubType(tNull) = tNull</li>
	 * <li>tSubType(tBoolean, tShort) = { tBoolean, tByte, tShort }</li>
	 * </ul>
	 * 
	 * @param type
	 *            a set of types.
	 * @return the sub types of type.
	 */
	public static Type tSubType(Type type) {
		return type.getSubType();
	}

	/**
	 * The typecode of this type. This should be one of the TC_ constants.
	 */
	final int typecode;

	/**
	 * Create a new type with the given type code.
	 */
	protected Type(int tc) {
		typecode = tc;
	}

	/**
	 * The sub types of this type.
	 * 
	 * @return tSubType(this).
	 */
	public Type getSubType() {
		return this;
	}

	/**
	 * The super types of this type.
	 * 
	 * @return tSuperType(this).
	 */
	public Type getSuperType() {
		return this;
	}

	/**
	 * Returns the hint type of this type set. This returns the singleton set
	 * containing only the `most likely' type in this set. This doesn't work for
	 * <code>tError</code> or <code>tUnknown</code>, and may lead to errors for
	 * certain range types.
	 * 
	 * @return the hint type.
	 */
	public Type getHint() {
		return getCanonic();
	}

	/**
	 * Returns the canonic type of this type set. The intention is, to return
	 * for each expression the type, that the java compiler would assign to this
	 * expression.
	 * 
	 * @return the canonic type.
	 */
	public Type getCanonic() {
		return this;
	}

	/**
	 * Returns the type code of this type. Don't use this; it is merily needed
	 * by the sub types (and the bytecode verifier, which has its own type
	 * merging methods).
	 * 
	 * @return the type code of the type.
	 */
	public final int getTypeCode() {
		return typecode;
	}

	/**
	 * Returns the number of stack/local entries an object of this type
	 * occupies.
	 * 
	 * @return 0 for tVoid, 2 for tDouble and tLong and 1 for every other type.
	 */
	public int stackSize() {
		switch (typecode) {
		case TC_VOID:
			return 0;
		case TC_ERROR:
		default:
			return 1;
		case TC_DOUBLE:
		case TC_LONG:
			return 2;
		}
	}

	/**
	 * Intersect this set of types with another type set and return the
	 * intersection.
	 * 
	 * @param type
	 *            the other type set.
	 * @return the intersection, tError, if the intersection is empty.
	 */
	public Type intersection(Type type) {
		if (this == tError || type == tError)
			return tError;
		if (this == tUnknown)
			return type;
		if (type == tUnknown || this == type)
			return this;
		/*
		 * We have two different singleton sets now.
		 */
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
			GlobalOptions.err.println("intersecting " + this + " and " + type
					+ " to <error>");
		return tError;
	}

	/**
	 * Checks if we need to cast to a middle type, before we can cast from
	 * fromType to this type. For example it is impossible to cast a String to a
	 * StringBuffer, but if we cast to Object in between this is allowed (it
	 * doesn't make much sense though).
	 * 
	 * @return the middle type, or null if it is not necessary.
	 */
	public Type getCastHelper(Type fromType) {
		return null;
	}

	/**
	 * Checks if this type represents a valid singleton type.
	 */
	public boolean isValidType() {
		return typecode <= TC_DOUBLE;
	}

	/**
	 * Checks if this is a class or array type (but not a null type).
	 * 
	 * @return true if this is a class or array type.
	 * @XXX remove this?
	 */
	public boolean isClassType() {
		return false;
	}

	/**
	 * Check if this type set and the other type set are not disjunct.
	 * 
	 * @param type
	 *            the other type set.
	 * @return true if this they aren't disjunct.
	 */
	public boolean isOfType(Type type) {
		return this.intersection(type) != Type.tError;
	}

	/**
	 * Generates the default name, that is the `natural' choice for local of
	 * this type.
	 * 
	 * @return the default name of a local of this type.
	 */
	public String getDefaultName() {
		switch (typecode) {
		case TC_LONG:
			return "l";
		case TC_FLOAT:
			return "f";
		case TC_DOUBLE:
			return "d";
		default:
			return "local";
		}
	}

	/**
	 * Generates the default value, that is the initial value of a field of this
	 * type.
	 * 
	 * @return the default value of a field of this type.
	 */
	public Object getDefaultValue() {
		switch (typecode) {
		case TC_LONG:
			return new Long(0);
		case TC_FLOAT:
			return new Float(0);
		case TC_DOUBLE:
			return new Double(0);
		default:
			return null;
		}
	}

	/**
	 * Returns the type signature of this type. You should only call this on
	 * singleton types.
	 * 
	 * @return the type (or method) signature of this type.
	 */
	public String getTypeSignature() {
		switch (typecode) {
		case TC_LONG:
			return "J";
		case TC_FLOAT:
			return "F";
		case TC_DOUBLE:
			return "D";
		default:
			return "?";
		}
	}

	/**
	 * Returns the java.lang.Class representing this type. You should only call
	 * this on singleton types.
	 * 
	 * @return the Class object representing this type.
	 */
	public Class getTypeClass() throws ClassNotFoundException {
		switch (typecode) {
		case TC_LONG:
			return Long.TYPE;
		case TC_FLOAT:
			return Float.TYPE;
		case TC_DOUBLE:
			return Double.TYPE;
		default:
			throw new AssertError("getTypeClass() called on illegal type");
		}
	}

	/**
	 * Returns a string representation describing this type set.
	 * 
	 * @return a string representation describing this type set.
	 */
	public String toString() {
		switch (typecode) {
		case TC_LONG:
			return "long";
		case TC_FLOAT:
			return "float";
		case TC_DOUBLE:
			return "double";
		case TC_NULL:
			return "null";
		case TC_VOID:
			return "void";
		case TC_UNKNOWN:
			return "<unknown>";
		case TC_ERROR:
		default:
			return "<error>";
		}
	}
}
