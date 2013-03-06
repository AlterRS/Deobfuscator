package alterrs.deob.asm.utility;

import alterrs.asm.tree.ClassNode;
import alterrs.asm.tree.MethodInsnNode;
import alterrs.asm.tree.MethodNode;

/**
 * @author Lazaro
 */
public class MethodID extends ID {
	public String owner;
	public String desc;

	public MethodID(String owner, String name, String desc) {
		super(name);
		this.owner = owner;
		this.desc = desc;
	}

	public MethodID(ClassNode c, MethodNode n) {
		this(c.name, n.name, n.desc);
	}

	public MethodID(MethodInsnNode n) {
		this(n.owner, n.name, n.desc);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MethodID)) {
			return false;
		}

		MethodID id = (MethodID) o;
		return name.equals(id.name) && owner.equals(id.owner)
				&& desc.equals(id.desc);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + owner.hashCode();
		result = prime * result + desc.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return owner.replace("/", ".") + "." + name + desc;
	}
}
