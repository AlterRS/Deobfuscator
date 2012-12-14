/* CaseBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: CaseBlock.java,v 4.15.4.2 2002/05/28 17:34:08 hoenicke Exp $
 */

package jode.flow;

import jode.expr.ConstOperator;
import jode.type.Type;

/**
 * This block represents a case instruction. A case instruction is a part of a
 * switch construction and may have a subpart or not (for multiple case directly
 * after each other.
 * 
 * @author Jochen Hoenicke
 */
public class CaseBlock extends StructuredBlock {
	/**
	 * The inner block that jumps to the subroutine, or null if this is a value
	 * only.
	 */
	StructuredBlock subBlock;

	/**
	 * The value of this case.
	 */
	int value;

	/**
	 * True, if this is the default case
	 */
	boolean isDefault = false;

	/**
	 * True, if the previous case falls through to this case
	 */
	boolean isFallThrough = false;

	/**
	 * True, if this is the last case in a switch
	 */
	boolean isLastBlock = false;

	public CaseBlock(int value) {
		this.value = value;
		subBlock = null;
	}

	public CaseBlock(int value, Jump dest) {
		this.value = value;
		subBlock = new EmptyBlock(dest);
		subBlock.outer = this;
	}

	public void checkConsistent() {
		if (!(outer instanceof SwitchBlock))
			throw new jode.AssertError("Inconsistency");
		super.checkConsistent();
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
		if (subBlock == oldBlock)
			subBlock = newBlock;
		else
			return false;
		return true;
	}

	/**
	 * Tells if we want braces around this case. We only need braces if there is
	 * a declaration on the first level (list of sequential blocks).
	 */
	protected boolean wantBraces() {
		StructuredBlock block = subBlock;
		if (block == null)
			return false;
		for (;;) {
			if (block.declare != null && !block.declare.isEmpty()) {
				/* A declaration; we need braces. */
				return true;
			}

			if (!(block instanceof SequentialBlock)) {
				/*
				 * This was the last block on the first level. Finally check if
				 * that is an declaring InstructionBlock.
				 */
				if (block instanceof InstructionBlock
						&& ((InstructionBlock) block).isDeclaration)
					return true;

				/*
				 * If we get here, we need no braces.
				 */
				return false;
			}

			StructuredBlock[] subBlocks = block.getSubBlocks();
			if (subBlocks[0] instanceof InstructionBlock
					&& ((InstructionBlock) subBlocks[0]).isDeclaration) {
				/*
				 * An instruction block declares on the same level as the
				 * surrounding SequentialBlock.
				 */
				return true;
			}

			/* continue with the second sub block. */
			block = subBlocks[1];
		}
	}

	/**
	 * Returns all sub block of this structured block.
	 */
	public StructuredBlock[] getSubBlocks() {
		return (subBlock != null) ? new StructuredBlock[] { subBlock }
				: new StructuredBlock[0];
	}

	public void dumpInstruction(jode.decompiler.TabbedPrintWriter writer)
			throws java.io.IOException {
		if (isDefault) {
			/*
			 * If this is the default case and does nothing, we can skip this.
			 * We have to make sure, that nothing flows into the default block
			 * though. XXX remove if sure.
			 */
			if (isLastBlock && subBlock instanceof EmptyBlock
					&& subBlock.jump == null)
				return;
			if (subBlock instanceof BreakBlock
					&& ((BreakBlock) subBlock).breaksBlock == this) {
				/* make sure that the previous block is correctly breaked */
				if (isFallThrough) {
					writer.tab();
					subBlock.dumpSource(writer);
					writer.untab();
				}
				return;
			}
			if (isFallThrough) {
				writer.tab();
				writer.println("/* fall through */");
				writer.untab();
			}
			writer.print("default:");
		} else {
			if (isFallThrough) {
				writer.tab();
				writer.println("/* fall through */");
				writer.untab();
			}
			ConstOperator constOp = new ConstOperator(new Integer(value));
			Type type = ((SwitchBlock) outer).getInstruction().getType();
			constOp.setType(type);
			constOp.makeInitializer(type);
			writer.print("case " + constOp.toString() + ":");
		}
		if (subBlock != null) {
			boolean needBraces = wantBraces();
			if (needBraces)
				writer.openBrace();
			else
				writer.println();
			if (subBlock != null) {
				writer.tab();
				subBlock.dumpSource(writer);
				writer.untab();
			}
			if (needBraces)
				writer.closeBrace();
		} else
			writer.println();
	}

	/**
	 * Determines if there is a sub block, that flows through to the end of this
	 * block. If this returns true, you know that jump is null.
	 * 
	 * @return true, if the jump may be safely changed.
	 */
	public boolean jumpMayBeChanged() {
		return subBlock.jump != null || subBlock.jumpMayBeChanged();
	}
}
