package alterrs.deob.trans;

import java.util.HashMap;
import java.util.Map;

import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.LocalExpr;
import EDU.purdue.cs.bloat.tree.NegExpr;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

/**
 * 
 * This code fixes some basic arithmetic logic in the bytecode.
 * A simple example would be:
 * 
 * -x + 2 
 * -> (neg plus pos = pos - neg)
 * 2 - x 
 * 
 * or
 * 
 * x - -2
 * -> (neg minus neg = pos)
 * x + 2
 * @author Shawn D.
 */
public class SimpleArithmeticDeobfuscation extends TreeNodeVisitor {

	private int addLogic;
	private int subLogic;
	private int lastSubs = 0;
	private final Map<ArithExpr, Boolean[]> fixable = new HashMap<>();
	
	public void onFinish() {
		System.out.println("Collected "+fixable.size()+" fixable pieces of arithmetic logic. Iterating...");
		for (ArithExpr expr : fixable.keySet()) {
				Boolean[] flags = fixable.get(expr);
				boolean op_add = flags[0];

				if (op_add) {
					addLogic++;
				} else {
					subLogic++;
				}

				boolean left = flags[0];
				expr.replaceWith(new ArithExpr(op_add ? ArithExpr.SUB : ArithExpr.ADD, left ? expr.right() : expr.left(), left ? expr.left() : expr.right(), expr.type()));
		}
		System.out.println("Arithmetic logic corrected: IADD Logic: "+addLogic+", ISUB Logic: "+subLogic);
	}

	@Override
	public void visitArithExpr(final ClassNode c, final MethodNode m, final ArithExpr expr) {
		try {
			if (expr.hasParent() && expr.left().type().isIntegral() && expr.right().type().isIntegral()) {
				Number leftVal = getValue(expr.left());
				Number rightVal = getValue(expr.right());
				if (leftVal != null && rightVal != null) {
					boolean isLeft = leftVal.longValue() < 0;
					switch(expr.operation()) {
					case ArithExpr.ADD:
					case ArithExpr.SUB:
						if (isLeft || rightVal.longValue() < 0) {
							fixable.put(expr, new Boolean[] { expr.operation() == ArithExpr.ADD, isLeft });
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Number add(Expr left, Expr right, boolean get) {
		Number leftVal = getValue(left);
		Number rightVal = getValue(right);

		if (leftVal == null || rightVal == null) {
			return null;
		}

		return leftVal.longValue() + rightVal.longValue();
	}

	public Number getValue(Expr expr) {
		if (expr instanceof ConstantExpr) {
			ConstantExpr cst = (ConstantExpr) expr;
			if (cst.value() instanceof Long) {
				return null;
			}
			if (cst.value() instanceof Float) {
				return (float) cst.value();
			}
			if (cst.value() instanceof Double) {
				return (double) cst.value();
			}
			if (cst.value() instanceof Byte) {
				return (byte) cst.value();
			}
			return (int) cst.value();
		}
		if (expr instanceof LocalExpr) {
			return ((LocalExpr) expr).index();
		}
		if (expr instanceof NegExpr) {
			return getValue(((NegExpr) expr).expr());
		}
		if (expr instanceof ArithExpr) {
			ArithExpr arith = (ArithExpr) expr;

			Number lv = getValue(arith.left());
			if (lv != null) {
				return lv;
			}
			return getValue(arith.right());
		}
		return null;
	}

}
