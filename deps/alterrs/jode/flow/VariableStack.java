/* VariableStack Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: VariableStack.java,v 1.8.4.1 2002/05/28 17:34:09 hoenicke Exp $
 */

package alterrs.jode.flow;

import alterrs.jode.decompiler.LocalInfo;
import alterrs.jode.expr.Expression;
import alterrs.jode.expr.LocalLoadOperator;

/**
 * This class represents the state of the stack at various points in the
 * program. Each entry is a anonymous local, which is used instead of the PUSH /
 * stack_i statements.
 * <p>
 * <p/>
 * This class is immutable, but note, that the local infos can get merged.
 * 
 * @see FlowBlock.mapStackToLocal
 * @see FlowBlock.removePush
 */
public class VariableStack {
	public final static VariableStack EMPTY = new VariableStack();

	final LocalInfo[] stackMap;

	private VariableStack() {
		stackMap = new LocalInfo[0];
	}

	private VariableStack(LocalInfo[] stack) {
		stackMap = stack;
	}

	public boolean isEmpty() {
		return stackMap.length == 0;
	}

	public VariableStack pop(int count) {
		LocalInfo[] newStack = new LocalInfo[stackMap.length - count];
		System.arraycopy(stackMap, 0, newStack, 0, stackMap.length - count);
		return new VariableStack(newStack);
	}

	public VariableStack push(LocalInfo li) {
		return poppush(0, li);
	}

	public VariableStack poppush(int count, LocalInfo li) {
		LocalInfo[] newStack = new LocalInfo[stackMap.length - count + 1];
		System.arraycopy(stackMap, 0, newStack, 0, stackMap.length - count);
		newStack[stackMap.length - count] = li;
		return new VariableStack(newStack);
	}

	public VariableStack peek(int count) {
		LocalInfo[] peeked = new LocalInfo[count];
		System.arraycopy(stackMap, stackMap.length - count, peeked, 0, count);
		return new VariableStack(peeked);
	}

	public void merge(VariableStack other) {
		if (stackMap.length != other.stackMap.length) {
			throw new IllegalArgumentException("stack length differs");
		}
		for (int i = 0; i < stackMap.length; i++) {
			if (stackMap[i].getType().stackSize() != other.stackMap[i]
					.getType().stackSize())
				throw new IllegalArgumentException(
						"stack element length differs at " + i);
			stackMap[i].combineWith(other.stackMap[i]);
		}
	}

	/**
	 * Merge to VariableStacks. Either one may be null, in which case the other
	 * is returned.
	 */
	public static VariableStack merge(VariableStack first, VariableStack second) {
		if (first == null)
			return second;
		else if (second == null)
			return first;
		first.merge(second);
		return first;
	}

	public Expression mergeIntoExpression(Expression expr) {
		/* assert expr.getFreeOperandCount() == stackMap.length */

		for (int i = stackMap.length - 1; i >= 0; i--) {
			// if (!used.contains(stackMap[i]))
			// used.addElement(stackMap[i]);
			expr = expr.addOperand(new LocalLoadOperator(stackMap[i].getType(),
					null, stackMap[i]));
		}
		return expr;
	}

	public VariableStack executeSpecial(SpecialBlock special) {
		if (special.type == special.POP) {
			int popped = 0;
			int newLength = stackMap.length;
			while (popped < special.count) {
				newLength--;
				popped += stackMap[newLength].getType().stackSize();
			}
			if (popped != special.count)
				throw new IllegalArgumentException("wrong POP");
			LocalInfo[] newStack = new LocalInfo[newLength];
			System.arraycopy(stackMap, 0, newStack, 0, newLength);
			return new VariableStack(newStack);
		} else if (special.type == special.DUP) {
			int popped = 0;
			int numDup = 0;
			int startDup = stackMap.length;
			while (popped < special.count) {
				startDup--;
				numDup++;
				popped += stackMap[startDup].getType().stackSize();
			}
			if (popped != special.count)
				throw new IllegalArgumentException("wrong DUP");
			int destDup = startDup;
			int depth = 0;
			while (depth < special.depth) {
				destDup--;
				depth += stackMap[destDup].getType().stackSize();
			}
			if (depth != special.depth)
				throw new IllegalArgumentException("wrong DUP");
			LocalInfo[] newStack = new LocalInfo[stackMap.length + numDup];
			System.arraycopy(stackMap, 0, newStack, 0, destDup);
			System.arraycopy(stackMap, startDup, newStack, destDup, numDup);
			System.arraycopy(stackMap, destDup, newStack, destDup + numDup,
					startDup - destDup);
			System.arraycopy(stackMap, startDup, newStack, startDup + numDup,
					numDup);
			return new VariableStack(newStack);
		} else if (special.type == special.SWAP) {
			LocalInfo[] newStack = new LocalInfo[stackMap.length];
			System.arraycopy(stackMap, 0, newStack, 0, stackMap.length - 2);
			if (stackMap[stackMap.length - 2].getType().stackSize() != 1
					|| stackMap[stackMap.length - 1].getType().stackSize() != 1)
				throw new IllegalArgumentException("wrong SWAP");
			newStack[stackMap.length - 2] = stackMap[stackMap.length - 1];
			newStack[stackMap.length - 1] = stackMap[stackMap.length - 2];
			return new VariableStack(newStack);
		} else
			throw new alterrs.jode.AssertError("Unknown SpecialBlock");
	}

	public String toString() {
		StringBuffer result = new StringBuffer("[");
		for (int i = 0; i < stackMap.length; i++) {
			if (i > 0)
				result.append(", ");
			result.append(stackMap[i].getName());
		}
		return result.append("]").toString();
	}
}
