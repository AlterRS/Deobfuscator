/* FieldOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: FieldOperator.java.in,v 4.4.2.2 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import java.lang.reflect.Modifier;
import java.util.Collection;

import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.InnerClassInfo;
import jode.bytecode.Reference;
import jode.bytecode.TypeSignature;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.FieldAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.Options;
import jode.decompiler.Scope;
import jode.decompiler.TabbedPrintWriter;
import jode.type.ClassInterfacesType;
import jode.type.NullType;
import jode.type.Type;


/**
 * This class contains everything shared between PutFieldOperator and
 * GetFieldOperator
 */
public abstract class FieldOperator extends Operator {
	MethodAnalyzer methodAnalyzer;
	boolean staticFlag;
	Reference ref;
	Type classType;

	public FieldOperator(MethodAnalyzer methodAnalyzer, boolean staticFlag,
			Reference ref) {
		super(Type.tType(ref.getType()));
		this.methodAnalyzer = methodAnalyzer;
		this.staticFlag = staticFlag;
		this.classType = Type.tType(ref.getClazz());
		this.ref = ref;
		if (staticFlag)
			methodAnalyzer.useType(classType);
		initOperands(staticFlag ? 0 : 1);
	}

	public int getPriority() {
		return 950;
	}

	public void updateSubTypes() {
		if (!staticFlag)
			subExpressions[0].setType(Type.tSubType(classType));
	}

	public void updateType() {
		updateParentType(getFieldType());
	}

	public boolean isStatic() {
		return staticFlag;
	}

	public ClassInfo getClassInfo() {
		if (classType instanceof ClassInterfacesType)
			return ((ClassInterfacesType) classType).getClassInfo();
		return null;
	}

	/**
	 * Returns the field analyzer for the field, if the field is declared in the
	 * same class or some outer class as the method containing this instruction.
	 * Otherwise it returns null.
	 * 
	 * @return see above.
	 */
	public FieldAnalyzer getField() {
		ClassInfo clazz = getClassInfo();
		if (clazz != null) {
			ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
			while (true) {
				if (clazz == ana.getClazz()) {
					int field = ana.getFieldIndex(ref.getName(),
							Type.tType(ref.getType()));
					if (field >= 0)
						return ana.getField(field);
					return null;
				}
				if (ana.getParent() == null)
					return null;
				if (ana.getParent() instanceof MethodAnalyzer)
					ana = ((MethodAnalyzer) ana.getParent()).getClassAnalyzer();
				else if (ana.getParent() instanceof ClassAnalyzer)
					ana = (ClassAnalyzer) ana.getParent();
				else
					throw new jode.AssertError("Unknown parent");
			}
		}
		return null;
	}

	public String getFieldName() {
		return ref.getName();
	}

	public Type getFieldType() {
		return Type.tType(ref.getType());
	}

	private static FieldInfo getFieldInfo(ClassInfo clazz, String name,
			String type) {
		while (clazz != null) {
			FieldInfo field = clazz.findField(name, type);
			if (field != null)
				return field;

			ClassInfo[] ifaces = clazz.getInterfaces();
			for (int i = 0; i < ifaces.length; i++) {
				field = getFieldInfo(ifaces[i], name, type);
				if (field != null)
					return field;
			}

			clazz = clazz.getSuperclass();
		}
		return null;
	}

	public FieldInfo getFieldInfo() {
		ClassInfo clazz;
		if (ref.getClazz().charAt(0) == '[')
			clazz = ClassInfo.javaLangObject;
		else
			clazz = TypeSignature.getClassInfo(ref.getClazz());
		return getFieldInfo(clazz, ref.getName(), ref.getType());
	}

	public boolean needsCast(Type type) {
		if (type instanceof NullType)
			return true;
		if (!(type instanceof ClassInterfacesType && classType instanceof ClassInterfacesType))
			return false;

		ClassInfo clazz = ((ClassInterfacesType) classType).getClassInfo();
		ClassInfo parClazz = ((ClassInterfacesType) type).getClassInfo();
		FieldInfo field = clazz.findField(ref.getName(), ref.getType());

		find_field: while (field == null) {
			ClassInfo ifaces[] = clazz.getInterfaces();
			for (int i = 0; i < ifaces.length; i++) {
				field = ifaces[i].findField(ref.getName(), ref.getType());
				if (field != null)
					break find_field;
			}
			clazz = clazz.getSuperclass();
			if (clazz == null)
				/* Weird, field not existing? */
				return false;
			field = clazz.findField(ref.getName(), ref.getType());
		}
		if (Modifier.isPrivate(field.getModifiers()))
			return parClazz != clazz;
		else if ((field.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC)) == 0) {
			/*
			 * Field is protected. We need a cast if parClazz is in other
			 * package than clazz.
			 */
			int lastDot = clazz.getName().lastIndexOf('.');
			if (lastDot == -1
					|| lastDot != parClazz.getName().lastIndexOf('.')
					|| !(parClazz.getName().startsWith(clazz.getName()
							.substring(0, lastDot))))
				return true;
		}

		while (clazz != parClazz && clazz != null) {
			FieldInfo[] fields = parClazz.getFields();
			for (int i = 0; i < fields.length; i++) {
				if (fields[i].getName().equals(ref.getName()))
					return true;
			}
			parClazz = parClazz.getSuperclass();
		}
		return false;
	}

	public InnerClassInfo getOuterClassInfo(ClassInfo ci) {
		if (ci != null) {
			InnerClassInfo[] outers = ci.getOuterClasses();
			if (outers != null)
				return outers[0];
		}
		return null;
	}

	/**
	 * We add the named method scoped classes to the declarables.
	 */
	public void fillDeclarables(Collection used) {
		ClassInfo clazz = getClassInfo();
		InnerClassInfo outer = getOuterClassInfo(clazz);
		ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);

		if ((Options.options & Options.OPTION_ANON) != 0 && outer != null
				&& outer.outer == null && outer.name != null
				&& clazzAna != null && clazzAna.getParent() == methodAnalyzer) {

			/*
			 * This is a named method scope class, declare it. But first declare
			 * all method scoped classes, that are used inside; order does
			 * matter.
			 */
			clazzAna.fillDeclarables(used);
			used.add(clazzAna);
		}
		super.fillDeclarables(used);
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		boolean opIsThis = !staticFlag
				&& subExpressions[0] instanceof ThisOperator;
		String fieldName = ref.getName();
		if (staticFlag) {
			if (!classType.equals(Type.tClass(methodAnalyzer.getClazz()))
					|| methodAnalyzer.findLocal(fieldName) != null) {
				writer.printType(classType);
				writer.breakOp();
				writer.print(".");
			}
			writer.print(fieldName);
		} else if (needsCast(subExpressions[0].getType().getCanonic())) {
			writer.print("(");
			writer.startOp(writer.EXPL_PAREN, 1);
			writer.print("(");
			writer.printType(classType);
			writer.print(") ");
			writer.breakOp();
			subExpressions[0].dumpExpression(writer, 700);
			writer.endOp();
			writer.print(")");
			writer.breakOp();
			writer.print(".");
			writer.print(fieldName);
		} else {
			if (opIsThis) {
				ThisOperator thisOp = (ThisOperator) subExpressions[0];
				Scope scope = writer.getScope(thisOp.getClassInfo(),
						Scope.CLASSSCOPE);

				if (scope == null
						|| writer.conflicts(fieldName, scope, Scope.FIELDNAME)) {
					thisOp.dumpExpression(writer, 950);
					writer.breakOp();
					writer.print(".");
				} else if (writer.conflicts(fieldName, scope,
						Scope.AMBIGUOUSNAME)
						|| (/*
							 * This is a inherited field conflicting with a
							 * field name in some outer class.
							 */
						getField() == null && writer.conflicts(fieldName, null,
								Scope.NOSUPERFIELDNAME))) {
					thisOp.dumpExpression(writer, 950);
					writer.breakOp();
					writer.print(".");
				}
			} else {
				subExpressions[0].dumpExpression(writer, 950);
				writer.breakOp();
				writer.print(".");
			}
			writer.print(fieldName);
		}
	}
}
