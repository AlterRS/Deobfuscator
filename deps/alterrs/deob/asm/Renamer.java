package alterrs.deob.asm;

import static alterrs.asm.Opcodes.ACC_ABSTRACT;
import static alterrs.asm.Opcodes.ACC_NATIVE;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
import alterrs.commons.Attributes;
import alterrs.deob.asm.ref.ClassNameDeobfuscator;
import alterrs.deob.asm.ref.FieldNameDeobfuscator;
import alterrs.deob.asm.ref.MethodNameDeobfuscator;
import alterrs.deob.asm.utility.ClassCollection;
import alterrs.deob.asm.utility.Transformer;

/**
 * @author Lazaro
 */
public class Renamer {
	public static Renamer ctx = null;

	public static void main(String[] args) {
		try {
			ctx = new Renamer();
			ctx.run();
		} catch (Throwable e) {
			e.printStackTrace();

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e2) {
			}

			System.exit(1);
		}
	}

	public Attributes atr = new Attributes();

	public ClassCollection module;

	private void run() throws Throwable {
		URL inputURL = new URL("jar:file:./input.jar!/");
		if (!new File("./input.jar").exists()) {
			throw new RuntimeException("Could not locate \"input.jar\"!");
		}

		List<Transformer> transformers = new LinkedList<Transformer>();
		transformers.add(new ClassNameDeobfuscator());
		transformers.add(new FieldNameDeobfuscator());
		transformers.add(new MethodNameDeobfuscator());

		System.out.println("Loading classes...");
		module = new ClassCollection(inputURL);
		module.load();
		System.out.println("Loaded " + module.getClasses().size() + " client classes!");
		System.out.println();
		
		transform(module, transformers);

		module.transform();

		System.out.println("Saving client...");
		module.save(new File("./input2.jar"));
		System.out.println("Saved client");
	}

	private void transform(ClassCollection module,
			List<Transformer> transformers) {
		trans: for (Transformer t : transformers) {
			try {
				System.out.println("Applying " + t + "...");

				t.transform(module);
				for (ClassNode c : module.getClasses().values()) {
					if (t.finished()) {
						t.onFinish();
						continue trans;
					}

					t.visitClass(c);

					for (Object fo : new ArrayList(c.fields)) {
						if (t.finished()) {
							break;
						}

						FieldNode f = (FieldNode) fo;

						t.visitField(c, f);
					}

					for (Object mo : new ArrayList(c.methods)) {
						if (t.finished()) {
							break;
						}

						MethodNode m = (MethodNode) mo;
						if ((m.access & ACC_ABSTRACT) != 0
								|| (m.access & ACC_NATIVE) != 0) {
							continue;
						}

						t.visitMethod(c, m);

						AbstractInsnNode[] insns = m.instructions.toArray();
						for (AbstractInsnNode insn : insns) {
							if (insn == null || !m.instructions.contains(insn)) {
								continue;
							}
							if (t.finished()) {
								break;
							}

							t.visitInsn(c, m, insn);

							if (insn instanceof IntInsnNode) {
								t.visitIntInsn(c, m, (IntInsnNode) insn);
							}
							if (insn instanceof VarInsnNode) {
								t.visitVarInsn(c, m, (VarInsnNode) insn);
							}
							if (insn instanceof TypeInsnNode) {
								t.visitTypeInsn(c, m, (TypeInsnNode) insn);
							}
							if (insn instanceof FieldInsnNode) {
								t.visitFieldInsn(c, m, (FieldInsnNode) insn);
							}
							if (insn instanceof MethodInsnNode) {
								t.visitMethodInsn(c, m, (MethodInsnNode) insn);
							}
							if (insn instanceof JumpInsnNode) {
								t.visitJumpInsn(c, m, (JumpInsnNode) insn);
							}
							if (insn instanceof LdcInsnNode) {
								t.visitLdcInsn(c, m, (LdcInsnNode) insn);
							}
							if (insn instanceof IincInsnNode) {
								t.visitIincInsn(c, m, (IincInsnNode) insn);
							}
							if (insn instanceof TableSwitchInsnNode) {
								t.visitTableSwitchInsn(c, m,
										(TableSwitchInsnNode) insn);
							}
							if (insn instanceof LookupSwitchInsnNode) {
								t.visitLookupSwitchInsn(c, m,
										(LookupSwitchInsnNode) insn);
							}
							if (insn instanceof MultiANewArrayInsnNode) {
								t.visitMultiANewArrayInsn(c, m,
										(MultiANewArrayInsnNode) insn);
							}
						}

						for (Object tcbo : m.tryCatchBlocks) {
							if (t.finished()) {
								break;
							}
							TryCatchBlockNode tcb = (TryCatchBlockNode) tcbo;

							t.visitTryCatchBlock(c, m, tcb);
						}
					}
				}
				if (t.finished() || t.forceFinish()) {
					t.onFinish();
				}
				System.out.println();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
