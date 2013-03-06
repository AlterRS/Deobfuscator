package alterrs.deob.asm.ref;

import static alterrs.asm.Opcodes.ACC_NATIVE;

import java.util.Set;

import alterrs.asm.tree.ClassNode;
import alterrs.asm.tree.MethodNode;
import alterrs.deob.asm.Renamer;
import alterrs.deob.asm.utility.AbstractTransformer;
import alterrs.deob.asm.utility.ClassCollection;
import alterrs.deob.asm.utility.ID;
import alterrs.deob.asm.utility.IdentifierUtil;
import alterrs.deob.asm.utility.MethodID;

/**
 * @author Lazaro Brito
 */
public class MethodNameDeobfuscator extends AbstractTransformer {
	private int methodId = 1, nCount = 0;

	public MethodNameDeobfuscator() {
		super(true);
	}

	@Override
	public void transform(ClassCollection cc) {
		for (int level = 0; level < 32; level++) {
			for (ClassNode c : cc.getClasses().values()) {
				if (cc.getLevel(c) == level) {
					methodLoop: for (Object mo : c.methods) {
						MethodNode m = (MethodNode) mo;

						boolean illegal = IdentifierUtil
								.illegalIdentifier(m.name);

						boolean force = false;
						if (Renamer.ctx.atr.<Set<ID>> get("ignoredClasses")
								.contains(new ID(c.name))) {
							if (!illegal)
								continue methodLoop;
							force = true;
						}

						if (Renamer.ctx.module.getRefactorer().getMethod(
								new MethodID(c, m)) == null) {
							if ((m.access & ACC_NATIVE) == 0
									&& m.name.length() <= 3) {
								boolean decl = false;
								ClassNode[] ds = Renamer.ctx.module
										.getDeclorators(new MethodID(c, m));
								for (ClassNode d : ds) {
									if (c == d) {
										decl = true;
									}

									if (Renamer.ctx.atr.<Set<ID>> get(
											"ignoredClasses").contains(
											new ID(d.name))) {
										if (!illegal)
											continue methodLoop;
										force = true;
									}
								}
								if (decl) {
									boolean nativeMethod = false;
									for (ClassNode c2 : ds) {
										MethodNode m2 = Renamer.ctx.module
												.method(new MethodID(c2, m));
										if ((m2.access & ACC_NATIVE) != 0) {
											if (!illegal) {
												continue methodLoop;
											}

											nativeMethod = true;
											nCount++;
										}

										for (ClassNode c3 : Renamer.ctx.module
												.getInheritors(new MethodID(c2,
														m))) {
											MethodNode m3 = Renamer.ctx.module
													.method(new MethodID(c3, m));
											if ((m3.access & ACC_NATIVE) != 0) {
												if (!illegal) {
													continue methodLoop;
												}

												nativeMethod = true;
												nCount++;
											}
										}
									}

									String oldName = null;
									oldNameLoop: for (ClassNode c2 : ds) {
										for (ClassNode c3 : Renamer.ctx.module
												.getInheritors(new MethodID(c2,
														m))) {
											oldName = Renamer.ctx.module
													.getRefactorer()
													.getMethod(
															new MethodID(c3, m));
											if (oldName != null)
												break oldNameLoop;
										}
									}

									String name = oldName == null ? (!nativeMethod ? ("method" + methodId++)
											: ("native_" + m.name))
											: oldName;

									for (ClassNode c2 : ds) {
										Renamer.ctx.module.getRefactorer()
												.refactorMethod(
														new MethodID(c2, m),
														name);
										for (ClassNode c3 : Renamer.ctx.module
												.getInheritors(new MethodID(c2,
														m))) {
											Renamer.ctx.module
													.getRefactorer()
													.refactorMethod(
															new MethodID(c3, m),
															name);
										}
									}

									if (force || nativeMethod) {
										System.out.println(" ^ Forced to rename method : "
												+ new MethodID(c, m));
									}
								}
							} else if ((m.access & ACC_NATIVE) != 0 && illegal) {
								String name = "native_" + m.name;
								Renamer.ctx.module.getRefactorer().refactorMethod(
										new MethodID(c, m), name);
								nCount++;

								System.out.println(" ^ Forced to rename method : "
										+ new MethodID(c, m));
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onFinish() {
		System.out.println(" ^ Refactored " + (methodId - 1) + " methods!");
		if (nCount > 0) {
			System.out.println(" ^ Refactored " + nCount
					+ " native methods due to illegal naming!");
		}
	}
}
