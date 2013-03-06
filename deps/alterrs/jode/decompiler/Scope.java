/* Scope Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: Scope.java,v 1.4.4.1 2002/05/28 17:34:03 hoenicke Exp $
 */

package alterrs.jode.decompiler;

/**
 * This interface describes a scope. The basic scopes are: the package scope,
 * the class scope (one more for each inner class) and the method scope.
 * 
 * @author Jochen Hoenicke
 */
public interface Scope {
	public final int PACKAGENAME = 0;
	public final int CLASSNAME = 1;
	public final int METHODNAME = 2;
	public final int FIELDNAME = 3;
	public final int AMBIGUOUSNAME = 4;
	public final int LOCALNAME = 5;

	public final int NOSUPERMETHODNAME = 12;
	public final int NOSUPERFIELDNAME = 13;

	public final int CLASSSCOPE = 1;
	public final int METHODSCOPE = 2;

	/**
	 * Simplifies the given name.
	 * 
	 * @param name
	 *            the name to simplify.
	 * @param usageType
	 *            the context of this name.
	 * @return null if the name hasn't a simplification in current scope, the
	 *         simplified name otherwise.
	 */
	/**
	 * Tells if this is the scope of name
	 */
	public boolean isScopeOf(Object object, int scopeType);

	public boolean conflicts(String name, int usageType);
}
