/* ClassDeclarer Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: ClassDeclarer.java,v 1.1.4.1 2002/05/28 17:34:03 hoenicke Exp $
 */

package alterrs.jode.decompiler;

import alterrs.jode.bytecode.ClassInfo;

/**
 * This is the interface for objects, that a method can declare
 */
public interface ClassDeclarer {
	/**
	 * Get the parent of this ClassDeclarer.
	 * 
	 * @return null if this is the outermost instance.
	 */
	public ClassDeclarer getParent();

	/**
	 * Get the class analyzer for the given anonymous class info. It will search
	 * it in the classes we declare and in the parent class declarer.
	 * 
	 * @return null if the class analyzer doesn't yet exists.
	 */
	public ClassAnalyzer getClassAnalyzer(ClassInfo ci);

	public void addClassAnalyzer(ClassAnalyzer classAna);
}
