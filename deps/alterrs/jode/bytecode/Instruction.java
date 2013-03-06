/* Instruction Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: Instruction.java,v 1.9.2.1 2002/05/28 17:34:00 hoenicke Exp $
 */

package alterrs.jode.bytecode;

/**
 * This class represents an instruction in the byte code.
 */
public final class Instruction implements Opcodes {
	/**
	 * The opcode of the instruction. We map some opcodes, e.g.
	 * 
	 * <pre>
	 * iload_[0-3] -> iload, ldc_w -> ldc, wide iinc -> iinc.
	 * </pre>
	 */
	// a byte would be enough, but then we would need an unsigned convert.
	private int opcode;
	/**
	 * If this opcode uses a local this gives the slot. For multianewarray this
	 * gives the dimension.
	 */
	private int shortData;
	/**
	 * The address of this opcode.
	 */
	private int addr;
	/**
	 * Optional object data for this opcode. There are four different usages of
	 * this field:
	 * <dl>
	 * <dt>opc_ldc / opc_ldc2_w</dt>
	 * <dd>The constant of type Integer/Long/Float/Double/String.</dd>
	 * <dt>opc_invokexxx / opc_xxxfield / opc_xxxstatic</dt>
	 * <dd>The field/method Reference</dd>
	 * <dt>opc_new / opc_checkcast / opc_instanceof / opc_multianewarray</dt>
	 * <dd>The typesignature of the class/array</dd>
	 * <dt>opc_lookupswitch</dt>
	 * <dd>The array of values of type int[]</dd>
	 * </dl>
	 */
	private Object objData;
	/**
	 * The successors of this opcodes, where flow may lead to (except that
	 * nextByAddr is implicit if !alwaysJump). The value null means no
	 * successor, if there is one succesor, this is of type Instruction,
	 * otherwise, this is an array of Instruction.
	 */
	private Object succs;
	/**
	 * The predecessors of this opcode, orthogonal to the succs array. This must
	 * be null or a non empty array.
	 */
	private Instruction[] preds;
	/**
	 * The next instruction in code order.
	 */
	Instruction nextByAddr;
	/**
	 * The previous instruction in code order, useful when changing the order.
	 */
	Instruction prevByAddr;

	/**
	 * You can use this field to add some info to each instruction. After using,
	 * you must set it to null again.
	 * 
	 * @XXX Do we really need this. Every field here can quickly take half a
	 *      megabyte!
	 */
	private Object tmpInfo;

	public Instruction(int opcode) {
		this.opcode = opcode;
	}

	/**
	 * Returns the opcode of the instruction. We map some opcodes:
	 * 
	 * <pre>
	 * [iflda]load_x           -&gt; [iflda]load
	 * [iflda]store_x          -&gt; [iflda]store
	 * [ifa]const_xx, ldc_w    -&gt; ldc
	 * [dl]const_xx            -&gt; ldc2_w
	 * wide opcode             -&gt; opcode
	 * tableswitch             -&gt; lookupswitch
	 * [a]newarray             -&gt; multianewarray
	 * </pre>
	 */
	public final int getOpcode() {
		return opcode;
	}

	/**
	 * Returns the address of this opcode. As long as you don't remove or insert
	 * instructions, you can be sure, that the addresses of the opcodes are
	 * unique, and that
	 * 
	 * <pre>
	 * instr.getAddr() + instr.getLength() == instr.getNextByAddr().getAddr()
	 * 
	 * <pre>
	 * 
	 * If you insert/remove Instructions, you should be aware that the
	 * above property is not guaranteed anymore.
	 */
	public final int getAddr() {
		return addr;
	}

	public final int getNextAddr() {
		return nextByAddr.addr;
	}

	/**
	 * Returns the length of this opcode. See getAddr() for some notes. Note
	 * that the length doesn't necessarily reflect the real length, when this
	 * bytecode is written again, since the length of an ldc instruction depends
	 * on the number of entries in constant pool, and the order they are
	 * allocated.
	 */
	public final int getLength() {
		return getNextAddr() - addr;
	}

	final void setAddr(int addr) {
		this.addr = addr;
	}

	public final boolean hasLocalSlot() {
		return opcode == opc_iinc || opcode == opc_ret || opcode >= opc_iload
				&& opcode <= opc_aload || opcode >= opc_istore
				&& opcode <= opc_astore;
	}

	public final int getLocalSlot()
	/*
	 * { require { hasLocalSlot() :: "Instruction has no slot" } }
	 */{
		return shortData;
	}

	public final void setLocalSlot(int slot)
	/*
	 * { require { hasLocalSlot() :: "Instruction has no slot" } }
	 */{
		shortData = slot;
	}

	/**
	 * Optional integer data for this opcode. There are various uses for this:
	 * <dl>
	 * <dt>opc_iinc</dt>
	 * <dd>The value by which the constant is increased/decreased. (short)</dd>
	 * <dt>opc_multianewarray</dt>
	 * <dd>The number of dimensions (1..255)</dd>
	 * </dl>
	 */
	public final int getIncrement()
	/*
	 * { require { opcode == opc_iinc || opcode == opc_multianewarray || opcode
	 * == opc_tableswitch :: "Instruction has no int data" } }
	 */{
		/* shortData already used for local slot */
		return ((Short) objData).shortValue();
	}

	/**
	 * Optional integer data for this opcode. There are various uses for this:
	 * <dl>
	 * <dt>opc_iinc</dt>
	 * <dd>The value by which the constant is increased/decreased. (short)</dd>
	 * <dt>opc_multianewarray</dt>
	 * <dd>The number of dimensions (1..255)</dd>
	 * </dl>
	 */
	public final void setIncrement(int incr)
	/*
	 * { require { opcode == opc_iinc || opcode == opc_multianewarray ::
	 * "Instruction has no int data" } }
	 */{
		/* shortData already used for local slot */
		objData = new Short((short) incr);
	}

	/**
     *
     */
	public final int getDimensions()
	/*
	 * { require { opcode == opc_multianewarray ::
	 * "Instruction has no dimensions" } }
	 */{
		return shortData;
	}

	/**
     *
     */
	public final void setDimensions(int dims)
	/*
	 * { require { opcode == opc_multianewarray ::
	 * "Instruction has no dimensions" } }
	 */{
		shortData = dims;
	}

	public final Object getConstant()
	/*
	 * { require { opcode == opc_ldc || opcode == opc_ldc2_w ::
	 * "Instruction has no constant" } }
	 */{
		return objData;
	}

	public final void setConstant(Object constant)
	/*
	 * { require { opcode == opc_ldc || opcode == opc_ldc2_w ::
	 * "Instruction has no constant" } }
	 */{
		objData = constant;
	}

	public final Reference getReference()
	/*
	 * { require { opcode >= opc_getstatic && opcode <= opc_invokeinterface ::
	 * "Instruction has no reference" } }
	 */{
		return (Reference) objData;
	}

	public final void setReference(Reference ref)
	/*
	 * { require { opcode >= opc_getstatic && opcode <= opc_invokeinterface ::
	 * "Instruction has no reference" } }
	 */{
		objData = ref;
	}

	public final String getClazzType()
	/*
	 * { require { opcode == opc_new || opcode == opc_checkcast || opcode ==
	 * opc_instanceof || opcode == opc_multianewarray ::
	 * "Instruction has no typesig" } }
	 */{
		return (String) objData;
	}

	public final void setClazzType(String type)
	/*
	 * { require { opcode == opc_new || opcode == opc_checkcast || opcode ==
	 * opc_instanceof || opcode == opc_multianewarray ::
	 * "Instruction has no typesig" } }
	 */{
		objData = type;
	}

	public final int[] getValues()
	/*
	 * { require { opcode == opc_lookupswitch :: "Instruction has no values" } }
	 */{
		return (int[]) objData;
	}

	public final void setValues(int[] values)
	/*
	 * { require { opcode == opc_lookupswitch :: "Instruction has no values" } }
	 */{
		objData = values;
	}

	public final boolean doesAlwaysJump() {
		switch (opcode) {
		case opc_ret:
		case opc_goto:
		case opc_jsr:
		case opc_tableswitch:
		case opc_lookupswitch:
		case opc_ireturn:
		case opc_lreturn:
		case opc_freturn:
		case opc_dreturn:
		case opc_areturn:
		case opc_return:
		case opc_athrow:
			return true;
		default:
			return false;
		}
	}

	public final Instruction[] getPreds() {
		return preds;
	}

	/**
	 * Returns true if this opcode has successors, other than the implicit
	 * getNextByAddr().
	 */
	public boolean hasSuccs() {
		return succs != null;
	}

	/**
	 * Returns the successors of this opcodes, where flow may lead to (except
	 * that nextByAddr is implicit if !alwaysJump). The value null means that
	 * there is no successor.
	 */
	public final Instruction[] getSuccs() {
		if (succs instanceof Instruction)
			return new Instruction[] { (Instruction) succs };
		return (Instruction[]) succs;
	}

	/**
	 * Returns the single successor of this opcodes. This gives the target of a
	 * goto, jsr, or if opcode.
	 * 
	 * @return null if there is no successor, otherwise the successor.
	 * @throws ClassCastException
	 *             if this has more than one succ.
	 */
	public final Instruction getSingleSucc() {
		return (Instruction) succs;
	}

	public final Instruction getPrevByAddr() {
		if (prevByAddr.opcode == opc_impdep1)
			return null;
		return prevByAddr;
	}

	public final Instruction getNextByAddr() {
		if (nextByAddr.opcode == opc_impdep1)
			return null;
		return nextByAddr;
	}

	public final Object getTmpInfo() {
		return tmpInfo;
	}

	public final void setTmpInfo(Object info) {
		tmpInfo = info;
	}

	// INTERNAL FUNCTIONS TO KEEP PREDS AND SUCCS CONSISTENT

	final void removeSuccs() {
		if (succs == null)
			return;
		if (succs instanceof Instruction[]) {
			Instruction[] ss = (Instruction[]) succs;
			for (int i = 0; i < ss.length; i++)
				if (ss[i] != null)
					ss[i].removePredecessor(this);
		} else
			((Instruction) succs).removePredecessor(this);
		succs = null;
	}

	/**
	 * @param to
	 *            may be null
	 */
	private final void promoteSuccs(Instruction from, Instruction to) {
		if (succs == from)
			succs = to;
		else if (succs instanceof Instruction[]) {
			Instruction[] ss = (Instruction[]) succs;
			for (int i = 0; i < ss.length; i++)
				if (ss[i] == from)
					ss[i] = to;
		}
	}

	/**
	 * @throws ClassCastException
	 *             if newSuccs is neither an Instruction nor an array of
	 *             instructions.
	 */
	public final void setSuccs(Object newSuccs) {
		if (succs == newSuccs)
			return;
		removeSuccs();
		if (newSuccs == null)
			return;
		if (newSuccs instanceof Instruction[]) {
			Instruction[] ns = (Instruction[]) newSuccs;
			switch (ns.length) {
			case 0:
				break;
			case 1:
				succs = ns[0];
				ns[0].addPredecessor(this);
				break;
			default:
				succs = ns;
				for (int i = 0; i < ns.length; i++)
					ns[i].addPredecessor(this);
				break;
			}
		} else {
			succs = newSuccs;
			((Instruction) newSuccs).addPredecessor(this);
		}
	}

	void addPredecessor(Instruction pred) {
		if (preds == null) {
			preds = new Instruction[] { pred };
			return;
		}
		int predsLength = preds.length;
		Instruction[] newPreds = new Instruction[predsLength + 1];
		System.arraycopy(preds, 0, newPreds, 0, predsLength);
		newPreds[predsLength] = pred;
		preds = newPreds;
	}

	void removePredecessor(Instruction pred) {
		/* Hopefully it doesn't matter if this is slow */
		int predLength = preds.length;
		if (predLength == 1) {
			if (preds[0] != pred)
				throw new alterrs.jode.AssertError(
						"removing not existing predecessor");
			preds = null;
		} else {
			Instruction[] newPreds = new Instruction[predLength - 1];
			int j;
			for (j = 0; preds[j] != pred; j++)
				newPreds[j] = preds[j];
			System.arraycopy(preds, j + 1, newPreds, j, predLength - j - 1);
			preds = newPreds;
		}
	}

	// ADDING, REMOVING AND REPLACING INSTRUCTIONS

	/**
	 * Replaces the opcode of this instruction. You should only use the mapped
	 * opcodes:
	 * 
	 * <pre>
	 * [iflda]load_x           -&gt; [iflda]load
	 * [iflda]store_x          -&gt; [iflda]store
	 * [ifa]const_xx, ldc_w    -&gt; ldc
	 * [dl]const_xx            -&gt; ldc2_w
	 * wide opcode             -&gt; opcode
	 * tableswitch             -&gt; lookupswitch
	 * [a]newarray             -&gt; multianewarray
	 * </pre>
	 */
	public final void replaceInstruction(Instruction newInstr,
			BytecodeInfo codeinfo) {
		/* remove predecessors of successors */
		removeSuccs();

		newInstr.addr = addr;
		nextByAddr.prevByAddr = newInstr;
		newInstr.nextByAddr = nextByAddr;
		prevByAddr.nextByAddr = newInstr;
		newInstr.prevByAddr = prevByAddr;
		prevByAddr = null;
		nextByAddr = null;

		/* promote the successors of the predecessors to newInstr */
		if (preds != null) {
			for (int j = 0; j < preds.length; j++)
				preds[j].promoteSuccs(this, newInstr);
			newInstr.preds = preds;
			preds = null;
		}

		/* adjust exception handlers */
		Handler[] handlers = codeinfo.getExceptionHandlers();
		for (int i = 0; i < handlers.length; i++) {
			if (handlers[i].start == this)
				handlers[i].start = newInstr;
			if (handlers[i].end == this)
				handlers[i].end = newInstr;
			if (handlers[i].catcher == this)
				handlers[i].catcher = newInstr;
		}

		/* adjust local variable table and line number table */
		LocalVariableInfo[] lvt = codeinfo.getLocalVariableTable();
		if (lvt != null) {
			for (int i = 0; i < lvt.length; i++) {
				if (lvt[i].start == this)
					lvt[i].start = newInstr;
				if (lvt[i].end == this)
					lvt[i].end = newInstr;
			}
		}
		LineNumber[] lnt = codeinfo.getLineNumberTable();
		if (lnt != null) {
			for (int i = 0; i < lnt.length; i++) {
				if (lnt[i].start == this)
					lnt[i].start = newInstr;
			}
		}
	}

	void appendInstruction(Instruction newInstr, BytecodeInfo codeinfo) {
		newInstr.addr = nextByAddr.addr;

		newInstr.nextByAddr = nextByAddr;
		nextByAddr.prevByAddr = newInstr;
		newInstr.prevByAddr = this;
		nextByAddr = newInstr;

		/* adjust exception handlers end */
		Handler[] handlers = codeinfo.getExceptionHandlers();
		if (handlers != null) {
			for (int i = 0; i < handlers.length; i++) {
				if (handlers[i].end == this)
					handlers[i].end = newInstr;
			}
		}
	}

	/**
	 * Removes this instruction (as if it would be replaced by a nop).
	 */
	void removeInstruction(BytecodeInfo codeinfo) {

		/* remove from chained list and adjust addr / length */
		prevByAddr.nextByAddr = nextByAddr;
		nextByAddr.prevByAddr = prevByAddr;

		/* remove predecessors of successors */
		removeSuccs();

		/* promote the predecessors to next instruction */
		if (preds != null) {
			for (int j = 0; j < preds.length; j++)
				preds[j].promoteSuccs(this, nextByAddr);
			if (nextByAddr.preds == null)
				nextByAddr.preds = preds;
			else {
				Instruction[] newPreds = new Instruction[nextByAddr.preds.length
						+ preds.length];
				System.arraycopy(nextByAddr.preds, 0, newPreds, 0,
						nextByAddr.preds.length);
				System.arraycopy(preds, 0, newPreds, nextByAddr.preds.length,
						preds.length);
				nextByAddr.preds = newPreds;
			}
			preds = null;
		}

		/* adjust exception handlers */
		Handler[] handlers = codeinfo.getExceptionHandlers();
		for (int i = 0; i < handlers.length; i++) {
			if (handlers[i].start == this && handlers[i].end == this) {
				/*
				 * Remove the handler. This is very seldom, so we can make it
				 * slow
				 */
				Handler[] newHandlers = new Handler[handlers.length - 1];
				System.arraycopy(handlers, 0, newHandlers, 0, i);
				System.arraycopy(handlers, i + 1, newHandlers, i,
						handlers.length - (i + 1));
				handlers = newHandlers;
				codeinfo.setExceptionHandlers(newHandlers);
				i--;
			} else {
				if (handlers[i].start == this)
					handlers[i].start = nextByAddr;
				if (handlers[i].end == this)
					handlers[i].end = prevByAddr;
				if (handlers[i].catcher == this)
					handlers[i].catcher = nextByAddr;
			}
		}

		/* adjust local variable table and line number table */
		LocalVariableInfo[] lvt = codeinfo.getLocalVariableTable();
		if (lvt != null) {
			for (int i = 0; i < lvt.length; i++) {
				if (lvt[i].start == this && lvt[i].end == this) {
					/*
					 * Remove the local variable info. This is very seldom, so
					 * we can make it slow
					 */
					LocalVariableInfo[] newLVT = new LocalVariableInfo[lvt.length - 1];
					System.arraycopy(lvt, 0, newLVT, 0, i);
					System.arraycopy(lvt, i + 1, newLVT, i, newLVT.length - i);
					lvt = newLVT;
					codeinfo.setLocalVariableTable(newLVT);
					i--;
				} else {
					if (lvt[i].start == this)
						lvt[i].start = nextByAddr;
					if (lvt[i].end == this)
						lvt[i].end = prevByAddr;
				}
			}
		}
		LineNumber[] lnt = codeinfo.getLineNumberTable();
		if (lnt != null) {
			for (int i = 0; i < lnt.length; i++) {
				if (lnt[i].start == this) {
					if (nextByAddr.opcode == opc_impdep1
							|| (i + 1 < lnt.length && lnt[i + 1].start == nextByAddr)) {
						/*
						 * Remove the line number. This is very seldom, so we
						 * can make it slow
						 */
						LineNumber[] newLNT = new LineNumber[lnt.length - 1];
						System.arraycopy(lnt, 0, newLNT, 0, i);
						System.arraycopy(lnt, i + 1, newLNT, i, newLNT.length
								- i);
						lnt = newLNT;
						codeinfo.setLineNumberTable(newLNT);
						i--;
					} else
						lnt[i].start = nextByAddr;
				}
			}
		}

		prevByAddr = null;
		nextByAddr = null;
	}

	public int compareTo(Instruction instr) {
		if (addr != instr.addr)
			return addr - instr.addr;
		if (this == instr)
			return 0;
		do {
			instr = instr.nextByAddr;
			if (instr.addr > addr)
				return -1;
		} while (instr != this);
		return 1;
	}

	/**
	 * This returns the number of stack entries this instruction pushes and pops
	 * from the stack. The result fills the given array.
	 * 
	 * @param poppush
	 *            an array of two ints. The first element will get the number of
	 *            pops, the second the number of pushes.
	 */
	public void getStackPopPush(int[] poppush)
	/*
	 * { require { poppush != null && poppush.length == 2 ::
	 * "poppush must be an array of two ints" } }
	 */{
		byte delta = (byte) stackDelta.charAt(opcode);
		if (delta < 0x40) {
			poppush[0] = delta & 7;
			poppush[1] = delta >> 3;
		} else {
			switch (opcode) {
			case opc_invokevirtual:
			case opc_invokespecial:
			case opc_invokestatic:
			case opc_invokeinterface: {
				Reference ref = getReference();
				String typeSig = ref.getType();
				poppush[0] = opcode != opc_invokestatic ? 1 : 0;
				poppush[0] += TypeSignature.getArgumentSize(typeSig);
				poppush[1] = TypeSignature.getReturnSize(typeSig);
				break;
			}

			case opc_putfield:
			case opc_putstatic: {
				Reference ref = getReference();
				poppush[1] = 0;
				poppush[0] = TypeSignature.getTypeSize(ref.getType());
				if (opcode == opc_putfield)
					poppush[0]++;
				break;
			}
			case opc_getstatic:
			case opc_getfield: {
				Reference ref = getReference();
				poppush[1] = TypeSignature.getTypeSize(ref.getType());
				poppush[0] = opcode == opc_getfield ? 1 : 0;
				break;
			}

			case opc_multianewarray: {
				poppush[1] = 1;
				poppush[0] = getDimensions();
				break;
			}
			default:
				throw new alterrs.jode.AssertError("Unknown Opcode: " + opcode);
			}
		}
	}

	public Instruction findMatchingPop() {
		int poppush[] = new int[2];
		getStackPopPush(poppush);

		int count = poppush[1];
		Instruction instr = this;
		while (true) {
			if (instr.succs != null || instr.doesAlwaysJump())
				return null;
			instr = instr.nextByAddr;
			if (instr.preds != null)
				return null;

			instr.getStackPopPush(poppush);
			if (count == poppush[0])
				return instr;
			count += poppush[1] - poppush[0];
		}
	}

	public Instruction findMatchingPush() {
		int count = 0;
		Instruction instr = this;
		int poppush[] = new int[2];
		while (true) {
			if (instr.preds != null)
				return null;
			instr = instr.prevByAddr;
			if (instr == null || instr.succs != null || instr.doesAlwaysJump())
				return null;

			instr.getStackPopPush(poppush);
			if (count < poppush[1]) {
				return count == 0 ? instr : null;
			}
			count += poppush[0] - poppush[1];
		}
	}

	public String getDescription() {
		StringBuffer result = new StringBuffer(String.valueOf(addr))
				.append('_').append(Integer.toHexString(hashCode()))
				.append(": ").append(opcodeString[opcode]);
		if (opcode != opc_lookupswitch) {
			if (hasLocalSlot())
				result.append(' ').append(getLocalSlot());
			if (succs != null)
				result.append(' ').append(((Instruction) succs).addr);
			if (objData != null)
				result.append(' ').append(objData);
			if (opcode == opc_multianewarray)
				result.append(' ').append(getDimensions());
		} else {
			int[] values = getValues();
			Instruction[] succs = getSuccs();
			for (int i = 0; i < values.length; i++) {
				result.append(' ').append(values[i]).append("->")
						.append(((Instruction) succs[i]).addr);
			}
			result.append(' ').append("default: ")
					.append(((Instruction) succs[values.length]).addr);
		}
		return result.toString();
	}

	public String toString() {
		return "" + addr + "_" + Integer.toHexString(hashCode());
	}

	private final static String stackDelta = "\000\010\010\010\010\010\010\010\010\020\020\010\010\010\020\020\010\010\010\010\020\010\020\010\020\010\010\010\010\010\020\020\020\020\010\010\010\010\020\020\020\020\010\010\010\010\012\022\012\022\012\012\012\012\001\002\001\002\001\001\001\001\001\002\002\002\002\001\001\001\001\002\002\002\002\001\001\001\001\003\004\003\004\003\003\003\003\001\002\021\032\043\042\053\064\022\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\011\022\011\022\012\023\012\023\012\023\012\024\012\024\012\024\000\021\011\021\012\012\022\011\021\021\012\022\012\011\011\011\014\012\012\014\014\001\001\001\001\001\001\002\002\002\002\002\002\002\002\000\010\000\001\001\001\002\001\002\001\000\100\100\100\100\100\100\100\100\177\010\011\011\011\001\011\011\001\001\177\100\001\001\000\010";

	/*
	 * stackDelta contains \100 if stack count of opcode is variable \177 if
	 * opcode is illegal, or 8*stack_push + stack_pop otherwise The above values
	 * are extracted from following list with: perl -ne'/"(.*)"/ and print $1'
	 * 
	 * "\000" // nop "\010\010\010\010\010\010\010\010" // aconst_null,
	 * iconst_m?[0-5] "\020\020\010\010\010\020\020" // [lfd]const_[0-2]
	 * "\010\010\010\010\020" // sipush bipush ldcx "\010\020\010\020\010" //
	 * [ilfda]load "\010\010\010\010" "\020\020\020\020" "\010\010\010\010"
	 * "\020\020\020\020" "\010\010\010\010" "\012\022\012\022\012\012\012\012"
	 * // [ilfdabcs]aload "\001\002\001\002\001" // [ilfda]store
	 * "\001\001\001\001" "\002\002\002\002" "\001\001\001\001"
	 * "\002\002\002\002" "\001\001\001\001" "\003\004\003\004\003\003\003\003"
	 * // [ilfdabcs]astore "\001\002" // pop "\021\032\043\042\053\064" //
	 * dup2?(_x[12])? "\022" // swap "\012\024\012\024" // [ilfd]add
	 * "\012\024\012\024" // [ilfd]sub "\012\024\012\024" // [ilfd]mul
	 * "\012\024\012\024" // [ilfd]div "\012\024\012\024" // [ilfd]rem
	 * "\011\022\011\022" // [ilfd]neg "\012\023\012\023\012\023" //
	 * [il]u?sh[lr] "\012\024\012\024\012\024" // [il](and|or|xor) "\000" //
	 * opc_iinc "\021\011\021" // i2[lfd] "\012\012\022" // l2[ifd]
	 * "\011\021\021" // f2[ild] "\012\022\012" // d2[ilf] "\011\011\011" //
	 * i2[bcs] "\014\012\012\014\014" // [lfd]cmp.? "\001\001\001\001\001\001"
	 * // if.. "\002\002\002\002\002\002" // if_icmp.. "\002\002" // if_acmp..
	 * "\000\010\000\001\001" // goto,jsr,ret, .*switch
	 * "\001\002\001\002\001\000" // [ilfda]?return "\100\100\100\100" //
	 * (get/put)(static|field) "\100\100\100\100" // invoke.*
	 * "\177\010\011\011\011" // 186 - 190 "\001\011\011\001\001" // 191 - 195
	 * "\177\100\001\001" // 196 - 199 "\000\010" // goto_w, jsr_w
	 */
}
