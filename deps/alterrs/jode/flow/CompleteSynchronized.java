/* CompleteSynchronized Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: CompleteSynchronized.java,v 4.14.4.1 2002/05/28 17:34:08 hoenicke Exp $
 */

package alterrs.jode.flow;

import alterrs.jode.GlobalOptions;
import alterrs.jode.expr.Expression;
import alterrs.jode.expr.LocalLoadOperator;
import alterrs.jode.expr.LocalStoreOperator;
import alterrs.jode.expr.MonitorEnterOperator;
import alterrs.jode.expr.StoreInstruction;

public class CompleteSynchronized {

	/**
	 * This combines the monitorenter into a synchronized statement
	 * 
	 * @param flow
	 *            The FlowBlock that is transformed
	 */
	public static boolean enter(SynchronizedBlock synBlock, StructuredBlock last) {

		if (!(last.outer instanceof SequentialBlock))
			return false;

		/* If the program is well formed, the following succeed */
		SequentialBlock sequBlock = (SequentialBlock) synBlock.outer;
		if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
			return false;

		Expression monenter = ((InstructionBlock) sequBlock.subBlocks[0])
				.getInstruction();

		if (!(monenter instanceof MonitorEnterOperator))
			return false;

		Expression loadOp = ((MonitorEnterOperator) monenter)
				.getSubExpressions()[0];

		if (!(loadOp instanceof LocalLoadOperator)
				|| (((LocalLoadOperator) loadOp).getLocalInfo() != synBlock.local
						.getLocalInfo()))
			return false;

		if (GlobalOptions.verboseLevel > 0)
			GlobalOptions.err.print('s');

		synBlock.isEntered = true;
		synBlock.moveDefinitions(last.outer, last);
		last.replace(last.outer);
		return true;
	}

	/**
	 * This combines the initial expression describing the object into a
	 * synchronized statement
	 * 
	 * @param flow
	 *            The FlowBlock that is transformed
	 */
	public static boolean combineObject(SynchronizedBlock synBlock,
			StructuredBlock last) {

		/* Is there another expression? */
		if (!(last.outer instanceof SequentialBlock))
			return false;
		SequentialBlock sequBlock = (SequentialBlock) last.outer;

		if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
			return false;
		InstructionBlock ib = (InstructionBlock) sequBlock.subBlocks[0];

		if (!(ib.getInstruction() instanceof StoreInstruction))
			return false;
		StoreInstruction assign = (StoreInstruction) ib.getInstruction();

		if (!(assign.getLValue() instanceof LocalStoreOperator))
			return false;
		LocalStoreOperator lvalue = (LocalStoreOperator) assign.getLValue();

		if (lvalue.getLocalInfo() != synBlock.local.getLocalInfo()
				|| assign.getSubExpressions()[1] == null)
			return false;

		synBlock.object = assign.getSubExpressions()[1];
		synBlock.moveDefinitions(last.outer, last);
		last.replace(last.outer);
		return true;
	}
}
