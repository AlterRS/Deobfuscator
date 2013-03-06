/* MultiIdentifierMatcher Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: MultiIdentifierMatcher.java.in,v 1.1.2.2 2002/05/28 17:34:17 hoenicke Exp $
 */

package alterrs.jode.obfuscator.modules;

import java.util.Collection;

import alterrs.jode.obfuscator.Identifier;
import alterrs.jode.obfuscator.IdentifierMatcher;
import alterrs.jode.obfuscator.OptionHandler;

public class MultiIdentifierMatcher implements IdentifierMatcher, OptionHandler {
	/**
	 * Useful constant for giving to the constructor.
	 */
	public static boolean OR = true;
	/**
	 * Useful constant for giving to the constructor.
	 */
	public static boolean AND = false;

	IdentifierMatcher[] matchers;
	boolean isOr;

	/**
	 * Create an empty MultiIdentifierMatcher.
	 */
	public MultiIdentifierMatcher() {
		this.matchers = new IdentifierMatcher[0];
	}

	/**
	 * Create an IdentifierMatcher out of other matchers.
	 * 
	 * @param isOr
	 *            if true, match should return the logical (shortcut) or of the
	 *            underlying matchers, if false it returns the logical and.
	 * @param matchers
	 *            the underlying matchers
	 */
	public MultiIdentifierMatcher(boolean isOr, IdentifierMatcher[] matchers) {
		this.isOr = isOr;
		this.matchers = matchers;
	}

	public void setOption(String option, Collection values) {
		if (option.equals("or")) {
			isOr = true;
			matchers = (IdentifierMatcher[]) values
					.toArray(new IdentifierMatcher[values.size()]);
		} else if (option.equals("and")) {
			isOr = false;
			matchers = (IdentifierMatcher[]) values
					.toArray(new IdentifierMatcher[values.size()]);
		} else
			throw new IllegalArgumentException("Invalid option `" + option
					+ "'.");
	}

	public boolean matches(Identifier ident) {
		for (int i = 0; i < matchers.length; i++) {
			if (matchers[i].matches(ident) == isOr)
				return isOr;
		}
		return !isOr;
	}

	public boolean matchesSub(Identifier ident, String name) {
		for (int i = 0; i < matchers.length; i++) {
			if (matchers[i].matchesSub(ident, name) == isOr)
				return isOr;
		}
		return !isOr;
	}

	public String getNextComponent(Identifier ident) {
		if (isOr == AND) {
			for (int i = 0; i < matchers.length; i++) {
				String next = matchers[i].getNextComponent(ident);
				if (next != null && matchesSub(ident, next))
					return next;
			}
			return null;
		}
		// OR case
		String next = null;
		for (int i = 0; i < matchers.length; i++) {
			if (!matchesSub(ident, null))
				continue;
			if (next != null
					&& !matchers[i].getNextComponent(ident).equals(next))
				return null;
			next = matchers[i].getNextComponent(ident);
			if (next == null)
				return null;
		}
		return next;
	}
}
