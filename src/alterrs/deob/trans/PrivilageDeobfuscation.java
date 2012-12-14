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

import EDU.purdue.cs.bloat.reflect.Modifiers;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.NodeVisitor;

public class PrivilageDeobfuscation extends NodeVisitor {
	public int count = 0;
	
	@Override
	public void visitMethod(ClassNode c, MethodNode m) {
		if(m.name().equals("finalize") && ((m.info.modifiers() & Modifiers.PUBLIC) == 0) && ((m.info.modifiers() & Modifiers.PRIVATE) == 0) && (m.info.modifiers() & Modifiers.PROTECTED) == 0) {
			m.info.setModifiers(m.info.modifiers() | Modifiers.PUBLIC);
			count++;
		}
	}
	
	@Override
	public void onFinish() {
		System.out.println("Publicized " + count + " finalize() methods!");
	}
}
