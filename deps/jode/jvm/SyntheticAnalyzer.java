/* SyntheticAnalyzer Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: SyntheticAnalyzer.java.in,v 1.2.2.5 2002/05/28 17:34:12 hoenicke Exp $
 */

package jode.jvm;

import java.lang.reflect.Modifier;
import java.util.Iterator;

import jode.bytecode.BytecodeInfo;
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.Handler;
import jode.bytecode.Instruction;
import jode.bytecode.MethodInfo;
import jode.bytecode.Opcodes;
import jode.bytecode.Reference;
import jode.bytecode.TypeSignature;
import jode.type.MethodType;
import jode.type.Type;


public class SyntheticAnalyzer implements Opcodes {
	public final static int UNKNOWN = 0;
	public final static int GETCLASS = 1;
	public final static int ACCESSGETFIELD = 2;
	public final static int ACCESSPUTFIELD = 3;
	public final static int ACCESSMETHOD = 4;
	public final static int ACCESSGETSTATIC = 5;
	public final static int ACCESSPUTSTATIC = 6;
	public final static int ACCESSSTATICMETHOD = 7;
	public final static int ACCESSCONSTRUCTOR = 8;
	public final static int ACCESSDUPPUTFIELD = 9;
	public final static int ACCESSDUPPUTSTATIC = 10;

	int kind = UNKNOWN;
	Reference reference;
	MethodInfo method;
	int unifyParam = -1;

	public SyntheticAnalyzer(MethodInfo method, boolean checkName) {
		this.method = method;
		if (method.getBytecode() == null)
			return;
		if (!checkName || method.getName().equals("class$"))
			if (checkGetClass())
				return;
		if (!checkName || method.getName().startsWith("access$"))
			if (checkAccess())
				return;
		if (method.getName().equals("<init>"))
			if (checkConstructorAccess())
				return;
	}

	public int getKind() {
		return kind;
	}

	public Reference getReference() {
		return reference;
	}

	/**
	 * Gets the index of the dummy parameter for an ACCESSCONSTRUCTOR. Normally
	 * the 1 but for inner classes it may be 2.
	 */
	public int getUnifyParam() {
		return unifyParam;
	}

	private static final int[] getClassOpcodes = { opc_aload, opc_invokestatic,
			opc_areturn, opc_astore, opc_new, opc_dup, opc_aload,
			opc_invokevirtual, opc_invokespecial, opc_athrow };
	private static final Reference[] getClassRefs = {
			null,
			Reference.getReference("Ljava/lang/Class;", "forName",
					"(Ljava/lang/String;)Ljava/lang/Class;"),
			null,
			null,
			null,
			null,
			null,
			Reference.getReference("Ljava/lang/Throwable;", "getMessage",
					"()Ljava/lang/String;"),
			Reference.getReference("Ljava/lang/NoClassDefFoundError;",
					"<init>", "(Ljava/lang/String;)V"), null };

	boolean checkGetClass() {
		if (!method.isStatic()
				|| !(method.getType()
						.equals("(Ljava/lang/String;)Ljava/lang/Class;")))
			return false;

		BytecodeInfo bytecode = method.getBytecode();

		Handler[] excHandlers = bytecode.getExceptionHandlers();
		if (excHandlers.length != 1
				|| !"java.lang.ClassNotFoundException"
						.equals(excHandlers[0].type))
			return false;

		int excSlot = -1;
		int i = 0;
		for (Iterator iter = bytecode.getInstructions().iterator(); iter
				.hasNext(); i++) {
			Instruction instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (i == getClassOpcodes.length
					|| instr.getOpcode() != getClassOpcodes[i])
				return false;
			if (i == 0
					&& (instr.getLocalSlot() != 0 || excHandlers[0].start != instr))
				return false;
			if (i == 2 && excHandlers[0].end != instr)
				return false;
			if (i == 3) {
				if (excHandlers[0].catcher != instr)
					return false;
				excSlot = instr.getLocalSlot();
			}
			if (i == 4
					&& !instr.getClazzType().equals(
							"Ljava/lang/NoClassDefFoundError;"))
				return false;
			if (i == 6 && instr.getLocalSlot() != excSlot)
				return false;
			if (getClassRefs[i] != null
					&& !getClassRefs[i].equals(instr.getReference()))
				return false;
		}
		this.kind = GETCLASS;
		return true;
	}

	private final int modifierMask = Modifier.PUBLIC | Modifier.STATIC;

	public boolean checkStaticAccess() {
		ClassInfo clazzInfo = method.getClazzInfo();
		BytecodeInfo bytecode = method.getBytecode();
		Iterator iter = bytecode.getInstructions().iterator();
		boolean dupSeen = false;

		Instruction instr = (Instruction) iter.next();
		while (instr.getOpcode() == opc_nop && iter.hasNext())
			instr = (Instruction) iter.next();
		if (instr.getOpcode() == opc_getstatic) {
			Reference ref = instr.getReference();
			ClassInfo refClazz = TypeSignature.getClassInfo(ref.getClazz());
			if (!refClazz.superClassOf(clazzInfo))
				return false;
			FieldInfo refField = refClazz.findField(ref.getName(),
					ref.getType());
			if ((refField.getModifiers() & modifierMask) != Modifier.STATIC)
				return false;
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (instr.getOpcode() < opc_ireturn
					|| instr.getOpcode() > opc_areturn)
				return false;
			/* For valid bytecode the type matches automatically */
			reference = ref;
			kind = ACCESSGETSTATIC;
			return true;
		}
		int params = 0, slot = 0;
		while (instr.getOpcode() >= opc_iload && instr.getOpcode() <= opc_aload
				&& instr.getLocalSlot() == slot) {
			params++;
			slot += (instr.getOpcode() == opc_lload || instr.getOpcode() == opc_dload) ? 2
					: 1;
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
		}
		if (instr.getOpcode() == (opc_dup - 3) + 3 * slot) {
			/*
			 * This is probably a opc_dup or opc_dup2, preceding a opc_putstatic
			 */
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (instr.getOpcode() != opc_putstatic)
				return false;
			dupSeen = true;
		}
		if (instr.getOpcode() == opc_putstatic) {
			if (params != 1)
				return false;
			/* For valid bytecode the type of param matches automatically */
			Reference ref = instr.getReference();
			ClassInfo refClazz = TypeSignature.getClassInfo(ref.getClazz());
			if (!refClazz.superClassOf(clazzInfo))
				return false;
			FieldInfo refField = refClazz.findField(ref.getName(),
					ref.getType());
			if ((refField.getModifiers() & modifierMask) != Modifier.STATIC)
				return false;
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (dupSeen) {
				if (instr.getOpcode() < opc_ireturn
						|| instr.getOpcode() > opc_areturn)
					return false;
				kind = ACCESSDUPPUTSTATIC;
			} else {
				if (instr.getOpcode() != opc_return)
					return false;
				kind = ACCESSPUTSTATIC;
			}
			reference = ref;
			return true;
		}
		if (instr.getOpcode() == opc_invokestatic) {
			Reference ref = instr.getReference();
			ClassInfo refClazz = TypeSignature.getClassInfo(ref.getClazz());
			if (!refClazz.superClassOf(clazzInfo))
				return false;
			MethodInfo refMethod = refClazz.findMethod(ref.getName(),
					ref.getType());
			MethodType refType = Type.tMethod(ref.getType());
			if ((refMethod.getModifiers() & modifierMask) != Modifier.STATIC
					|| refType.getParameterTypes().length != params)
				return false;
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (refType.getReturnType() == Type.tVoid) {
				if (instr.getOpcode() != opc_return)
					return false;
			} else {
				if (instr.getOpcode() < opc_ireturn
						|| instr.getOpcode() > opc_areturn)
					return false;
			}

			/* For valid bytecode the types matches automatically */
			reference = ref;
			kind = ACCESSSTATICMETHOD;
			return true;
		}
		return false;
	}

	public boolean checkAccess() {
		ClassInfo clazzInfo = method.getClazzInfo();
		BytecodeInfo bytecode = method.getBytecode();
		Handler[] excHandlers = bytecode.getExceptionHandlers();
		boolean dupSeen = false;
		if (excHandlers != null && excHandlers.length != 0)
			return false;

		if (method.isStatic()) {
			if (checkStaticAccess())
				return true;
		}

		Iterator iter = bytecode.getInstructions().iterator();
		Instruction instr = (Instruction) iter.next();
		while (instr.getOpcode() == opc_nop && iter.hasNext())
			instr = (Instruction) iter.next();
		if (instr.getOpcode() != opc_aload || instr.getLocalSlot() != 0)
			return false;
		instr = (Instruction) iter.next();
		while (instr.getOpcode() == opc_nop && iter.hasNext())
			instr = (Instruction) iter.next();

		if (instr.getOpcode() == opc_getfield) {
			Reference ref = instr.getReference();
			ClassInfo refClazz = TypeSignature.getClassInfo(ref.getClazz());
			if (!refClazz.superClassOf(clazzInfo))
				return false;
			FieldInfo refField = refClazz.findField(ref.getName(),
					ref.getType());
			if ((refField.getModifiers() & modifierMask) != 0)
				return false;
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (instr.getOpcode() < opc_ireturn
					|| instr.getOpcode() > opc_areturn)
				return false;
			/* For valid bytecode the type matches automatically */
			reference = ref;
			kind = ACCESSGETFIELD;
			return true;
		}
		int params = 0, slot = 1;
		while (instr.getOpcode() >= opc_iload && instr.getOpcode() <= opc_aload
				&& instr.getLocalSlot() == slot) {
			params++;
			slot += (instr.getOpcode() == opc_lload || instr.getOpcode() == opc_dload) ? 2
					: 1;
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
		}
		if (instr.getOpcode() == (opc_dup_x1 - 6) + 3 * slot) {
			/*
			 * This is probably a opc_dup_x1 or opc_dup2_x1, preceding a
			 * opc_putfield
			 */
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (instr.getOpcode() != opc_putfield)
				return false;
			dupSeen = true;
		}
		if (instr.getOpcode() == opc_putfield) {
			if (params != 1)
				return false;
			/* For valid bytecode the type of param matches automatically */
			Reference ref = instr.getReference();
			ClassInfo refClazz = TypeSignature.getClassInfo(ref.getClazz());
			if (!refClazz.superClassOf(clazzInfo))
				return false;
			FieldInfo refField = refClazz.findField(ref.getName(),
					ref.getType());
			if ((refField.getModifiers() & modifierMask) != 0)
				return false;

			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (dupSeen) {
				if (instr.getOpcode() < opc_ireturn
						|| instr.getOpcode() > opc_areturn)
					return false;
				kind = ACCESSDUPPUTFIELD;
			} else {
				if (instr.getOpcode() != opc_return)
					return false;
				kind = ACCESSPUTFIELD;
			}
			reference = ref;
			return true;
		}
		if (instr.getOpcode() == opc_invokespecial) {
			Reference ref = instr.getReference();
			ClassInfo refClazz = TypeSignature.getClassInfo(ref.getClazz());
			if (!refClazz.superClassOf(clazzInfo))
				return false;
			MethodInfo refMethod = refClazz.findMethod(ref.getName(),
					ref.getType());
			MethodType refType = Type.tMethod(ref.getType());
			if ((refMethod.getModifiers() & modifierMask) != 0
					|| refType.getParameterTypes().length != params)
				return false;
			instr = (Instruction) iter.next();
			while (instr.getOpcode() == opc_nop && iter.hasNext())
				instr = (Instruction) iter.next();
			if (refType.getReturnType() == Type.tVoid) {
				if (instr.getOpcode() != opc_return)
					return false;
			} else {
				if (instr.getOpcode() < opc_ireturn
						|| instr.getOpcode() > opc_areturn)
					return false;
			}

			/* For valid bytecode the types matches automatically */
			reference = ref;
			kind = ACCESSMETHOD;
			return true;
		}
		return false;
	}

	public boolean checkConstructorAccess() {
		ClassInfo clazzInfo = method.getClazzInfo();
		BytecodeInfo bytecode = method.getBytecode();
		String[] paramTypes = TypeSignature.getParameterTypes(method.getType());
		Handler[] excHandlers = bytecode.getExceptionHandlers();
		if (excHandlers != null && excHandlers.length != 0)
			return false;
		Iterator iter = bytecode.getInstructions().iterator();

		Instruction instr = (Instruction) iter.next();
		while (instr.getOpcode() == opc_nop && iter.hasNext())
			instr = (Instruction) iter.next();
		int params = 0, slot = 0;
		while (instr.getOpcode() >= opc_iload && instr.getOpcode() <= opc_aload) {

			if (instr.getLocalSlot() > slot && unifyParam == -1 && params > 0
					&& paramTypes[params - 1].charAt(0) == 'L') {
				unifyParam = params;
				params++;
				slot++;
			}
			if (instr.getLocalSlot() != slot)
				return false;

			params++;
			slot += (instr.getOpcode() == opc_lload || instr.getOpcode() == opc_dload) ? 2
					: 1;
			instr = (Instruction) iter.next();
		}
		if (params > 0 && instr.getOpcode() == opc_invokespecial) {

			if (unifyParam == -1 && params <= paramTypes.length
					&& paramTypes[params - 1].charAt(0) == 'L')
				unifyParam = params++;

			Reference ref = instr.getReference();
			ClassInfo refClazz = TypeSignature.getClassInfo(ref.getClazz());
			if (refClazz != clazzInfo)
				return false;
			MethodInfo refMethod = refClazz.findMethod(ref.getName(),
					ref.getType());
			MethodType refType = Type.tMethod(ref.getType());
			if ((refMethod.getModifiers() & modifierMask) != 0
					|| !refMethod.getName().equals("<init>")
					|| unifyParam == -1
					|| refType.getParameterTypes().length != params - 2)
				return false;

			instr = (Instruction) iter.next();
			if (instr.getOpcode() != opc_return)
				return false;

			/*
			 * We don't check if types matches. No problem since we only need to
			 * make sure, this constructor doesn't do anything more than relay
			 * to the real one.
			 */
			reference = ref;
			kind = ACCESSCONSTRUCTOR;
			return true;
		}
		return false;
	}
}
