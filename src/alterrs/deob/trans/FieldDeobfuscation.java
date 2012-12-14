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

import EDU.purdue.cs.bloat.cfg.Block;
import EDU.purdue.cs.bloat.cfg.FlowGraph;
import EDU.purdue.cs.bloat.reflect.Modifiers;
import EDU.purdue.cs.bloat.tree.CallMethodExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.ExprStmt;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.MemRefExpr;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import EDU.purdue.cs.bloat.tree.Stmt;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import alterrs.deob.Deobfuscator;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.FieldNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

public class FieldDeobfuscation extends TreeNodeVisitor {
	public int count = 0, count2 = 0;
	
	@Override
	public void visitMethod(ClassNode c, MethodNode m) {
		if(!m.name().equals("<init>")) {
			return;
		}
		
		FlowGraph fg = m.graph();

		Block block = (Block) fg.trace().get(1);
		Stmt stmt = (Stmt) block.tree().stmts().get(1);
		if(stmt instanceof ExprStmt) {
			ExprStmt exprStmt = (ExprStmt) stmt;
			Expr expr = exprStmt.expr();
			if(expr instanceof StoreExpr) {
				Stmt labelStmt = (Stmt) block.tree().stmts().get(0);
				Stmt stmt2 = (Stmt) block.tree().stmts().get(2);
				if(stmt2 instanceof ExprStmt) {
					ExprStmt initStmt = (ExprStmt) stmt2;
					Expr initExpr = initStmt.expr();
					if(initExpr instanceof CallMethodExpr) { 
						ExprStmt s1 = (ExprStmt) initStmt.clone();
						ExprStmt s2 = (ExprStmt) exprStmt.clone();
						
						block.tree().removeStmt(exprStmt);
						block.tree().removeStmt(initStmt);
						block.tree().addStmtAfter(s1, labelStmt);
						block.tree().addStmtAfter(s2, s1);
						count2++;
					}
				}
			}
		}
	}
	
	@Override
	public void visitMemRefExpr(ClassNode c, MethodNode m, MemRefExpr expr) {
		FieldNode field = null;
		if(expr instanceof FieldExpr) {
			FieldExpr f = (FieldExpr) expr;
			field = Deobfuscator.getApp().field(f.field());
		} else if(expr instanceof StaticFieldExpr) {
			StaticFieldExpr f = (StaticFieldExpr) expr;
			field = Deobfuscator.getApp().field(f.field());
		} else {
			return;
		}
		
		if(field == null) {
			return;
		}
		
		if((field.info.modifiers() & Modifiers.FINAL) != 0) {
			field.info.setModifiers(field.info.modifiers() & ~Modifiers.FINAL);
			count++;
		}
	}
	
	@Override
	public void onFinish() {
		System.out.println("Definalized " + count + " fields!");
		System.out.println("Reorganized " + count2 + " initialization blocks!");
	}
}
