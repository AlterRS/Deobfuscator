package alterrs.deob.asm.utility;

import alterrs.asm.tree.AbstractInsnNode;
import alterrs.asm.tree.LdcInsnNode;
import alterrs.asm.tree.MethodNode;

/**
 * @author Lazaro
 */
public class InsnFinder {
	public static interface Constraint<T extends AbstractInsnNode> {
		public boolean accept(T n);
	}

	private AbstractInsnNode[] instructions;
	private int index;

	public InsnFinder(MethodNode m) {
		instructions = m.instructions.toArray();
		index = -1;
	}

	public void index(int index) {
		this.index = index;
	}

	public AbstractInsnNode current() {
		return (index < 0 || index >= instructions.length) ? null
				: instructions[index];
	}

	public AbstractInsnNode next() {
		++index;
		return current();
	}

	public AbstractInsnNode previous() {
		--index;
		return current();
	}

	public <T extends AbstractInsnNode> T next(Class<T> type) {
		while (++index < instructions.length) {
			AbstractInsnNode instr = instructions[index];
			if (type.isAssignableFrom(instr.getClass())) {
				return (T) instr;
			}
		}
		return null;
	}

	public <T extends AbstractInsnNode> T previous(Class<T> type) {
		while (--index < instructions.length) {
			AbstractInsnNode instr = instructions[index];
			if (type.isAssignableFrom(instr.getClass())) {
				return (T) instr;
			}
		}
		return null;
	}

	public <T extends AbstractInsnNode> T next(int... opcodes) {
		while (++index < instructions.length) {
			AbstractInsnNode instr = instructions[index];
			for (int opcode : opcodes) {
				if (instr.getOpcode() == opcode) {
					return (T) instr;
				}
			}
		}
		return null;
	}

	public <T extends AbstractInsnNode> T previous(int... opcodes) {
		while (--index < instructions.length) {
			AbstractInsnNode instr = instructions[index];
			for (int opcode : opcodes) {
				if (instr.getOpcode() == opcode) {
					return (T) instr;
				}
			}
		}
		return null;
	}

	public <T extends AbstractInsnNode> T next(Class<T> type,
			Constraint<T> constr) {
		while (++index < instructions.length) {
			AbstractInsnNode instr = instructions[index];
			if (type.isAssignableFrom(instr.getClass())
					&& (constr == null || constr.accept((T) instr))) {
				return type.cast(instr);
			}
		}
		return null;
	}

	public <T extends AbstractInsnNode> T previous(Class<T> type,
			Constraint<T> constr) {
		while (--index >= 0) {
			AbstractInsnNode instr = instructions[index];
			if (type.isAssignableFrom(instr.getClass())
					&& (constr == null || constr.accept((T) instr))) {
				return type.cast(instr);
			}
		}
		return null;
	}

	public <T extends AbstractInsnNode> T next(Constraint<T> constr,
			int... opcodes) {
		while (++index < instructions.length) {
			AbstractInsnNode instr = instructions[index];
			for (int opcode : opcodes) {
				if (instr.getOpcode() == opcode
						&& (constr == null || constr.accept((T) instr))) {
					return (T) instr;
				}
			}
		}
		return null;
	}

	public <T extends AbstractInsnNode> T previous(Constraint<T> constr,
			int... opcodes) {
		while (--index < instructions.length) {
			AbstractInsnNode instr = instructions[index];
			for (int opcode : opcodes) {
				if (instr.getOpcode() == opcode
						&& (constr == null || constr.accept((T) instr))) {
					return (T) instr;
				}
			}
		}
		return null;
	}

	public LdcInsnNode nextLDC(final Object val) {
		Constraint constraint = new Constraint() {
			public boolean accept(AbstractInsnNode n) {
				LdcInsnNode ldc = (LdcInsnNode) n;
				return ldc.cst.equals(val);
			}
		};
		return next(LdcInsnNode.class, constraint);
	}

	public LdcInsnNode previousLDC(final Object val) {
		Constraint constraint = new Constraint() {
			public boolean accept(AbstractInsnNode n) {
				LdcInsnNode ldc = (LdcInsnNode) n;
				return ldc.cst.equals(val);
			}
		};
		return next(LdcInsnNode.class, constraint);
	}
}
