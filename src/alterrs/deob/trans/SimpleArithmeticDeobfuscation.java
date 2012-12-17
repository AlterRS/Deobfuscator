package alterrs.deob.trans;

import java.util.HashMap;
import java.util.Map;

import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.CastExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import EDU.purdue.cs.bloat.tree.FieldExpr;
import EDU.purdue.cs.bloat.tree.LocalExpr;
import EDU.purdue.cs.bloat.tree.NegExpr;
import EDU.purdue.cs.bloat.tree.StaticFieldExpr;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import EDU.purdue.cs.bloat.tree.ZeroCheckExpr;
import alterrs.deob.Deobfuscator;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.FieldNode;
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

	/**
	 * Counter fields for result printing.
	 */
	private int addLogic, subLogic, negationLogic;
	/**
	 * The fixable expressions (non-negated). These are stored here until {@code onFinish} is invoked
	 * because replacing during visitation causes the thread to slow to a lock.
	 */
	private final Map<ArithExpr, Boolean[]> fixable = new HashMap<>();

	@Override
	public void onFinish() {
		System.out.println("Collected "+(fixable.size() + negationLogic)+" fixable pieces of arithmetic logic. Iterating...");
		for (ArithExpr expr : fixable.keySet()) {
			if (expr.hasParent()) {
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
		}
		System.out.println("Arithmetic logic corrected: IADD Logic: "+addLogic+", ISUB Logic: "+subLogic+", Negation logic: "+negationLogic);
	}

	@Override
	public void visitArithExpr(final ClassNode c, final MethodNode m, final ArithExpr expr) {
		try {
			boolean isNegatedLeft = expr.left() instanceof NegExpr;
			boolean isNegated = isNegatedLeft || expr.right() instanceof NegExpr;
			
			if (expr.hasParent() && expr.left().type().isIntegral() && expr.right().type().isIntegral()) {
				
				Number leftVal = getValue(expr.left());
				Number rightVal = getValue(expr.right());
				
				if (leftVal != null && rightVal != null || isNegated) {
					boolean isLeft = isNegated ? isNegatedLeft : leftVal.longValue() < 0;
					
					switch(expr.operation()) {
					case ArithExpr.ADD:
					case ArithExpr.SUB:
						if (isNegated) {
							NegExpr negation = (NegExpr) (isNegatedLeft ? expr.left() : expr.right());
							expr.replaceWith(new ArithExpr(expr.operation() == ArithExpr.ADD ? ArithExpr.SUB : ArithExpr.ADD, isNegatedLeft ? negation.expr() : expr.left(), isNegatedLeft ? expr.right() : negation.expr(), expr.type()));
							negationLogic++;
							break;
						}
						
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

	/**
	 * Gets the number value for the specified expression, or null if
	 * there is no usable number.
	 * @param expr The expression to get the value of.
	 * @return The number value.
	 */
	public static final Number getValue(Expr expr) {
		if (expr instanceof NegExpr) {
			return getValue(((NegExpr) expr).expr());
		}
		if (expr instanceof LocalExpr) {
			return ((LocalExpr) expr).index();
		}
		if (expr instanceof StoreExpr) {
			return getValue(((StoreExpr) expr).expr());
		}
		if (expr instanceof CastExpr) {
			return getValue(((CastExpr) expr).expr());
		}
		if (expr instanceof ConstantExpr) {
			return (Number) ((ConstantExpr) expr).value();
		}
		if (expr instanceof FieldExpr) {
			FieldNode fn = Deobfuscator.getApp().field(((FieldExpr) expr).field());
			return fn != null ? fn.info.constantValue() : null;
		}
		if (expr instanceof StaticFieldExpr) {
			FieldNode fn = Deobfuscator.getApp().field(((StaticFieldExpr) expr).field());
			return fn != null ? fn.info.constantValue() : null;
		}
		if (expr instanceof ArithExpr) {
			ArithExpr arith = (ArithExpr) expr;
			
			Number lv = getValue(arith.left());
			if (lv != null && lv.longValue() < 0) {
				return lv;
			}
			
			Number rv = getValue(arith.right());
			if (rv != null && rv.longValue() < 0) {
				return rv;
			}
		}
		return null;
	}

}
