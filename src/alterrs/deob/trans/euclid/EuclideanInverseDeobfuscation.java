package alterrs.deob.trans.euclid;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.LocalExpr;
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
	private int storeUnfoldConditions;
	private List<ConstantExpr> failed = new ArrayList<>();
	private int unsafeUnfolds;

	public void onFinish() {
		int pairs = EuclideanPairIdentifier.PAIRS.size();
		int trueUnfolded = (unfoldedConditions + storeUnfoldConditions) - failed.size();
		System.out.println("Reversed euclidean algorithm. Pairs: "+pairs+"   Conditioning results:  Simple="+simpleConditions+" Total unfolded="+trueUnfolded+",   Unfolded(store exprs)="+storeUnfoldConditions+", Results summary: "+(trueUnfolded + simpleConditions) +" / "+(unfoldedConditions + storeUnfoldConditions + simpleConditions)+"\n\tUnsafe unfolds="+unsafeUnfolds);
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
								} else if (!failed.contains(cst)) {
									failed.add(cst);
									System.out.println("Unable to unfold cst:   base_val="+cst.value()+", parent_cls_name="+cst.parent().getClass().getSimpleName().toUpperCase()+", bits="+pair.bits());
									System.out.println(" continued ->    stmt="+cst.stmt()+"\n\tcontinued->, default_unsafe="+pair.isUnsafe());
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

	@Override
	public void visitStoreExpr(ClassNode c, MethodNode m, StoreExpr expr) {
		if (expr.expr() instanceof ConstantExpr) {
			ConstantExpr cst = (ConstantExpr) expr.expr();

			boolean isStaticTarget = expr.target() instanceof StaticFieldExpr;
			if (cst.type().isIntegral() && expr.target() instanceof FieldExpr || isStaticTarget) {

				EuclideanNumberPair pair = EuclideanPairIdentifier.PAIRS.get(isStaticTarget ? ((StaticFieldExpr) expr.target()).field() : ((FieldExpr) expr.target()).field());

				if (pair != null) {
					Number val = (Number) cst.value();
					if (val instanceof Integer) {
						cst.replaceWith(new ConstantExpr(val.intValue() * pair.quotient().intValue(), Type.INTEGER));
					} else {
						cst.replaceWith(new ConstantExpr(val.longValue() * pair.quotient().longValue(), Type.LONG));
					}
					storeUnfoldConditions++;
					if (failed.contains(cst)) {
						failed.remove(cst);
					}
				}
			}
		}
	}

	@Override
	public synchronized void visitLocalExpr(ClassNode c, MethodNode m, LocalExpr expr) {
		if (expr.parent() instanceof ArithExpr) {
			ArithExpr arith = (ArithExpr) expr.parent();

			if (arith.operation() == ArithExpr.MUL) {
				boolean left = arith.left() == expr && arith.right() instanceof ConstantExpr;
				boolean right = arith.right() == expr && arith.left() instanceof ConstantExpr;
				if (left || right) {
					ConstantExpr cst = ((ConstantExpr) (left ? arith.right() : arith.left()));
					if (cst.value() instanceof Integer || cst.value() instanceof Long) {
						unfoldedConditions++;
						arith.replaceWith((Expr) expr.clone());
					}
				}
			}
		}
	}

	@Override
	public synchronized void visitConstantExpr(ClassNode c, MethodNode m, ConstantExpr expr) {
		if (expr.hasParent() && expr.parent() instanceof ArithExpr) {
			ArithExpr parent = (ArithExpr) expr.parent();
			boolean left = parent.left() == expr && (parent.right() instanceof FieldExpr || parent.right() instanceof StaticFieldExpr);
			boolean right = parent.right() == expr && (parent.left() instanceof FieldExpr || parent.left() instanceof StaticFieldExpr);
			if (left || right) {
				boolean isStatic = (right && parent.left() instanceof StaticFieldExpr) || (left && parent.right() instanceof StaticFieldExpr);
				EuclideanNumberPair pair = EuclideanPairIdentifier.PAIRS.get(isStatic && left ? ((StaticFieldExpr) parent.right()).field() : isStatic && right ? ((StaticFieldExpr) parent.left()).field() : right ? ((FieldExpr) parent.left()).field() : ((FieldExpr) parent.right()).field());
				if (pair != null) {
					switch(parent.operation()) {
					case ArithExpr.ADD:
					case ArithExpr.SUB:
						Number val = (Number) expr.value();
						if (val instanceof Integer) {
							expr.replaceWith(new ConstantExpr(val.intValue() * pair.quotient().intValue(), Type.INTEGER));
						} else {
							expr.replaceWith(new ConstantExpr(val.longValue() * pair.quotient().longValue(), Type.LONG));
						}
						unfoldedConditions++;
						break;
					}
				}
			}
		}
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
			if (base.parent() instanceof ArithExpr) {
				Number baseVal = (Number) base.value();
				int val = baseVal.intValue() * pair.product().intValue();
				return new ConstantExpr(val, base.type());
			}
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
			if (val == 1) {
				return (Expr) oppSide.clone();
			}
			return new ConstantExpr(val, base.type());
		}

		long val = baseVal.longValue() * pair.product().longValue();

		if (storePair != null) {
			val *= storePair.quotient().longValue();
		}

		if(val != 1 || val != pair.gcd().longValue()) {
			return new ConstantExpr(val, base.type());
		}
		return null;
	}
}
