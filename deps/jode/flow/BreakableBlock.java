/* BreakableBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: BreakableBlock.java,v 3.4.4.1 2002/05/28 17:34:08 hoenicke Exp $
 */

package jode.flow;

/**
 * This is a structured block, that supports break.
 */
public interface BreakableBlock {

	/**
	 * Returns the label of this block and creates a new label, if there wasn't
	 * a label previously.
	 */
	public String getLabel();

	/**
	 * Is called by BreakBlock, to tell us that this block is breaked.
	 */
	public void setBreaked();

	/**
	 * Is called by BreakBlock, to tell us what the stack can be after a break.
	 * 
	 * @param stack
	 *            the stack at the break.
	 */
	public void mergeBreakedStack(VariableStack stack);
}
