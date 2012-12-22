package alterrs.deob.trans.redundancy;

import EDU.purdue.cs.bloat.tree.CallExpr;
import EDU.purdue.cs.bloat.tree.CallMethodExpr;
import EDU.purdue.cs.bloat.tree.CallStaticExpr;
import alterrs.deob.trans.redundancy.graph.CallGraphBuilder;
import alterrs.deob.trans.redundancy.graph.CallGraphBuilder.CallGraph;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.util.NodeVisitor;

/**
 * Removes redundant methods by searching built call graphs.
 * @author Shawn D.
 *
 */
public class RedundantMethodDeobfuscation extends NodeVisitor {

	private int deleted;
	private int calls;
	
	@Override
	public void onFinish() {
		System.out.println("Removed "+CallGraphBuilder.GRAPHS.size()+" dummy methods and restored proper calls for "+calls+" calls to the methods.");
	}
	
	@Override
	public void visitClass(ClassNode cn) {
		for (CallGraph graph : CallGraphBuilder.GRAPHS.keySet()) {
			CallGraph other = CallGraphBuilder.GRAPHS.get(graph);
			
			if (graph.calls().size() > other.calls().size()) {// This means that "other" is the true dummy method.
				for (CallExpr call : other.calls()) {
					if (call instanceof CallStaticExpr) {
						call.replaceWith(new CallStaticExpr(call.params(), graph.method().editor().memberRef(), call.type()));
					} else if (call instanceof CallMethodExpr) {
						CallMethodExpr mthd = (CallMethodExpr) call;
						call.replaceWith(new CallMethodExpr(mthd.kind(), mthd.receiver(), call.params(), graph.method().editor().memberRef(), call.type()));
					}
					calls++;
				}
				other.method().editor().delete();
				deleted++;
			} else {
				for (CallExpr call : graph.calls()) {
					if (call instanceof CallStaticExpr) {
						call.replaceWith(new CallStaticExpr(call.params(), other.method().editor().memberRef(), call.type()));
					} else if (call instanceof CallMethodExpr) {
						CallMethodExpr mthd = (CallMethodExpr) call;
						call.replaceWith(new CallMethodExpr(mthd.kind(), mthd.receiver(), call.params(), other.method().editor().memberRef(), call.type()));
					}
					calls++;
				}
				graph.method().editor().delete();
			}
		}
	}
	
}
