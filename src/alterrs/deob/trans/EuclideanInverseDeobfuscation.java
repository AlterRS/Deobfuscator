package alterrs.deob.trans;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.IfStmt;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

/**
 *
 * Reverses number obfuscation which uses the
 * euclidean GCD algorithm.
 * Referenced super_'s post on the "Integer obfuscation" discussion.
 * @see http://en.wikipedia.org/wiki/Euclidean_algorithm for var name explanations.
 * @author Shawn D.
 */
public class EuclideanInverseDeobfuscation extends TreeNodeVisitor {

	private int count;

	public void onFinish() {
		System.out.println("Reversed "+count+" encrypted numbers!");
	}

	@Override
	public void visitArithExpr(final ClassNode c, final MethodNode m, final ArithExpr expr) {
		try {
			if (expr.operation() == ArithExpr.MUL) {
				boolean left = expr.left() instanceof ConstantExpr && !(expr.right() instanceof ConstantExpr);
				boolean right = !(expr.left() instanceof ConstantExpr) && expr.right() instanceof ConstantExpr;
				if (left || right) {
					Expr oppSide = left ? expr.right() : expr.left();

					if (oppSide instanceof FieldExpr || oppSide instanceof StaticFieldExpr) {
						ConstantExpr cst = (ConstantExpr) (left ? expr.left() : expr.right());
						boolean isLong = cst.value() instanceof Long;
						if (cst != null && cst.value() instanceof Long || cst.value() instanceof Integer) {
							BigInteger push = BigInteger.valueOf(isLong ? (Long) cst.value() : (Integer) cst.value());

							if ((push.longValue() & 1) == 0) {// Is the number even? If so, it is not invertible.
								return;
							}

							AtomicBoolean unsafe = new AtomicBoolean(false);
							BigInteger deciphered = decipher(push, unsafe);

							if (unsafe.get()) {
								System.out.println("WARNING! Unsafe stmt (bad GCD): \n\t"+expr.stmt());
								return;
							}

							cst.replaceWith(new ConstantExpr(deciphered.longValue(), cst.type()));
							expr.replaceWith((Expr) oppSide.clone());
							count++;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shifts the value given left, 32 bits
	 * and inverses the value (^-1).
	 * @param val The value to inverse.
	 */
	public BigInteger inverse(BigInteger val) {
		BigInteger shift = BigInteger.ONE.shiftLeft(32);
		return val.modInverse(shift);
	}

	/**
	 * Deciphers the actual given quotient.
	 */
	public BigInteger decipher(BigInteger quotient, AtomicBoolean unsafe){
		BigInteger product = inverse(quotient);
		BigInteger g = gcd(quotient, product);

		if (g.longValue() != 1) {// Double check common divisor
			// Make sure that "g" truly is common.
			long v1 = product.divide(g).multiply(quotient).longValue();
			long v2 = quotient.divide(g).multiply(product).longValue();

			if (v1 != v2) {
				unsafe.set(true);
			}
		}

		BigInteger k = g.multiply(product);
		return quotient.multiply(k);
	}

	/**
	 * Finds the greatest common divisor for
	 * a set of BigIntegers.
	 * @param given Must contain at least 2 values.
	 */
	public BigInteger gcd(BigInteger... given) {
		BigInteger g = given[0].gcd(given[1]);
		for (int i = 2; i < given.length; i++) {
			g = g.gcd(given[i]);
		}
		return g;
	}

}
