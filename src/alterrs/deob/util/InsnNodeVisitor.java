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
package alterrs.deob.util;

import java.util.List;

import EDU.purdue.cs.bloat.editor.Instruction;
import EDU.purdue.cs.bloat.editor.Label;
import EDU.purdue.cs.bloat.editor.MethodEditor;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.MethodNode;

public abstract class InsnNodeVisitor extends NodeVisitor {
	protected Instruction next(MethodNode m, Instruction insn) {
		MethodEditor editor = m.editor();
		
		@SuppressWarnings("rawtypes")
		List code = editor.code();
		int index = code.indexOf(insn);
		int offset = 1;

		Object e = null;
		while((code.size() > (index + offset)) && !((e = code.get(index + offset++)) instanceof Instruction) && e != null);
		return e instanceof Instruction ? ((Instruction) e) : null;
	}
	
	protected Instruction prev(MethodNode m, Instruction insn) {
		MethodEditor editor = m.editor();
		
		@SuppressWarnings("rawtypes")
		List code = editor.code();
		int index = code.indexOf(insn);
		int offset = 1;

		Object e = null;
		while((index - offset) >= 0 && !((e = code.get(index - offset++)) instanceof Instruction) && e != null);
		return e instanceof Instruction ? ((Instruction) e) : null;
	}
	
	public void visitInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitLabel(ClassNode c, MethodNode m, Label label) {
	}
	
	public void visitLoadInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitStoreInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitIncInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitThrowInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitInvokeInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitRetInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitReturnInsn(ClassNode c, MethodNode m, Instruction insn) {	
	}
	
	public void visitSwitchInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitJumpInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
	
	public void visitJsrInsn(ClassNode c, MethodNode m, Instruction insn) {
	}
}
