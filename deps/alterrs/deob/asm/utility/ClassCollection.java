package alterrs.deob.asm.utility;

import static alterrs.asm.Opcodes.ACC_INTERFACE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import alterrs.asm.ClassReader;
import alterrs.asm.ClassWriter;
import alterrs.asm.tree.ClassNode;
import alterrs.asm.tree.FieldNode;
import alterrs.asm.tree.MethodNode;
import alterrs.commons.Attributes;

/**
 * @author Lazaro
 */
public class ClassCollection {
	private URL jarURL;

	private Map<String, ClassNode> classes = null;
	private Attributes attributes = new Attributes();
	private Refactorer refactorer = new Refactorer(this);

	private Map<ID, ClassNode> cachedExternalClasses = new HashMap<ID, ClassNode>();

	public ClassCollection(URL jarURL) {
		this.jarURL = jarURL;
	}

	public void load() throws IOException {
		classes = new HashMap<String, ClassNode>();

		JarFile jar = ((JarURLConnection) jarURL.openConnection()).getJarFile();
		Enumeration<JarEntry> entries = jar.entries();
		if (!entries.hasMoreElements()) {
			return;
		}
		JarEntry entry;
		while (entries.hasMoreElements()) {
			entry = entries.nextElement();

			if (!entry.getName().endsWith(".class")) {
				continue;
			}

			ClassNode c = new ClassNode();
			ClassReader cr = new ClassReader(jar.getInputStream(jar
					.getEntry(entry.getName())));
			cr.accept(c, ClassReader.SKIP_FRAMES);

			classes.put(c.name, c);
		}
	}

	public void transform() {
		refactorer.run();
	}

	public void save(File dst) throws IOException {
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(dst));
		for (ClassNode c : classes.values()) {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			c.accept(cw);
			JarEntry je = new JarEntry(c.name + ".class");
			jos.putNextEntry(je);
			jos.write(cw.toByteArray());
			jos.closeEntry();
		}
		jos.close();
	}

	public Map<String, ClassNode> getClasses() {
		return classes;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	public Refactorer getRefactorer() {
		return refactorer;
	}

	public FieldNode field(FieldID id) {
		ClassNode c = classes.get(id.owner);
		if (c != null) {
			for (Object fo : c.fields) {
				FieldNode f = (FieldNode) fo;
				if (f.name.equals(id.name) && f.desc.equals(id.desc)) {
					return f;
				}
			}
		}
		return null;
	}

	public MethodNode method(MethodID id) {
		ClassNode c = classes.get(id.owner);
		if (c != null) {
			for (Object mo : c.methods) {
				MethodNode m = (MethodNode) mo;
				if (m.name.equals(id.name) && m.desc.equals(id.desc)) {
					return m;
				}
			}
		}
		return null;
	}

	public List<ClassNode> getInterfaces() {
		List<ClassNode> list = new ArrayList<ClassNode>();
		for (ClassNode c : getClasses().values()) {
			if ((c.access & ACC_INTERFACE) != 0) {
				list.add(c);
			}
		}
		return list;
	}

	public ClassNode[] getDeclorators(MethodID id) {
		ClassNode c = getClass(id.owner);
		if(c == null) {
			System.out.println("Null owner for " + id);
		}

		List<ClassNode> list = new ArrayList<ClassNode>();

		for (ClassNode c2 : getInterfaces(c)) {
			if (c2 != c) {
				for (Object mo : c2.methods) {
					MethodNode m2 = (MethodNode) mo;
					if (m2.name.equals(id.name) && m2.desc.equals(id.desc)) {
						list.add(c2);
					}
				}
			}
		}

		if (list.isEmpty()) {
			hierarchyLoop: for (ClassNode c2 : getHierarchy(c)) {
				if (c2 != c) {
					for (Object mo : c2.methods) {
						MethodNode m2 = (MethodNode) mo;
						if (m2.name.equals(id.name) && m2.desc.equals(id.desc)) {
							list.add(c2);
							break hierarchyLoop;
						}
					}
				}
			}
		}

		if (list.isEmpty() && ((c.access & ACC_INTERFACE) != 0)) {
			for (ClassNode c2 : getInterfaces()) {
				if (c2 != c) {
					for (Object mo : c2.methods) {
						MethodNode m2 = (MethodNode) mo;
						if (m2.name.equals(id.name) && m2.desc.equals(id.desc)) {
							list.add(c2);
						}
					}
				}
			}
		}

		if (list.isEmpty() || ((c.access & ACC_INTERFACE) != 0)) {
			list.add(c);
		}

		return list.toArray(new ClassNode[0]);
	}

	public ClassNode[] getInheritors(MethodID id) {
		ClassNode c = getClass(id.owner);

		List<ClassNode> inheritors = new LinkedList<ClassNode>();
		classLoop: for (ClassNode c2 : getClasses().values()) {
			ClassNode[] inherited = (c.access & ACC_INTERFACE) == 0 ? getHierarchy(c2)
					: getInterfaces(c2);
			int index = ArrayUtil.searchArray(inherited, c);
			if (index >= 0) {
				for (Object mo : c2.methods) {
					MethodNode m2 = (MethodNode) mo;
					if (m2.name.equals(id.name) && m2.desc.equals(id.desc)
							&& !inheritors.contains(c2)) {
						inheritors.add(c2);
						continue classLoop;
					}
				}
			}
		}
		return inheritors.toArray(new ClassNode[0]);
	}

	public ClassNode[] getHierarchy(ClassNode c) {
		LinkedList<ClassNode> hierarchy = new LinkedList<ClassNode>();

		ClassNode c2 = c;
		while (c2 != null) {
			hierarchy.addFirst(c2);

			if (c2.superName == null) {
				break;
			}

			c2 = getClass(c2.superName);
		}

		return hierarchy.toArray(new ClassNode[0]);
	}

	public ClassNode[] getInterfaces(ClassNode c) {
		LinkedList<ClassNode> interfaces = new LinkedList<ClassNode>();

		ClassNode[] hierarchy = getHierarchy(c);

		for (ClassNode c2 : hierarchy) {
			if ((c2.access & ACC_INTERFACE) != 0) {
				interfaces.add(c2);
			}

			for (Object c3o : c2.interfaces) {
				ClassNode c3 = getClass((String) c3o);
				interfaces.addFirst(c3);

				for (ClassNode c4 : getInterfaces(c3)) {
					interfaces.addFirst(c4);
				}
			}
		}

		return interfaces.toArray(new ClassNode[0]);
	}

	/*
	 * public ClassNode[] getInterfaces(ClassNode c) { LinkedList<ClassNode>
	 * interfaces = new LinkedList<ClassNode>();
	 * 
	 * ClassNode[] hierarchy; if ((c.access & ACC_INTERFACE) == 0) { hierarchy =
	 * getHierarchy(c); } else { hierarchy = new ClassNode[]{c}; }
	 * 
	 * for (ClassNode c2 : hierarchy) { for (Object c3o : c2.interfaces) {
	 * ClassNode c3 = getClass((String) c3o); interfaces.addFirst(c3); if
	 * (!c3.interfaces.isEmpty()) { for (ClassNode c4 : getInterfaces(c3)) {
	 * interfaces.addFirst(c4); } } } }
	 * 
	 * return interfaces.toArray(new ClassNode[0]); }
	 */

	public ClassNode getClass(String name) {
		if (name == null) {
			return null;
		}

		ClassNode c = getClasses().get(name);
		if (c == null) {
			c = cachedExternalClasses.get(new ID(name));

			if (c == null) {
				c = new ClassNode();
				ClassReader cr;
				try {
					cr = new ClassReader(name);
				} catch (IOException e) {
					return null;
				}
				cr.accept(c, 0);

				cachedExternalClasses.put(new ID(name), c);
			}
		}
		return c;
	}

	public int getLevel(ClassNode c) {
		if ((c.access & ACC_INTERFACE) != 0) {
			return 0;
		}

		return getHierarchy(c).length - 1;
	}

	public boolean isExternal(ID id) {
		return cachedExternalClasses.containsKey(id);
	}
}
