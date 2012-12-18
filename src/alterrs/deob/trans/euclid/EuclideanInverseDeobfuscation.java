package alterrs.deob.trans.euclid;

import java.math.BigInteger;

import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.MemExpr;
import EDU.purdue.cs.bloat.tree.Node;
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
	private int failedUnfolds;

	public void onFinish() {
		int pairs = EuclideanPairIdentifier.PAIRS.size();
		int trueUnfolded = unfoldedConditions - failedUnfolds;
		System.out.println("Reversed euclidean algorithm. Pairs: "+pairs+"   Conditioning results:  Simple="+simpleConditions+" Unfolded="+trueUnfolded+",   Results summary: "+(trueUnfolded + simpleConditions) +" / "+(unfoldedConditions + simpleConditions));
	}

	@Override
	public synchronized void visitArithExpr(final ClassNode c, final MethodNode m, final ArithExpr expr) {
		try {
			boolean left = expr.left() instanceof ConstantExpr && !(expr.right() instanceof ConstantExpr);
			boolean right = !(expr.left() instanceof ConstantExpr) && expr.right() instanceof ConstantExpr;
			if (left || right) {

				Expr oppSide = left ? expr.right() : expr.left();
				ConstantExpr cst = (ConstantExpr) (left ? expr.left() : expr.right());
				boolean isStaticOpp = oppSide instanceof StaticFieldExpr;
				if (cst.type().isIntegral() && oppSide instanceof FieldExpr || isStaticOpp) {

					EuclideanNumberPair pair = EuclideanPairIdentifier.PAIRS.get(isStaticOpp ? ((StaticFieldExpr) oppSide).field() : ((FieldExpr) oppSide).field());

					if (pair != null) {
						switch(expr.operation()) {
						case ArithExpr.MUL:
							BigInteger product = pair.product();
							BigInteger quotient = pair.quotient();

							Number val = (Number) cst.value();

							boolean isQuotient = (pair.bits() == 32 && val.intValue() == quotient.intValue()) || (pair.bits() == 64 && val.longValue() == quotient.longValue());;
							boolean isProduct = (pair.bits() == 32 && val.intValue() == product.intValue()) || (pair.bits() == 64 && val.longValue() == product.longValue());
							if (isQuotient || isProduct) {
								expr.replaceWith((Expr) oppSide.clone());
								simpleConditions++;
							} else {
								Expr unfolded = unfold(pair, cst, oppSide);

								if (unfolded != null) {
									unfoldedConditions++;
									cst.replaceWith(unfolded);
								} else {
									System.out.println("Unable to unfold cst:   base_val="+cst.value()+", parent_cls_name="+cst.parent().getClass().getSimpleName().toUpperCase()+", bits="+pair.bits());
									System.out.println(" continued ->    stmt="+cst.stmt()+"\n\tcontinued->, default_unsafe="+pair.isUnsafe());
									failedUnfolds++;
								}
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

	public boolean matches(EuclideanNumberPair pair, Number val) {
		BigInteger product = pair.product();
		BigInteger quotient = pair.quotient();
		return (pair.bits() == 32 && (val.intValue() == product.intValue() || (int) val.intValue() == quotient.intValue())) || (pair.bits() == 64 && (val.longValue() == quotient.longValue()) ||  (val.longValue() == product.longValue()));
	}

	/**
	 * Returns an unfolded constant value for the given base constant.
	 * @param pair The pair to unfold.
	 * @param base The base constant that needs unfolding.
	 * @param oppSide The expression on the opposite side of the base.
	 * @return an unfolded {@link ConstantExpr}.
	 */
	public Expr unfold(EuclideanNumberPair pair, ConstantExpr base, Expr oppSide) {
		if (!base.hasParent()) {
			return null;
		}

		Node parent = base;
		while((parent = parent.parent()) != null) {
			if(parent instanceof StoreExpr) {
				break;
			}
		}

		if (parent == null) {
			return null;
		}

		StoreExpr store = (StoreExpr) parent;

		MemExpr target = store.target();
		EuclideanNumberPair storePair = null;

		boolean isStatic = target instanceof StaticFieldExpr;
		if (target instanceof FieldExpr || isStatic) {
			storePair = EuclideanPairIdentifier.PAIRS.get(isStatic ? ((StaticFieldExpr) target).field() : ((FieldExpr) target).field());
		}

		Number baseVal = (Number) base.value();
		if (baseVal instanceof Integer) {
			int val = baseVal.intValue() * pair.product().intValue();
			if (storePair != null) {
				val *= storePair.quotient().intValue();
			}
			if (val != pair.gcd().intValue() || val != 1) {
				// If this is true, the integer truly needs to be completed
				// unfolded from it's current state.
				if((Math.abs(val) & 0xffff) != Math.abs(val)) {
					int in = baseVal.intValue() * pair.quotient().intValue();
					if(storePair != null) {
						in *= storePair.product().intValue();
					}
					if((Math.abs(in) & 0xffff) == Math.abs(in)) {
						return new ConstantExpr(in, Type.INTEGER);
					}
					in = baseVal.intValue() * pair.quotient().intValue();
					if(storePair != null) {
						in *= storePair.quotient().intValue();
					}
					if((Math.abs(in) & 0xffff) == Math.abs(in)) {
						return new ConstantExpr(in, Type.INTEGER);
					}
					in = baseVal.intValue() * pair.quotient().intValue();
					if(storePair != null) {
						in *= storePair.product().intValue();
					}
					if((Math.abs(in) & 0xffff) == Math.abs(in)) {
						return new ConstantExpr(in, Type.INTEGER);
					}
				}
			}
			return (Expr) oppSide.clone();
		}
		
		long val = baseVal.longValue() * pair.product().longValue();
		if (storePair != null) {
			val *= storePair.quotient().longValue();
		}
		
		if(val != 1 || val != pair.gcd().intValue()) {
			return new ConstantExpr(val, Type.LONG);
		}
		
		return null;
	}
}
