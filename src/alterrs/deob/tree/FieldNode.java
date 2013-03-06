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
package alterrs.deob.tree;

import EDU.purdue.cs.bloat.editor.FieldEditor;
import EDU.purdue.cs.bloat.reflect.FieldInfo;

public class FieldNode {
	public final ClassNode owner;
	public final FieldInfo info;
	public final FieldEditor editor;
	
	public FieldNode(ClassNode owner, FieldInfo info, FieldEditor editor) {
		this.owner = owner;
		this.info = info;
		this.editor = editor;
	}
	
	public String name() {
		return editor.name();
	}
	
	public String signature() {
		return editor.nameAndType().type().descriptor();
	}
	
	@Override
	public String toString() {
		return owner.name() + "." + name() + signature();
	}
	
	@Override
	public boolean equals(Object object) {
		if(object instanceof FieldNode) {
			FieldNode n2 = (FieldNode) object;
			if(n2.owner.equals(owner) && n2.name().equals(name()) && n2.signature().equals(signature())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (name().hashCode() & 0xffff) << 16 | signature().hashCode(); 
	}
}
