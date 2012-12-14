/**
 * Copyright (C) <2012> <Lazaro Brito>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial 
 * portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package alterrs.deob.trans;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import EDU.purdue.cs.bloat.cfg.Block;
import EDU.purdue.cs.bloat.cfg.FlowGraph;
import EDU.purdue.cs.bloat.cfg.Handler;
import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.tree.GotoStmt;
import EDU.purdue.cs.bloat.tree.NewExpr;
import EDU.purdue.cs.bloat.tree.Stmt;
import EDU.purdue.cs.bloat.tree.TreeVisitor;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.TreeNodeVisitor;

public class TryCatchDeobfuscation extends TreeNodeVisitor {
	public int count = 0;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void visitMethod(ClassNode c, MethodNode m) {
		FlowGraph fg = m.graph();
		for(Iterator it = new HashSet(fg.handlersMap().entrySet()).iterator(); it.hasNext();) {
			Map.Entry<Block, Handler> handlerEntry = (Entry<Block, Handler>) it.next();
			
			Handler handler = handlerEntry.getValue();
			
			if(handler.catchType().equals(Type.getType(RuntimeException.class))) {
				final AtomicBoolean flag = new AtomicBoolean(false);
				
				Block block = ((GotoStmt) handler.catchBlock().tree().lastStmt()).target();
				block.tree().visitChildren(new TreeVisitor() {
					@Override
					public void visitStmt(Stmt s) {
						s.visitChildren(this);
					}
					
					@Override
					public void visitNewExpr(NewExpr n) {
						if(n.objectType().equals(Type.getType(StringBuilder.class))) {
							flag.set(true);
						}
					}
				});
				
				if(flag.get()) {
					it.remove();
					
					fg.removeNode(handler.catchBlock().label());
					fg.removeNode(block.label());
					count++;
				}
			}
		}
	}
	
	@Override
	public void onFinish() {
		System.out.println("Removed " + count + " try-catches!");
	}
}
