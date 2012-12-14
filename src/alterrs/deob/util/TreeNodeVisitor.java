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
package alterrs.deob.util;

import EDU.purdue.cs.bloat.tree.AddressStoreStmt;
import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ArrayLengthExpr;
import EDU.purdue.cs.bloat.tree.ArrayRefExpr;
import EDU.purdue.cs.bloat.tree.CallExpr;
import EDU.purdue.cs.bloat.tree.CallMethodExpr;
import EDU.purdue.cs.bloat.tree.CallStaticExpr;
import EDU.purdue.cs.bloat.tree.CastExpr;
import EDU.purdue.cs.bloat.tree.CatchExpr;
import EDU.purdue.cs.bloat.tree.CheckExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.DefExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.ExprStmt;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.GotoStmt;
import EDU.purdue.cs.bloat.tree.IfCmpStmt;
import EDU.purdue.cs.bloat.tree.IfStmt;
import EDU.purdue.cs.bloat.tree.IfZeroStmt;
import EDU.purdue.cs.bloat.tree.InitStmt;
import EDU.purdue.cs.bloat.tree.InstanceOfExpr;
import EDU.purdue.cs.bloat.tree.JsrStmt;
import EDU.purdue.cs.bloat.tree.LabelStmt;
import EDU.purdue.cs.bloat.tree.LocalExpr;
import EDU.purdue.cs.bloat.tree.MemExpr;
import EDU.purdue.cs.bloat.tree.MemRefExpr;
import EDU.purdue.cs.bloat.tree.MonitorStmt;
import EDU.purdue.cs.bloat.tree.NegExpr;
import EDU.purdue.cs.bloat.tree.NewArrayExpr;
import EDU.purdue.cs.bloat.tree.NewExpr;
import EDU.purdue.cs.bloat.tree.NewMultiArrayExpr;
import EDU.purdue.cs.bloat.tree.PhiCatchStmt;
import EDU.purdue.cs.bloat.tree.PhiJoinStmt;
import EDU.purdue.cs.bloat.tree.PhiStmt;
import EDU.purdue.cs.bloat.tree.RCExpr;
import EDU.purdue.cs.bloat.tree.RetStmt;
import EDU.purdue.cs.bloat.tree.ReturnAddressExpr;
import EDU.purdue.cs.bloat.tree.ReturnExprStmt;
import EDU.purdue.cs.bloat.tree.ReturnStmt;
import EDU.purdue.cs.bloat.tree.SCStmt;
import EDU.purdue.cs.bloat.tree.SRStmt;
import EDU.purdue.cs.bloat.tree.ShiftExpr;
import EDU.purdue.cs.bloat.tree.StackExpr;
import EDU.purdue.cs.bloat.tree.StackManipStmt;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import EDU.purdue.cs.bloat.tree.Stmt;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import EDU.purdue.cs.bloat.tree.SwitchStmt;
import EDU.purdue.cs.bloat.tree.ThrowStmt;
import EDU.purdue.cs.bloat.tree.UCExpr;
import EDU.purdue.cs.bloat.tree.VarExpr;
import EDU.purdue.cs.bloat.tree.ZeroCheckExpr;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;

public class TreeNodeVisitor extends NodeVisitor {
	public void visitExprStmt(ClassNode c, MethodNode m, ExprStmt stmt) {
	}

	public void visitIfStmt(ClassNode c, MethodNode m, IfStmt stmt) {
	}

	public void visitIfCmpStmt(ClassNode c, MethodNode m, IfCmpStmt stmt) {
	}

	public void visitIfZeroStmt(ClassNode c, MethodNode m, IfZeroStmt stmt) {
	}

	public void visitInitStmt(ClassNode c, MethodNode m, InitStmt stmt) {
	}

	public void visitGotoStmt(ClassNode c, MethodNode m, GotoStmt stmt) {
	}

	public void visitLabelStmt(ClassNode c, MethodNode m, LabelStmt stmt) {
	}

	public void visitMonitorStmt(ClassNode c, MethodNode m, MonitorStmt stmt) {
	}

	public void visitPhiStmt(ClassNode c, MethodNode m, PhiStmt stmt) {
	}

	public void visitCatchExpr(ClassNode c, MethodNode m, CatchExpr expr) {
	}

	public void visitDefExpr(ClassNode c, MethodNode m, DefExpr expr) {
	}

	public void visitStackManipStmt(ClassNode c, MethodNode m, StackManipStmt stmt) {
	}

	public void visitPhiCatchStmt(ClassNode c, MethodNode m, PhiCatchStmt stmt) {
	}

	public void visitPhiJoinStmt(ClassNode c, MethodNode m, PhiJoinStmt stmt) {
	}

	public void visitRetStmt(ClassNode c, MethodNode m, RetStmt stmt) {
	}

	public void visitReturnExprStmt(ClassNode c, MethodNode m, ReturnExprStmt stmt) {
	}

	public void visitReturnStmt(ClassNode c, MethodNode m, ReturnStmt stmt) {
	}

	public void visitAddressStoreStmt(ClassNode c, MethodNode m, AddressStoreStmt stmt) {
	}

	public void visitStoreExpr(ClassNode c, MethodNode m, StoreExpr expr) {
	}

	public void visitJsrStmt(ClassNode c, MethodNode m, JsrStmt stmt) {
	}

	public void visitSwitchStmt(ClassNode c, MethodNode m, SwitchStmt stmt) {
	}

	public void visitThrowStmt(ClassNode c, MethodNode m, ThrowStmt stmt) {
	}

	public void visitStmt(ClassNode c, MethodNode m, Stmt stmt) {
	}

	public void visitSCStmt(ClassNode c, MethodNode m, SCStmt stmt) {
	}

	public void visitSRStmt(ClassNode c, MethodNode m, SRStmt stmt) {
	}

	public void visitArithExpr(ClassNode c, MethodNode m, ArithExpr expr) {
	}

	public void visitArrayLengthExpr(ClassNode c, MethodNode m, ArrayLengthExpr expr) {
	}

	public void visitMemExpr(ClassNode c, MethodNode m, MemExpr expr) {
	}

	public void visitMemRefExpr(ClassNode c, MethodNode m, MemRefExpr expr) {
	}

	public void visitArrayRefExpr(ClassNode c, MethodNode m, ArrayRefExpr expr) {
	}

	public void visitCallExpr(ClassNode c, MethodNode m, CallExpr expr) {
	}

	public void visitCallMethodExpr(ClassNode c, MethodNode m, CallMethodExpr expr) {
	}

	public void visitCallStaticExpr(ClassNode c, MethodNode m, CallStaticExpr expr) {
	}

	public void visitCastExpr(ClassNode c, MethodNode m, CastExpr expr) {
	}

	public void visitConstantExpr(ClassNode c, MethodNode m, ConstantExpr expr) {
	}

	public void visitFieldExpr(ClassNode c, MethodNode m, FieldExpr expr) {
	}

	public void visitInstanceOfExpr(ClassNode c, MethodNode m, InstanceOfExpr expr) {
	}

	public void visitLocalExpr(ClassNode c, MethodNode m, LocalExpr expr) {
	}

	public void visitNegExpr(ClassNode c, MethodNode m, NegExpr expr) {
	}

	public void visitNewArrayExpr(ClassNode c, MethodNode m, NewArrayExpr expr) {
	}

	public void visitNewExpr(ClassNode c, MethodNode m, NewExpr expr) {
	}

	public void visitNewMultiArrayExpr(ClassNode c, MethodNode m, NewMultiArrayExpr expr) {
	}

	public void visitCheckExpr(ClassNode c, MethodNode m, CheckExpr expr) {
	}

	public void visitZeroCheckExpr(ClassNode c, MethodNode m, ZeroCheckExpr expr) {
	}

	public void visitRCExpr(ClassNode c, MethodNode m, RCExpr expr) {
	}

	public void visitUCExpr(ClassNode c, MethodNode m, UCExpr expr) {
	}

	public void visitReturnAddressExpr(ClassNode c, MethodNode m, ReturnAddressExpr expr) {
	}

	public void visitShiftExpr(ClassNode c, MethodNode m, ShiftExpr expr) {
	}

	public void visitStackExpr(ClassNode c, MethodNode m, StackExpr expr) {
	}

	public void visitVarExpr(ClassNode c, MethodNode m, VarExpr expr) {
	}

	public void visitStaticFieldExpr(ClassNode c, MethodNode m, StaticFieldExpr expr) {
	}

	public void visitExpr(ClassNode c, MethodNode m, Expr expr) {
	}

}
