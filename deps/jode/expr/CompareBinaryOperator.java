/* CompareBinaryOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: CompareBinaryOperator.java,v 4.11.2.1 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import jode.decompiler.TabbedPrintWriter;
import jode.type.Type;

public class CompareBinaryOperator extends Operator {
	boolean allowsNaN = false;
	Type compareType;

	public CompareBinaryOperator(Type type, int op) {
		super(Type.tBoolean, op);
		compareType = type;
		initOperands(2);
	}

	public CompareBinaryOperator(Type type, int op, boolean allowsNaN) {
		super(Type.tBoolean, op);
		compareType = type;
		this.allowsNaN = allowsNaN;
		initOperands(2);
	}

	public int getPriority() {
		switch (getOperatorIndex()) {
		case 26:
		case 27:
			return 500;
		case 28:
		case 29:
		case 30:
		case 31:
			return 550;
		}
		throw new RuntimeException("Illegal operator");
	}

	public Type getCompareType() {
		return compareType;
	}

	public void updateSubTypes() {
		subExpressions[0].setType(Type.tSubType(compareType));
		subExpressions[1].setType(Type.tSubType(compareType));
	}

	public void updateType() {
		Type leftType = Type.tSuperType(subExpressions[0].getType());
		Type rightType = Type.tSuperType(subExpressions[1].getType());
		compareType = compareType.intersection(leftType)
				.intersection(rightType);
		subExpressions[0].setType(Type.tSubType(rightType));
		subExpressions[1].setType(Type.tSubType(leftType));
		/* propagate hints? XXX */
	}

	public Expression negate() {
		if (!allowsNaN || getOperatorIndex() <= NOTEQUALS_OP) {
			setOperatorIndex(getOperatorIndex() ^ 1);
			return this;
		}
		return super.negate();
	}

	public boolean opEquals(Operator o) {
		return (o instanceof CompareBinaryOperator)
				&& o.operatorIndex == operatorIndex;
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		subExpressions[0].dumpExpression(writer, getPriority() + 1);
		writer.breakOp();
		writer.print(getOperatorString());
		subExpressions[1].dumpExpression(writer, getPriority() + 1);
	}
}
