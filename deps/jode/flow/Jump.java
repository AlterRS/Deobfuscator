/* Jump Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: Jump.java,v 3.12.2.1 2002/05/28 17:34:09 hoenicke Exp $
 */

package jode.flow;

/**
 * This class represents an unconditional jump.
 */
public class Jump {
	/**
	 * The structured block that precedes this jump.
	 */
	StructuredBlock prev;
	/**
	 * The destination block of this jump, null if not known, or illegal.
	 */
	FlowBlock destination;

	/**
	 * The jumps in a flow block, that have the same destination, are in a link
	 * list. This field points to the next jump in this link.
	 */
	Jump next;

	/**
	 * The stack map. This tells how many objects are on stack at begin of the
	 * flow block, and to what locals they are maped.
	 * 
	 * @see FlowBlock.mapStackToLocal
	 */
	VariableStack stackMap;

	public Jump(FlowBlock dest) {
		this.destination = dest;
	}

	public Jump(Jump jump) {
		destination = jump.destination;
		next = jump.next;
		jump.next = this;
	}

	/**
	 * Print the source code for this structured block. This handles everything
	 * that is unique for all structured blocks and calls dumpInstruction
	 * afterwards.
	 * 
	 * @param writer
	 *            The tabbed print writer, where we print to.
	 */
	public void dumpSource(jode.decompiler.TabbedPrintWriter writer)
			throws java.io.IOException {
		if (destination == null)
			writer.println("GOTO null-ptr!!!!!");
		else
			writer.println("GOTO " + destination.getLabel());
	}
}
