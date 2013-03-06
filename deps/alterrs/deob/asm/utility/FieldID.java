package alterrs.deob.asm.utility;

import alterrs.asm.tree.ClassNode;
import alterrs.asm.tree.FieldInsnNode;
import alterrs.asm.tree.FieldNode;

/**
 * @author Lazaro
 */
public class FieldID extends ID {
	public String owner;
	public String desc;

	public FieldID(String owner, String name, String desc) {
		super(name);
		this.owner = owner;
		this.desc = desc;
	}

	public FieldID(ClassNode c, FieldNode f) {
		this(c.name, f.name, f.desc);
	}

	public FieldID(FieldInsnNode n) {
		this(n.owner, n.name, n.desc);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FieldID)) {
			return false;
		}

		FieldID id = (FieldID) o;
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
		return owner.replace("/", ".") + "." + name + " " + desc;
	}
}
