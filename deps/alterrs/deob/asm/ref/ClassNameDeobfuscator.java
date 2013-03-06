package alterrs.deob.asm.ref;

import static alterrs.asm.Opcodes.ACC_INTERFACE;
import static alterrs.asm.Opcodes.ACC_NATIVE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import alterrs.asm.tree.AbstractInsnNode;
import alterrs.asm.tree.ClassNode;
import alterrs.asm.tree.LdcInsnNode;
import alterrs.asm.tree.MethodNode;
import alterrs.deob.asm.Renamer;
import alterrs.deob.asm.utility.AbstractTransformer;
import alterrs.deob.asm.utility.ClassCollection;
import alterrs.deob.asm.utility.ID;

/**
 * @author Lazaro Brito
 */
public class ClassNameDeobfuscator extends AbstractTransformer {
	private int interfaceId = 1;
	private Map<ID, Integer> classIds = new HashMap<ID, Integer>();
	private int cCount = 0;

	public ClassNameDeobfuscator() {
		super(true);
		Renamer.ctx.atr.set("ignoredClasses", new HashSet<ID>());
	}

	@Override
	public void transform(ClassCollection cc) {
		for (int level = 0; level < 32; level++) {
			classLoop: for (ClassNode c : cc.getClasses().values()) {
				if (cc.getLevel(c) == level) {
					String cName = c.name.substring(
							c.name.lastIndexOf("/") + 1, c.name.length());
					if (cName.length() <= 3
							&& !containsNativeMethods(c)
							&& Renamer.ctx.module.getRefactorer().getClass(
									new ID(c)) == null) {
						String name;
						if ((c.access & ACC_INTERFACE) != 0) {
							name = "Interface" + interfaceId++;
						} else {
							name = name(c);
							cCount++;
						}
						Renamer.ctx.module.getRefactorer().refactorClass(
								new ID(c), name);
					} else {
						for (Object mo : c.methods) {
							MethodNode m = (MethodNode) mo;
							if (m.name.equals("<init>")) {
								for (AbstractInsnNode n : m.instructions
										.toArray()) {
									if (n instanceof LdcInsnNode) {
										if (((LdcInsnNode) n).cst
												.equals("sw3d")) {
											Renamer.ctx.atr.<Set<ID>> get(
													"ignoredClasses").add(
													new ID(c));
											continue classLoop;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onFinish() {
		System.out.println(" ^ Refactored " + cCount + " classes and "
				+ (interfaceId - 1) + " interfaces!");
	}

	private int id(ClassNode c) {
		if (c.superName == null) {
			return 1;
		} else {
			Integer i = classIds.get(new ID(c.superName));
			if (i == null) {
				i = 0;
			}
			i++;
			classIds.put(new ID(c.superName), i);
			return i;
		}
	}

	private String name(ClassNode c) {
		int id = id(c);
		int level = Renamer.ctx.module.getLevel(c);

		if (level == 1) {
			return "Class" + id;
		} else {
			StringBuilder name = new StringBuilder();
			String superName = Renamer.ctx.module.getRefactorer().getClass(
					new ID(c.superName));
			if (superName != null) {
				name.append(superName);
			} else {
				name.append(c.superName.substring(
						c.superName.lastIndexOf("/") + 1, c.superName.length()));
			}
			name.append("_Sub").append(id);
			return name.toString();
		}
	}

	private boolean containsNativeMethods(ClassNode c) {
		for (Object mo : c.methods) {
			MethodNode m = (MethodNode) mo;
			if ((m.access & ACC_NATIVE) != 0) {
				return true;
			}
		}
		return false;
	}
}
