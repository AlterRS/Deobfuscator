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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import EDU.purdue.cs.bloat.editor.MemberRef;
import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.MemExpr;
import EDU.purdue.cs.bloat.tree.Node;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

public class MultiplierDeobfuscationPhase1 extends TreeNodeVisitor {
	public static class FieldCodec {
		private static class Inverse {
			public final long value;
			public final int multiplier;
			
			public Inverse(long inverse) {
				this(inverse, 1);
			}
			
			public Inverse(long inverse, int divider) {
				this.value = inverse;
				this.multiplier = divider;
			}
		}
		
		public final long decoder;
		public final long encoder;
		public final int bits;
		
		public final boolean unsafe;
		
		public final boolean store;
		
		public FieldCodec(long decoder, long encoder, int bits, boolean unsafe, boolean store) {
			this.decoder = decoder;
			this.encoder = encoder;
			this.bits = bits;
			this.unsafe = unsafe;
			this.store = store;
		}
		
		public static FieldCodec codec(long value, int bits, boolean store) {
			Inverse inverse = inverse(value, bits);
			value /= inverse.multiplier;
			
			boolean unsafe = inverse.multiplier != 1;
			if(store) {
				return new FieldCodec(inverse.value, value, bits, unsafe, true);
			} else {
				return new FieldCodec(value, inverse.value, bits, unsafe, false);
			}
		}
		
		private static Inverse inverse(long input, int bits) {
			BigInteger a = BigInteger.valueOf(input);
			BigInteger modulus = BigInteger.ONE.shiftLeft(bits);
			try {
				return new Inverse(a.modInverse(modulus).longValue());
			} catch(ArithmeticException e) {
				int i = 1;
				
				while(i++ <= 256) {
					try {
						return new Inverse(a.multiply(BigInteger.valueOf(i).modInverse(modulus)).modInverse(modulus).longValue(), i);
					} catch(ArithmeticException e2) {
					}
				}
				
				throw new ArithmeticException("BigInteger not invertible.");
			}
		}
		
		@Override
		public String toString() {
			if(bits == 32) {
				return "decoder: " + ((int) decoder) + ", encoder: " + ((int) encoder);
			} else {
				return "decoder: " + decoder + ", encoder: " + encoder;
			}
		}
	}
	
	public static final Map<MemberRef, FieldCodec> codecs = new HashMap<MemberRef, FieldCodec>();

	@Override
	public void visitArithExpr(ClassNode c, MethodNode m, ArithExpr expr) {
		if(expr.operation() == '*') {
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
					if(expr.parent() instanceof StoreExpr) {
						MemExpr memExpr = ((StoreExpr) expr.parent()).target();
						if(memExpr instanceof FieldExpr || memExpr instanceof StaticFieldExpr) {
							return;
						}
					}
					
					if(value instanceof Integer) {
						int v = value.intValue();
						if((Math.abs(v) & 0xffff) == Math.abs(v)) {
							return;
						}
						
					} else if(value instanceof Long) {
						long v = value.longValue();
						if((Math.abs(v)  & 0xffffffffL) == Math.abs(v)) {
							return;
						}
					}
					
					Node n = expr;
					while((n = n.parent()) != null) {
						if(n instanceof ArithExpr) {
							return;
						}
					}
					
					boolean store = false;
					n = expr;
					while((n = n.parent()) != null) {
						if(n instanceof StoreExpr) {
							store = true;
							break;
						}
					}
					
					if(store) {
						//return;
					}

					try {
						FieldCodec codec = FieldCodec.codec(value.longValue(), value instanceof Integer ? 32 : 64, store);
						
						FieldCodec oldCodec = codecs.get(field);
						if(oldCodec != null && !oldCodec.store) {
							return;
						}
						
						codecs.put(field, codec);
					} catch(ArithmeticException e) {
					}
				}
			}
		}
	}

	public void onFinish() {
		System.out.println("Mapped " + codecs.size() + " field codecs!");
	}
}
