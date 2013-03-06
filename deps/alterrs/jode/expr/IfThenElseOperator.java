/* IfThenElseOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: IfThenElseOperator.java,v 2.17.2.3 2002/05/28 17:34:06 hoenicke Exp $
 */

package alterrs.jode.expr;

import alterrs.jode.decompiler.FieldAnalyzer;
import alterrs.jode.decompiler.TabbedPrintWriter;
import alterrs.jode.type.Type;

public class IfThenElseOperator extends Operator {
	public IfThenElseOperator(Type type) {
		super(type, 0);
		initOperands(3);
	}

	public int getPriority() {
		return 200;
	}

	public void updateSubTypes() {
		subExpressions[0].setType(Type.tBoolean);
		subExpressions[1].setType(Type.tSubType(type));
		subExpressions[2].setType(Type.tSubType(type));
	}

	public void updateType() {
		Type commonType = Type.tSuperType(subExpressions[1].getType())
				.intersection(Type.tSuperType(subExpressions[2].getType()));
		updateParentType(commonType);
	}

	public Expression simplify() {
		if (getType().isOfType(Type.tBoolean)) {
			if (subExpressions[1] instanceof ConstOperator
					&& subExpressions[2] instanceof ConstOperator) {
				ConstOperator c1 = (ConstOperator) subExpressions[1];
				ConstOperator c2 = (ConstOperator) subExpressions[2];
				if (c1.getValue().equals(new Integer(1))
						&& c2.getValue().equals(new Integer(0)))
					return subExpressions[0].simplify();
				if (c2.getValue().equals(new Integer(1))
						&& c1.getValue().equals(new Integer(0)))
					return subExpressions[0].negate().simplify();
			}
		}
		if (subExpressions[0] instanceof CompareUnaryOperator
				&& ((((CompareUnaryOperator) subExpressions[0])
						.getOperatorIndex() & ~1) == Operator.COMPARE_OP)) {
			CompareUnaryOperator cmp = (CompareUnaryOperator) subExpressions[0];
			int cmpType = cmp.getOperatorIndex() & 1;
			if ((subExpressions[2 - cmpType] instanceof GetFieldOperator)
					&& (subExpressions[1 + cmpType] instanceof StoreInstruction)) {
				// Check for
				// class$classname != null ? class$classname :
				// (class$classname = class$("classname"))
				// and replace with
				// classname.class
				GetFieldOperator get = (GetFieldOperator) subExpressions[2 - cmpType];
				StoreInstruction put = (StoreInstruction) subExpressions[1 + cmpType];
				int opIndex = cmp.getOperatorIndex();
				FieldAnalyzer field;
				if (put.getLValue() instanceof PutFieldOperator
						&& ((field = ((PutFieldOperator) put.getLValue())
								.getField()) != null)
						&& field.isSynthetic()
						&& put.lvalueMatches(get)
						&& (cmp.subExpressions[0] instanceof GetFieldOperator)
						&& put.lvalueMatches((GetFieldOperator) cmp.subExpressions[0])
						&& put.subExpressions[1] instanceof InvokeOperator) {
					InvokeOperator invoke = (InvokeOperator) put.subExpressions[1];
					if (invoke.isGetClass()
							&& invoke.subExpressions[0] instanceof ConstOperator
							&& (invoke.subExpressions[0].getType()
									.equals(Type.tString))) {
						String clazz = (String) ((ConstOperator) invoke.subExpressions[0])
								.getValue();
						if (field.setClassConstant(clazz))
							return new ClassFieldOperator(
									clazz.charAt(0) == '[' ? Type.tType(clazz)
											: Type.tClass(clazz));
					}
				}
			}
		}
		return super.simplify();
	}

	public boolean opEquals(Operator o) {
		return (o instanceof IfThenElseOperator);
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		subExpressions[0].dumpExpression(writer, 201);
		writer.breakOp();
		writer.print(" ? ");
		int subPriority = 0;
		if (!subExpressions[1].getType().getHint()
				.isOfType(subExpressions[2].getType())) {
			writer.startOp(writer.IMPL_PAREN, 2);
			/* We need a cast here */
			writer.print("(");
			writer.printType(getType().getHint());
			writer.print(") ");
			subPriority = 700;
		}
		subExpressions[1].dumpExpression(writer, subPriority);
		if (subPriority == 700)
			writer.endOp();
		writer.breakOp();
		writer.print(" : ");
		subExpressions[2].dumpExpression(writer, 200);
	}
}
