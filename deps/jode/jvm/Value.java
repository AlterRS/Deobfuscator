/* Value Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: Value.java,v 1.5.2.1 2002/05/28 17:34:12 hoenicke Exp $
 */

package jode.jvm;

/**
 * This class represents a stack value.
 * 
 * @author Jochen Hoenicke
 */
class Value {
	Object value;
	NewObject newObj;

	public Value() {
	}

	public void setObject(Object obj) {
		newObj = null;
		value = obj;
	}

	public Object objectValue() {
		if (newObj != null)
			return newObj.objectValue();
		return value;
	}

	public void setInt(int i) {
		newObj = null;
		value = new Integer(i);
	}

	public int intValue() {
		return ((Integer) value).intValue();
	}

	public void setLong(long i) {
		newObj = null;
		value = new Long(i);
	}

	public long longValue() {
		return ((Long) value).longValue();
	}

	public void setFloat(float i) {
		newObj = null;
		value = new Float(i);
	}

	public float floatValue() {
		return ((Float) value).floatValue();
	}

	public void setDouble(double i) {
		newObj = null;
		value = new Double(i);
	}

	public double doubleValue() {
		return ((Double) value).doubleValue();
	}

	public void setNewObject(NewObject n) {
		newObj = n;
	}

	public NewObject getNewObject() {
		return newObj;
	}

	public void setValue(Value val) {
		value = val.value;
		newObj = val.newObj;
	}

	public String toString() {
		return newObj != null ? newObj.toString() : "" + value;
	}
}
