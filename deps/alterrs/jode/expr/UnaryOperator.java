/* UnaryOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: UnaryOperator.java,v 2.13.4.2 2002/05/28 17:34:06 hoenicke Exp $
 */

package alterrs.jode.expr;

import alterrs.jode.decompiler.Options;
import alterrs.jode.decompiler.TabbedPrintWriter;
import alterrs.jode.type.Type;

public class UnaryOperator extends Operator {
	public UnaryOperator(Type type, int op) {
		super(type, op);
		initOperands(1);
	}

	public int getPriority() {
		return 700;
	}

	public Expression negate() {
		if (getOperatorIndex() == LOG_NOT_OP) {
			if (subExpressions != null)
				return subExpressions[0];
			else
				return new NopOperator(Type.tBoolean);
		}
		return super.negate();
	}

	public void updateSubTypes() {
		subExpressions[0].setType(Type.tSubType(type));
	}

	public void updateType() {
		updateParentType(Type.tSuperType(subExpressions[0].getType()));
	}

	public boolean opEquals(Operator o) {
		return (o instanceof UnaryOperator) && o.operatorIndex == operatorIndex;
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.print(getOperatorString());
		if ((Options.outputStyle & Options.GNU_SPACING) != 0)
			writer.print(" ");
		subExpressions[0].dumpExpression(writer, 700);
	}
}
