/* InstanceOfOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: InstanceOfOperator.java,v 4.12.2.1 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import jode.decompiler.TabbedPrintWriter;
import jode.type.Type;

public class InstanceOfOperator extends Operator {

	Type instanceType;

	public InstanceOfOperator(Type type) {
		super(Type.tBoolean, 0);
		this.instanceType = type;
		initOperands(1);
	}

	public int getPriority() {
		return 550;
	}

	public void updateSubTypes() {
		subExpressions[0].setType(Type.tUObject);
	}

	public void updateType() {
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		/*
		 * There are special cases where a cast isn't allowed. We must cast to
		 * the common super type before. In these cases instanceof always return
		 * false, but we want to decompile even bad programs.
		 */
		Type superType = instanceType
				.getCastHelper(subExpressions[0].getType());
		if (superType != null) {
			writer.startOp(writer.IMPL_PAREN, 2);
			writer.print("(");
			writer.printType(superType);
			writer.print(") ");
			writer.breakOp();
			subExpressions[0].dumpExpression(writer, 700);
			writer.endOp();
		} else
			subExpressions[0].dumpExpression(writer, 550);
		writer.breakOp();
		writer.print(" instanceof ");
		writer.printType(instanceType);
	}
}
