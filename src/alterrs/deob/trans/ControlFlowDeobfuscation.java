/**
 * Copyright (C) <2012> <Lazaro Brito>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial 
 * portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package alterrs.deob.trans;

import java.util.concurrent.atomic.AtomicBoolean;

import EDU.purdue.cs.bloat.cfg.Block;
import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.GotoStmt;
import EDU.purdue.cs.bloat.tree.IfCmpStmt;
import EDU.purdue.cs.bloat.tree.LocalExpr;
import EDU.purdue.cs.bloat.tree.NewExpr;
import EDU.purdue.cs.bloat.tree.ReturnStmt;
import EDU.purdue.cs.bloat.tree.Stmt;
import EDU.purdue.cs.bloat.tree.TreeVisitor;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

public class ControlFlowDeobfuscation extends TreeNodeVisitor {
	public int count = 0;
	
	@Override
	public void visitIfCmpStmt(ClassNode c, final MethodNode m, final IfCmpStmt stmt) {
		final LocalExpr local;
		final ConstantExpr ldc;
		if(stmt.left() instanceof LocalExpr) {
			local = (LocalExpr) stmt.left();
			if(stmt.right() instanceof ConstantExpr) {
				ldc = (ConstantExpr) stmt.right();
			} else {
				ldc = null;
			}
		} else if(stmt.right() instanceof LocalExpr) {
			local = (LocalExpr) stmt.right();
			if(stmt.left() instanceof ConstantExpr) {
				ldc = (ConstantExpr) stmt.left();
			} else {
				ldc = null;
			}
		} else {
			local = null;
			ldc = null;
		}
		
		if(local == null || ldc == null) {
			return;
		}
		
		if(!(ldc.value() instanceof Integer) && !(ldc.value() instanceof Long)) {
			return;
		}
		
		final AtomicBoolean flag = new AtomicBoolean(false);
		stmt.falseTarget().visitChildren(new TreeVisitor() {
			@Override
			public void visitStmt(Stmt s) {
				s.visitChildren(this);
			}
			
			@Override
			public void visitReturnStmt(ReturnStmt r) {
				if(ldc.value() instanceof Integer) {
					int v = (Integer) ldc.value();
					if((Math.abs(v) & 0xfffff) != Math.abs(v)) {
						flag.set(true);
					}
				} else {
					long v = (Long) ldc.value();
					if((Math.abs(v) & 0xffffffff) != Math.abs(v)) {
						flag.set(true);
					}
				}
			}
			
			@Override
			public void visitGotoStmt(GotoStmt g) {
				if(g.target().equals(g.block()) || g.target().equals(stmt.falseTarget())) {
					flag.set(true);
				}
			}
			
			@Override
			public void visitNewExpr(NewExpr n) {
				if(n.objectType().equals(Type.getType(IllegalStateException.class))) {
					flag.set(true);
				}
			}
		});
		
		if(flag.get()) {
			m.graph().visit(new TreeVisitor() {
				@Override
				public void visitStmt(Stmt s) {
					s.visitChildren(this);
				}
				
				@Override
				public void visitLocalExpr(LocalExpr l) {
					if(!l.hasParent()) {
						return;
					}
					
					if(local.index() == l.index()) {
						if(l.parent() instanceof IfCmpStmt) {
							final IfCmpStmt cmp = (IfCmpStmt) l.parent();
							Block t = cmp.trueTarget();
							Block f = cmp.falseTarget();
							
							//m.graph().removeNode(cmp.block().label());
							cmp.replaceWith(new GotoStmt(t));
							//cmp.block().tree().removeStmt(cmp);
							if(t != f) {
								m.graph().removeNode(f.label());
							}
							
							count++;
							
						}
					}
				}
			});
		}
	}
	
	@Override
	public void onFinish() {
		System.out.println("Removed " + count + " control-flow nodes!");
	}
}
