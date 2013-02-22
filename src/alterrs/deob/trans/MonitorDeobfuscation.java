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

import EDU.purdue.cs.bloat.editor.Instruction;
import EDU.purdue.cs.bloat.editor.MethodEditor;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.InsnNodeVisitor;

import static EDU.purdue.cs.bloat.editor.Instruction.*;

public class MonitorDeobfuscation extends InsnNodeVisitor {
	public int count = 0;

	@Override
	public void visitInsn(ClassNode c, MethodNode m, Instruction monitor) {
		if(monitor.opcodeClass() == opc_monitorenter) {
			MethodEditor e = m.editor();

			Instruction astore = prev(m, monitor);
			if(astore == null) 
				return;
			
			Instruction dup = prev(m, astore);
			if(dup == null)
				return;
			
			if(astore.opcodeClass() == opc_astore && dup.opcodeClass() == opc_dup) {
				e.code().remove(astore);
				e.replaceCodeAt(astore, e.code().indexOf(dup));
				e.insertCodeAt(new Instruction(opc_aload, astore.operand()), e.code().indexOf(astore) + 1);
				
				count++;
			}
		}
	}
	
	@Override
	public void onFinish() {
		System.out.println("Transformed " + count + " monitorenters!");
	}
}
