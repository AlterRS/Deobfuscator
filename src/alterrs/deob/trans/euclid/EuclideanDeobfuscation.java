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
package alterrs.deob.trans.euclid;

import java.util.concurrent.atomic.AtomicReference;

import EDU.purdue.cs.bloat.editor.MemberRef;
import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.MemExpr;
import EDU.purdue.cs.bloat.tree.Node;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import EDU.purdue.cs.bloat.tree.TreeVisitor;
import alterrs.deob.Deobfuscator;
import alterrs.deob.trans.euclid.EuclideanPairIdentifier.EuclideanNumberPair;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

/**
 * Removes the field multiplications.
 * 
 * @author Lazaro Brito
 *
 */
public class EuclideanDeobfuscation extends TreeNodeVisitor {
	private int simple = 0;
	private int unfold = 0;
	private int unsafeUnfold = 0;
	
	public void visitArithExpr(ClassNode c, MethodNode m, ArithExpr expr) {
		if(expr.operation() != ArithExpr.MUL && expr.operation() != ArithExpr.ADD && expr.operation() != ArithExpr.SUB) {
			return;
		}
		
		ConstantExpr constant = null;
		Expr other = null;
		if(expr.left() instanceof ConstantExpr) {
			constant = (ConstantExpr) expr.left();
			other = expr.right();
		} else if(expr.right() instanceof ConstantExpr) {
			constant = (ConstantExpr) expr.right();
			other = expr.left();
		}
		
		if(constant != null) {
			final AtomicReference<MemberRef> atomicField = new AtomicReference<>(null);
			expr.visitChildren(new TreeVisitor() {
				@Override
				public void visitExpr(Expr child) {
					if(child instanceof ConstantExpr) {
						return;
					}
					
					if(child instanceof FieldExpr) {
						atomicField.set(((FieldExpr) child).field());
					} else if(child instanceof StaticFieldExpr) {
						atomicField.set(((StaticFieldExpr) child).field());
					} else if(child instanceof StoreExpr) {
						if(child.parent() instanceof ArithExpr && ((ArithExpr) child.parent()).operation() != ArithExpr.MUL) {
							return;
						}
						
						child.visitChildren(this);
					}
				}
			});
			EuclideanNumberPair loadCodec = atomicField.get() != null ? EuclideanPairIdentifier.PAIRS.get(Deobfuscator.getApp().field(atomicField.get())) : null;
			
			StoreExpr store = null;
			Node n = expr;
			while((n = n.parent()) != null) {
				if(n instanceof StoreExpr) {
					store = (StoreExpr) n;
					break;
				}
			}
			EuclideanNumberPair storeCodec = null;
			if(store != null) {
				MemExpr memExpr = store.target();
				if(memExpr instanceof FieldExpr) {
					storeCodec = EuclideanPairIdentifier.PAIRS.get(Deobfuscator.getApp().field(((FieldExpr) memExpr).field()));
				} else if(memExpr instanceof StaticFieldExpr) {
					storeCodec = EuclideanPairIdentifier.PAIRS.get(Deobfuscator.getApp().field(((StaticFieldExpr) memExpr).field()));
				}
			}
			
			if(expr.operation() != ArithExpr.MUL && storeCodec == loadCodec) {
				loadCodec = null;
			}
			
			decrypt(loadCodec, storeCodec, expr, constant, other);
		}
	}
	
	public void decrypt(EuclideanNumberPair loadCodec, EuclideanNumberPair storeCodec, Expr expr, ConstantExpr constant, Expr other) {	
		if(loadCodec == null && storeCodec == null) {
			return;
		}

		Number encodedValue = (Number) constant.value();
		Number decodedValue;
		boolean unsafe = false;
		if(encodedValue instanceof Integer) {
			int v = encodedValue.intValue();
			if(loadCodec != null) v *= loadCodec.product().intValue();
			if(storeCodec != null) v *= storeCodec.quotient().intValue();
			decodedValue = v;
			
			if((Math.abs(v) & 0xfffff) != Math.abs(v)) {
				unsafe = true;
			}
		} else if(encodedValue instanceof Long) {
			long v = encodedValue.longValue();
			if(loadCodec != null) v *= loadCodec.product().longValue();
			if(storeCodec != null) v *= storeCodec.quotient().longValue();
			decodedValue = v;
			
			if((Math.abs(v) & 0xffffffff) != Math.abs(v)) {
				unsafe = true;
			}
		} else {
			return;
		}
		
		if(expr instanceof ArithExpr && ((ArithExpr) expr).operation() == ArithExpr.MUL && decodedValue.longValue() == 1 && other != null) {
			expr.replaceWith((Node) other.clone());
			
			simple++;
		} else {
			constant.replaceWith(new ConstantExpr(decodedValue, constant.type()));
			
			unfold++;
			if(unsafe) {
				unsafeUnfold++;
			}
		}
	}
	
	@Override
	public void visitStoreExpr(ClassNode c, MethodNode m, StoreExpr expr) {
		MemberRef field;
		if(expr.target() instanceof FieldExpr) {
			field = ((FieldExpr) expr.target()).field();
		} else if(expr.target() instanceof StaticFieldExpr) {
			field = ((StaticFieldExpr) expr.target()).field();
		} else {
			return;
		}
		if(expr.expr() instanceof ConstantExpr) {
			EuclideanNumberPair codec = EuclideanPairIdentifier.PAIRS.get(Deobfuscator.getApp().field(field));
			if(codec != null) {
				ConstantExpr constant = (ConstantExpr) expr.expr();
				
				decrypt(null, codec, expr, constant, null);
			}
		}
	}
	
	public void onFinish() {
		System.out.println("Removed " + simple + " simple codec multiplications!");
		System.out.println("Unfolded " + unfold + " complicated codec multiplications! " + (unfold - unsafeUnfold) + " safe unfolds and " + (unsafeUnfold) + " unsafe unfolds!");
	}
}
