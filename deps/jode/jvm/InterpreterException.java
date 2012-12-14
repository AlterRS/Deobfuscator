/* InterpreterException Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: InterpreterException.java,v 1.3.2.1 2002/05/28 17:34:12 hoenicke Exp $
 */

package jode.jvm;

/**
 * This exception is thrown by the interpreter on various conditions.
 * 
 * @author Jochen Hoenicke
 */
public class InterpreterException extends Exception {
	public InterpreterException(String detail) {
		super(detail);
	}

	public InterpreterException() {
		super();
	}
}
