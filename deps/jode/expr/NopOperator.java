/* NopOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: NopOperator.java,v 2.15.4.1 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import jode.decompiler.TabbedPrintWriter;
import jode.type.Type;

/**
 * A NopOperator takes one arguments and returns it again. It is used as
 * placeholder when the real operator is not yet known (e.g. in SwitchBlock, but
 * also in every other Operator).
 * <p/>
 * A Nop operator doesn't have subExpressions; getSubExpressions() simply
 * returns zero.
 * 
 * @author Jochen Hoenicke
 */
public class NopOperator extends Expression {
	public NopOperator(Type type) {
		super(type);
	}

	public int getFreeOperandCount() {
		return 1;
	}

	public int getPriority() {
		return 1000;
	}

	public void updateSubTypes() {
	}

	public void updateType() {
	}

	public Expression addOperand(Expression op) {
		op.setType(type);
		op.parent = parent;
		return op;
	}

	public boolean isConstant() {
		return false;
	}

	public boolean equals(Object o) {
		return (o instanceof NopOperator);
	}

	public Expression simplify() {
		return this;
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.print("POP");
	}
}
