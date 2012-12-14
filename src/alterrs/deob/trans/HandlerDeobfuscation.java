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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import EDU.purdue.cs.bloat.editor.Label;
import EDU.purdue.cs.bloat.editor.MethodEditor;
import EDU.purdue.cs.bloat.editor.TryCatch;
import EDU.purdue.cs.bloat.editor.Type;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.NodeVisitor;

public class HandlerDeobfuscation extends NodeVisitor {
	public int count = 0;
	
	@Override
	public void visitMethod(ClassNode c, MethodNode m) {
		MethodEditor e = m.editor();
		
		Map<Integer, List<TryCatch>> tryCatchPositions = new HashMap<>();
		for(Object tco : e.tryCatches()) {
			TryCatch tc = (TryCatch) tco;
			
			int handlerPos = e.code().indexOf(tc.handler());
			if(!tryCatchPositions.containsKey(handlerPos)) {
				List<TryCatch> handlers = new LinkedList<>();
				handlers.add(tc);
				
				tryCatchPositions.put(handlerPos, handlers);
			} else {
				tryCatchPositions.get(handlerPos).add(tc);
			}
		}
		
		for (Map.Entry<Integer, List<TryCatch>> entry : tryCatchPositions
				.entrySet()) {
			List<TryCatch> tryCatches = entry.getValue();
			if (tryCatches.size() > 1) {
				int startPos = Integer.MAX_VALUE;
				int endPos = 0;

				Label handler = tryCatches.get(0).handler();
				Type type = tryCatches.get(0).type();

				for (TryCatch tc : tryCatches) {
					int startPos_ = e.code().indexOf(tc.start());
					if (startPos_ < startPos) {
						startPos = startPos_;
					}

					int endPos_ = e.code().indexOf(tc.end());
					if (endPos_ > endPos) {
						endPos = endPos_;
					}

					e.tryCatches().remove(tc);
				}

				e.addTryCatch(new TryCatch((Label) e.codeElementAt(startPos), (Label) e.codeElementAt(endPos), handler, type));
				count++;
			}
		}
	}
	
	@Override
	public void onFinish() {
		System.out.println("Reorganized " + count + " handler tables!");
	}
}
