/* EmptyBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: EmptyBlock.java,v 3.6.4.2 2002/05/28 17:34:09 hoenicke Exp $
 */

package jode.flow;

import jode.decompiler.TabbedPrintWriter;

/**
 * This is the structured block for an empty block.
 */
public class EmptyBlock extends StructuredBlock {
	public EmptyBlock() {
	}

	public EmptyBlock(Jump jump) {
		setJump(jump);
	}

	/**
	 * Tells if this block is empty and only changes control flow.
	 */
	public boolean isEmpty() {
		return true;
	}

	/**
	 * Appends a block to this block.
	 * 
	 * @return the new combined block.
	 */
	public StructuredBlock appendBlock(StructuredBlock block) {
		if (outer instanceof ConditionalBlock) {
			IfThenElseBlock ifBlock = new IfThenElseBlock(
					((ConditionalBlock) outer).getInstruction());
			ifBlock.moveDefinitions(outer, this);
			ifBlock.replace(outer);
			ifBlock.moveJump(outer.jump);
			ifBlock.setThenBlock(this);
		}
		block.replace(this);
		return block;
	}

	/**
	 * Prepends a block to this block.
	 * 
	 * @return the new combined block.
	 */
	public StructuredBlock prependBlock(StructuredBlock block) {
		/* For empty blocks: append == prepend modulo jump */
		block = appendBlock(block);
		block.moveJump(this.jump);
		return block;
	}

	public void dumpInstruction(TabbedPrintWriter writer)
			throws java.io.IOException {
		/*
		 * Only print the comment if jump null, since otherwise the block isn't
		 * completely empty ;-)
		 */
		if (jump == null)
			writer.println("/* empty */");
	}
}
