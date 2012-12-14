/* CreateConstantArray Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: CreateConstantArray.java,v 1.16.2.1 2002/05/28 17:34:08 hoenicke Exp $
 */

package jode.flow;

import jode.GlobalOptions;
import jode.expr.ArrayStoreOperator;
import jode.expr.ConstOperator;
import jode.expr.ConstantArrayOperator;
import jode.expr.Expression;
import jode.expr.NewArrayOperator;
import jode.expr.NopOperator;
import jode.expr.StoreInstruction;

public class CreateConstantArray {

	public static boolean transform(InstructionContainer ic,
			StructuredBlock last) {
		/*
		 * Situation: PUSH new Array[] // or a constant array operator. DUP //
		 * duplicate array reference POP[index] = value ...
		 */
		if (last.outer instanceof SequentialBlock) {

			SequentialBlock sequBlock = (SequentialBlock) last.outer;

			if (!(ic.getInstruction() instanceof StoreInstruction)
					|| ic.getInstruction().getFreeOperandCount() != 1
					|| !(sequBlock.subBlocks[0] instanceof SpecialBlock)
					|| !(sequBlock.outer instanceof SequentialBlock))
				return false;

			StoreInstruction store = (StoreInstruction) ic.getInstruction();

			if (!(store.getLValue() instanceof ArrayStoreOperator))
				return false;

			ArrayStoreOperator lvalue = (ArrayStoreOperator) store.getLValue();

			if (!(lvalue.getSubExpressions()[0] instanceof NopOperator)
					|| !(lvalue.getSubExpressions()[1] instanceof ConstOperator))
				return false;

			Expression expr = store.getSubExpressions()[1];
			ConstOperator indexOp = (ConstOperator) lvalue.getSubExpressions()[1];
			SpecialBlock dup = (SpecialBlock) sequBlock.subBlocks[0];
			sequBlock = (SequentialBlock) sequBlock.outer;

			if (dup.type != SpecialBlock.DUP || dup.depth != 0
					|| dup.count != 1
					|| !(indexOp.getValue() instanceof Integer)
					|| !(sequBlock.subBlocks[0] instanceof InstructionBlock))
				return false;

			int index = ((Integer) indexOp.getValue()).intValue();
			InstructionBlock ib = (InstructionBlock) sequBlock.subBlocks[0];

			if (ib.getInstruction() instanceof NewArrayOperator) {
				/* This is the first element */
				NewArrayOperator newArray = (NewArrayOperator) ib
						.getInstruction();
				if (newArray.getDimensions() != 1
						|| !(newArray.getSubExpressions()[0] instanceof ConstOperator))
					return false;

				ConstOperator countop = (ConstOperator) newArray
						.getSubExpressions()[0];
				if (!(countop.getValue() instanceof Integer))
					return false;

				int arraylength = ((Integer) countop.getValue()).intValue();
				if (arraylength <= index)
					return false;

				if (GlobalOptions.verboseLevel > 0)
					GlobalOptions.err.print('a');

				ConstantArrayOperator cao = new ConstantArrayOperator(
						newArray.getType(), arraylength);
				cao.setValue(index, expr);
				ic.setInstruction(cao);
				ic.moveDefinitions(sequBlock, last);
				last.replace(sequBlock);
				return true;
			} else if (ib.getInstruction() instanceof ConstantArrayOperator) {
				ConstantArrayOperator cao = (ConstantArrayOperator) ib
						.getInstruction();
				if (cao.setValue(index, expr)) {
					/* adding Element succeeded */
					ic.setInstruction(cao);
					ic.moveDefinitions(sequBlock, last);
					last.replace(sequBlock);
					return true;
				}
			}
		}
		return false;
	}
}
