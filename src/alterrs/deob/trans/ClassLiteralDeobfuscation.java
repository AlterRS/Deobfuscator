package alterrs.deob.trans;

import EDU.purdue.cs.bloat.cfg.FlowGraph;
import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.StoreExpr;
import EDU.purdue.cs.bloat.tree.TreeVisitor;
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
public class ClassLiteralDeobfuscation extends TreeNodeVisitor {

	private int count;

	public void onFinish() {
		System.out.println("Reversed "+count+" encrypted numbers!");
	}

	@Override
	public void visitStoreExpr(ClassNode c, MethodNode m, final StoreExpr expr) {
		FlowGraph fg = m.graph();
		Type type = expr.type();
		fg.visit(new TreeVisitor() {
			@Override
			public void visitConstantExpr(final ConstantExpr cst) {
				if (expr.target().type() == expr.type() && expr.type())
				System.out.println(expr.target().type());
			}
		});
	}

}
