package alterrs.deob.trans.redundancy.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import EDU.purdue.cs.bloat.tree.CallExpr;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.NodeVisitor;

/**
 * Identifies redundant methods in the client and then
 * builds the call graph for them.
 * @author Shawn D.
 */
public class CallGraphBuilder extends NodeVisitor {

	private final List<CallGraph> tmpGraphs = new ArrayList<>();
	public static final Map<CallGraph, CallGraph> GRAPHS = new HashMap<>(); 


	public static final class CallGraph {
		private List<CallExpr> calls = new ArrayList<>();
		private MethodNode method;

		public CallGraph(MethodNode method) {
			this.method = method;
		}
		
		public List<CallExpr> calls() {
			return calls;
		}
		
		public MethodNode method() {
			return method;
		}
	}

	@Override
	public void onFinish() {
		System.out.println("Built "+(GRAPHS.size() * 2)+" call graphs for methods with dummy counterparts.");
	}

	@Override
	public void visitClass(ClassNode cn) {
		synchronized(tmpGraphs) {
			for (MethodNode mn : cn.methods()) {
				tmpGraphs.add(new CallGraph(mn));
			}
		}
		synchronized(tmpGraphs) {
			for (CallGraph graph : tmpGraphs) {
				for (MethodNode mn : cn.methods()) {
					if (graph.method != mn && graph.method.info.code().length == mn.info.code().length && mn.name().equals(graph.method.name()) && mn.signature() == graph.method.signature() && graph.method.graph().trace().size() == mn.graph().trace().size()) {
						boolean match = true;
						
						outer: for (byte code : graph.method.info.code()) {
							for (byte mcode : mn.info.code()) {
								if (code != mcode) {
									match = false;
									break outer;
								}
							}
						}
						
						if (match) {
							if (GRAPHS.containsKey(graph)) {
								continue;
							}
							GRAPHS.put(graph, new CallGraph(mn));
						}
					}
				}
			}
		}
	}
	
}
