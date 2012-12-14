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

import java.util.Vector;

import EDU.purdue.cs.bloat.cfg.Block;

/**
 * LEGatherer visits a basic block and returns all the LocalExprs in a vector
 * 
 * @author Thomas VanDrunen
 */

public class LEGatherer extends TreeVisitor {

	Vector LEs;

	Vector getLEs(final Block b) {

		LEs = new Vector();

		visitBlock(b);

		return LEs;
	}

	public void visitLocalExpr(final LocalExpr expr) {
		LEs.addElement(expr);
	}

}
