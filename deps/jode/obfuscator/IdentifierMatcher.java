/* IdentifierMatcher Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: IdentifierMatcher.java,v 1.3.2.1 2002/05/28 17:34:14 hoenicke Exp $
 */

package jode.obfuscator;

public interface IdentifierMatcher {
	/**
	 * Returns true, if the ident is matched by this matcher.
	 */
	public boolean matches(Identifier ident);

	/**
	 * Returns true, if there may be a sub ident, that is matched by this
	 * matcher.
	 * 
	 * @param subIdent
	 *            the name of the sub ident, or null if every name is okay.
	 */
	public boolean matchesSub(Identifier ident, String subIdent);

	/**
	 * Returns the unique name of the single sub item, for which matches or
	 * matchesSub returns true.
	 * 
	 * @return the unique name, or null, if there is not a unique sub item.
	 */
	public String getNextComponent(Identifier ident);
}
