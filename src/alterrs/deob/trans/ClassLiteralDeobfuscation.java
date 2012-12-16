package alterrs.deob.trans;

import EDU.purdue.cs.bloat.editor.MemberRef;
import EDU.purdue.cs.bloat.editor.NameAndType;
import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.tree.CallStaticExpr;
import EDU.purdue.cs.bloat.tree.ConstantExpr;
import EDU.purdue.cs.bloat.tree.Expr;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

/**
 *
 * This is used to fix an issue with the ZKM rename tool/JODE decompiler.
 * Both of those applications do not recognize class literals (i.e java.lang.String.class).
 * In order to fix this, we simply create calls to Class.forName(String).
 * A safer way to do this would be to insert a method which encapsulates the call with a try catch,
 * and invokes said method, but this works for now.
 * @author Shawn D.
 */
public class ClassLiteralDeobfuscation extends TreeNodeVisitor {

	private int count;

	public void onFinish() {
		System.out.println("Inserted "+count+" calls to Class.forName(java/lang/String) over top of defined class literal constants.");
	}

	@Override
	public void visitConstantExpr(ClassNode c, MethodNode m, final ConstantExpr expr) {
		if (expr.value() instanceof Type) {
			Type typeValue = (Type) expr.value();

			String name = typeValue.toString().replaceAll("/", ".").replaceFirst("L", "").replace(";", "");

			CallStaticExpr forName = new CallStaticExpr(new Expr[] { new ConstantExpr(name, Type.STRING) }, new MemberRef(Type.CLASS, new NameAndType("forName", Type.getType(new Type[] { Type.STRING }, Type.CLASS))), Type.CLASS);
			expr.replaceWith(forName);
			count++;
		}
	}

}
