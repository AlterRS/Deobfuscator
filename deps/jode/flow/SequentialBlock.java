/* SequentialBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: SequentialBlock.java.in,v 4.1.2.1 2002/05/28 17:34:09 hoenicke Exp $
 */

package jode.flow;

import java.util.Set;

import jode.decompiler.TabbedPrintWriter;
import jode.expr.LocalStoreOperator;
import jode.expr.StoreInstruction;
import jode.util.SimpleSet;


/**
 * A sequential block combines exactly two structured blocks to a new one. The
 * first sub block mustn't be another sequential block, instead the second sub
 * block should be used for this.
 */
public class SequentialBlock extends StructuredBlock {
	StructuredBlock[] subBlocks;

	public SequentialBlock() {
		subBlocks = new StructuredBlock[2];
	}

	public void setFirst(StructuredBlock sb) {
		subBlocks[0] = sb;
		sb.outer = this;
		sb.setFlowBlock(flowBlock);
	}

	public void setSecond(StructuredBlock sb) {
		subBlocks[1] = sb;
		sb.outer = this;
		sb.setFlowBlock(flowBlock);
	}

	public void checkConsistent() {
		super.checkConsistent();
		if (subBlocks[0].jump != null
				|| subBlocks[0] instanceof SequentialBlock || jump != null)
			throw new jode.AssertError("Inconsistency");
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
		if (stack == null)
			jode.GlobalOptions.err
					.println("map stack to local called with null: " + this
							+ " in " + this.flowBlock);
		VariableStack middle = subBlocks[0].mapStackToLocal(stack);
		if (middle != null)
			// Otherwise the second block is at least "logical" dead code
			return subBlocks[1].mapStackToLocal(middle);
		jode.GlobalOptions.err.println("Dead code after Block "
				+ subBlocks[0]);
		return null;
	}

	/**
	 * This method should remove local variables that are only written and read
	 * one time directly after another. <br>
	 * <p/>
	 * This is especially important for stack locals, that are created when
	 * there are unusual swap or dup instructions, but also makes inlined
	 * functions more pretty (but not that close to the bytecode).
	 */
	public void removeOnetimeLocals() {
		StructuredBlock secondBlock = subBlocks[1];
		if (secondBlock instanceof SequentialBlock)
			secondBlock = ((SequentialBlock) secondBlock).subBlocks[0];
		if (subBlocks[0] instanceof InstructionBlock
				&& secondBlock instanceof InstructionContainer) {
			InstructionBlock first = (InstructionBlock) subBlocks[0];
			InstructionContainer second = (InstructionContainer) secondBlock;
			/*
			 * check if subBlocks[0] writes to a local, second reads that local,
			 * the local is only used by this two blocks, and there are no side
			 * effects. In that case replace the LoadLocal with the
			 * righthandside of subBlocks[0] and replace subBlocks[1] with this
			 * block. Call removeOnetimelLocals on subBlocks[1] afterwards and
			 * return.
			 */

			if (first.getInstruction() instanceof StoreInstruction) {
				StoreInstruction store = (StoreInstruction) first
						.getInstruction();
				if (store.getLValue() instanceof LocalStoreOperator
						&& (((LocalStoreOperator) store.getLValue())
								.getLocalInfo().getUseCount() == 2)
						&& (second.getInstruction().canCombine(store) > 0)) {
					System.err.println("before: " + first + second);

					second.setInstruction(second.getInstruction()
							.combine(store));
					System.err.println("after: " + second);
					StructuredBlock sb = subBlocks[1];
					sb.moveDefinitions(this, sb);
					sb.replace(this);
					sb.removeOnetimeLocals();
					return;
				}
			}
		}
		super.removeOnetimeLocals();
	}

	/**
	 * Returns the block where the control will normally flow to, when the given
	 * sub block is finished (<em>not</em> ignoring the jump after this block).
	 * (This is overwritten by SequentialBlock and SwitchBlock). If this isn't
	 * called with a direct sub block, the behaviour is undefined, so take care.
	 * 
	 * @return null, if the control flows to another FlowBlock.
	 */
	public StructuredBlock getNextBlock(StructuredBlock subBlock) {
		if (subBlock == subBlocks[0]) {
			if (subBlocks[1].isEmpty())
				return subBlocks[1].getNextBlock();
			else
				return subBlocks[1];
		}
		return getNextBlock();
	}

	public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
		if (subBlock == subBlocks[0]) {
			if (subBlocks[1].isEmpty())
				return subBlocks[1].getNextFlowBlock();
			else
				return null;
		}
		return getNextFlowBlock();
	}

	/**
	 * Tells if the sub block is the single exit point of the current block.
	 * 
	 * @param subBlock
	 *            the sub block.
	 * @return true, if the sub block is the single exit point of the current
	 *         block.
	 */
	public boolean isSingleExit(StructuredBlock subBlock) {
		return (subBlock == subBlocks[1]);
	}

	/**
	 * Propagate the used set. Sequential blocks are special, because they "use"
	 * everything the first block uses. This is, because the first block can't
	 * declare something that is only visible in the first block.
	 * 
	 * @return all locals that are used in this block or in some sub block (this
	 *         is <i>not</i> the used set).
	 */
	public Set propagateUsage() {
		used = new SimpleSet();
		Set allUse = new SimpleSet();
		Set childUse0 = subBlocks[0].propagateUsage();
		Set childUse1 = subBlocks[1].propagateUsage();
		/*
		 * All variables used somewhere inside both sub blocks, are used in this
		 * block, too. Also the variables used in first block are used in this
		 * block, except when it can be declared locally. (Note that
		 * subBlocks[0].used != childUse0)
		 */
		used.addAll(subBlocks[0].used);
		if (subBlocks[0] instanceof LoopBlock)
			((LoopBlock) subBlocks[0]).removeLocallyDeclareable(used);
		allUse.addAll(childUse0);
		allUse.addAll(childUse1);
		childUse0.retainAll(childUse1);
		used.addAll(childUse0);
		return allUse;
	}

	/**
	 * Make the declarations, i.e. initialize the declare variable to correct
	 * values. This will declare every variable that is marked as used, but not
	 * done.
	 * 
	 * @param done
	 *            The set of the already declare variables.
	 */
	public void makeDeclaration(Set done) {
		super.makeDeclaration(done);
		if (subBlocks[0] instanceof InstructionBlock)
			/*
			 * An instruction block may declare a variable for us.
			 */
			((InstructionBlock) subBlocks[0]).checkDeclaration(this.declare);
	}

	public void dumpInstruction(TabbedPrintWriter writer)
			throws java.io.IOException {
		subBlocks[0].dumpSource(writer);
		subBlocks[1].dumpSource(writer);
	}

	/**
	 * Replaces the given sub block with a new block.
	 * 
	 * @param oldBlock
	 *            the old sub block.
	 * @param newBlock
	 *            the new sub block.
	 * @return false, if oldBlock wasn't a direct sub block.
	 */
	public boolean replaceSubBlock(StructuredBlock oldBlock,
			StructuredBlock newBlock) {
		for (int i = 0; i < 2; i++) {
			if (subBlocks[i] == oldBlock) {
				subBlocks[i] = newBlock;
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all sub block of this structured block.
	 */
	public StructuredBlock[] getSubBlocks() {
		return subBlocks;
	}

	/**
	 * Determines if there is a sub block, that flows through to the end of this
	 * block. If this returns true, you know that jump is null.
	 * 
	 * @return true, if the jump may be safely changed.
	 */
	public boolean jumpMayBeChanged() {
		return (subBlocks[1].jump != null || subBlocks[1].jumpMayBeChanged());
	}
}
