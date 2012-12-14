/* ThrowBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ThrowBlock.java,v 4.13.2.1 2002/05/28 17:34:09 hoenicke Exp $
 */

package jode.flow;

import jode.decompiler.TabbedPrintWriter;
import jode.expr.Expression;

/**
 * This is the structured block for an Throw block.
 */
public class ThrowBlock extends ReturnBlock {
	public ThrowBlock(Expression instr) {
		super(instr);
	}

	public void dumpInstruction(TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.print("throw ");
		instr.dumpExpression(writer.NO_PAREN, writer);
		writer.println(";");
	}
}
