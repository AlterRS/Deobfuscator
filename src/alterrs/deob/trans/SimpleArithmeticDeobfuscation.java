package alterrs.deob.trans;

import java.math.BigInteger;

import EDU.purdue.cs.bloat.tree.ArithExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
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
 * 
 * This class also swaps all left side expressions to do with multiplication
 * to the right, if it is a ConstantExpr.
 * @author Shawn D.
 */
public class SimpleArithmeticDeobfuscation extends TreeNodeVisitor {

	/**
	 * Counter fields for result printing.
	 */
	private int addLogic, subLogic;
	private int multPref;

	@Override
	public void onFinish() {
		System.out.println("Arithmetic logic corrected: IADD Logic: "+addLogic+", ISUB Logic: "+subLogic+", Standardized multiplication: "+multPref);
	}

	@Override
	public synchronized void visitArithExpr(final ClassNode c, final MethodNode m, final ArithExpr expr) {
		try {
			if (expr.hasParent() && expr.parent().hasParent() && expr.left().type().isIntegral() && expr.right().type().isIntegral()) {
				if (expr.operation() == ArithExpr.SUB || expr.operation() == ArithExpr.ADD) {
					// change x - -y to x + y
					// change x + -y to x - y

					if (expr.right() instanceof ConstantExpr) {
						ConstantExpr cst = (ConstantExpr) expr.right();

						BigInteger val = BigInteger.valueOf((int) cst.value());

						if (val.intValue() < 0) {
							expr.right().replaceWith(new ConstantExpr(-val.intValue(), expr.type()));

							boolean add = expr.operation() == ArithExpr.ADD;
							expr.replaceWith(new ArithExpr(add ? ArithExpr.SUB : ArithExpr.ADD, expr.left(), expr.right(), expr.type()), false);

							if (add) {
								addLogic++;
							} else {
								subLogic++;
							}
						}
					}
					
					if (expr.operation() == ArithExpr.ADD) {
						// change -1 + x to x - 1
						// change -1 - -x to -1 + x to x - 1
						if (expr.left() instanceof ConstantExpr) {
							ConstantExpr cst = (ConstantExpr) expr.left();

							BigInteger val = BigInteger.valueOf((int) cst.value());

							if (val.intValue() < 0) {
								System.out.print(c.name()+"."+m.name()+", \t"+expr.stmt()+"\n\t\t");
								expr.replaceWith(new ArithExpr(ArithExpr.SUB, expr.right(), new ConstantExpr(-val.intValue(), expr.type()), expr.type()), false);
								addLogic++;
							}
						}
					}
				}
				
				if (expr.operation() == ArithExpr.MUL && expr.left() instanceof ConstantExpr) {
					// change cst * field to field * cst
					// change cst * var to var * cst
					// This is 100% for personal preference and does not altar any real logic in the code at all.
					
					expr.replaceWith(new ArithExpr(ArithExpr.MUL, expr.right(), expr.left(), expr.type()), false);
					multPref++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
