/* WildCard Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: WildCard.java.in,v 1.1.2.1 2002/05/28 17:34:17 hoenicke Exp $
 */

package jode.obfuscator.modules;

import java.util.Collection;

import jode.obfuscator.Identifier;
import jode.obfuscator.IdentifierMatcher;
import jode.obfuscator.OptionHandler;


public class WildCard implements IdentifierMatcher, OptionHandler {

	String wildcard;
	int firstStar;

	public WildCard() {
	}

	public WildCard(String wild) {
		wildcard = wild;
		firstStar = wildcard.indexOf('*');
	}

	public void setOption(String option, Collection values) {
		if (option.equals("value")) {
			if (values.size() != 1)
				throw new IllegalArgumentException(
						"Wildcard supports only one value.");
			wildcard = (String) values.iterator().next();
			firstStar = wildcard.indexOf('*');
		} else
			throw new IllegalArgumentException("Invalid option `" + option
					+ "'.");
	}

	public String getNextComponent(Identifier ident) {
		String prefix = ident.getFullName();
		if (prefix.length() > 0)
			prefix += ".";

		int lastDot = prefix.length();
		if (!wildcard.startsWith(prefix))
			return null;

		int nextDot = wildcard.indexOf('.', lastDot);
		if (nextDot > 0 && (nextDot <= firstStar || firstStar == -1))
			return wildcard.substring(lastDot, nextDot);
		else if (firstStar == -1)
			return wildcard.substring(lastDot);
		else
			return null;
	}

	public boolean matchesSub(Identifier ident, String subident) {
		String prefix = ident.getFullName();
		if (prefix.length() > 0)
			prefix += ".";
		if (subident != null)
			prefix += subident;
		if (firstStar == -1 || firstStar >= prefix.length())
			return wildcard.startsWith(prefix);
		return prefix.startsWith(wildcard.substring(0, firstStar));
	}

	public boolean matches(Identifier ident) {
		String test = ident.getFullName();
		if (firstStar == -1) {
			if (wildcard.equals(test)) {
				return true;
			}
			return false;
		}
		if (!test.startsWith(wildcard.substring(0, firstStar)))
			return false;

		test = test.substring(firstStar);
		int lastWild = firstStar;
		int nextWild;
		while ((nextWild = wildcard.indexOf('*', lastWild + 1)) != -1) {
			String pattern = wildcard.substring(lastWild + 1, nextWild);
			while (!test.startsWith(pattern)) {
				if (test.length() == 0)
					return false;
				test = test.substring(1);
			}
			test = test.substring(nextWild - lastWild - 1);
			lastWild = nextWild;
		}

		return test.endsWith(wildcard.substring(lastWild + 1));
	}

	public String toString() {
		return "Wildcard " + wildcard;
	}
}
