/* CombineIfGotoExpressions Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: CombineIfGotoExpressions.java,v 4.15.2.1 2002/05/28 17:34:08 hoenicke Exp $
 */

package alterrs.jode.flow;

import alterrs.jode.expr.BinaryOperator;
import alterrs.jode.expr.CombineableOperator;
import alterrs.jode.expr.Expression;
import alterrs.jode.type.Type;

public class CombineIfGotoExpressions {

	public static boolean transform(ConditionalBlock cb, StructuredBlock last) {
		if (cb.jump == null || !(last.outer instanceof SequentialBlock))
			return false;

		SequentialBlock sequBlock = (SequentialBlock) cb.outer;
		Expression firstCond, secondCond;

		secondCond = cb.getInstruction();
		Expression lastCombined = secondCond;

		while (sequBlock.subBlocks[0] instanceof InstructionBlock) {
			InstructionBlock ib = (InstructionBlock) sequBlock.subBlocks[0];

			if (!(sequBlock.outer instanceof SequentialBlock))
				return false;

			Expression expr = ib.getInstruction();
			if (!(expr instanceof CombineableOperator)
					|| (lastCombined.canCombine((CombineableOperator) expr)
							+ secondCond.canCombine((CombineableOperator) expr) <= 0))
				/*
				 * Tricky, the above is true, iff one of the two Expressions
				 * conflict, or both fail.
				 */
				return false;

			lastCombined = expr;

			sequBlock = (SequentialBlock) sequBlock.outer;
		}

		if (sequBlock.subBlocks[0] instanceof ConditionalBlock) {

			ConditionalBlock cbprev = (ConditionalBlock) sequBlock.subBlocks[0];

			Jump prevJump = cbprev.trueBlock.jump;

			int operator;
			if (prevJump.destination == cb.jump.destination) {
				operator = BinaryOperator.LOG_AND_OP;
				firstCond = cbprev.getInstruction().negate();
			} else if (prevJump.destination == cb.trueBlock.jump.destination) {
				operator = BinaryOperator.LOG_OR_OP;
				firstCond = cbprev.getInstruction();
			} else
				return false;

			/*
			 * We have changed some instructions above. We may never return with
			 * a failure now.
			 */

			sequBlock = (SequentialBlock) cb.outer;
			while (sequBlock.subBlocks[0] instanceof InstructionBlock) {
				/*
				 * Now combine the expression. Everything should succeed,
				 * because we have checked above.
				 */
				InstructionBlock ib = (InstructionBlock) sequBlock.subBlocks[0];

				Expression expr = ib.getInstruction();

				secondCond = secondCond.combine((CombineableOperator) expr);
				sequBlock = (SequentialBlock) sequBlock.outer;
			}

			cb.flowBlock.removeSuccessor(prevJump);
			prevJump.prev.removeJump();
			Expression cond = new BinaryOperator(Type.tBoolean, operator)
					.addOperand(secondCond).addOperand(firstCond);
			cb.setInstruction(cond);
			cb.moveDefinitions(sequBlock, last);
			last.replace(sequBlock);
			return true;
		}
		return false;
	}
}
