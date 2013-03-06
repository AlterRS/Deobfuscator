package alterrs.deob.trans.euclid;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.MemExpr;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import EDU.purdue.cs.bloat.tree.TreeVisitor;
import alterrs.deob.Deobfuscator;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.FieldNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

/**
 * Phase 1
 * 
 * Generates the euclidean pairs for each integer/long field.
 *
 * @author Lazaro Brito
 * @author Shawn D.
 */
public class EuclideanPairIdentifier extends TreeNodeVisitor {

	/**
	 * The place we store pairs for post phases.
	 */
	public static final Map<FieldNode, EuclideanNumberPair> PAIRS = new HashMap<>();

	public void onFinish() {
		System.out
				.println("Found " + PAIRS.size() + " euclidean number pairs!");
	}

	public static final class EuclideanNumberPair {

		/**
		 * The greatest common divisor, product, and quotient. Product is used
		 * to encode values, where quotient is used to decode them. GCD usually
		 * = product * quotient. True value is the decoded value {@see <init>}
		 */
		private BigInteger product, quotient, gcd, trueValue;

		/**
		 * If the unsafe flag is flagged {@code true}, the gcd was not <1>. This
		 * is a very bad thing.
		 */
		private boolean unsafe;

		/**
		 * The amount of bits in this pair of numbers. 32 = int, 64 = long
		 */
		private int bits;

		public EuclideanNumberPair(BigInteger product, BigInteger quotient,
				BigInteger gcd, int bits, boolean unsafe) {
			this.product = product;
			this.quotient = quotient;
			this.gcd = gcd;
			this.bits = bits;

			BigInteger k = gcd.multiply(product);
			this.trueValue = quotient.multiply(k);
		}

		public BigInteger product() {
			return product;
		}

		public BigInteger quotient() {
			return quotient;
		}

		public BigInteger gcd() {
			return gcd;
		}

		public BigInteger trueValue() {
			return trueValue;
		}

		public int bits() {
			return bits;
		}

		public boolean isUnsafe() {
			return unsafe;
		}
	}

	@Override
	public synchronized void visitArithExpr(final ClassNode c,
			final MethodNode m, final ArithExpr expr) {
		if (expr.operation() == ArithExpr.MUL) {
			boolean left = expr.left() instanceof ConstantExpr
					&& !(expr.right() instanceof ConstantExpr);
			boolean right = !(expr.left() instanceof ConstantExpr)
					&& expr.right() instanceof ConstantExpr;
			if (left || right) {
				final Expr oppSide = left ? expr.right() : expr.left();
				ConstantExpr cst = (ConstantExpr) (left ? expr.left()
						: expr.right());
				
				final AtomicBoolean safe = new AtomicBoolean(true);
				final AtomicReference<StoreExpr> store = new AtomicReference<StoreExpr>(null);
				expr.stmt().visitChildren(new TreeVisitor() {
					@Override
					public void visitExpr(Expr expr) {
						if(expr instanceof StoreExpr) {
							if(store.get() != null) {
								safe.set(false);
							}
							store.set((StoreExpr) expr);
						}
					}
				});
				if(!safe.get()) {
					return;
				}
				
				FieldNode f1 = null;
				if(store.get() != null) {
					MemExpr target = store.get().target();
					if(target instanceof FieldExpr) {
						f1 = Deobfuscator.getApp().field(((FieldExpr) target).field());
					} else if(target instanceof StaticFieldExpr) {
						f1 = Deobfuscator.getApp().field(((StaticFieldExpr) target).field());
					}
				}
				FieldNode f2 = null;
				if(oppSide instanceof FieldExpr) {
					f2 = Deobfuscator.getApp().field(((FieldExpr) oppSide).field());
				} else if(oppSide instanceof StaticFieldExpr) {
					f2 = Deobfuscator.getApp().field(((StaticFieldExpr) oppSide).field());
				}
				
				if(store.get() != null && f2 != null && f1 != f2) {
					return;
				}
				
				final FieldNode f = f1 == null ? f2 : f1;
				if(f == null) {
					return;
				}
				
				EuclideanNumberPair prev = PAIRS.get(f);
				if (prev != null) {
					return;
				}
				
				expr.stmt().visitChildren(new TreeVisitor() {
					@Override
					public void visitExpr(Expr expr) {
						FieldNode f3 = null;
						if(expr instanceof FieldExpr) {
							f3 = Deobfuscator.getApp().field(((FieldExpr) expr).field());
						} else if(expr instanceof StaticFieldExpr) {
							f3 = Deobfuscator.getApp().field(((StaticFieldExpr) expr).field());
						}
						
						if(f3 != null && f3 != f) {
							safe.set(false);
						}
					}
				});
				if(!safe.get()) {
					return;
				}

				if (cst.value() instanceof Integer || cst.value() instanceof Long) {
					long val = cst.value() instanceof Long ? ((long) cst.value()) : (int) cst.value();
					
					BigInteger quotient = BigInteger.valueOf(val);
					if ((val & 1) == 0) {// Not invertible. (Even number)
						return;
					}
					
					EuclideanNumberPair p = decipher(quotient, cst.value() instanceof Long ? 64 : 32, store.get() != null);
					PAIRS.put(f, p);
				}
			}
		}
	}

	/**
	 * Deciphers the given quotient and returns a new
	 * {@link EuclideanNumberPair}.
	 * 
	 * @param quotient
	 *            The quotient value.
	 * @param unsafe
	 *            The unsafe flag.
	 * @return a new {@link EuclideanNumberPair}.
	 */
	public static final EuclideanNumberPair decipher(BigInteger quotient, int bits, boolean store) {
		boolean unsafe = false;
		
		BigInteger product = inverse(quotient, bits);
		BigInteger g = gcd(product, quotient);

		if (g.longValue() != 1) {// Double check common divisor
			// Make sure that "g" truly is common.
			long v1 = product.divide(g).multiply(quotient).longValue();
			long v2 = quotient.divide(g).multiply(product).longValue();

			if (v1 != v2) {
				unsafe = true;
			}
		}

		if(!store)
			return new EuclideanNumberPair(product, quotient, g, bits, unsafe);
		else 
			return new EuclideanNumberPair(quotient, product, g, bits, unsafe);

	}

	/**
	 * Shifts the value given left, 32 bits and inverses the value (^-1).
	 * 
	 * @param val
	 *            The value to inverse.
	 */
	public static BigInteger inverse(BigInteger val, int bits) {
		BigInteger shift = BigInteger.ONE.shiftLeft(bits);
		return val.modInverse(shift);
	}

	/**
	 * Finds the greatest common divisor for a set of BigIntegers.
	 * 
	 * @param given
	 *            Must contain at least 2 values.
	 */
	public static BigInteger gcd(BigInteger... given) {
		BigInteger g = given[0].gcd(given[1]);
		for (int i = 2; i < given.length; i++) {
			g = g.gcd(given[i]);
		}
		return g;
	}
}
