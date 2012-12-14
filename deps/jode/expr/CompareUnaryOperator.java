/* CompareUnaryOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: CompareUnaryOperator.java,v 2.16.2.1 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import jode.decompiler.TabbedPrintWriter;
import jode.type.Type;

public class CompareUnaryOperator extends Operator {
	boolean objectType;
	Type compareType;

	public CompareUnaryOperator(Type type, int op) {
		super(Type.tBoolean, op);
		compareType = type;
		objectType = (type.isOfType(Type.tUObject));
		initOperands(1);
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
	}

	public void updateType() {
	}

	public Expression simplify() {
		if (subExpressions[0] instanceof CompareToIntOperator) {

			CompareToIntOperator cmpOp = (CompareToIntOperator) subExpressions[0];

			boolean negated = false;
			int opIndex = getOperatorIndex();
			if (cmpOp.allowsNaN && getOperatorIndex() > NOTEQUALS_OP) {
				if (cmpOp.greaterOnNaN == (opIndex == GREATEREQ_OP || opIndex == GREATER_OP)) {
					negated = true;
					opIndex ^= 1;
				}
			}
			Expression newOp = new CompareBinaryOperator(cmpOp.compareType,
					opIndex, cmpOp.allowsNaN).addOperand(
					cmpOp.subExpressions[1])
					.addOperand(cmpOp.subExpressions[0]);

			if (negated)
				return newOp.negate().simplify();
			return newOp.simplify();
		}
		if (subExpressions[0].getType().isOfType(Type.tBoolean)) {
			/* xx == false */
			if (getOperatorIndex() == EQUALS_OP)
				return subExpressions[0].negate().simplify();
			/* xx != false */
			if (getOperatorIndex() == NOTEQUALS_OP)
				return subExpressions[0].simplify();
		}
		return super.simplify();
	}

	public Expression negate() {
		if ((getType() != Type.tFloat && getType() != Type.tDouble)
				|| getOperatorIndex() <= NOTEQUALS_OP) {
			setOperatorIndex(getOperatorIndex() ^ 1);
			return this;
		}
		return super.negate();
	}

	public boolean opEquals(Operator o) {
		return (o instanceof CompareUnaryOperator)
				&& o.getOperatorIndex() == getOperatorIndex();
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		subExpressions[0].dumpExpression(writer, getPriority() + 1);
		writer.breakOp();
		writer.print(getOperatorString());
		writer.print(objectType ? "null" : "0");
	}
}
