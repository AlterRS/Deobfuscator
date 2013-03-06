/* CreateClassField Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: CreateClassField.java,v 1.8.2.2 2002/05/28 17:34:08 hoenicke Exp $
 */

package alterrs.jode.flow;

import alterrs.jode.expr.ClassFieldOperator;
import alterrs.jode.expr.CompareUnaryOperator;
import alterrs.jode.expr.ConstOperator;
import alterrs.jode.expr.Expression;
import alterrs.jode.expr.GetFieldOperator;
import alterrs.jode.expr.InvokeOperator;
import alterrs.jode.expr.Operator;
import alterrs.jode.expr.PutFieldOperator;
import alterrs.jode.expr.StoreInstruction;
import alterrs.jode.type.Type;

public class CreateClassField {

	public static boolean transform(IfThenElseBlock ifBlock,
			StructuredBlock last) {
		// convert
		// if (class$classname == null)
		// class$classname = class$("java.lang.Object");
		// to
		// if (classname.class == null) {
		// }
		if (!(ifBlock.cond instanceof CompareUnaryOperator)
				|| !(((Operator) ifBlock.cond).getOperatorIndex() == Operator.EQUALS_OP)
				|| !(ifBlock.thenBlock instanceof InstructionBlock)
				|| ifBlock.elseBlock != null)
			return false;

		if (ifBlock.thenBlock.jump != null
				&& (ifBlock.jump == null || (ifBlock.jump.destination != ifBlock.thenBlock.jump.destination)))
			return false;

		CompareUnaryOperator cmp = (CompareUnaryOperator) ifBlock.cond;
		Expression instr = ((InstructionBlock) ifBlock.thenBlock)
				.getInstruction();
		if (!(cmp.getSubExpressions()[0] instanceof GetFieldOperator)
				|| !(instr instanceof StoreInstruction))
			return false;

		StoreInstruction store = (StoreInstruction) instr;
		if (!(store.getLValue() instanceof PutFieldOperator))
			return false;
		PutFieldOperator put = (PutFieldOperator) store.getLValue();
		if (put.getField() == null
				|| !put.matches((GetFieldOperator) cmp.getSubExpressions()[0])
				|| !(store.getSubExpressions()[1] instanceof InvokeOperator))
			return false;

		InvokeOperator invoke = (InvokeOperator) store.getSubExpressions()[1];
		if (!invoke.isGetClass())
			return false;

		Expression param = invoke.getSubExpressions()[0];

		if (param instanceof ConstOperator
				&& ((ConstOperator) param).getValue() instanceof String) {
			String clazz = (String) ((ConstOperator) param).getValue();
			if (put.getField().setClassConstant(clazz)) {
				cmp.setSubExpressions(
						0,
						new ClassFieldOperator(clazz.charAt(0) == '[' ? Type
								.tType(clazz) : Type.tClass(clazz)));
				EmptyBlock empty = new EmptyBlock();
				empty.moveJump(ifBlock.thenBlock.jump);
				ifBlock.setThenBlock(empty);
				return true;
			}
		}
		return false;
	}
}
