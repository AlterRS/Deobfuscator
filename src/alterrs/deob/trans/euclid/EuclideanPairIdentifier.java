package alterrs.deob.trans.euclid;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import EDU.purdue.cs.bloat.editor.MemberRef;
import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ArrayRefExpr;
import EDU.purdue.cs.bloat.tree.CallMethodExpr;
import EDU.purdue.cs.bloat.tree.CallStaticExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.IfCmpStmt;
import EDU.purdue.cs.bloat.tree.NewArrayExpr;
import EDU.purdue.cs.bloat.tree.Node;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

/**
 *
 * PHASE 1
 * Reverses number obfuscation which uses the
 * euclidean GCD algorithm.
 * Referenced super_'s post on the "Integer obfuscation" discussion.
 * @see http://en.wikipedia.org/wiki/Euclidean_algorithm for var name explanations.
 * @author Shawn D.
 */
public class EuclideanPairIdentifier extends TreeNodeVisitor {

	/**
	 * The place we store 
	 */
	public static final Map<MemberRef, EuclideanNumberPair> PAIRS = new HashMap<>();
	
	public void onFinish() {
		int unsafe = 0;
		for (EuclideanNumberPair pair : PAIRS.values()) {
			if (pair.unsafe) {
				unsafe++;
			}
		}
		System.out.println("Found "+PAIRS.size()+" euclidean number pairs! Safe: "+(PAIRS.size() - unsafe)+"    Unsafe: "+unsafe);
	}

	public static final class EuclideanNumberPair {

		/**
		 * The greatest common divisor, product, and quotient.
		 * Product is used to encode values, where quotient is used to decode them.
		 * GCD usually = product * quotient.
		 * True value is the decoded value {@see <init>}
		 */
		private BigInteger product, quotient, gcd, trueValue;
		
		/**
		 * If this is flagged {@code true}, the gcd was not <1>.
		 * This is a very bad thing.
		 */
		private boolean unsafe;

		/**
		 * The amount of bits in this pair of numbers.
		 * 32 = int, 64 = long
		 */
		private int bits;
		
		public EuclideanNumberPair(BigInteger product, BigInteger quotient, BigInteger gcd, int bits, boolean unsafe) {
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
	public synchronized void visitArithExpr(final ClassNode c, final MethodNode m, final ArithExpr expr) {
		try {
			if (expr.operation() == ArithExpr.MUL) {
				boolean left = expr.left() instanceof ConstantExpr && !(expr.right() instanceof ConstantExpr);
				boolean right = !(expr.left() instanceof ConstantExpr) && expr.right() instanceof ConstantExpr;
				if (left || right) {

					Expr oppSide = left ? expr.right() : expr.left();
					ConstantExpr cst = (ConstantExpr) (left ? expr.left() : expr.right());

					boolean isStaticOpp = oppSide instanceof StaticFieldExpr;
					if (cst.type().isIntegral() && oppSide instanceof FieldExpr || isStaticOpp) {
						Node parent = expr.parent();
						if (parent instanceof IfCmpStmt || parent instanceof CallMethodExpr || parent instanceof CallStaticExpr || parent instanceof NewArrayExpr || parent instanceof ArrayRefExpr || parent instanceof ArithExpr) { 
							// If the above condition is satisfied, we know the number is being used and not stored,
							// therefore, we've found a quotient number.
							// Improving this statement will improve the amount of pairs found, therefore increasing the overall
							// results of the phase 2 transformer. We mark this as a TODO .
							boolean isLongCst = cst.value() instanceof Long;
							long val = isLongCst ? ((long) cst.value()) : (int) cst.value();
							
							if ((val & 1) == 0) {
								return;
							}
							
							BigInteger quotient = BigInteger.valueOf(val);
							AtomicBoolean unsafe = new AtomicBoolean(false);
							
							EuclideanNumberPair pair = decipher(quotient, isLongCst ? 64 : 32, unsafe);
							
							
							MemberRef field =  isStaticOpp ? ((StaticFieldExpr) oppSide).field() : ((FieldExpr) oppSide).field();
							EuclideanNumberPair prev = PAIRS.get(field);
							
							if (prev != null) {
								return;
							}
							
							PAIRS.put(field, pair);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Deciphers the given quotient and returns a new {@link EuclideanNumberPair}.
	 * @param quotient The quotient value.
	 * @param unsafe The unsafe flag.
	 * @return a new {@link EuclideanNumberPair}.
	 */
	public static final EuclideanNumberPair decipher(BigInteger quotient, int bits, AtomicBoolean unsafe) {
		BigInteger product = inverse(quotient, bits);
		BigInteger g = gcd(product, quotient);

		if (g.longValue() != 1) {// Double check common divisor
			// Make sure that "g" truly is common.
			long v1 = product.divide(g).multiply(quotient).longValue();
			long v2 = quotient.divide(g).multiply(product).longValue();

			if (v1 != v2) {
				unsafe.set(true);
			}
		}
		
		return new EuclideanNumberPair(product, quotient, g, bits, unsafe.get());

	}

	/**
	 * Shifts the value given left, 32 bits
	 * and inverses the value (^-1).
	 * @param val The value to inverse.
	 */
	public static BigInteger inverse(BigInteger val, int bits) {
		BigInteger shift = BigInteger.ONE.shiftLeft(bits);
		return val.modInverse(shift);
	}

	/**
	 * Finds the greatest common divisor for
	 * a set of BigIntegers.
	 * @param given Must contain at least 2 values.
	 */
	public static BigInteger gcd(BigInteger... given) {
		BigInteger g = given[0].gcd(given[1]);
		for (int i = 2; i < given.length; i++) {
			g = g.gcd(given[i]);
		}
		return g;
	}

}
