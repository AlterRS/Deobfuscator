/* ArrayLengthOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ArrayLengthOperator.java,v 2.12.4.1 2002/05/28 17:34:05 hoenicke Exp $
 */

package alterrs.jode.expr;

import alterrs.jode.decompiler.TabbedPrintWriter;
import alterrs.jode.type.Type;

public class ArrayLengthOperator extends Operator {

	public ArrayLengthOperator() {
		super(Type.tInt, 0);
		initOperands(1);
	}

	public int getPriority() {
		return 950;
	}

	public void updateSubTypes() {
		subExpressions[0].setType(Type.tArray(Type.tUnknown));
	}

	public void updateType() {
	}

	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		subExpressions[0].dumpExpression(writer, 900);
		writer.print(".length");
	}
}
