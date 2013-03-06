package alterrs.deob.asm.utility;

import alterrs.asm.tree.AbstractInsnNode;
import alterrs.asm.tree.ClassNode;
import alterrs.asm.tree.FieldInsnNode;
import alterrs.asm.tree.FieldNode;
import alterrs.asm.tree.IincInsnNode;
import alterrs.asm.tree.IntInsnNode;
import alterrs.asm.tree.JumpInsnNode;
import alterrs.asm.tree.LdcInsnNode;
import alterrs.asm.tree.LookupSwitchInsnNode;
import alterrs.asm.tree.MethodInsnNode;
import alterrs.asm.tree.MethodNode;
import alterrs.asm.tree.MultiANewArrayInsnNode;
import alterrs.asm.tree.TableSwitchInsnNode;
import alterrs.asm.tree.TryCatchBlockNode;
import alterrs.asm.tree.TypeInsnNode;
import alterrs.asm.tree.VarInsnNode;

/**
 * @author Lazaro
 */
public class AbstractTransformer implements Transformer {
	protected boolean finished = false;
	private boolean forceFinish = false;

	public AbstractTransformer() {
		this(false);
	}

	public AbstractTransformer(boolean forceFinish) {
		this.forceFinish = forceFinish;
	}

	@Override
	public void transform(ClassCollection cc) {
	}

	@Override
	public boolean finished() {
		return finished;
	}

	public boolean forceFinish() {
		return forceFinish;
	}

	@Override
	public void onFinish() {
	}

	@Override
	public void visitClass(ClassNode c) {
	}

	@Override
	public void visitMethod(ClassNode c, MethodNode m) {
	}

	@Override
	public void visitField(ClassNode c, FieldNode f) {
	}

	@Override
	public void visitInsn(ClassNode c, MethodNode m, AbstractInsnNode n) {
	}

	@Override
	public void visitIntInsn(ClassNode c, MethodNode m, IntInsnNode n) {
	}

	@Override
	public void visitVarInsn(ClassNode c, MethodNode m, VarInsnNode n) {
	}

	@Override
	public void visitTypeInsn(ClassNode c, MethodNode m, TypeInsnNode n) {
	}

	@Override
	public void visitFieldInsn(ClassNode c, MethodNode m, FieldInsnNode n) {
	}

	@Override
	public void visitMethodInsn(ClassNode c, MethodNode m, MethodInsnNode n) {
	}

	@Override
	public void visitJumpInsn(ClassNode c, MethodNode m, JumpInsnNode n) {
	}

	@Override
	public void visitLdcInsn(ClassNode c, MethodNode m, LdcInsnNode n) {
	}

	@Override
	public void visitIincInsn(ClassNode c, MethodNode m, IincInsnNode n) {
	}

	@Override
	public void visitTableSwitchInsn(ClassNode c, MethodNode m,
			TableSwitchInsnNode n) {
	}

	@Override
	public void visitLookupSwitchInsn(ClassNode c, MethodNode m,
			LookupSwitchInsnNode n) {
	}

	@Override
	public void visitMultiANewArrayInsn(ClassNode c, MethodNode m,
			MultiANewArrayInsnNode n) {
	}

	@Override
	public void visitTryCatchBlock(ClassNode c, MethodNode m,
			TryCatchBlockNode n) {
	}
}
