/* BreakBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: BreakBlock.java,v 3.7.4.1 2002/05/28 17:34:08 hoenicke Exp $
 */

package jode.flow;

import jode.decompiler.TabbedPrintWriter;

/**
 *
 */
public class BreakBlock extends StructuredBlock {
	StructuredBlock breaksBlock;
	String label;

	public BreakBlock(BreakableBlock breaksBlock, boolean needsLabel) {
		this.breaksBlock = (StructuredBlock) breaksBlock;
		breaksBlock.setBreaked();
		if (needsLabel)
			label = breaksBlock.getLabel();
		else
			label = null;
	}

	public void checkConsistent() {
		super.checkConsistent();
		StructuredBlock sb = outer;
		while (sb != breaksBlock) {
			if (sb == null)
				throw new RuntimeException("Inconsistency");
			sb = sb.outer;
		}
	}

	/**
	 * Tells if this block is empty and only changes control flow.
	 */
	public boolean isEmpty() {
		return true;
	}

	/**
	 * Returns the block where the control will normally flow to, when this
	 * block is finished.
	 */
	public StructuredBlock getNextBlock() {
		return breaksBlock.getNextBlock();
	}

	/**
	 * Returns the flow block where the control will normally flow to, when this
	 * block is finished (not ignoring the jump after this block).
	 * 
	 * @return null, if the control flows into a non empty structured block or
	 *         if this is the outermost block.
	 */
	public FlowBlock getNextFlowBlock() {
		return breaksBlock.getNextFlowBlock();
	}

	/**
	 * This is called after the analysis is completely done. It will remove all
	 * PUSH/stack_i expressions, (if the bytecode is correct).
	 * 
	 * @param stack
	 *            the stackmap at begin of the block
	 * @return null since the stack has no successor.
	 */
	public VariableStack mapStackToLocal(VariableStack stack) {
		((BreakableBlock) breaksBlock).mergeBreakedStack(stack);
		return null;
	}

	/**
	 * Tells if this block needs braces when used in a if or while block.
	 * 
	 * @return true if this block should be sorrounded by braces.
	 */
	public boolean needsBraces() {
		return false;
	}

	public void dumpInstruction(TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.println("break" + (label == null ? "" : " " + label) + ";");
	}

	public boolean jumpMayBeChanged() {
		return true;
	}
}
