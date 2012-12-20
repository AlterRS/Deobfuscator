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
package alterrs.deob.trans.test;

import EDU.purdue.cs.bloat.editor.MemberRef;
import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.MemExpr;
import EDU.purdue.cs.bloat.tree.Node;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import alterrs.deob.trans.test.MultiplierDeobfuscationPhase1.FieldCodec;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

public class MultiplierDeobfuscationPhase2 extends TreeNodeVisitor {
	private int simpleCodecCount = 0;
	private int arithConstantCount = 0;
	private int unsafeArith = 0;
	private int unsafeStore = 0;
	private int storeConstantCount = 0;

	@Override
	public void visitArithExpr(ClassNode c, MethodNode m, ArithExpr expr) {
		ConstantExpr constant = null;
		Expr leftOver = null;
		if(expr.left() instanceof ConstantExpr) {
			constant = (ConstantExpr) expr.left();
			leftOver = expr.right();
		} else if(expr.right() instanceof ConstantExpr) {
			constant = (ConstantExpr) expr.right();
			leftOver = expr.left();
		}
		
		if(constant != null) {
			Number value = (Number) constant.value();
			if(!(value instanceof Integer || value instanceof Long)) {
				return;
			}
			
			MemberRef field = null;
			if(leftOver instanceof FieldExpr) {
				field = ((FieldExpr) leftOver).field();
			} else if(leftOver instanceof StaticFieldExpr) {
				field = ((StaticFieldExpr) leftOver).field();
			}
			if(field != null) {			
				FieldCodec codec = MultiplierDeobfuscationPhase1.codecs.get(field);
				if(codec != null) {
					switch(expr.operation()) {
					case '*':
						if((codec.bits == 32 && (value.intValue() == ((int) codec.decoder)) ||  (value.intValue() == ((int) codec.encoder))) || (codec.bits == 64 && (value.longValue() == codec.decoder) ||  (value.longValue() == codec.encoder))) {
							// Remove multiplication
							expr.replaceWith((Expr) leftOver.clone());
							
							simpleCodecCount++;
						} else {
							// Unfold constant
							StoreExpr store = null;
							Node n = expr;
							while((n = n.parent()) != null) {
								if(n instanceof StoreExpr) {
									store = (StoreExpr) n;
									break;
								}
							}
							
							FieldCodec storeCodec = null;
							if(store instanceof StoreExpr) {
								MemExpr memExpr = store.target();
								if(memExpr instanceof FieldExpr) {
									storeCodec = MultiplierDeobfuscationPhase1.codecs.get(((FieldExpr) memExpr).field());
								} else if(memExpr instanceof StaticFieldExpr) {
									storeCodec = MultiplierDeobfuscationPhase1.codecs.get(((StaticFieldExpr) memExpr).field());
								}
							}
							
							// storeField = field * value;
							if(value instanceof Integer) {
								int v = value.intValue() * ((int) codec.encoder);
								if(storeCodec != null) {
									v *= ((int) storeCodec.decoder);
								}
								
								if(v != 1) {
									if((Math.abs(v) & 0xffff) != Math.abs(v)) {
										unsafeArith++;
										//System.out.println(field + " * " + v + "     unsafe: " + codec.unsafe + ", unsafe2: " + (storeCodec != null ? storeCodec.unsafe :"null") );
										//System.out.println("codec : [" + codec + "], store_codec decoder: [" + storeCodec + "], inverse: " + in);
										//System.out.println(expr.stmt());	
									} else {
										System.out.println(field.declaringClass().className() + "." + field.name() + " * " + value + " ---> " + field.name() + " * " + v);
									}
									
									
									constant.replaceWith(new ConstantExpr(v, Type.INTEGER));
								} else {
									expr.replaceWith((Expr) leftOver.clone());
								}
							} else {
								long v = value.longValue() * codec.encoder;
								if(storeCodec != null) {
									v *= storeCodec.decoder;
								}
								
								if(v != 1) {
									if((Math.abs(v) & 0xffff) != Math.abs(v)) {
										unsafeArith++;
									}
									constant.replaceWith(new ConstantExpr(v, Type.LONG));
								}
							}
							
							arithConstantCount++;
						}
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void visitStoreExpr(ClassNode c, MethodNode m, StoreExpr expr) {
		if(expr.expr() instanceof ConstantExpr) {
			MemberRef field;
			if(expr.target() instanceof FieldExpr) {
				field = ((FieldExpr) expr.target()).field();
			} else if(expr.target() instanceof StaticFieldExpr) {
				field = ((StaticFieldExpr) expr.target()).field();
			} else {
				return;
			}
			
			FieldCodec codec = MultiplierDeobfuscationPhase1.codecs.get(field);
			if(codec != null) {
				ConstantExpr constant = (ConstantExpr) expr.expr();
				
				Number value = (Number) constant.value();
				if(value instanceof Integer) {
					int v = value.intValue() * ((int) codec.decoder);	
					if((Math.abs(v) & 0xffff) != Math.abs(v)) {
						unsafeStore++;
					}
					constant.replaceWith(new ConstantExpr(v, Type.INTEGER));
				} else {
					long v = value.longValue() * codec.decoder;
					if((Math.abs(v) & 0xffffffff) != Math.abs(v)) {
						unsafeStore++;
					}
					constant.replaceWith(new ConstantExpr(v, Type.LONG));
				}
					
				storeConstantCount++;
			}
		}
	}
	
	public void onFinish() {
		System.out.println("Removed " + simpleCodecCount + " simple codec multiplications!");
		System.out.println("Unfolded " + arithConstantCount + " arithmetic codec constants! " + (arithConstantCount - unsafeArith) + " safe unfolds and " + (unsafeArith) + " unsafe unfolds!");
		System.out.println("Unfolded " + storeConstantCount + " store codec constants! " + (storeConstantCount - unsafeStore) + " safe unfolds and " + (unsafeStore) + " unsafe unfolds!");
	}
}
