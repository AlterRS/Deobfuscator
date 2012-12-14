/*
 * All files in the distribution of BLOAT (Bytecode Level Optimization and
 * Analysis tool for Java(tm)) are Copyright 1997-2001 by the Purdue
 * Research Foundation of Purdue University.  All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package EDU.purdue.cs.bloat.tree;

import EDU.purdue.cs.bloat.cfg.Block;

/**
 * IfStmt is a super class of statements in which some expression is evaluated
 * and one of two branches is taken.
 * 
 * @see IfCmpStmt
 * @see IfZeroStmt
 */
public abstract class IfStmt extends JumpStmt {
	int comparison; // Type of comparison that is performed

	Block trueTarget; // Code to jump to if IfStmt is true

	Block falseTarget; // Code to jump to if IfStmt is false

	// Compairson operators...
	public static final int EQ = 0;

	public static final int NE = 1;

	public static final int GT = 2;

	public static final int GE = 3;

	public static final int LT = 4;

	public static final int LE = 5;

	/**
	 * Constructor.
	 * 
	 * @param comparison
	 *            Comparison operator used in this if statement.
	 * @param trueTarget
	 *            Basic Block that is executed when if statement is true.
	 * @param falseTarget
	 *            Basic Block that is executed when if statement is false.
	 */
	public IfStmt(final int comparison, final Block trueTarget,
			final Block falseTarget) {
		this.comparison = comparison;
		this.trueTarget = trueTarget;
		this.falseTarget = falseTarget;
	}

	/**
	 * @return Comparison operator for this if statement.
	 */
	public int comparison() {
		return comparison;
	}

	/**
	 * Set the comparison operator for this if statement to its logical
	 * negative.
	 */
	public void negate() {
		switch (comparison) {
		case EQ:
			comparison = IfStmt.NE;
			break;
		case NE:
			comparison = IfStmt.EQ;
			break;
		case LT:
			comparison = IfStmt.GE;
			break;
		case GE:
			comparison = IfStmt.LT;
			break;
		case GT:
			comparison = IfStmt.LE;
			break;
		case LE:
			comparison = IfStmt.GT;
			break;
		}

		final Block t = trueTarget;
		trueTarget = falseTarget;
		falseTarget = t;
	}

	public void setTrueTarget(final Block target) {
		this.trueTarget = target;
	}

	public void setFalseTarget(final Block target) {
		this.falseTarget = target;
	}

	public Block trueTarget() {
		return trueTarget;
	}

	public Block falseTarget() {
		return falseTarget;
	}
}
