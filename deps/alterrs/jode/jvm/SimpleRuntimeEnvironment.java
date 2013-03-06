/* SimpleRuntimeEnvironment Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: SimpleRuntimeEnvironment.java,v 1.8.2.1 2002/05/28 17:34:12 hoenicke Exp $
 */

package alterrs.jode.jvm;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import alterrs.jode.bytecode.Reference;
import alterrs.jode.bytecode.TypeSignature;

public class SimpleRuntimeEnvironment implements RuntimeEnvironment {

	public static Object fromReflectType(String typeSig, Object value) {
		switch (typeSig.charAt(0)) {
		case 'Z':
			return new Integer(((Boolean) value).booleanValue() ? 1 : 0);
		case 'B':
		case 'S':
			return new Integer(((Number) value).intValue());
		case 'C':
			return new Integer(((Character) value).charValue());
		default:
			return value;
		}
	}

	public static Object toReflectType(String typeSig, Object value) {
		switch (typeSig.charAt(0)) {
		case 'Z':
			return new Boolean(((Integer) value).intValue() != 0);
		case 'B':
			return new Byte(((Integer) value).byteValue());
		case 'S':
			return new Short(((Integer) value).shortValue());
		case 'C':
			return new Character((char) ((Integer) value).intValue());
		default:
			return value;
		}
	}

	public Object getField(Reference ref, Object obj)
			throws InterpreterException {
		Field f;
		try {
			Class clazz = TypeSignature.getClass(ref.getClazz());
			try {
				f = clazz.getField(ref.getName());
			} catch (NoSuchFieldException ex) {
				f = clazz.getDeclaredField(ref.getName());
			}
		} catch (ClassNotFoundException ex) {
			throw new InterpreterException(ref + ": Class not found");
		} catch (NoSuchFieldException ex) {
			throw new InterpreterException("Constructor " + ref + " not found");
		} catch (SecurityException ex) {
			throw new InterpreterException(ref + ": Security exception");
		}
		try {
			return fromReflectType(ref.getType(), f.get(obj));
		} catch (IllegalAccessException ex) {
			throw new InterpreterException("Field " + ref + " not accessible");
		}
	}

	public void putField(Reference ref, Object obj, Object value)
			throws InterpreterException {
		Field f;
		try {
			Class clazz = TypeSignature.getClass(ref.getClazz());
			try {
				f = clazz.getField(ref.getName());
			} catch (NoSuchFieldException ex) {
				f = clazz.getDeclaredField(ref.getName());
			}
		} catch (ClassNotFoundException ex) {
			throw new InterpreterException(ref + ": Class not found");
		} catch (NoSuchFieldException ex) {
			throw new InterpreterException("Constructor " + ref + " not found");
		} catch (SecurityException ex) {
			throw new InterpreterException(ref + ": Security exception");
		}
		try {
			f.set(obj, toReflectType(ref.getType(), value));
		} catch (IllegalAccessException ex) {
			throw new InterpreterException("Field " + ref + " not accessible");
		}
	}

	public Object invokeConstructor(Reference ref, Object[] params)
			throws InterpreterException, InvocationTargetException {
		Constructor c;
		try {
			String[] paramTypeSigs = TypeSignature.getParameterTypes(ref
					.getType());
			Class clazz = TypeSignature.getClass(ref.getClazz());
			Class[] paramTypes = new Class[paramTypeSigs.length];
			for (int i = 0; i < paramTypeSigs.length; i++) {
				params[i] = toReflectType(paramTypeSigs[i], params[i]);
				paramTypes[i] = TypeSignature.getClass(paramTypeSigs[i]);
			}
			try {
				c = clazz.getConstructor(paramTypes);
			} catch (NoSuchMethodException ex) {
				c = clazz.getDeclaredConstructor(paramTypes);
			}
		} catch (ClassNotFoundException ex) {
			throw new InterpreterException(ref + ": Class not found");
		} catch (NoSuchMethodException ex) {
			throw new InterpreterException("Constructor " + ref + " not found");
		} catch (SecurityException ex) {
			throw new InterpreterException(ref + ": Security exception");
		}

		try {
			return c.newInstance(params);
		} catch (IllegalAccessException ex) {
			throw new InterpreterException("Constructor " + ref
					+ " not accessible");
		} catch (InstantiationException ex) {
			throw new InterpreterException("InstantiationException in " + ref
					+ ".");
		}
	}

	public Object invokeMethod(Reference ref, boolean isVirtual, Object cls,
			Object[] params) throws InterpreterException,
			InvocationTargetException {
		if (!isVirtual && cls != null) /* XXX */
			throw new InterpreterException("Can't invoke nonvirtual Method "
					+ ref + ".");

		Method m;
		try {
			String[] paramTypeSigs = TypeSignature.getParameterTypes(ref
					.getType());
			Class clazz = TypeSignature.getClass(ref.getClazz());
			Class[] paramTypes = new Class[paramTypeSigs.length];
			for (int i = 0; i < paramTypeSigs.length; i++) {
				params[i] = toReflectType(paramTypeSigs[i], params[i]);
				paramTypes[i] = TypeSignature.getClass(paramTypeSigs[i]);
			}
			try {
				m = clazz.getMethod(ref.getName(), paramTypes);
			} catch (NoSuchMethodException ex) {
				m = clazz.getDeclaredMethod(ref.getName(), paramTypes);
			}
		} catch (ClassNotFoundException ex) {
			throw new InterpreterException(ref + ": Class not found");
		} catch (NoSuchMethodException ex) {
			throw new InterpreterException("Method " + ref + " not found");
		} catch (SecurityException ex) {
			throw new InterpreterException(ref + ": Security exception");
		}
		String retType = TypeSignature.getReturnType(ref.getType());
		try {
			return fromReflectType(retType, m.invoke(cls, params));
		} catch (IllegalAccessException ex) {
			throw new InterpreterException("Method " + ref + " not accessible");
		}
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
		return obj != null && !clazz.isInstance(obj);
	}

	public Object newArray(String type, int[] dimensions)
			throws InterpreterException, NegativeArraySizeException {
		Class clazz;
		try {
			/* get the base class (strip leading "[") */
			clazz = TypeSignature.getClass(type.substring(dimensions.length));
		} catch (ClassNotFoundException ex) {
			throw new InterpreterException("Class " + ex.getMessage()
					+ " not found");
		}
		return Array.newInstance(clazz, dimensions);
	}

	public void enterMonitor(Object obj) throws InterpreterException {
		throw new InterpreterException("monitor not implemented");
	}

	public void exitMonitor(Object obj) throws InterpreterException {
		throw new InterpreterException("monitor not implemented");
	}
}
