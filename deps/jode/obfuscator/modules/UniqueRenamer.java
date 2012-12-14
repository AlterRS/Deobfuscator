/* UniqueRenamer Copyright (C) 1999-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: UniqueRenamer.java.in,v 1.1.2.1 2002/05/28 17:34:17 hoenicke Exp $
 */

package jode.obfuscator.modules;

import java.util.Iterator;

import jode.obfuscator.Identifier;
import jode.obfuscator.Renamer;


public class UniqueRenamer implements Renamer {
	static int serialnr = 0;

	public Iterator generateNames(Identifier ident) {
		return new Iterator() {
			public boolean hasNext() {
				return true;
			}

			public Object next() {
				return "xxx" + serialnr++;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
