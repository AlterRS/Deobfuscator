/* ConvertOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ConvertOperator.java,v 2.14.4.1 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import jode.decompiler.TabbedPrintWriter;
import jode.type.Type;

public class ConvertOperator extends Operator {
	Type from;

	public ConvertOperator(Type from, Type to) {
		super(to, 0);
		this.from = from;
		initOperands(1);
	}

	public boolean opEquals(Operator o) {
		return (o instanceof ConvertOperator) && type == o.type;
	}

	public int getPriority() {
		return 700;
	}

	public void updateSubTypes() {
		subExpressions[0].setType(Type.tSubType(from));
	}

	public void updateType() {
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		writer.print("(");
		writer.printType(type.getCanonic());
		writer.print(") ");
		subExpressions[0].dumpExpression(writer, 700);
	}
}
