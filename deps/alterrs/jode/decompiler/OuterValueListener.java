/* OuterValueListener Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: OuterValueListener.java,v 1.2.2.1 2002/05/28 17:34:03 hoenicke Exp $
 */

package alterrs.jode.decompiler;

/**
 * Interface, that every one should implement who is interested in outer value
 * changes.
 * <p/>
 * outerValues
 */
public interface OuterValueListener {
	/**
	 * Tells that the guessed number of outerValues was too big and thus needs
	 * shrinking right now.
	 * 
	 * @param clazzAna
	 *            The clazzAnalyzer for which this info is.
	 * @param newCount
	 *            The new number of outer values (not slot number) before the
	 *            first parameter.
	 */
	public void shrinkingOuterValues(OuterValues ov, int newCount);
}
