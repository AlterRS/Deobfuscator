/* CreateExpression Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: CreateExpression.java,v 4.17.4.1 2002/05/28 17:34:08 hoenicke Exp $
 */

package alterrs.jode.flow;

import alterrs.jode.GlobalOptions;
import alterrs.jode.expr.CombineableOperator;
import alterrs.jode.expr.Expression;

/**
 * This transformation creates expressions. It transforms
 * 
 * <pre>
 *  Sequ[expr_1, Sequ[expr_2, ..., Sequ[expr_n, op] ...]]
 * </pre>
 * 
 * to
 * 
 * <pre>
 *  expr(op, [ expr_1, ..., expr_n ])
 * </pre>
 */
public class CreateExpression {

	/**
	 * This does the transformation.
	 * 
	 * @param FlowBlock
	 *            the flow block to transform.
	 * @return true if flow block was simplified.
	 */
	public static boolean transform(InstructionContainer ic,
			StructuredBlock last) {
		int params = ic.getInstruction().getFreeOperandCount();
		if (params == 0)
			return false;

		if (!(last.outer instanceof SequentialBlock))
			return false;
		SequentialBlock sequBlock = (SequentialBlock) last.outer;

		/*
		 * First check if Expression can be created, but do nothing yet.
		 */
		Expression lastExpression = ic.getInstruction();
		while (true) {
			if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
				return false;

			Expression expr = ((InstructionBlock) sequBlock.subBlocks[0])
					.getInstruction();

			if (!expr.isVoid())
				break;

			if (expr.getFreeOperandCount() > 0
					|| !(expr instanceof CombineableOperator)
					|| lastExpression.canCombine((CombineableOperator) expr) <= 0)
				return false;

			/*
			 * Hmm, we should really set lastExpression to
			 * lastExpression.combine(expr), but that may change the expressions
			 * :-( XXX
			 * 
			 * We do a conservative approach and check if there are no possible
			 * side effects with the skipped expressions. Theoretically we would
			 * only have to check expressions, that are combined at an earlier
			 * point.
			 */
			SequentialBlock block = sequBlock;
			while (block != last.outer) {
				block = (SequentialBlock) block.subBlocks[1];
				if (((InstructionBlock) block.subBlocks[0]).getInstruction()
						.hasSideEffects(expr))
					return false;
			}

			if (!(sequBlock.outer instanceof SequentialBlock))
				return false;
			sequBlock = (SequentialBlock) sequBlock.outer;
		}

		/*
		 * Now, do the combination. Everything must succeed now.
		 */
		sequBlock = (SequentialBlock) last.outer;
		lastExpression = ic.getInstruction();
		while (true) {

			Expression expr = ((InstructionBlock) sequBlock.subBlocks[0])
					.getInstruction();

			if (!expr.isVoid()) {
				lastExpression = lastExpression.addOperand(expr);
				break;
			}

			lastExpression = lastExpression.combine((CombineableOperator) expr);
			sequBlock = (SequentialBlock) sequBlock.outer;
		}

		if (GlobalOptions.verboseLevel > 0
				&& lastExpression.getFreeOperandCount() == 0)
			GlobalOptions.err.print('x');

		ic.setInstruction(lastExpression);
		ic.moveDefinitions(sequBlock, last);
		last.replace(sequBlock);
		return true;
	}
}
