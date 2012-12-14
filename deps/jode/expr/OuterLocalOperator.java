/* OuterLocalOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: OuterLocalOperator.java,v 1.3.2.1 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import jode.GlobalOptions;
import jode.decompiler.LocalInfo;
import jode.decompiler.TabbedPrintWriter;

public class OuterLocalOperator extends Operator {
	LocalInfo local;

	public OuterLocalOperator(LocalInfo local) {
		super(local.getType());
		this.local = local;
		initOperands(0);
	}

	public boolean isConstant() {
		return true;
	}

	public int getPriority() {
		return 1000;
	}

	public LocalInfo getLocalInfo() {
		return local.getLocalInfo();
	}

	public void updateSubTypes() {
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
			GlobalOptions.err.println("setType of " + local.getName() + ": "
					+ local.getType());
		local.setType(type);
	}

	public void updateType() {
	}

	public boolean opEquals(Operator o) {
		return (o instanceof OuterLocalOperator && ((OuterLocalOperator) o).local
				.getSlot() == local.getSlot());
	}

	public Expression simplify() {
		return super.simplify();
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.print(local.getName());
	}
}
