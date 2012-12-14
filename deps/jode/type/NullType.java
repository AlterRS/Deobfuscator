/* NullType Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: NullType.java,v 1.7.4.2 2002/05/28 17:34:22 hoenicke Exp $
 */

package jode.type;

/**
 * This class represents the NullType. The null type is special as it may only
 * occur as top type in a range type. It represents the type of the null
 * constant, which may be casted to any object. <br>
 * <p/>
 * Question: Should we replace tUObject = tRange(tObject, tNull) by tNull?
 * Question2: if not, should null have type tNull?
 * 
 * @author Jochen Hoenicke
 */
public class NullType extends ReferenceType {
	public NullType() {
		super(TC_NULL);
	}

	public Type getSubType() {
		return this;
	}

	public Type createRangeType(ReferenceType bottomType) {
		return tRange(bottomType, this);
	}

	/**
	 * Returns the generalized type of this and type. We have two classes and
	 * multiple interfaces. The result should be the object that is the the
	 * super class of both objects and all interfaces, that one class or
	 * interface of each type implements.
	 */
	public Type getGeneralizedType(Type type) {
		if (type.typecode == TC_RANGE)
			type = ((RangeType) type).getTop();
		return type;
	}

	/**
	 * Returns the specialized type of this and type. We have two classes and
	 * multiple interfaces. The result should be the object that extends both
	 * objects and the union of all interfaces.
	 */
	public Type getSpecializedType(Type type) {
		if (type.typecode == TC_RANGE)
			type = ((RangeType) type).getBottom();
		return type;
	}

	public String toString() {
		return "tNull";
	}
}
