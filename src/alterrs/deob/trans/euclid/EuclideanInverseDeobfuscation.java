package alterrs.deob.trans.euclid;

import java.math.BigInteger;

import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import alterrs.deob.trans.euclid.EuclideanPairIdentifier.EuclideanNumberPair;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

/**
 *
 * PHASE 2
 * Reverses number obfuscation which uses the
 * euclidean GCD algorithm.
 * Referenced super_'s post on the "Integer obfuscation" discussion.
 * @see http://en.wikipedia.org/wiki/Euclidean_algorithm for var name explanations.
 * @author Shawn D.
 */
public class EuclideanInverseDeobfuscation extends TreeNodeVisitor {

	private int simpleConditions;
	private int unfoldedConditions;

	public void onFinish() {
		System.out.println("Reversed euclidean algorithm. Pairs: "+EuclideanPairIdentifier.PAIRS.size()+"   Conditioning results:  Simple="+simpleConditions+" Unfolded="+unfoldedConditions);
	}

	@Override
	public void visitArithExpr(final ClassNode c, final MethodNode m, final ArithExpr expr) {
		try {
			boolean left = expr.left() instanceof ConstantExpr && !(expr.right() instanceof ConstantExpr);
			boolean right = !(expr.left() instanceof ConstantExpr) && expr.right() instanceof ConstantExpr;
			if (left || right) {

				Expr oppSide = left ? expr.right() : expr.left();
				ConstantExpr cst = (ConstantExpr) (left ? expr.left() : expr.right());
				if (cst.type().isIntegral() && oppSide instanceof FieldExpr || oppSide instanceof StaticFieldExpr) {
					EuclideanNumberPair pair = EuclideanPairIdentifier.PAIRS.get(oppSide);

					if (pair != null) {
						switch(expr.operation()) {
						case ArithExpr.MUL:
							BigInteger product = pair.product();
							BigInteger quotient = pair.quotient();
							
							long val = pair.bits() == 64 ? ((long) cst.value()) : (int) cst.value();

							if (pair.bits() == 32 && ((int) val == product.intValue() || (int) val == quotient.intValue()) || pair.bits() == 64 && (val == product.longValue() || val == quotient.longValue())) {
								expr.replaceWith((Expr) oppSide.clone());
								simpleConditions++;
							} else {
								ConstantExpr unfolded = unfold(pair, cst, oppSide);
								unfoldedConditions++;
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns an unfolded constant value for the given base constant.
	 * @param pair The pair to unfold.
	 * @param base The base constant that needs unfolding.
	 * @param oppSide The expression on the opposite side of the base.
	 * @return an unfolded {@link ConstantExpr}.
	 */
	public ConstantExpr unfold(EuclideanNumberPair pair, ConstantExpr base, Expr oppSide) {
		if (!base.hasParent() || !(((Expr)base).parent() instanceof StoreExpr)) {
			return null;
		}
		StoreExpr store = (StoreExpr) base.parent();
		return new ConstantExpr(pair.trueValue(), base.type());
	}
}
