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
package alterrs.deob;

import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.NodeVisitor;

public class Chunk implements Runnable {
	public final ClassNode[] classes;

	public Chunk(ClassNode[] classes) {
		this.classes = classes;
	}

	public void accept(NodeVisitor visitor) {
		for (ClassNode c : classes) {
			c.accept(visitor);
		}
	}

	@Override
	public void run() {
		try {
			accept(new NodeVisitor() {
				@Override
				public void visitMethod(ClassNode c, MethodNode m) {
					m.graph();
				}
			});
			for (NodeVisitor visitor : Deobfuscator.TREE_TRANSFORMERS[Deobfuscator.getPhase()]) {
				accept(visitor);
			}
			accept(new NodeVisitor() {
				@Override
				public void visitMethod(ClassNode c, MethodNode m) {
					m.releaseGraph();
				}
			});
		} catch(Exception e) {
			System.out.println();
			System.err.println("Error caught while transforming chunk!");
			e.printStackTrace();
		}
		Deobfuscator.onFinish(this);
	}
}
