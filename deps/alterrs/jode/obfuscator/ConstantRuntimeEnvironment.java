/* ConstantRuntimeEnvironment Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: ConstantRuntimeEnvironment.java.in,v 1.2.2.2 2002/05/28 17:34:14 hoenicke Exp $
 */

package alterrs.jode.obfuscator;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import alterrs.jode.bytecode.BytecodeInfo;
import alterrs.jode.bytecode.Reference;
import alterrs.jode.bytecode.TypeSignature;
import alterrs.jode.jvm.Interpreter;
import alterrs.jode.jvm.InterpreterException;
import alterrs.jode.jvm.SimpleRuntimeEnvironment;

public class ConstantRuntimeEnvironment extends SimpleRuntimeEnvironment {

	/**
	 * The references that may be used in constant methods.
	 */
	static Set whiteList = new HashSet();

	static void addWhite(Reference ref) {
		whiteList.add(ref);
	}

	public static boolean isWhite(Reference ref) {
		return whiteList.contains(ref);
	}

	public static boolean isWhite(String retTypeSig) {
		return retTypeSig.length() == 1 // primitive type
				|| whiteList.contains(retTypeSig);
	}

	static {
		addWhite(Reference.getReference("Ljava/lang/String;", "toCharArray",
				"()[C"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "<init>",
				"(Ljava/lang/String;)V"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "<init>",
				"()V"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(C)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(B)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(S)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(Z)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(F)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(I)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(J)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "append",
				"(D)Ljava/lang/StringBuffer;"));
		addWhite(Reference.getReference("Ljava/lang/StringBuffer;", "toString",
				"()Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "<init>", "()V"));
		addWhite(Reference
				.getReference("Ljava/lang/String;", "<init>", "([C)V"));
		addWhite(Reference.getReference("Ljava/lang/String;", "<init>",
				"([CII)V"));
		addWhite(Reference.getReference("Ljava/lang/String;", "<init>",
				"(Ljava/lang/String;)V"));
		addWhite(Reference.getReference("Ljava/lang/String;", "<init>",
				"(Ljava/lang/StringBuffer;)V"));
		addWhite(Reference.getReference("Ljava/lang/String;", "length", "()I"));
		addWhite(Reference.getReference("Ljava/lang/String;", "replace",
				"(CC)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(Z)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(B)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(S)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(C)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(D)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(F)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(I)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(J)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "valueOf",
				"(Ljava/lang/Object;)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "substring",
				"(I)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/String;", "substring",
				"(II)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava.lang/reflect/Modifier;",
				"toString", "(I)Ljava/lang/String;"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "abs", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "abs", "(F)F"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "abs", "(I)I"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "abs", "(J)J"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "acos", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "asin", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "atan", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "atan2", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "ceil", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "cos", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "exp", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "floor", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "IEEEremainder",
				"(DD)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "log", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "max", "(DD)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "max", "(FF)F"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "max", "(II)I"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "max", "(JJ)J"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "min", "(DD)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "min", "(FF)F"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "min", "(II)I"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "min", "(JJ)J"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "pow", "(DD)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "rint", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "round", "(D)J"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "round", "(F)I"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "sin", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "sqrt", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "tan", "(D)D"));
		addWhite(Reference
				.getReference("Ljava/lang/Math;", "toDegrees", "(D)D"));
		addWhite(Reference
				.getReference("Ljava/lang/Math;", "toRadians", "(D)D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "E", "D"));
		addWhite(Reference.getReference("Ljava/lang/Math;", "PI", "D"));

		whiteList.add("Ljava/lang/String;");
		// whiteList.add("Ljava/lang/Class;");
		// whiteList.add("Ljava/lang/reflect/Method;");
		// whiteList.add("Ljava/lang/reflect/Field;");
	}

	private Interpreter interpreter;
	private Identifier currentFieldListener;

	public ConstantRuntimeEnvironment() {
		interpreter = new Interpreter(this);
	}

	public void setFieldListener(Identifier fl) {
		currentFieldListener = fl;
	}

	public static Object getDefaultValue(String typeSig) {
		switch (typeSig.charAt(0)) {
		case 'Z':
		case 'B':
		case 'S':
		case 'C':
		case 'I':
			return new Integer(0);
		case 'J':
			return new Long(0L);
		case 'D':
			return new Double(0.0);
		case 'F':
			return new Float(0.0F);
		default:
			return null;
		}
	}

	public Object getField(Reference ref, Object obj)
			throws InterpreterException {
		if (isWhite(ref))
			return super.getField(ref, obj);
		FieldIdentifier fi = (FieldIdentifier) Main.getClassBundle()
				.getIdentifier(ref);
		if (fi != null && !fi.isNotConstant()) {
			Object result = fi.getConstant();
			if (currentFieldListener != null)
				fi.addFieldListener(currentFieldListener);
			if (result == null)
				result = getDefaultValue(ref.getType());
			return result;
		}
		throw new InterpreterException("Field " + ref + " not constant");
	}

	public void putField(Reference ref, Object obj, Object value)
			throws InterpreterException {
		throw new InterpreterException("Modifying Field " + ref + ".");
	}

	public Object invokeConstructor(Reference ref, Object[] params)
			throws InterpreterException, InvocationTargetException {
		if (isWhite(ref))
			return super.invokeConstructor(ref, params);
		throw new InterpreterException("Creating new Object " + ref + ".");
	}

	public Object invokeMethod(Reference ref, boolean isVirtual, Object cls,
			Object[] params) throws InterpreterException,
			InvocationTargetException {
		if (isWhite(ref))
			return super.invokeMethod(ref, isVirtual, cls, params);
		MethodIdentifier mi = (MethodIdentifier) Main.getClassBundle()
				.getIdentifier(ref);
		if (mi != null) {
			BytecodeInfo code = mi.info.getBytecode();
			if (code != null)
				return interpreter.interpretMethod(code, cls, params);
		}
		throw new InterpreterException("Invoking library method " + ref + ".");
	}

	public boolean instanceOf(Object obj, String className)
			throws InterpreterException {
		Class clazz;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException ex) {
			throw new InterpreterException("Class " + ex.getMessage()
					+ " not found");
		}
		return obj != null && clazz.isInstance(obj);
	}

	public Object newArray(String type, int[] dimensions)
			throws InterpreterException, NegativeArraySizeException {
		if (type.length() == dimensions.length + 1) {
			Class clazz;
			try {
				clazz = TypeSignature.getClass(type
						.substring(dimensions.length));
			} catch (ClassNotFoundException ex) {
				throw new InterpreterException("Class " + ex.getMessage()
						+ " not found");
			}
			return Array.newInstance(clazz, dimensions);
		}
		throw new InterpreterException("Creating object array.");
	}
}
