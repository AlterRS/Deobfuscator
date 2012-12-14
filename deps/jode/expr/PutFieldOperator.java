/* PutFieldOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: PutFieldOperator.java,v 4.24.2.1 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import jode.bytecode.Reference;
import jode.decompiler.MethodAnalyzer;

public class PutFieldOperator extends FieldOperator implements LValueExpression {

	public PutFieldOperator(MethodAnalyzer methodAnalyzer, boolean staticFlag,
			Reference ref) {
		super(methodAnalyzer, staticFlag, ref);
	}

	public boolean matches(Operator loadop) {
		return loadop instanceof GetFieldOperator
				&& ((GetFieldOperator) loadop).ref.equals(ref);
	}

	public boolean opEquals(Operator o) {
		return o instanceof PutFieldOperator
				&& ((PutFieldOperator) o).ref.equals(ref);
	}
}
