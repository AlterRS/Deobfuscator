/* JsrBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: JsrBlock.java,v 4.7.4.2 2002/05/28 17:34:09 hoenicke Exp $
 */

package jode.flow;

import jode.decompiler.LocalInfo;
import jode.type.Type;

/**
 * This block represents a jsr instruction. A jsr instruction is used to call
 * the finally block, or to call the monitorexit block in a synchronized block.
 * 
 * @author Jochen Hoenicke
 */
public class JsrBlock extends StructuredBlock {
	/**
	 * The inner block that jumps to the subroutine.
	 */
	StructuredBlock innerBlock;
	boolean good = false;

	public JsrBlock(Jump subroutine, Jump next) {
		innerBlock = new EmptyBlock(subroutine);
		innerBlock.outer = this;
		setJump(next);
	}

	public void setGood(boolean g) {
		good = g;
	}

	public boolean isGood() {
		return good;
	}

	/*
	 * The implementation of getNext[Flow]Block is the standard implementation
	 */

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
		if (innerBlock == oldBlock)
			innerBlock = newBlock;
		else
			return false;
		return true;
	}

	/**
	 * This is called after the analysis is completely done. It will remove all
	 * PUSH/stack_i expressions, (if the bytecode is correct).
	 * <p>
	 * The default implementation merges the stack after each sub block. This
	 * may not be, what you want.
	 * <p>
	 * 
	 * @param initialStack
	 *            the stackmap at begin of the block
	 * @return the stack after the block has executed.
	 * @throw RuntimeException if something did get wrong.
	 */
	public VariableStack mapStackToLocal(VariableStack stack) {
		/*
		 * There shouldn't be any JSR blocks remaining, but who knows.
		 */
		/*
		 * The innerBlock is startet with a new stack entry (return address) It
		 * should GOTO immediately and never complete.
		 */
		LocalInfo retAddr = new LocalInfo();
		retAddr.setType(Type.tUObject);
		innerBlock.mapStackToLocal(stack.push(retAddr));
		if (jump != null) {
			jump.stackMap = stack;
			return null;
		}
		return stack;
	}

	/**
	 * Returns all sub block of this structured block.
	 */
	public StructuredBlock[] getSubBlocks() {
		return new StructuredBlock[] { innerBlock };
	}

	public void dumpInstruction(jode.decompiler.TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.println("JSR");
		writer.tab();
		innerBlock.dumpSource(writer);
		writer.untab();
	}
}
