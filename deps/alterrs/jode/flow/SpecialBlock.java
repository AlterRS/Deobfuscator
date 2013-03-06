/* SpecialBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: SpecialBlock.java,v 1.15.2.2 2002/05/28 17:34:09 hoenicke Exp $
 */

package alterrs.jode.flow;

import alterrs.jode.decompiler.TabbedPrintWriter;
import alterrs.jode.expr.CompareUnaryOperator;
import alterrs.jode.expr.Expression;
import alterrs.jode.expr.InvokeOperator;
import alterrs.jode.expr.Operator;
import alterrs.jode.expr.PopOperator;
import alterrs.jode.expr.StoreInstruction;

/**
 * This is the structured block for atomic instructions.
 */
public class SpecialBlock extends StructuredBlock {

	public static int DUP = 0;
	public static int SWAP = 1;
	public static int POP = 2;
	private static String[] output = { "DUP", "SWAP", "POP" };

	/**
	 * The type, one of DUP or SWAP
	 */
	int type;
	/**
	 * The count of stack entries that are transformed. This is 1 for swap, and
	 * 1 or 2 for dup.
	 */
	int count;
	/**
	 * The depth that the dupped element should be put to (0,1 or 2). For swap
	 * this is zero.
	 */
	int depth;

	public SpecialBlock(int type, int count, int depth, Jump jump) {
		this.type = type;
		this.count = count;
		this.depth = depth;
		setJump(jump);
	}

	/**
	 * This does take the instr into account and modifies stack accordingly. It
	 * then calls super.mapStackToLocal.
	 * 
	 * @param stack
	 *            the stack before the instruction is called
	 * @return stack the stack afterwards.
	 */
	public VariableStack mapStackToLocal(VariableStack stack) {
		/* a SpecialBlock is special :-) */
		VariableStack after = stack.executeSpecial(this);
		return super.mapStackToLocal(after);
	}

	public void removePush() {
		/* XXX */
		removeBlock();
	}

	public void dumpInstruction(TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.println(output[type] + ((count == 1) ? "" : "2")
				+ ((depth == 0) ? "" : "_X" + depth));
	}

	public boolean doTransformations() {
		return (type == SWAP && removeSwap(flowBlock.lastModified))
				|| (type == POP && removePop(flowBlock.lastModified));
	}

	public boolean removeSwap(StructuredBlock last) {

		/*
		 * Remove non needed swaps; convert:
		 * 
		 * PUSH expr1 PUSH expr2 SWAP
		 * 
		 * to:
		 * 
		 * PUSH expr2 PUSH expr1
		 */
		if (last.outer instanceof SequentialBlock
				&& last.outer.outer instanceof SequentialBlock
				&& last.outer.getSubBlocks()[0] instanceof InstructionBlock
				&& last.outer.outer.getSubBlocks()[0] instanceof InstructionBlock) {

			InstructionBlock block1 = (InstructionBlock) last.outer.outer
					.getSubBlocks()[0];
			InstructionBlock block2 = (InstructionBlock) last.outer
					.getSubBlocks()[0];

			Expression expr1 = block1.getInstruction();
			Expression expr2 = block2.getInstruction();

			if (expr1.isVoid() || expr2.isVoid()
					|| expr1.getFreeOperandCount() != 0
					|| expr2.getFreeOperandCount() != 0
					|| expr1.hasSideEffects(expr2)
					|| expr2.hasSideEffects(expr1))
				return false;

			/*
			 * PUSH expr1 == block1 PUSH expr2 SWAP ...
			 */
			last.outer.replace(block1.outer);
			/*
			 * PUSH expr2 SWAP ...
			 */
			block1.replace(this);
			block1.moveJump(jump);
			/*
			 * PUSH expr2 PUSH expr1
			 */
			block1.flowBlock.lastModified = block1;
			return true;
		}
		return false;
	}

	public boolean removePop(StructuredBlock last) {

		/*
		 * There are three possibilities:
		 * 
		 * PUSH method_invocation() POP[sizeof PUSH] to: method_invocation()
		 * 
		 * With java1.3 due to access$ methods the method_invocation can already
		 * be a non void store instruction.
		 * 
		 * PUSH arg1 PUSH arg2 POP2 to: if (arg1 == arg2) empty
		 * 
		 * PUSH arg1 POP to: if (arg1 != 0) empty
		 */

		if (last.outer instanceof SequentialBlock
				&& last.outer.getSubBlocks()[0] instanceof InstructionBlock) {

			if (jump != null && jump.destination == null)
				return false;

			InstructionBlock prev = (InstructionBlock) last.outer
					.getSubBlocks()[0];
			Expression instr = prev.getInstruction();

			if (instr.getType().stackSize() == count) {
				StructuredBlock newBlock;
				if (instr instanceof InvokeOperator
						|| instr instanceof StoreInstruction) {
					Expression newExpr = new PopOperator(instr.getType())
							.addOperand(instr);
					prev.setInstruction(newExpr);
					newBlock = prev;
				} else {
					Expression newCond = new CompareUnaryOperator(
							instr.getType(), Operator.NOTEQUALS_OP)
							.addOperand(instr);
					IfThenElseBlock newIfThen = new IfThenElseBlock(newCond);
					newIfThen.setThenBlock(new EmptyBlock());
					newBlock = newIfThen;
				}
				// We don't move the definitions of the special block, but
				// it shouldn't have any.
				newBlock.moveDefinitions(last.outer, last);
				newBlock.moveJump(jump);
				if (this == last) {
					newBlock.replace(last.outer);
					flowBlock.lastModified = newBlock;
				} else {
					newBlock.replace(this);
					last.replace(last.outer);
				}
				return true;
			}
		}
		return false;
	}
}
