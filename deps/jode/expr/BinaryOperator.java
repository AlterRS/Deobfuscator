/* BinaryOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: BinaryOperator.java,v 4.12.2.1 2002/05/28 17:34:05 hoenicke Exp $
 */

package jode.expr;

import jode.decompiler.TabbedPrintWriter;
import jode.type.Type;

public class BinaryOperator extends Operator {

	public BinaryOperator(Type type, int op) {
		super(type, op);
		initOperands(2);
	}

	public int getPriority() {
		switch (operatorIndex) {
		case 1:
		case 2:
			return 610;
		case 3:
		case 4:
		case 5:
			return 650;
		case 6:
		case 7:
		case 8:
			return 600;
		case 9:
			return 450;
		case 10:
			return 410;
		case 11:
			return 420;
		case 12:
		case 13:
		case 14:
		case 15:
		case 16:
		case 17:
		case 18:
		case 19:
		case 20:
		case 21:
		case 22:
		case 23:
			return 100;
		case LOG_OR_OP:
			return 310;
		case LOG_AND_OP:
			return 350;
		}
		throw new RuntimeException("Illegal operator");
	}

	public void updateSubTypes() {
		subExpressions[0].setType(Type.tSubType(type));
		subExpressions[1].setType(Type.tSubType(type));
	}

	public void updateType() {
		Type leftType = Type.tSuperType(subExpressions[0].getType());
		Type rightType = Type.tSuperType(subExpressions[1].getType());
		subExpressions[0].setType(Type.tSubType(rightType));
		subExpressions[1].setType(Type.tSubType(leftType));
		updateParentType(leftType.intersection(rightType));
	}

	public Expression negate() {
		if (getOperatorIndex() == LOG_AND_OP || getOperatorIndex() == LOG_OR_OP) {
			setOperatorIndex(getOperatorIndex() ^ 1);
			for (int i = 0; i < 2; i++) {
				subExpressions[i] = subExpressions[i].negate();
				subExpressions[i].parent = this;
			}
			return this;
		}
		return super.negate();
	}

	public boolean opEquals(Operator o) {
		return (o instanceof BinaryOperator)
				&& o.operatorIndex == operatorIndex;
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		subExpressions[0].dumpExpression(writer, getPriority());
		writer.breakOp();
		writer.print(getOperatorString());
		subExpressions[1].dumpExpression(writer, getPriority() + 1);
	}
}
