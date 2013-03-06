package alterrs.deob.asm.utility;

import alterrs.asm.tree.ClassNode;

/**
 * @author Lazaro
 */
public class ID {
	public String name;

	public ID(String name) {
		if (name.endsWith(";")) {
			name = name.substring(name.indexOf('L') + 1, name.length() - 1);
		}
		this.name = name;

	}

	public ID(ClassNode c) {
		this(c.name);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ID)) {
			return false;
		}
		return name.equals(((ID) o).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
