/* CodeVerifier Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: CodeVerifier.java.in,v 1.4.2.4 2002/05/28 17:34:12 hoenicke Exp $
 */

package alterrs.jode.jvm;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;

import alterrs.jode.AssertError;
import alterrs.jode.GlobalOptions;
import alterrs.jode.bytecode.BytecodeInfo;
import alterrs.jode.bytecode.ClassInfo;
import alterrs.jode.bytecode.Handler;
import alterrs.jode.bytecode.Instruction;
import alterrs.jode.bytecode.MethodInfo;
import alterrs.jode.bytecode.Opcodes;
import alterrs.jode.bytecode.Reference;
import alterrs.jode.bytecode.TypeSignature;

public class CodeVerifier implements Opcodes {
	ClassInfo ci;
	MethodInfo mi;
	BytecodeInfo bi;

	String methodType;
	String returnType;

	static Type tNull = Type.tType("0");
	static Type tInt = Type.tType("I");
	static Type tLong = Type.tType("J");
	static Type tFloat = Type.tType("F");
	static Type tDouble = Type.tType("D");
	static Type tString = Type.tType("Ljava/lang/String;");
	static Type tNone = Type.tType("?");
	static Type tSecondPart = new Type("2");
	static Type tObject = new Type("Ljava/lang/Object;");

	/**
	 * We need some more types, than mentioned in jvm.
	 */
	private static class Type {
		/*
		 * "ZBCSIFJD" are the normal primitive types. "L...;" is normal class
		 * type. "[..." is normal array type "?" stands for type error "N...;"
		 * stands for new uninitialized type. "0" stands for null type. "R"
		 * stands for return address type. "2" stands for second half of a two
		 * word type.
		 */
		private String typeSig;

		/**
		 * The dependant instruction. This has two usages:
		 * <dl>
		 * <dt>"N...;"</dt>
		 * <dd>The new instruction, or null if this is the this param of
		 * &lt;init&gt;.</dd>
		 * <dt>"R"</dt>
		 * <dd>The <i>target</i> of the jsr.
		 */
		private Instruction instr;

		public Type(String typeSig) {
			this.typeSig = typeSig;
		}

		public Type(String typeSig, Instruction instr) {
			this.typeSig = typeSig;
			this.instr = instr;
		}

		public static Type tType(String typeSig) {
			// unify them?
			return new Type(typeSig);
		}

		public static Type tType(String typeSig, Instruction instr) {
			// unify them?
			return new Type(typeSig, instr);
		}

		public String getTypeSig() {
			return typeSig;
		}

		public Instruction getInstruction() {
			return instr;
		}

		/**
		 * @param t2
		 *            the type signature of the type to check for. This may be
		 *            one of the special signatures:
		 *            <dl>
		 *            <dt>"[*"
		 *            <dt>
		 *            <dd>array of something</dd>
		 *            <dt>"+"</dt>
		 *            <dd>(uninitialized) object/returnvalue type</dd>
		 *            <dt>"2", "R"</dt>
		 *            <dd>as the typeSig parameter</dd>
		 *            </dl>
		 * @return true, iff this is castable to t2 by a widening cast.
		 */
		public boolean isOfType(String destSig) {
			String thisSig = typeSig;
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_VERIFIER) != 0)
				GlobalOptions.err.println("isOfType(" + thisSig + "," + destSig
						+ ")");
			if (thisSig.equals(destSig))
				return true;

			char c1 = thisSig.charAt(0);
			char c2 = destSig.charAt(0);
			switch (c2) {
			case 'Z':
			case 'B':
			case 'C':
			case 'S':
			case 'I':
				/* integer type */
				return ("ZBCSI".indexOf(c1) >= 0);
			case '+':
				return ("L[nNR0".indexOf(c1) >= 0);

			case '[':
				if (c1 == '0')
					return true;
				while (c1 == '[' && c2 == '[') {
					thisSig = thisSig.substring(1);
					destSig = destSig.substring(1);
					c1 = thisSig.charAt(0);
					c2 = destSig.charAt(0);
				}

				if (c2 == '*')
					/* destType is array of unknowns */
					return true;
				/*
				 * Note that short[] is only compatible to short[], therefore we
				 * only need to handle Object types specially.
				 */

				if (c2 != 'L')
					return false;
				/* fall through */
			case 'L':
				if (c1 == '0')
					return true;
				if ("L[".indexOf(c1) < 0)
					return false;

				ClassInfo wantedType = TypeSignature.getClassInfo(destSig);
				if (wantedType.isInterface()
						|| wantedType == ClassInfo.javaLangObject)
					return true;
				if (c1 == 'L')
					return wantedType.superClassOf(TypeSignature
							.getClassInfo(thisSig));
			}
			return false;
		}

		/**
		 * @return The common super type of this and type2.
		 */
		public Type mergeType(Type type2) {
			String sig1 = typeSig;
			String sig2 = type2.typeSig;

			if (this.equals(type2))
				return this;

			char c1 = sig1.charAt(0);
			char c2 = sig2.charAt(0);
			if (c1 == '*')
				return type2;
			if (c2 == '*')
				return this;
			if ("ZBCSI".indexOf(c1) >= 0 && "ZBCSI".indexOf(c2) >= 0)
				return this;

			if (c1 == '0')
				return ("L[0".indexOf(c2) >= 0) ? type2 : tNone;
			if (c2 == '0')
				return ("L[".indexOf(c1) >= 0) ? this : tNone;

			int dimensions = 0;
			/*
			 * Note that short[] is only compatible to short[], therefore we
			 * make the array handling after the primitive type handling. Also
			 * note that we don't allow arrays of special types.
			 */
			while (c1 == '[' && c2 == '[') {
				sig1 = sig1.substring(1);
				sig2 = sig2.substring(1);
				c1 = sig1.charAt(0);
				c2 = sig2.charAt(0);
				dimensions++;
			}

			// One of them is array now, the other is an object,
			// the common super is tObject
			if ((c1 == '[' && c2 == 'L') || (c1 == 'L' && c2 == '[')) {
				if (dimensions == 0)
					return tObject;
				StringBuffer result = new StringBuffer(dimensions + 18);
				for (int i = 0; i < dimensions; i++)
					result.append("[");
				result.append("Ljava/lang/Object;");
				return tType(result.toString());
			}

			if (c1 == 'L' && c2 == 'L') {
				ClassInfo clazz1 = TypeSignature.getClassInfo(sig1);
				ClassInfo clazz2 = TypeSignature.getClassInfo(sig2);
				if (clazz1.superClassOf(clazz2))
					return this;
				if (clazz2.superClassOf(clazz1))
					return type2;
				do {
					clazz1 = clazz1.getSuperclass();
				} while (!clazz1.superClassOf(clazz2));
				StringBuffer result = new StringBuffer(dimensions
						+ clazz1.getName().length() + 2);
				for (int i = 0; i < dimensions; i++)
					result.append("[");
				result.append("L").append(clazz1.getName().replace('.', '/'))
						.append(";");
				return tType(result.toString());
			}

			// Both were arrays, but of different primitive types. The
			// common super is tObject with one dimension less.
			if (dimensions > 0) {
				if (dimensions == 1)
					return tObject;
				StringBuffer result = new StringBuffer(dimensions + 17);
				for (int i = 0; i < dimensions - 1; i++)
					result.append("[");
				result.append("Ljava/lang/Object;");
				return tType(result.toString());
			}
			return tNone;
		}

		public boolean equals(Object other) {
			if (other instanceof Type) {
				Type type2 = (Type) other;
				return typeSig.equals(type2.typeSig) && instr == type2.instr;
			}
			return false;
		}

		public String toString() {
			if (instr != null)
				return typeSig + "@" + instr.getAddr();
			return typeSig;
		}
	}

	/**
	 * JLS 4.9.6: Verifying code that contains a finally clause: - Each
	 * instruction keeps track of the list of jsr targets. - For each
	 * instruction and each jsr needed to reach that instruction a bit vector is
	 * maintained of all local vars accessed or modified.
	 */

	class VerifyInfo implements Cloneable {
		Type[] stack = new Type[bi.getMaxStack()];
		Type[] locals = new Type[bi.getMaxLocals()];
		Instruction[] jsrTargets = null;
		BitSet[] jsrLocals = null;
		int stackHeight = 0;
		int maxHeight = 0;
		/*
		 * If this is a jsr target, this field contains the single allowed ret
		 * instruction.
		 */Instruction retInstr = null;

		public Object clone() {
			try {
				VerifyInfo result = (VerifyInfo) super.clone();
				result.stack = (Type[]) stack.clone();
				result.locals = (Type[]) locals.clone();
				return result;
			} catch (CloneNotSupportedException ex) {
				throw new AssertError("Clone not supported?");
			}
		}

		public final void reserve(int count) throws VerifyException {
			if (stackHeight + count > maxHeight) {
				maxHeight = stackHeight + count;
				if (maxHeight > stack.length)
					throw new VerifyException("stack overflow");
			}
		}

		public final void need(int count) throws VerifyException {
			if (stackHeight < count)
				throw new VerifyException("stack underflow");
		}

		public final void push(Type type) throws VerifyException {
			reserve(1);
			stack[stackHeight++] = type;
		}

		public final Type pop() throws VerifyException {
			need(1);
			return stack[--stackHeight];
		}

		public String toString() {
			StringBuffer result = new StringBuffer("locals:[");
			String comma = "";
			for (int i = 0; i < locals.length; i++) {
				result.append(comma).append(i).append(':');
				result.append(locals[i]);
				comma = ",";
			}
			result.append("], stack:[");
			comma = "";
			for (int i = 0; i < stackHeight; i++) {
				result.append(comma).append(stack[i]);
				comma = ",";
			}
			if (jsrTargets != null) {
				result.append("], jsrs:[");
				comma = "";
				for (int i = 0; i < jsrTargets.length; i++) {
					result.append(comma).append(jsrTargets[i])
							.append(jsrLocals[i]);
					comma = ",";
				}
			}
			return result.append("]").toString();
		}
	}

	public CodeVerifier(ClassInfo ci, MethodInfo mi, BytecodeInfo bi) {
		this.ci = ci;
		this.mi = mi;
		this.bi = bi;
		this.methodType = mi.getType();
		this.returnType = TypeSignature.getReturnType(methodType);
	}

	public VerifyInfo initInfo() {
		VerifyInfo info = new VerifyInfo();
		int pos = 1;
		int slot = 0;
		if (!mi.isStatic()) {
			String clazzName = ci.getName().replace('.', '/');
			if (mi.getName().equals("<init>"))
				info.locals[slot++] = Type.tType("N" + clazzName + ";", null);
			else
				info.locals[slot++] = Type.tType("L" + clazzName + ";");
		}
		while (methodType.charAt(pos) != ')') {
			int start = pos;
			pos = TypeSignature.skipType(methodType, pos);
			String paramType = methodType.substring(start, pos);
			info.locals[slot++] = Type.tType(paramType);
			if (TypeSignature.getTypeSize(paramType) == 2)
				info.locals[slot++] = tSecondPart;
		}
		while (slot < bi.getMaxLocals())
			info.locals[slot++] = tNone;
		return info;
	}

	public boolean mergeInfo(Instruction instr, VerifyInfo info)
			throws VerifyException {
		if (instr.getTmpInfo() == null) {
			instr.setTmpInfo(info);
			return true;
		}
		boolean changed = false;
		VerifyInfo oldInfo = (VerifyInfo) instr.getTmpInfo();
		if (oldInfo.stackHeight != info.stackHeight)
			throw new VerifyException("Stack height differ at: "
					+ instr.getDescription());
		for (int i = 0; i < oldInfo.stackHeight; i++) {
			Type newType = oldInfo.stack[i].mergeType(info.stack[i]);
			if (!newType.equals(oldInfo.stack[i])) {
				if (newType == tNone)
					throw new VerifyException("Type error while merging: "
							+ oldInfo.stack[i] + " and " + info.stack[i]);
				changed = true;
				oldInfo.stack[i] = newType;
			}
		}
		for (int i = 0; i < bi.getMaxLocals(); i++) {
			Type newType = oldInfo.locals[i].mergeType(info.locals[i]);
			if (!newType.equals(oldInfo.locals[i])) {
				changed = true;
				oldInfo.locals[i] = newType;
			}
		}
		if (oldInfo.jsrTargets != null) {
			int jsrDepth;
			if (info.jsrTargets == null)
				jsrDepth = 0;
			else {
				jsrDepth = info.jsrTargets.length;
				int infoPtr = 0;
				oldInfo_loop: for (int oldInfoPtr = 0; oldInfoPtr < oldInfo.jsrTargets.length; oldInfoPtr++) {
					for (int i = infoPtr; i < jsrDepth; i++) {
						if (oldInfo.jsrTargets[oldInfoPtr] == info.jsrTargets[i]) {
							System.arraycopy(info.jsrTargets, i,
									info.jsrTargets, infoPtr, jsrDepth - i);
							jsrDepth -= (i - infoPtr);
							infoPtr++;
							continue oldInfo_loop;
						}
					}
				}
				jsrDepth = infoPtr;
			}
			if (jsrDepth != oldInfo.jsrTargets.length) {
				if (jsrDepth == 0)
					oldInfo.jsrTargets = null;
				else {
					oldInfo.jsrTargets = new Instruction[jsrDepth];
					System.arraycopy(info.jsrTargets, 0, oldInfo.jsrTargets, 0,
							jsrDepth);
				}
				changed = true;
			}
		}
		return changed;
	}

	String[] types = { "I", "J", "F", "D", "+", "B", "C", "S" };
	String[] arrayTypes = { "[I", "[J", "[F", "[D", "[Ljava/lang/Object;",
			"[B", "[C", "[S" };

	public VerifyInfo modelEffect(Instruction instr, VerifyInfo prevInfo)
			throws VerifyException {
		int jsrLength = prevInfo.jsrTargets != null ? prevInfo.jsrTargets.length
				: 0;
		VerifyInfo result = (VerifyInfo) prevInfo.clone();
		int opcode = instr.getOpcode();
		switch (opcode) {
		case opc_nop:
		case opc_goto:
			break;
		case opc_ldc: {
			Type type;
			Object constant = instr.getConstant();
			if (constant == null)
				type = tNull;
			else if (constant instanceof Integer)
				type = tInt;
			else if (constant instanceof Float)
				type = tFloat;
			else
				type = tString;
			result.push(type);
			break;
		}
		case opc_ldc2_w: {
			Type type;
			Object constant = instr.getConstant();
			if (constant instanceof Long)
				type = tLong;
			else
				type = tDouble;
			result.push(type);
			result.push(tSecondPart);
			break;
		}
		case opc_iload:
		case opc_lload:
		case opc_fload:
		case opc_dload:
		case opc_aload: {
			if (jsrLength > 0
					&& (!result.jsrLocals[jsrLength - 1].get(instr
							.getLocalSlot()) || ((opcode & 0x1) == 0 && !result.jsrLocals[jsrLength - 1]
							.get(instr.getLocalSlot() + 1)))) {
				result.jsrLocals = (BitSet[]) result.jsrLocals.clone();
				result.jsrLocals[jsrLength - 1] = (BitSet) result.jsrLocals[jsrLength - 1]
						.clone();
				result.jsrLocals[jsrLength - 1].set(instr.getLocalSlot());
				if ((opcode & 0x1) == 0)
					result.jsrLocals[jsrLength - 1]
							.set(instr.getLocalSlot() + 1);
			}
			if ((opcode & 0x1) == 0
					&& result.locals[instr.getLocalSlot() + 1] != tSecondPart)
				throw new VerifyException(instr.getDescription());
			Type type = result.locals[instr.getLocalSlot()];
			if (!type.isOfType(types[opcode - opc_iload]))
				throw new VerifyException(instr.getDescription());
			result.push(type);
			if ((opcode & 0x1) == 0)
				result.push(tSecondPart);
			break;
		}
		case opc_iaload:
		case opc_laload:
		case opc_faload:
		case opc_daload:
		case opc_aaload:
		case opc_baload:
		case opc_caload:
		case opc_saload: {
			if (!result.pop().isOfType("I"))
				throw new VerifyException(instr.getDescription());
			Type arrType = result.pop();
			if (!arrType.isOfType(arrayTypes[opcode - opc_iaload])
					&& (opcode != opc_baload || !arrType.isOfType("[Z")))
				throw new VerifyException(instr.getDescription());

			String typeSig = arrType.getTypeSig();
			Type elemType = (typeSig.charAt(0) == '[' ? Type.tType(typeSig
					.substring(1)) : (opcode == opc_aaload ? tNull : Type
					.tType(types[opcode - opc_iaload])));
			result.push(elemType);
			if (((1 << opcode - opc_iaload) & 0xa) != 0)
				result.push(tSecondPart);
			break;
		}
		case opc_istore:
		case opc_lstore:
		case opc_fstore:
		case opc_dstore:
		case opc_astore: {
			if (jsrLength > 0
					&& (!result.jsrLocals[jsrLength - 1].get(instr
							.getLocalSlot()) || ((opcode & 0x1) != 0 && !result.jsrLocals[jsrLength - 1]
							.get(instr.getLocalSlot() + 1)))) {
				result.jsrLocals = (BitSet[]) result.jsrLocals.clone();
				result.jsrLocals[jsrLength - 1] = (BitSet) result.jsrLocals[jsrLength - 1]
						.clone();
				result.jsrLocals[jsrLength - 1].set(instr.getLocalSlot());
				if ((opcode & 0x1) != 0)
					result.jsrLocals[jsrLength - 1]
							.set(instr.getLocalSlot() + 1);
			}
			if ((opcode & 0x1) != 0 && result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			Type type = result.pop();
			if (!type.isOfType(types[opcode - opc_istore]))
				if (opcode != opc_astore || !type.isOfType("R"))
					throw new VerifyException(instr.getDescription());
			result.locals[instr.getLocalSlot()] = type;
			if ((opcode & 0x1) != 0)
				result.locals[instr.getLocalSlot() + 1] = tSecondPart;
			break;
		}
		case opc_iastore:
		case opc_lastore:
		case opc_fastore:
		case opc_dastore:
		case opc_aastore:
		case opc_bastore:
		case opc_castore:
		case opc_sastore: {
			if (((1 << opcode - opc_iastore) & 0xa) != 0
					&& result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			Type type = result.pop();
			if (!result.pop().isOfType("I"))
				throw new VerifyException(instr.getDescription());
			Type arrType = result.pop();
			if (!arrType.isOfType(arrayTypes[opcode - opc_iastore])
					&& (opcode != opc_bastore || !arrType.isOfType("[Z")))
				throw new VerifyException(instr.getDescription());
			String elemType = opcode >= opc_bastore ? "I" : types[opcode
					- opc_iastore];
			if (!type.isOfType(elemType))
				throw new VerifyException(instr.getDescription());
			break;
		}
		case opc_pop:
		case opc_pop2: {
			int count = opcode - (opc_pop - 1);
			result.need(count);
			result.stackHeight -= count;
			break;
		}
		case opc_dup:
		case opc_dup_x1:
		case opc_dup_x2: {
			int depth = opcode - opc_dup;
			result.reserve(1);
			result.need(depth + 1);
			if (result.stack[result.stackHeight - 1] == tSecondPart)
				throw new VerifyException(instr.getDescription());

			int stackdepth = result.stackHeight - (depth + 1);
			if (result.stack[stackdepth] == tSecondPart)
				throw new VerifyException(instr.getDescription()
						+ " on long or double");
			for (int i = result.stackHeight; i > stackdepth; i--)
				result.stack[i] = result.stack[i - 1];
			result.stack[stackdepth] = result.stack[result.stackHeight++];
			break;
		}
		case opc_dup2:
		case opc_dup2_x1:
		case opc_dup2_x2: {
			int depth = opcode - opc_dup2;
			result.reserve(2);
			result.need(depth + 2);
			if (result.stack[result.stackHeight - 2] == tSecondPart)
				throw new VerifyException(instr.getDescription()
						+ " on misaligned long or double");
			int stacktop = result.stackHeight;
			int stackdepth = stacktop - (depth + 2);
			if (result.stack[stackdepth] == tSecondPart)
				throw new VerifyException(instr.getDescription()
						+ " on long or double");
			for (int i = stacktop; i > stackdepth; i--)
				result.stack[i + 1] = result.stack[i - 1];
			result.stack[stackdepth + 1] = result.stack[stacktop + 1];
			result.stack[stackdepth] = result.stack[stacktop];
			result.stackHeight += 2;
			break;
		}
		case opc_swap: {
			result.need(2);
			if (result.stack[result.stackHeight - 2] == tSecondPart
					|| result.stack[result.stackHeight - 1] == tSecondPart)
				throw new VerifyException(instr.getDescription()
						+ " on misaligned long or double");
			Type tmp = result.stack[result.stackHeight - 1];
			result.stack[result.stackHeight - 1] = result.stack[result.stackHeight - 2];
			result.stack[result.stackHeight - 2] = tmp;
			break;
		}
		case opc_iadd:
		case opc_ladd:
		case opc_fadd:
		case opc_dadd:
		case opc_isub:
		case opc_lsub:
		case opc_fsub:
		case opc_dsub:
		case opc_imul:
		case opc_lmul:
		case opc_fmul:
		case opc_dmul:
		case opc_idiv:
		case opc_ldiv:
		case opc_fdiv:
		case opc_ddiv:
		case opc_irem:
		case opc_lrem:
		case opc_frem:
		case opc_drem: {
			String type = types[(opcode - opc_iadd) & 3];
			if ((opcode & 1) != 0 && result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType(type))
				throw new VerifyException(instr.getDescription());
			if ((opcode & 1) != 0) {
				result.need(2);
				if (result.stack[result.stackHeight - 1] != tSecondPart
						|| !result.stack[result.stackHeight - 2].isOfType(type))
					throw new VerifyException(instr.getDescription());
			} else {
				result.need(1);
				if (!result.stack[result.stackHeight - 1].isOfType(type))
					throw new VerifyException(instr.getDescription());
			}
			break;
		}
		case opc_ineg:
		case opc_lneg:
		case opc_fneg:
		case opc_dneg: {
			String type = types[(opcode - opc_ineg) & 3];
			if ((opcode & 1) != 0) {
				result.need(2);
				if (result.stack[result.stackHeight - 1] != tSecondPart
						|| !result.stack[result.stackHeight - 2].isOfType(type))
					throw new VerifyException(instr.getDescription());
			} else {
				result.need(1);
				if (!result.stack[result.stackHeight - 1].isOfType(type))
					throw new VerifyException(instr.getDescription());
			}
			break;
		}
		case opc_ishl:
		case opc_lshl:
		case opc_ishr:
		case opc_lshr:
		case opc_iushr:
		case opc_lushr:
			if (!result.pop().isOfType("I"))
				throw new VerifyException(instr.getDescription());

			if ((opcode & 1) != 0) {
				result.need(2);
				if (result.stack[result.stackHeight - 1] != tSecondPart
						|| !result.stack[result.stackHeight - 2].isOfType("J"))
					throw new VerifyException(instr.getDescription());
			} else {
				result.need(1);
				if (!result.stack[result.stackHeight - 1].isOfType("I"))
					throw new VerifyException(instr.getDescription());
			}
			break;

		case opc_iand:
		case opc_land:
		case opc_ior:
		case opc_lor:
		case opc_ixor:
		case opc_lxor:
			if ((opcode & 1) != 0 && result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType(types[opcode & 1]))
				throw new VerifyException(instr.getDescription());
			if ((opcode & 1) != 0) {
				result.need(2);
				if (result.stack[result.stackHeight - 1] != tSecondPart
						|| !result.stack[result.stackHeight - 2].isOfType("J"))
					throw new VerifyException(instr.getDescription());
			} else {
				result.need(1);
				if (!result.stack[result.stackHeight - 1].isOfType("I"))
					throw new VerifyException(instr.getDescription());
			}
			break;

		case opc_iinc:
			if (!result.locals[instr.getLocalSlot()].isOfType("I"))
				throw new VerifyException(instr.getDescription());
			break;
		case opc_i2l:
		case opc_i2f:
		case opc_i2d:
		case opc_l2i:
		case opc_l2f:
		case opc_l2d:
		case opc_f2i:
		case opc_f2l:
		case opc_f2d:
		case opc_d2i:
		case opc_d2l:
		case opc_d2f: {
			int from = (opcode - opc_i2l) / 3;
			int to = (opcode - opc_i2l) % 3;
			if (to >= from)
				to++;
			if ((from & 1) != 0 && result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType(types[from]))
				throw new VerifyException(instr.getDescription());

			result.push(Type.tType(types[to]));
			if ((to & 1) != 0)
				result.push(tSecondPart);
			break;
		}
		case opc_i2b:
		case opc_i2c:
		case opc_i2s:
			result.need(1);
			if (!result.stack[result.stackHeight - 1].isOfType("I"))
				throw new VerifyException(instr.getDescription());
			break;

		case opc_lcmp:
			if (result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType("J"))
				throw new VerifyException(instr.getDescription());
			if (result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType("J"))
				throw new VerifyException(instr.getDescription());
			result.push(tInt);
			break;
		case opc_dcmpl:
		case opc_dcmpg:
			if (result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType("D"))
				throw new VerifyException(instr.getDescription());
			if (result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType("D"))
				throw new VerifyException(instr.getDescription());
			result.push(tInt);
			break;
		case opc_fcmpl:
		case opc_fcmpg:
			if (!result.pop().isOfType("F"))
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType("F"))
				throw new VerifyException(instr.getDescription());
			result.push(tInt);
			break;

		case opc_ifeq:
		case opc_ifne:
		case opc_iflt:
		case opc_ifge:
		case opc_ifgt:
		case opc_ifle:
		case opc_tableswitch:
		case opc_lookupswitch:
			if (!result.pop().isOfType("I"))
				throw new VerifyException(instr.getDescription());
			break;

		case opc_if_icmpeq:
		case opc_if_icmpne:
		case opc_if_icmplt:
		case opc_if_icmpge:
		case opc_if_icmpgt:
		case opc_if_icmple:
			if (!result.pop().isOfType("I"))
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType("I"))
				throw new VerifyException(instr.getDescription());
			break;
		case opc_if_acmpeq:
		case opc_if_acmpne:
			if (!result.pop().isOfType("+"))
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType("+"))
				throw new VerifyException(instr.getDescription());
			break;
		case opc_ifnull:
		case opc_ifnonnull:
			if (!result.pop().isOfType("+"))
				throw new VerifyException(instr.getDescription());
			break;

		case opc_ireturn:
		case opc_lreturn:
		case opc_freturn:
		case opc_dreturn:
		case opc_areturn: {
			if (((1 << opcode - opc_ireturn) & 0xa) != 0
					&& result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			Type type = result.pop();
			if (!type.isOfType(types[opcode - opc_ireturn])
					|| !type.isOfType(TypeSignature.getReturnType(methodType)))
				throw new VerifyException(instr.getDescription());
			break;
		}
		case opc_jsr: {
			Instruction jsrTarget = instr.getSingleSucc();
			result.stack[result.stackHeight++] = Type.tType("R", jsrTarget);
			result.jsrTargets = new Instruction[jsrLength + 1];
			result.jsrLocals = new BitSet[jsrLength + 1];
			if (jsrLength > 0) {
				for (int i = 0; i < prevInfo.jsrTargets.length; i++)
					if (prevInfo.jsrTargets[i] == instr.getSingleSucc())
						throw new VerifyException(instr.getDescription()
								+ " is recursive");
				System.arraycopy(prevInfo.jsrTargets, 0, result.jsrTargets, 0,
						jsrLength);
				System.arraycopy(prevInfo.jsrLocals, 0, result.jsrLocals, 0,
						jsrLength);
			}
			result.jsrTargets[jsrLength] = instr.getSingleSucc();
			result.jsrLocals[jsrLength] = new BitSet();
			break;
		}
		case opc_return:
			if (!returnType.equals("V"))
				throw new VerifyException(instr.getDescription());
			break;
		case opc_getstatic: {
			Reference ref = instr.getReference();
			String type = ref.getType();
			result.push(Type.tType(type));
			if (TypeSignature.getTypeSize(type) == 2)
				result.push(tSecondPart);
			break;
		}
		case opc_getfield: {
			Reference ref = instr.getReference();
			String classType = ref.getClazz();
			if (!result.pop().isOfType(classType))
				throw new VerifyException(instr.getDescription());
			String type = ref.getType();
			result.push(Type.tType(type));
			if (TypeSignature.getTypeSize(type) == 2)
				result.push(tSecondPart);
			break;
		}
		case opc_putstatic: {
			Reference ref = instr.getReference();
			String type = ref.getType();
			if (TypeSignature.getTypeSize(type) == 2
					&& result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType(type))
				throw new VerifyException(instr.getDescription());
			break;
		}
		case opc_putfield: {
			Reference ref = instr.getReference();
			String type = ref.getType();
			if (TypeSignature.getTypeSize(type) == 2
					&& result.pop() != tSecondPart)
				throw new VerifyException(instr.getDescription());
			if (!result.pop().isOfType(type))
				throw new VerifyException(instr.getDescription());
			String classType = ref.getClazz();
			if (!result.pop().isOfType(classType))
				throw new VerifyException(instr.getDescription());
			break;
		}
		case opc_invokevirtual:
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface: {
			Reference ref = instr.getReference();
			String refmt = ref.getType();
			String[] paramTypes = TypeSignature.getParameterTypes(refmt);
			for (int i = paramTypes.length - 1; i >= 0; i--) {
				if (TypeSignature.getTypeSize(paramTypes[i]) == 2
						&& result.pop() != tSecondPart)
					throw new VerifyException(instr.getDescription());
				if (!result.pop().isOfType(paramTypes[i]))
					throw new VerifyException(instr.getDescription());
			}
			if (ref.getName().equals("<init>")) {
				Type clazz = result.pop();
				String typeSig = clazz.getTypeSig();
				String refClazz = ref.getClazz();
				if (opcode != opc_invokespecial || typeSig.charAt(0) != 'N'
						|| refClazz.charAt(0) != 'L')
					throw new VerifyException(instr.getDescription());
				if (!typeSig.substring(1).equals(refClazz.substring(1))) {
					ClassInfo uci = ClassInfo.forName(typeSig.substring(1,
							typeSig.length() - 1).replace('/', '.'));
					if (uci.getSuperclass() != TypeSignature
							.getClassInfo(refClazz)
							|| clazz.getInstruction() != null)
						throw new VerifyException(instr.getDescription());
				}
				Type newType = Type.tType("L" + typeSig.substring(1));
				for (int i = 0; i < result.stackHeight; i++)
					if (result.stack[i] == clazz)
						result.stack[i] = newType;
				for (int i = 0; i < result.locals.length; i++)
					if (result.locals[i] == clazz)
						result.locals[i] = newType;
			} else if (opcode != opc_invokestatic) {
				String classType = ref.getClazz();
				if (!result.pop().isOfType(classType))
					throw new VerifyException(instr.getDescription());
			}
			String type = TypeSignature.getReturnType(refmt);
			if (!type.equals("V")) {
				result.push(Type.tType(type));
				if (TypeSignature.getTypeSize(type) == 2)
					result.push(tSecondPart);
			}
			break;
		}
		case opc_new: {
			String clName = instr.getClazzType();
			result.stack[result.stackHeight++] = Type.tType(
					"N" + clName.substring(1), instr);
			break;
		}
		case opc_arraylength: {
			if (!result.pop().isOfType("[*"))
				throw new VerifyException(instr.getDescription());
			result.push(tInt);
			break;
		}
		case opc_athrow: {
			if (!result.pop().isOfType("Ljava/lang/Throwable;"))
				throw new VerifyException(instr.getDescription());
			break;
		}
		case opc_checkcast: {
			String classType = instr.getClazzType();
			if (!result.pop().isOfType("+"))
				throw new VerifyException(instr.getDescription());
			result.push(Type.tType(classType));
			break;
		}
		case opc_instanceof: {
			if (!result.pop().isOfType("Ljava/lang/Object;"))
				throw new VerifyException(instr.getDescription());
			result.push(tInt);
			break;
		}
		case opc_monitorenter:
		case opc_monitorexit:
			if (!result.pop().isOfType("Ljava/lang/Object;"))
				throw new VerifyException(instr.getDescription());
			break;
		case opc_multianewarray: {
			int dimension = instr.getDimensions();
			for (int i = dimension - 1; i >= 0; i--)
				if (!result.pop().isOfType("I"))
					throw new VerifyException(instr.getDescription());
			String classType = instr.getClazzType();
			result.push(Type.tType(classType));
			break;
		}
		default:
			throw new AssertError("Invalid opcode " + opcode);
		}
		return result;
	}

	public void doVerify() throws VerifyException {
		HashSet todoSet = new HashSet();

		Instruction firstInstr = (Instruction) bi.getInstructions().get(0);
		firstInstr.setTmpInfo(initInfo());
		todoSet.add(firstInstr);
		Handler[] handlers = bi.getExceptionHandlers();
		while (!todoSet.isEmpty()) {
			Iterator iter = todoSet.iterator();
			Instruction instr = (Instruction) iter.next();
			iter.remove();
			if (!instr.doesAlwaysJump() && instr.getNextByAddr() == null)
				throw new VerifyException("Flow can fall off end of method");

			VerifyInfo prevInfo = (VerifyInfo) instr.getTmpInfo();
			int opcode = instr.getOpcode();
			if (opcode == opc_ret) {
				Type retVarType = prevInfo.locals[instr.getLocalSlot()];
				if (prevInfo.jsrTargets == null || !retVarType.isOfType("R"))
					throw new VerifyException(instr.getDescription());
				int jsrLength = prevInfo.jsrTargets.length - 1;
				Instruction jsrTarget = retVarType.getInstruction();
				while (jsrTarget != prevInfo.jsrTargets[jsrLength])
					if (--jsrLength < 0)
						throw new VerifyException(instr.getDescription());
				VerifyInfo jsrTargetInfo = (VerifyInfo) jsrTarget.getTmpInfo();
				if (jsrTargetInfo.retInstr == null)
					jsrTargetInfo.retInstr = instr;
				else if (jsrTargetInfo.retInstr != instr)
					throw new VerifyException(
							"JsrTarget has more than one ret: "
									+ jsrTarget.getDescription());
				Instruction[] nextTargets;
				BitSet[] nextLocals;
				if (jsrLength > 0) {
					nextTargets = new Instruction[jsrLength];
					nextLocals = new BitSet[jsrLength];
					System.arraycopy(prevInfo.jsrTargets, 0, nextTargets, 0,
							jsrLength);
					System.arraycopy(prevInfo.jsrLocals, 0, nextLocals, 0,
							jsrLength);
				} else {
					nextTargets = null;
					nextLocals = null;
				}
				for (int i = 0; i < jsrTarget.getPreds().length; i++) {
					Instruction jsrInstr = jsrTarget.getPreds()[i];
					if (jsrInstr.getTmpInfo() != null)
						todoSet.add(jsrInstr);
				}
			} else {
				VerifyInfo info = modelEffect(instr, prevInfo);
				if (!instr.doesAlwaysJump())
					if (mergeInfo(instr.getNextByAddr(), info))
						todoSet.add(instr.getNextByAddr());
				if (opcode == opc_jsr) {
					VerifyInfo targetInfo = (VerifyInfo) instr.getSingleSucc()
							.getTmpInfo();
					if (targetInfo != null && targetInfo.retInstr != null) {
						VerifyInfo afterJsrInfo = (VerifyInfo) prevInfo.clone();
						VerifyInfo retInfo = (VerifyInfo) targetInfo.retInstr
								.getTmpInfo();
						BitSet usedLocals = retInfo.jsrLocals[retInfo.jsrLocals.length - 1];
						for (int j = 0; j < bi.getMaxLocals(); j++) {
							if (usedLocals.get(j))
								afterJsrInfo.locals[j] = retInfo.locals[j];
						}
						if (mergeInfo(instr.getNextByAddr(), afterJsrInfo))
							todoSet.add(instr.getNextByAddr());
					}
				}
				if (instr.getSuccs() != null) {
					for (int i = 0; i < instr.getSuccs().length; i++) {
						if (mergeInfo(instr.getSuccs()[i],
								(VerifyInfo) info.clone()))
							todoSet.add(instr.getSuccs()[i]);
					}
				}
				for (int i = 0; i < handlers.length; i++) {
					if (handlers[i].start.compareTo(instr) <= 0
							&& handlers[i].end.compareTo(instr) >= 0) {
						VerifyInfo excInfo = (VerifyInfo) prevInfo.clone();
						excInfo.stackHeight = 1;
						if (handlers[i].type != null)
							excInfo.stack[0] = Type.tType("L"
									+ handlers[i].type.replace('.', '/') + ";");
						else
							excInfo.stack[0] = Type
									.tType("Ljava/lang/Throwable;");
						if (mergeInfo(handlers[i].catcher, excInfo))
							todoSet.add(handlers[i].catcher);
					}
				}
			}
		}

		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_VERIFIER) != 0) {
			for (Iterator i = bi.getInstructions().iterator(); i.hasNext();) {
				Instruction instr = (Instruction) i.next();

				VerifyInfo info = (VerifyInfo) instr.getTmpInfo();
				if (info != null)
					GlobalOptions.err.println(info.toString());
				GlobalOptions.err.println(instr.getDescription());
			}
		}
		for (Iterator i = bi.getInstructions().iterator(); i.hasNext();) {
			Instruction instr = (Instruction) i.next();
			instr.setTmpInfo(null);
		}
	}

	public void verify() throws VerifyException {
		try {
			doVerify();
		} catch (VerifyException ex) {
			for (Iterator i = bi.getInstructions().iterator(); i.hasNext();) {
				Instruction instr = (Instruction) i.next();
				VerifyInfo info = (VerifyInfo) instr.getTmpInfo();
				if (info != null)
					GlobalOptions.err.println(info.toString());
				GlobalOptions.err.println(instr.getDescription());

				instr.setTmpInfo(null);
			}
			throw ex;
		}
	}
}
