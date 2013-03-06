/* PopOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: PopOperator.java,v 2.14.4.2 2002/05/28 17:34:06 hoenicke Exp $
 */

package alterrs.jode.expr;

import alterrs.jode.decompiler.TabbedPrintWriter;
import alterrs.jode.type.Type;

public class PopOperator extends Operator {

	Type popType;

	public PopOperator(Type argtype) {
		super(Type.tVoid, 0);
		popType = argtype;
		initOperands(1);
	}

	public int getPriority() {
		return 0;
	}

	public void updateSubTypes() {
		subExpressions[0].setType(Type.tSubType(popType));
	}

	public void updateType() {
	}

	public int getBreakPenalty() {
		if (subExpressions[0] instanceof Operator)
			return ((Operator) subExpressions[0]).getBreakPenalty();
		return 0;
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		/*
		 * Don't give a priority; we can't allow parens around a statement.
		 */
		subExpressions[0].dumpExpression(writer);
	}
}
