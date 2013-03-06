/* RetBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: RetBlock.java.in,v 4.1.2.1 2002/05/28 17:34:09 hoenicke Exp $
 */

package alterrs.jode.flow;

import java.util.Collections;
import java.util.Set;

import alterrs.jode.decompiler.LocalInfo;

/**
 * This block represents a ret instruction. A ret instruction is used to call
 * the finally block, or to call the monitorexit block in a synchronized block.
 * 
 * @author Jochen Hoenicke
 */
public class RetBlock extends StructuredBlock {
	/**
	 * The local containing the return address
	 */
	LocalInfo local;

	public RetBlock(LocalInfo local) {
		this.local = local;
	}

	/**
	 * Fill all in variables into the given VariableSet.
	 * 
	 * @param in
	 *            The VariableSet, the in variables should be stored to.
	 */
	public void fillInGenSet(Set in, Set gen) {
		in.add(local);
		gen.add(local);
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
		if (!stack.isEmpty())
			throw new IllegalArgumentException("stack is not empty at RET");
		return null;
	}

	public Set getDeclarables() {
		return Collections.singleton(local);
	}

	public void dumpInstruction(alterrs.jode.decompiler.TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.println("RET " + local);
	}
}
