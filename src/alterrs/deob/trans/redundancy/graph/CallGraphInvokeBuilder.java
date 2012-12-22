package alterrs.deob.trans.redundancy.graph;

import EDU.purdue.cs.bloat.tree.CallMethodExpr;
import EDU.purdue.cs.bloat.tree.CallStaticExpr;
import alterrs.deob.trans.redundancy.graph.CallGraphBuilder.CallGraph;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

/**
 * Adds all calls to specified call graph methods.
 * @author Shawn D.
 * TODO Fix this, this phase doesn't work at all yet.
 */
public class CallGraphInvokeBuilder extends TreeNodeVisitor {
	
	private int count;
	
	@Override
	public void onFinish() {
		System.out.println("Found "+count+" invocations of dummy methods.");
	}
	
	
	@Override
	public void visitCallMethodExpr(ClassNode c, MethodNode m, CallMethodExpr expr) {
		for (CallGraph graph : CallGraphBuilder.GRAPHS.keySet()) {
			CallGraph other = CallGraphBuilder.GRAPHS.get(graph);
			if (other.method().name() == expr.method().name()) {
				System.out.println("cme   "+other.method().name()+", "+expr.method().name());
			}
		}
	}

	
	@Override
	public void visitCallStaticExpr(ClassNode c, MethodNode m, CallStaticExpr expr) {
		for (CallGraph graph : CallGraphBuilder.GRAPHS.keySet()) {
			CallGraph other = CallGraphBuilder.GRAPHS.get(graph);
			if (other.method().name() == expr.method().name()) {
				System.out.println("cse    "+other.method().name()+", "+expr.method().name());
			}
		}
	}
	
}
