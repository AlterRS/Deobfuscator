/* BytecodeInfo Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: BytecodeInfo.java.in,v 4.9.2.8 2002/06/11 08:40:31 hoenicke Exp $
 */

package jode.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import jode.GlobalOptions;


/**
 * This class represents the byte code of a method. Each instruction is stored
 * in an Instruction instance.
 * <p/>
 * We canonicalize some opcodes: wide opcodes are mapped to short ones, opcodes
 * that load a constant are mapped to opc_ldc or opc_ldc2_w, and opc_xload_x /
 * opc_xstore_x opcodes are mapped to opc_xload / opc_xstore.
 */
public class BytecodeInfo extends BinaryInfo implements Opcodes {

	private MethodInfo methodInfo;
	private int maxStack, maxLocals;
	private Handler[] exceptionHandlers;
	private LocalVariableInfo[] lvt;
	private LineNumber[] lnt;

	/**
	 * A array of instructions, indexed by address. This array is only valid
	 * while reading the code.
	 */
	private Instruction[] instrs;

	private InstructionList instructions;

	private class InstructionList extends AbstractSequentialList {
		Instruction borderInstr;
		int instructionCount = 0;

		InstructionList() {
			// opc_impdep1 is used as border instruction, it may not
			// occur in bytecode.
			borderInstr = new Instruction(opc_impdep1);
			borderInstr.nextByAddr = borderInstr.prevByAddr = borderInstr;
		}

		public int size() {
			return instructionCount;
		}

		Instruction get0(int index) {
			Instruction instr = borderInstr;
			if (index < instructionCount / 2) {
				for (int i = 0; i <= index; i++)
					instr = instr.nextByAddr;
			} else {
				for (int i = instructionCount; i > index; i--)
					instr = instr.prevByAddr;
			}
			return instr;
		}

		public Object get(int index) {
			if (index < 0 || index >= instructionCount)
				throw new IllegalArgumentException();
			return get0(index);
		}

		public boolean add(Object o) {
			/* optimize add, since it is called many times by read() */
			instructionCount++;
			borderInstr.prevByAddr.appendInstruction((Instruction) o,
					BytecodeInfo.this);
			return true;
		}

		public ListIterator listIterator(final int startIndex) {
			if (startIndex < 0 || startIndex > instructionCount)
				throw new IllegalArgumentException();
			return new ListIterator() {
				Instruction instr = get0(startIndex);
				Instruction toRemove = null;
				int index = startIndex;

				public boolean hasNext() {
					return index < instructionCount;
				}

				public boolean hasPrevious() {
					return index > 0;
				}

				public Object next() {
					if (index >= instructionCount)
						throw new NoSuchElementException();
					index++;
					toRemove = instr;
					instr = instr.nextByAddr;
					// System.err.println("next: "+toRemove.getDescription());
					return toRemove;
				}

				public Object previous() {
					if (index == 0)
						throw new NoSuchElementException();
					index--;
					instr = instr.prevByAddr;
					toRemove = instr;
					// System.err.println("prev: "+toRemove.getDescription());
					return toRemove;
				}

				public int nextIndex() {
					return index;
				}

				public int previousIndex() {
					return index - 1;
				}

				public void remove() {
					if (toRemove == null)
						throw new IllegalStateException();
					// System.err.println("remove: "+toRemove.getDescription());
					instructionCount--;
					if (instr == toRemove)
						instr = instr.nextByAddr;
					else
						index--;
					toRemove.removeInstruction(BytecodeInfo.this);
					toRemove = null;
				}

				public void add(Object o) {
					instructionCount++;
					index++;
					// System.err.println("add: "
					// +((Instruction)o).getDescription()
					// +" after "+instr.prevByAddr
					// .getDescription());
					instr.prevByAddr.appendInstruction((Instruction) o,
							BytecodeInfo.this);
					toRemove = null;
				}

				public void set(Object o) {
					if (toRemove == null)
						throw new IllegalStateException();
					// System.err.println("replace "+toRemove.getDescription()
					// +" with "
					// +((Instruction)o).getDescription());
					toRemove.replaceInstruction((Instruction) o,
							BytecodeInfo.this);
					if (instr == toRemove)
						instr = (Instruction) o;
					toRemove = (Instruction) o;
				}
			};
		}

		void setLastAddr(int addr) {
			borderInstr.setAddr(addr);
		}

		int getCodeLength() {
			return borderInstr.getAddr();
		}
	}

	public BytecodeInfo(MethodInfo mi) {
		methodInfo = mi;
	}

	private final static Object[] constants = { null, new Integer(-1),
			new Integer(0), new Integer(1), new Integer(2), new Integer(3),
			new Integer(4), new Integer(5), new Long(0), new Long(1),
			new Float(0), new Float(1), new Float(2), new Double(0),
			new Double(1) };

	protected void readAttribute(String name, int length, ConstantPool cp,
			DataInputStream input, int howMuch) throws IOException {
		if ((howMuch & KNOWNATTRIBS) != 0 && name.equals("LocalVariableTable")) {
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0)
				GlobalOptions.err
						.println("LocalVariableTable of " + methodInfo);
			int count = input.readUnsignedShort();
			if (length != 2 + count * 10) {
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0)
					GlobalOptions.err
							.println("Illegal LVT length, ignoring it");
				return;
			}
			lvt = new LocalVariableInfo[count];
			for (int i = 0; i < count; i++) {
				lvt[i] = new LocalVariableInfo();
				int start = input.readUnsignedShort();
				int end = start + input.readUnsignedShort();
				int nameIndex = input.readUnsignedShort();
				int typeIndex = input.readUnsignedShort();
				int slot = input.readUnsignedShort();
				Instruction startInstr = start >= 0 && start < instrs.length ? instrs[start]
						: null;
				Instruction endInstr;
				if (end >= 0 && end < instrs.length)
					endInstr = instrs[end] == null ? null : instrs[end]
							.getPrevByAddr();
				else {
					endInstr = null;
					for (int nr = instrs.length - 1; nr >= 0; nr--) {
						if (instrs[nr] != null) {
							if (instrs[nr].getNextAddr() == end)
								endInstr = instrs[nr];
							break;
						}
					}
				}

				if (startInstr == null || endInstr == null || nameIndex == 0
						|| typeIndex == 0 || slot >= maxLocals
						|| cp.getTag(nameIndex) != cp.UTF8
						|| cp.getTag(typeIndex) != cp.UTF8) {

					// This is probably an evil lvt as created by HashJava
					// simply ignore it.
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0)
						GlobalOptions.err
								.println("Illegal entry, ignoring LVT");
					lvt = null;
					return;
				}
				lvt[i].start = startInstr;
				lvt[i].end = endInstr;
				lvt[i].name = cp.getUTF8(nameIndex);
				lvt[i].type = cp.getUTF8(typeIndex);
				lvt[i].slot = slot;
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0)
					GlobalOptions.err.println("\t" + lvt[i].name + ": "
							+ lvt[i].type + " range " + start + " - " + end
							+ " slot " + slot);
			}
		} else if ((howMuch & KNOWNATTRIBS) != 0
				&& name.equals("LineNumberTable")) {
			int count = input.readUnsignedShort();
			if (length != 2 + count * 4) {
				GlobalOptions.err
						.println("Illegal LineNumberTable, ignoring it");
				return;
			}
			lnt = new LineNumber[count];
			for (int i = 0; i < count; i++) {
				lnt[i] = new LineNumber();
				int start = input.readUnsignedShort();
				Instruction startInstr = instrs[start];
				if (startInstr == null) {
					GlobalOptions.err
							.println("Illegal entry, ignoring LineNumberTable table");
					lnt = null;
					return;
				}
				lnt[i].start = startInstr;
				lnt[i].linenr = input.readUnsignedShort();
			}
		} else
			super.readAttribute(name, length, cp, input, howMuch);
	}

	public void read(ConstantPool cp, DataInputStream input) throws IOException {
		maxStack = input.readUnsignedShort();
		maxLocals = input.readUnsignedShort();
		instructions = new InstructionList();
		int codeLength = input.readInt();
		instrs = new Instruction[codeLength];
		int[][] succAddrs = new int[codeLength][];
		{
			int addr = 0;
			while (addr < codeLength) {
				Instruction instr;
				int length;
				int opcode = input.readUnsignedByte();
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(addr + ": "
								+ opcodeString[opcode]);

				switch (opcode) {
				case opc_wide: {
					int wideopcode = input.readUnsignedByte();
					switch (wideopcode) {
					case opc_iload:
					case opc_fload:
					case opc_aload:
					case opc_istore:
					case opc_fstore:
					case opc_astore: {
						int slot = input.readUnsignedShort();
						if (slot >= maxLocals)
							throw new ClassFormatError("Invalid local slot "
									+ slot);
						instr = new Instruction(wideopcode);
						instr.setLocalSlot(slot);
						length = 4;
						if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
							GlobalOptions.err.print(" "
									+ opcodeString[wideopcode] + " " + slot);
						break;
					}
					case opc_lload:
					case opc_dload:
					case opc_lstore:
					case opc_dstore: {
						int slot = input.readUnsignedShort();
						if (slot >= maxLocals - 1)
							throw new ClassFormatError("Invalid local slot "
									+ slot);
						instr = new Instruction(wideopcode);
						instr.setLocalSlot(slot);
						length = 4;
						if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
							GlobalOptions.err.print(" "
									+ opcodeString[wideopcode] + " " + slot);
						break;
					}
					case opc_ret: {
						int slot = input.readUnsignedShort();
						if (slot >= maxLocals)
							throw new ClassFormatError("Invalid local slot "
									+ slot);
						instr = new Instruction(wideopcode);
						instr.setLocalSlot(slot);
						length = 4;
						if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
							GlobalOptions.err.print(" ret " + slot);
						break;
					}
					case opc_iinc: {
						int slot = input.readUnsignedShort();
						if (slot >= maxLocals)
							throw new ClassFormatError("Invalid local slot "
									+ slot);
						instr = new Instruction(wideopcode);
						instr.setLocalSlot(slot);
						instr.setIncrement(input.readShort());
						length = 6;
						if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
							GlobalOptions.err.print(" iinc " + slot + " "
									+ instr.getIncrement());
						break;
					}
					default:
						throw new ClassFormatError("Invalid wide opcode "
								+ wideopcode);
					}
					break;
				}
				case opc_iload_0:
				case opc_iload_1:
				case opc_iload_2:
				case opc_iload_3:
				case opc_lload_0:
				case opc_lload_1:
				case opc_lload_2:
				case opc_lload_3:
				case opc_fload_0:
				case opc_fload_1:
				case opc_fload_2:
				case opc_fload_3:
				case opc_dload_0:
				case opc_dload_1:
				case opc_dload_2:
				case opc_dload_3:
				case opc_aload_0:
				case opc_aload_1:
				case opc_aload_2:
				case opc_aload_3: {
					int slot = (opcode - opc_iload_0) & 3;
					if (slot >= maxLocals)
						throw new ClassFormatError("Invalid local slot " + slot);
					instr = new Instruction(opc_iload + (opcode - opc_iload_0)
							/ 4);
					instr.setLocalSlot(slot);
					length = 1;
					break;
				}
				case opc_istore_0:
				case opc_istore_1:
				case opc_istore_2:
				case opc_istore_3:
				case opc_fstore_0:
				case opc_fstore_1:
				case opc_fstore_2:
				case opc_fstore_3:
				case opc_astore_0:
				case opc_astore_1:
				case opc_astore_2:
				case opc_astore_3: {
					int slot = (opcode - opc_istore_0) & 3;
					if (slot >= maxLocals)
						throw new ClassFormatError("Invalid local slot " + slot);
					instr = new Instruction(opc_istore
							+ (opcode - opc_istore_0) / 4);
					instr.setLocalSlot(slot);
					length = 1;
					break;
				}
				case opc_lstore_0:
				case opc_lstore_1:
				case opc_lstore_2:
				case opc_lstore_3:
				case opc_dstore_0:
				case opc_dstore_1:
				case opc_dstore_2:
				case opc_dstore_3: {
					int slot = (opcode - opc_istore_0) & 3;
					if (slot >= maxLocals - 1)
						throw new ClassFormatError("Invalid local slot " + slot);
					instr = new Instruction(opc_istore
							+ (opcode - opc_istore_0) / 4);
					instr.setLocalSlot(slot);
					length = 1;
					break;
				}
				case opc_iload:
				case opc_fload:
				case opc_aload:
				case opc_istore:
				case opc_fstore:
				case opc_astore: {
					int slot = input.readUnsignedByte();
					if (slot >= maxLocals)
						throw new ClassFormatError("Invalid local slot " + slot);
					instr = new Instruction(opcode);
					instr.setLocalSlot(slot);
					length = 2;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + slot);
					break;
				}
				case opc_lstore:
				case opc_dstore:
				case opc_lload:
				case opc_dload: {
					int slot = input.readUnsignedByte();
					if (slot >= maxLocals - 1)
						throw new ClassFormatError("Invalid local slot " + slot);
					instr = new Instruction(opcode);
					instr.setLocalSlot(slot);
					length = 2;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + slot);
					break;
				}
				case opc_ret: {
					int slot = input.readUnsignedByte();
					if (slot >= maxLocals)
						throw new ClassFormatError("Invalid local slot " + slot);
					instr = new Instruction(opcode);
					instr.setLocalSlot(slot);
					length = 2;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + slot);
					break;
				}
				case opc_aconst_null:
				case opc_iconst_m1:
				case opc_iconst_0:
				case opc_iconst_1:
				case opc_iconst_2:
				case opc_iconst_3:
				case opc_iconst_4:
				case opc_iconst_5:
				case opc_fconst_0:
				case opc_fconst_1:
				case opc_fconst_2:
					instr = new Instruction(opc_ldc);
					instr.setConstant(constants[opcode - opc_aconst_null]);
					length = 1;
					break;
				case opc_lconst_0:
				case opc_lconst_1:
				case opc_dconst_0:
				case opc_dconst_1:
					instr = new Instruction(opc_ldc2_w);
					instr.setConstant(constants[opcode - opc_aconst_null]);
					length = 1;
					break;
				case opc_bipush:
					instr = new Instruction(opc_ldc);
					instr.setConstant(new Integer(input.readByte()));
					length = 2;
					break;
				case opc_sipush:
					instr = new Instruction(opc_ldc);
					instr.setConstant(new Integer(input.readShort()));
					length = 3;
					break;
				case opc_ldc: {
					int index = input.readUnsignedByte();
					int tag = cp.getTag(index);
					if (tag != cp.STRING && tag != cp.INTEGER
							&& tag != cp.FLOAT && tag != cp.CLASS)
						throw new ClassFormatException("wrong constant tag: "
								+ tag);
					instr = new Instruction(opcode);
					instr.setConstant(cp.getConstant(index));
					length = 2;
					break;
				}
				case opc_ldc_w: {
					int index = input.readUnsignedShort();
					int tag = cp.getTag(index);
					if (tag != cp.STRING && tag != cp.INTEGER
							&& tag != cp.FLOAT && tag != cp.CLASS)
						throw new ClassFormatException("wrong constant tag: "
								+ tag);
					instr = new Instruction(opc_ldc);
					instr.setConstant(cp.getConstant(index));
					length = 3;
					break;
				}
				case opc_ldc2_w: {
					int index = input.readUnsignedShort();
					int tag = cp.getTag(index);
					if (tag != cp.LONG && tag != cp.DOUBLE)
						throw new ClassFormatException("wrong constant tag: "
								+ tag);
					instr = new Instruction(opcode);
					instr.setConstant(cp.getConstant(index));
					length = 3;
					break;
				}
				case opc_iinc: {
					int slot = input.readUnsignedByte();
					if (slot >= maxLocals)
						throw new ClassFormatError("Invalid local slot " + slot);
					instr = new Instruction(opcode);
					instr.setLocalSlot(slot);
					instr.setIncrement(input.readByte());
					length = 3;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + slot + " "
								+ instr.getIncrement());
					break;
				}
				case opc_goto:
				case opc_jsr:
				case opc_ifeq:
				case opc_ifne:
				case opc_iflt:
				case opc_ifge:
				case opc_ifgt:
				case opc_ifle:
				case opc_if_icmpeq:
				case opc_if_icmpne:
				case opc_if_icmplt:
				case opc_if_icmpge:
				case opc_if_icmpgt:
				case opc_if_icmple:
				case opc_if_acmpeq:
				case opc_if_acmpne:
				case opc_ifnull:
				case opc_ifnonnull:
					instr = new Instruction(opcode);
					length = 3;
					succAddrs[addr] = new int[] { addr + input.readShort() };
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + succAddrs[addr][0]);
					break;

				case opc_goto_w:
				case opc_jsr_w:
					instr = new Instruction(opcode - (opc_goto_w - opc_goto));
					length = 5;
					succAddrs[addr] = new int[] { addr + input.readInt() };
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + succAddrs[addr][0]);
					break;

				case opc_tableswitch: {
					length = 3 - (addr % 4);
					input.readFully(new byte[length]);
					int def = input.readInt();
					int low = input.readInt();
					int high = input.readInt();
					int[] dests = new int[high - low + 1];
					int npairs = 0;
					for (int i = 0; i < dests.length; i++) {
						dests[i] = input.readInt();
						if (dests[i] != def)
							npairs++;
					}
					instr = new Instruction(opc_lookupswitch);
					succAddrs[addr] = new int[npairs + 1];
					int[] values = new int[npairs];
					int pos = 0;
					for (int i = 0; i < dests.length; i++) {
						if (dests[i] != def) {
							values[pos] = i + low;
							succAddrs[addr][pos] = addr + dests[i];
							pos++;
						}
					}
					succAddrs[addr][npairs] = addr + def;
					instr.setValues(values);
					length += 13 + 4 * (high - low + 1);
					break;
				}
				case opc_lookupswitch: {
					length = 3 - (addr % 4);
					input.readFully(new byte[length]);
					int def = input.readInt();
					int npairs = input.readInt();
					instr = new Instruction(opcode);
					succAddrs[addr] = new int[npairs + 1];
					int[] values = new int[npairs];
					for (int i = 0; i < npairs; i++) {
						values[i] = input.readInt();
						if (i > 0 && values[i - 1] >= values[i])
							throw new ClassFormatException(
									"lookupswitch not sorted");
						succAddrs[addr][i] = addr + input.readInt();
					}
					succAddrs[addr][npairs] = addr + def;
					instr.setValues(values);
					length += 9 + 8 * npairs;
					break;
				}

				case opc_getstatic:
				case opc_getfield:
				case opc_putstatic:
				case opc_putfield:
				case opc_invokespecial:
				case opc_invokestatic:
				case opc_invokevirtual: {
					int index = input.readUnsignedShort();
					int tag = cp.getTag(index);
					if (opcode < opc_invokevirtual) {
						if (tag != cp.FIELDREF)
							throw new ClassFormatException(
									"field tag mismatch: " + tag);
					} else {
						if (tag != cp.METHODREF)
							throw new ClassFormatException(
									"method tag mismatch: " + tag);
					}
					Reference ref = cp.getRef(index);
					if (ref.getName().charAt(0) == '<'
							&& (!ref.getName().equals("<init>") || opcode != opc_invokespecial))
						throw new ClassFormatException(
								"Illegal call of special method/field " + ref);
					instr = new Instruction(opcode);
					instr.setReference(ref);
					length = 3;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + ref);
					break;
				}
				case opc_invokeinterface: {
					int index = input.readUnsignedShort();
					int tag = cp.getTag(index);
					if (tag != cp.INTERFACEMETHODREF)
						throw new ClassFormatException(
								"interface tag mismatch: " + tag);
					Reference ref = cp.getRef(index);
					if (ref.getName().charAt(0) == '<')
						throw new ClassFormatException(
								"Illegal call of special method " + ref);
					int nargs = input.readUnsignedByte();
					if (TypeSignature.getArgumentSize(ref.getType()) != nargs - 1)
						throw new ClassFormatException(
								"Interface nargs mismatch: " + ref + " vs. "
										+ nargs);
					if (input.readUnsignedByte() != 0)
						throw new ClassFormatException(
								"Interface reserved param not zero");

					instr = new Instruction(opcode);
					instr.setReference(ref);
					length = 5;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + ref);
					break;
				}

				case opc_new:
				case opc_checkcast:
				case opc_instanceof: {
					String type = cp.getClassType(input.readUnsignedShort());
					if (opcode == opc_new && type.charAt(0) == '[')
						throw new ClassFormatException(
								"Can't create array with opc_new");
					instr = new Instruction(opcode);
					instr.setClazzType(type);
					length = 3;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + type);
					break;
				}
				case opc_multianewarray: {
					String type = cp.getClassType(input.readUnsignedShort());
					int dims = input.readUnsignedByte();
					if (dims == 0)
						throw new ClassFormatException(
								"multianewarray dimension is 0.");
					for (int i = 0; i < dims; i++) {
						/*
						 * Note that since type is a valid type signature, there
						 * must be a non bracket character, before the string is
						 * over. So there is no StringIndexOutOfBoundsException.
						 */
						if (type.charAt(i) != '[')
							throw new ClassFormatException(
									"multianewarray called for non array:"
											+ type);
					}
					instr = new Instruction(opcode);
					instr.setClazzType(type);
					instr.setDimensions(dims);
					length = 4;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + type + " " + dims);
					break;
				}
				case opc_anewarray: {
					String type = cp.getClassType(input.readUnsignedShort());
					instr = new Instruction(opc_multianewarray);
					instr.setClazzType(("[" + type).intern());
					instr.setDimensions(1);
					length = 3;
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + type);
					break;
				}
				case opc_newarray: {
					char sig = newArrayTypes
							.charAt(input.readUnsignedByte() - 4);
					String type = new String(new char[] { '[', sig });
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
						GlobalOptions.err.print(" " + type);
					instr = new Instruction(opc_multianewarray);
					instr.setClazzType(type);
					instr.setDimensions(1);
					length = 2;
					break;
				}

				case opc_nop:
				case opc_iaload:
				case opc_laload:
				case opc_faload:
				case opc_daload:
				case opc_aaload:
				case opc_baload:
				case opc_caload:
				case opc_saload:
				case opc_iastore:
				case opc_lastore:
				case opc_fastore:
				case opc_dastore:
				case opc_aastore:
				case opc_bastore:
				case opc_castore:
				case opc_sastore:
				case opc_pop:
				case opc_pop2:
				case opc_dup:
				case opc_dup_x1:
				case opc_dup_x2:
				case opc_dup2:
				case opc_dup2_x1:
				case opc_dup2_x2:
				case opc_swap:
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
				case opc_drem:
				case opc_ineg:
				case opc_lneg:
				case opc_fneg:
				case opc_dneg:
				case opc_ishl:
				case opc_lshl:
				case opc_ishr:
				case opc_lshr:
				case opc_iushr:
				case opc_lushr:
				case opc_iand:
				case opc_land:
				case opc_ior:
				case opc_lor:
				case opc_ixor:
				case opc_lxor:
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
				case opc_d2f:
				case opc_i2b:
				case opc_i2c:
				case opc_i2s:
				case opc_lcmp:
				case opc_fcmpl:
				case opc_fcmpg:
				case opc_dcmpl:
				case opc_dcmpg:
				case opc_ireturn:
				case opc_lreturn:
				case opc_freturn:
				case opc_dreturn:
				case opc_areturn:
				case opc_return:
				case opc_athrow:
				case opc_arraylength:
				case opc_monitorenter:
				case opc_monitorexit:
					instr = new Instruction(opcode);
					length = 1;
					break;
				default:
					throw new ClassFormatError("Invalid opcode " + opcode);
				}
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_BYTECODE) != 0)
					GlobalOptions.err.println();

				instrs[addr] = instr;
				instructions.add(instr);

				addr += length;
				instructions.setLastAddr(addr);
			}
			if (addr != codeLength)
				throw new ClassFormatError("last instruction too long");
		}
		for (Iterator iter = instructions.iterator(); iter.hasNext();) {
			Instruction instr = (Instruction) iter.next();
			int addr = instr.getAddr();
			if (addr < succAddrs.length && succAddrs[addr] != null) {
				int length = succAddrs[addr].length;
				Instruction[] succs = new Instruction[length];
				for (int i = 0; i < length; i++) {
					int succAddr = succAddrs[addr][i];
					if (succAddr < 0 || succAddr > codeLength
							|| succAddr >= instrs.length
							|| instrs[succAddr] == null)
						throw new ClassFormatException(
								"Illegal jump target at " + this + "@" + addr);
					succs[i] = instrs[succAddr];
				}
				instr.setSuccs(succs);
			}
		}
		succAddrs = null;

		{
			int handlersLength = input.readUnsignedShort();
			exceptionHandlers = new Handler[handlersLength];
			for (int i = 0; i < handlersLength; i++) {
				exceptionHandlers[i] = new Handler();
				exceptionHandlers[i].start = instrs[input.readUnsignedShort()];
				exceptionHandlers[i].end = instrs[input.readUnsignedShort()]
						.getPrevByAddr();
				exceptionHandlers[i].catcher = instrs[input.readUnsignedShort()];
				int index = input.readUnsignedShort();
				exceptionHandlers[i].type = (index == 0) ? null : cp
						.getClassName(index);

				if (exceptionHandlers[i].catcher.getOpcode() == opc_athrow) {
					/*
					 * There is an obfuscator, which inserts bogus exception
					 * entries jumping directly to a throw instruction. Remove
					 * those handlers.
					 */
					handlersLength--;
					i--;
					continue;
				}

				if (exceptionHandlers[i].start.getAddr() <= exceptionHandlers[i].catcher
						.getAddr()
						&& exceptionHandlers[i].end.getAddr() >= exceptionHandlers[i].catcher
								.getAddr()) {
					/*
					 * Javac 1.4 is a bit paranoid with finally and synchronize
					 * blocks and even breaks the JLS. We fix it here. Hopefully
					 * this won't produce any other problems.
					 */
					if (exceptionHandlers[i].start == exceptionHandlers[i].catcher) {
						handlersLength--;
						i--;
					} else {
						exceptionHandlers[i].end = exceptionHandlers[i].catcher
								.getPrevByAddr();
					}
				}
			}
			if (handlersLength < exceptionHandlers.length) {
				Handler[] newHandlers = new Handler[handlersLength];
				System.arraycopy(exceptionHandlers, 0, newHandlers, 0,
						handlersLength);
				exceptionHandlers = newHandlers;
			}
		}
		readAttributes(cp, input, FULLINFO);
		instrs = null;
	}

	public void dumpCode(java.io.PrintWriter output) {
		for (Iterator iter = instructions.iterator(); iter.hasNext();) {
			Instruction instr = (Instruction) iter.next();
			output.println(instr.getDescription() + " "
					+ Integer.toHexString(hashCode()));
			Instruction[] succs = instr.getSuccs();
			if (succs != null) {
				output.print("\tsuccs: " + succs[0]);
				for (int i = 1; i < succs.length; i++)
					output.print(", " + succs[i]);
				output.println();
			}
			if (instr.getPreds() != null) {
				output.print("\tpreds: " + instr.getPreds()[0]);
				for (int i = 1; i < instr.getPreds().length; i++)
					output.print(", " + instr.getPreds()[i]);
				output.println();
			}
		}
		for (int i = 0; i < exceptionHandlers.length; i++) {
			output.println("catch " + exceptionHandlers[i].type + " from "
					+ exceptionHandlers[i].start + " to "
					+ exceptionHandlers[i].end + " catcher "
					+ exceptionHandlers[i].catcher);
		}
	}

	public void reserveSmallConstants(GrowableConstantPool gcp) {
		next_instr: for (Iterator iter = instructions.iterator(); iter
				.hasNext();) {
			Instruction instr = (Instruction) iter.next();
			if (instr.getOpcode() == opc_ldc) {
				Object constant = instr.getConstant();
				if (constant == null)
					continue next_instr;
				for (int i = 1; i < constants.length; i++) {
					if (constant.equals(constants[i]))
						continue next_instr;
				}
				if (constant instanceof Integer) {
					int value = ((Integer) constant).intValue();
					if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
						continue next_instr;
				}
				gcp.reserveConstant(constant);
			}
		}
	}

	private void calculateMaxStack() {
		maxStack = 0;
		int[] stackHeights = new int[instructions.getCodeLength()];
		int[] poppush = new int[2];
		Stack todo = new Stack();

		for (int i = 0; i < stackHeights.length; i++)
			stackHeights[i] = -1;

		stackHeights[0] = 0;
		todo.push(instructions.get(0));
		while (!todo.isEmpty()) {
			Instruction instr = (Instruction) todo.pop();
			Instruction next = instr.getNextByAddr();
			Instruction[] succs = instr.getSuccs();
			int addr = instr.getAddr();
			instr.getStackPopPush(poppush);
			int sh = stackHeights[addr] - poppush[0] + poppush[1];
			// System.err.println("Instr: "+instr.getDescription()+
			// "; before: "+stackHeights[addr]+" after: "+sh);
			if (maxStack < sh)
				maxStack = sh;
			if (instr.getOpcode() == opc_jsr) {
				if (stackHeights[next.getAddr()] == -1) {
					stackHeights[next.getAddr()] = sh - 1;
					todo.push(next);
				}
				if (stackHeights[succs[0].getAddr()] == -1) {
					stackHeights[succs[0].getAddr()] = sh;
					todo.push(succs[0]);
				}
			} else {
				if (succs != null) {
					for (int i = 0; i < succs.length; i++) {
						if (stackHeights[succs[i].getAddr()] == -1) {
							stackHeights[succs[i].getAddr()] = sh;
							todo.push(succs[i]);
						}
					}
				}
				if (!instr.doesAlwaysJump()
						&& stackHeights[next.getAddr()] == -1) {
					stackHeights[next.getAddr()] = sh;
					todo.push(next);
				}
			}
			for (int i = 0; i < exceptionHandlers.length; i++) {
				if (exceptionHandlers[i].start.compareTo(instr) <= 0
						&& exceptionHandlers[i].end.compareTo(instr) >= 0) {
					int catcher = exceptionHandlers[i].catcher.getAddr();
					if (stackHeights[catcher] == -1) {
						stackHeights[catcher] = 1;
						todo.push(exceptionHandlers[i].catcher);
					}
				}
			}
		}
		// System.err.println("New maxStack: "+maxStack+" Locals: "+maxLocals);
	}

	public void prepareWriting(GrowableConstantPool gcp) {
		/*
		 * Recalculate addr, length, maxStack, maxLocals and add all constants
		 * to gcp
		 */
		int addr = 0;
		maxLocals = (methodInfo.isStatic() ? 0 : 1)
				+ TypeSignature.getArgumentSize(methodInfo.getType());

		for (Iterator iter = instructions.iterator(); iter.hasNext();) {
			Instruction instr = (Instruction) iter.next();
			int opcode = instr.getOpcode();
			instr.setAddr(addr);
			int length;
			switch_opc: switch (opcode) {
			case opc_ldc:
			case opc_ldc2_w: {
				Object constant = instr.getConstant();
				if (constant == null) {
					length = 1;
					break switch_opc;
				}
				for (int i = 1; i < constants.length; i++) {
					if (constant.equals(constants[i])) {
						length = 1;
						break switch_opc;
					}
				}
				if (opcode == opc_ldc2_w) {
					gcp.putLongConstant(constant);
					length = 3;
					break switch_opc;
				}
				if (constant instanceof Integer) {
					int value = ((Integer) constant).intValue();
					if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
						length = 2;
						break switch_opc;
					} else if (value >= Short.MIN_VALUE
							&& value <= Short.MAX_VALUE) {
						length = 3;
						break switch_opc;
					}
				}
				if (gcp.putConstant(constant) < 256) {
					length = 2;
				} else {
					length = 3;
				}
				break;
			}
			case opc_iinc: {
				int slot = instr.getLocalSlot();
				int increment = instr.getIncrement();
				if (slot < 256 && increment >= Byte.MIN_VALUE
						&& increment <= Byte.MAX_VALUE)
					length = 3;
				else
					length = 6;
				if (slot >= maxLocals)
					maxLocals = slot + 1;
				break;
			}
			case opc_iload:
			case opc_fload:
			case opc_aload:
			case opc_istore:
			case opc_fstore:
			case opc_astore: {
				int slot = instr.getLocalSlot();
				if (slot < 4)
					length = 1;
				else if (slot < 256)
					length = 2;
				else
					length = 4;
				if (slot >= maxLocals)
					maxLocals = slot + 1;
				break;
			}
			case opc_lload:
			case opc_dload:
			case opc_lstore:
			case opc_dstore: {
				int slot = instr.getLocalSlot();
				if (slot < 4)
					length = 1;
				else if (slot < 256)
					length = 2;
				else
					length = 4;
				if (slot + 1 >= maxLocals)
					maxLocals = slot + 2;
				break;
			}
			case opc_ret: {
				int slot = instr.getLocalSlot();
				if (slot < 256)
					length = 2;
				else
					length = 4;
				if (slot >= maxLocals)
					maxLocals = slot + 1;
				break;
			}
			case opc_lookupswitch: {
				length = 3 - (addr % 4);
				int[] values = instr.getValues();
				int npairs = values.length;
				if (npairs > 0) {
					int tablesize = values[npairs - 1] - values[0] + 1;
					if (4 + tablesize * 4 < 8 * npairs) {
						// Use a table switch
						length += 13 + 4 * tablesize;
						break;
					}
				}
				// Use a lookup switch
				length += 9 + 8 * npairs;
				break;
			}
			case opc_goto:
			case opc_jsr: {
				int dist = instr.getSingleSucc().getAddr() - instr.getAddr();
				if (dist < Short.MIN_VALUE || dist > Short.MAX_VALUE) {
					/* wide goto / jsr */
					length = 5;
					break;
				}
				/* fall through */
			}
			case opc_ifeq:
			case opc_ifne:
			case opc_iflt:
			case opc_ifge:
			case opc_ifgt:
			case opc_ifle:
			case opc_if_icmpeq:
			case opc_if_icmpne:
			case opc_if_icmplt:
			case opc_if_icmpge:
			case opc_if_icmpgt:
			case opc_if_icmple:
			case opc_if_acmpeq:
			case opc_if_acmpne:
			case opc_ifnull:
			case opc_ifnonnull:
				length = 3;
				break;
			case opc_multianewarray: {
				if (instr.getDimensions() == 1) {
					String clazz = instr.getClazzType().substring(1);
					if (newArrayTypes.indexOf(clazz.charAt(0)) != -1) {
						length = 2;
					} else {
						gcp.putClassType(clazz);
						length = 3;
					}
				} else {
					gcp.putClassType(instr.getClazzType());
					length = 4;
				}
				break;
			}
			case opc_getstatic:
			case opc_getfield:
			case opc_putstatic:
			case opc_putfield:
				gcp.putRef(gcp.FIELDREF, instr.getReference());
				length = 3;
				break;
			case opc_invokespecial:
			case opc_invokestatic:
			case opc_invokevirtual:
				gcp.putRef(gcp.METHODREF, instr.getReference());
				length = 3;
				break;
			case opc_invokeinterface:
				gcp.putRef(gcp.INTERFACEMETHODREF, instr.getReference());
				length = 5;
				break;
			case opc_new:
			case opc_checkcast:
			case opc_instanceof:
				gcp.putClassType(instr.getClazzType());
				length = 3;
				break;
			case opc_nop:
			case opc_iaload:
			case opc_laload:
			case opc_faload:
			case opc_daload:
			case opc_aaload:
			case opc_baload:
			case opc_caload:
			case opc_saload:
			case opc_iastore:
			case opc_lastore:
			case opc_fastore:
			case opc_dastore:
			case opc_aastore:
			case opc_bastore:
			case opc_castore:
			case opc_sastore:
			case opc_pop:
			case opc_pop2:
			case opc_dup:
			case opc_dup_x1:
			case opc_dup_x2:
			case opc_dup2:
			case opc_dup2_x1:
			case opc_dup2_x2:
			case opc_swap:
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
			case opc_drem:
			case opc_ineg:
			case opc_lneg:
			case opc_fneg:
			case opc_dneg:
			case opc_ishl:
			case opc_lshl:
			case opc_ishr:
			case opc_lshr:
			case opc_iushr:
			case opc_lushr:
			case opc_iand:
			case opc_land:
			case opc_ior:
			case opc_lor:
			case opc_ixor:
			case opc_lxor:
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
			case opc_d2f:
			case opc_i2b:
			case opc_i2c:
			case opc_i2s:
			case opc_lcmp:
			case opc_fcmpl:
			case opc_fcmpg:
			case opc_dcmpl:
			case opc_dcmpg:
			case opc_ireturn:
			case opc_lreturn:
			case opc_freturn:
			case opc_dreturn:
			case opc_areturn:
			case opc_return:
			case opc_athrow:
			case opc_arraylength:
			case opc_monitorenter:
			case opc_monitorexit:
				length = 1;
				break;
			default:
				throw new ClassFormatError("Invalid opcode " + opcode);
			}
			addr += length;
		}
		instructions.setLastAddr(addr);
		try {
			calculateMaxStack();
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			dumpCode(GlobalOptions.err);
		}
		for (int i = 0; i < exceptionHandlers.length; i++)
			if (exceptionHandlers[i].type != null)
				gcp.putClassName(exceptionHandlers[i].type);
		if (lvt != null) {
			gcp.putUTF8("LocalVariableTable");
			for (int i = 0; i < lvt.length; i++) {
				gcp.putUTF8(lvt[i].name);
				gcp.putUTF8(lvt[i].type);
			}
		}
		if (lnt != null)
			gcp.putUTF8("LineNumberTable");
		prepareAttributes(gcp);
	}

	protected int getKnownAttributeCount() {
		int count = 0;
		if (lvt != null)
			count++;
		if (lnt != null)
			count++;
		return count;
	}

	public void writeKnownAttributes(GrowableConstantPool gcp,
			DataOutputStream output) throws IOException {
		if (lvt != null) {
			output.writeShort(gcp.putUTF8("LocalVariableTable"));
			int count = lvt.length;
			int length = 2 + 10 * count;
			output.writeInt(length);
			output.writeShort(count);
			for (int i = 0; i < count; i++) {
				output.writeShort(lvt[i].start.getAddr());
				output.writeShort(lvt[i].end.getAddr() + lvt[i].end.getLength()
						- lvt[i].start.getAddr());
				output.writeShort(gcp.putUTF8(lvt[i].name));
				output.writeShort(gcp.putUTF8(lvt[i].type));
				output.writeShort(lvt[i].slot);
			}
		}
		if (lnt != null) {
			output.writeShort(gcp.putUTF8("LineNumberTable"));
			int count = lnt.length;
			int length = 2 + 4 * count;
			output.writeInt(length);
			output.writeShort(count);
			for (int i = 0; i < count; i++) {
				output.writeShort(lnt[i].start.getAddr());
				output.writeShort(lnt[i].linenr);
			}
		}
	}

	public void write(GrowableConstantPool gcp, DataOutputStream output)
			throws IOException {
		output.writeShort(maxStack);
		output.writeShort(maxLocals);
		output.writeInt(instructions.getCodeLength());
		for (Iterator iter = instructions.iterator(); iter.hasNext();) {
			Instruction instr = (Instruction) iter.next();
			int opcode = instr.getOpcode();
			switch_opc: switch (opcode) {
			case opc_iload:
			case opc_lload:
			case opc_fload:
			case opc_dload:
			case opc_aload:
			case opc_istore:
			case opc_lstore:
			case opc_fstore:
			case opc_dstore:
			case opc_astore: {
				int slot = instr.getLocalSlot();
				if (slot < 4) {
					if (opcode < opc_istore)
						output.writeByte(opc_iload_0 + 4 * (opcode - opc_iload)
								+ slot);
					else
						output.writeByte(opc_istore_0 + 4
								* (opcode - opc_istore) + slot);
				} else if (slot < 256) {
					output.writeByte(opcode);
					output.writeByte(slot);
				} else {
					output.writeByte(opc_wide);
					output.writeByte(opcode);
					output.writeShort(slot);
				}
				break;
			}
			case opc_ret: {
				int slot = instr.getLocalSlot();
				if (slot < 256) {
					output.writeByte(opcode);
					output.writeByte(slot);
				} else {
					output.writeByte(opc_wide);
					output.writeByte(opcode);
					output.writeShort(slot);
				}
				break;
			}
			case opc_ldc:
			case opc_ldc2_w: {
				Object constant = instr.getConstant();
				if (constant == null) {
					output.writeByte(opc_aconst_null);
					break switch_opc;
				}
				for (int i = 1; i < constants.length; i++) {
					if (constant.equals(constants[i])) {
						output.writeByte(opc_aconst_null + i);
						break switch_opc;
					}
				}
				if (opcode == opc_ldc2_w) {
					output.writeByte(opcode);
					output.writeShort(gcp.putLongConstant(constant));
				} else {
					if (constant instanceof Integer) {
						int value = ((Integer) constant).intValue();
						if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {

							output.writeByte(opc_bipush);
							output.writeByte(((Integer) constant).intValue());
							break switch_opc;
						} else if (value >= Short.MIN_VALUE
								&& value <= Short.MAX_VALUE) {
							output.writeByte(opc_sipush);
							output.writeShort(((Integer) constant).intValue());
							break switch_opc;
						}
					}
					if (instr.getLength() == 2) {
						output.writeByte(opc_ldc);
						output.writeByte(gcp.putConstant(constant));
					} else {
						output.writeByte(opc_ldc_w);
						output.writeShort(gcp.putConstant(constant));
					}
				}
				break;
			}
			case opc_iinc: {
				int slot = instr.getLocalSlot();
				int incr = instr.getIncrement();
				if (instr.getLength() == 3) {
					output.writeByte(opcode);
					output.writeByte(slot);
					output.writeByte(incr);
				} else {
					output.writeByte(opc_wide);
					output.writeByte(opcode);
					output.writeShort(slot);
					output.writeShort(incr);
				}
				break;
			}
			case opc_goto:
			case opc_jsr:
				if (instr.getLength() == 5) {
					/* wide goto or jsr */
					output.writeByte(opcode + (opc_goto_w - opc_goto));
					output.writeInt(instr.getSingleSucc().getAddr()
							- instr.getAddr());
					break;
				}
				/* fall through */
			case opc_ifeq:
			case opc_ifne:
			case opc_iflt:
			case opc_ifge:
			case opc_ifgt:
			case opc_ifle:
			case opc_if_icmpeq:
			case opc_if_icmpne:
			case opc_if_icmplt:
			case opc_if_icmpge:
			case opc_if_icmpgt:
			case opc_if_icmple:
			case opc_if_acmpeq:
			case opc_if_acmpne:
			case opc_ifnull:
			case opc_ifnonnull:
				output.writeByte(opcode);
				output.writeShort(instr.getSingleSucc().getAddr()
						- instr.getAddr());
				break;

			case opc_lookupswitch: {
				int align = 3 - (instr.getAddr() % 4);
				int[] values = instr.getValues();
				int npairs = values.length;
				int defAddr = instr.getSuccs()[npairs].getAddr()
						- instr.getAddr();

				if (npairs > 0) {
					int tablesize = values[npairs - 1] - values[0] + 1;
					if (4 + tablesize * 4 < 8 * npairs) {
						// Use a table switch
						output.writeByte(opc_tableswitch);
						output.write(new byte[align]);
						/* def */
						output.writeInt(defAddr);
						/* low */
						output.writeInt(values[0]);
						/* high */
						output.writeInt(values[npairs - 1]);
						int pos = values[0];
						for (int i = 0; i < npairs; i++) {
							while (pos++ < values[i])
								output.writeInt(defAddr);
							output.writeInt(instr.getSuccs()[i].getAddr()
									- instr.getAddr());
						}
						break;
					}
				}
				// Use a lookup switch
				output.writeByte(opc_lookupswitch);
				output.write(new byte[align]);
				/* def */
				output.writeInt(defAddr);
				output.writeInt(npairs);
				for (int i = 0; i < npairs; i++) {
					output.writeInt(values[i]);
					output.writeInt(instr.getSuccs()[i].getAddr()
							- instr.getAddr());
				}
				break;
			}

			case opc_getstatic:
			case opc_getfield:
			case opc_putstatic:
			case opc_putfield:
				output.writeByte(opcode);
				output.writeShort(gcp.putRef(gcp.FIELDREF, instr.getReference()));
				break;

			case opc_invokespecial:
			case opc_invokestatic:
			case opc_invokeinterface:
			case opc_invokevirtual: {
				Reference ref = instr.getReference();
				output.writeByte(opcode);
				if (opcode == opc_invokeinterface) {
					output.writeShort(gcp.putRef(gcp.INTERFACEMETHODREF, ref));
					output.writeByte(TypeSignature.getArgumentSize(ref
							.getType()) + 1);
					output.writeByte(0);
				} else
					output.writeShort(gcp.putRef(gcp.METHODREF, ref));
				break;
			}
			case opc_new:
			case opc_checkcast:
			case opc_instanceof:
				output.writeByte(opcode);
				output.writeShort(gcp.putClassType(instr.getClazzType()));
				break;
			case opc_multianewarray:
				if (instr.getDimensions() == 1) {
					String clazz = instr.getClazzType().substring(1);
					int index = newArrayTypes.indexOf(clazz.charAt(0));
					if (index != -1) {
						output.writeByte(opc_newarray);
						output.writeByte(index + 4);
					} else {
						output.writeByte(opc_anewarray);
						output.writeShort(gcp.putClassType(clazz));
					}
				} else {
					output.writeByte(opcode);
					output.writeShort(gcp.putClassType(instr.getClazzType()));
					output.writeByte(instr.getDimensions());
				}
				break;

			case opc_nop:
			case opc_iaload:
			case opc_laload:
			case opc_faload:
			case opc_daload:
			case opc_aaload:
			case opc_baload:
			case opc_caload:
			case opc_saload:
			case opc_iastore:
			case opc_lastore:
			case opc_fastore:
			case opc_dastore:
			case opc_aastore:
			case opc_bastore:
			case opc_castore:
			case opc_sastore:
			case opc_pop:
			case opc_pop2:
			case opc_dup:
			case opc_dup_x1:
			case opc_dup_x2:
			case opc_dup2:
			case opc_dup2_x1:
			case opc_dup2_x2:
			case opc_swap:
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
			case opc_drem:
			case opc_ineg:
			case opc_lneg:
			case opc_fneg:
			case opc_dneg:
			case opc_ishl:
			case opc_lshl:
			case opc_ishr:
			case opc_lshr:
			case opc_iushr:
			case opc_lushr:
			case opc_iand:
			case opc_land:
			case opc_ior:
			case opc_lor:
			case opc_ixor:
			case opc_lxor:
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
			case opc_d2f:
			case opc_i2b:
			case opc_i2c:
			case opc_i2s:
			case opc_lcmp:
			case opc_fcmpl:
			case opc_fcmpg:
			case opc_dcmpl:
			case opc_dcmpg:
			case opc_ireturn:
			case opc_lreturn:
			case opc_freturn:
			case opc_dreturn:
			case opc_areturn:
			case opc_return:
			case opc_athrow:
			case opc_arraylength:
			case opc_monitorenter:
			case opc_monitorexit:
				output.writeByte(opcode);
				break;
			default:
				throw new ClassFormatError("Invalid opcode " + opcode);
			}
		}

		output.writeShort(exceptionHandlers.length);
		for (int i = 0; i < exceptionHandlers.length; i++) {
			output.writeShort(exceptionHandlers[i].start.getAddr());
			output.writeShort(exceptionHandlers[i].end.getNextByAddr()
					.getAddr());
			output.writeShort(exceptionHandlers[i].catcher.getAddr());
			output.writeShort((exceptionHandlers[i].type == null) ? 0 : gcp
					.putClassName(exceptionHandlers[i].type));
		}
		writeAttributes(gcp, output);
	}

	public void dropInfo(int howMuch) {
		if ((howMuch & KNOWNATTRIBS) != 0) {
			lvt = null;
			lnt = null;
		}
		super.dropInfo(howMuch);
	}

	public int getSize() {
		/*
		 * maxStack: 2 maxLocals: 2 code: 4 + codeLength exc count: 2
		 * exceptions: n * 8 attributes: lvt_name: 2 lvt_length: 4 lvt_count: 2
		 * lvt_entries: n * 10 attributes: lnt_name: 2 lnt_length: 4 lnt_count:
		 * 2 lnt_entries: n * 4
		 */
		int size = 0;
		if (lvt != null)
			size += 8 + lvt.length * 10;
		if (lnt != null)
			size += 8 + lnt.length * 4;
		return 10 + instructions.getCodeLength() + exceptionHandlers.length * 8
				+ getAttributeSize() + size;
	}

	public int getMaxStack() {
		return maxStack;
	}

	public int getMaxLocals() {
		return maxLocals;
	}

	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

	public List getInstructions() {
		return instructions;
	}

	public Handler[] getExceptionHandlers() {
		return exceptionHandlers;
	}

	public LocalVariableInfo[] getLocalVariableTable() {
		return lvt;
	}

	public LineNumber[] getLineNumberTable() {
		return lnt;
	}

	public void setExceptionHandlers(Handler[] handlers) {
		exceptionHandlers = handlers;
	}

	public void setLocalVariableTable(LocalVariableInfo[] newLvt) {
		lvt = newLvt;
	}

	public void setLineNumberTable(LineNumber[] newLnt) {
		lnt = newLnt;
	}

	public String toString() {
		return "Bytecode " + methodInfo;
	}
}
