/* ClassInfo Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: ClassInfo.java.in,v 4.8.2.7 2002/11/24 15:51:56 hoenicke Exp $
 */

package jode.bytecode;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Iterator;

import jode.GlobalOptions;
import jode.util.UnifyHash;


/**
 * This class does represent a class similar to java.lang.Class. You can get the
 * super class and the interfaces. <br>
 * <p/>
 * The main difference to java.lang.Class is, that the objects are builded from
 * a stream containing the .class file, and that it uses the <code>Type</code>
 * to represent types instead of Class itself. <br>
 * <p/>
 * <h2>The InnerClasses attribute</h2>
 * <p/>
 * The InnerClasses attribute is transformed in a special way by this class so
 * we want to taker a closer look. According to the <a href=
 * "http://java.sun.com/products/jdk/1.1/docs/guide/innerclasses/spec/innerclasses.doc10.html#18814"
 * >inner class specification</a> there must be an InnerClass attribute for
 * every non top-level class that is referenced somewhere in the bytecode. This
 * implies that if this is an inner class, it must contain a inner class
 * attribute for itself. Before a class is referenced as outer class in an
 * InnerClass attribute, it must be described by another InnerClass attribute.
 * <br>
 * <p/>
 * Since every class references itself, there must be informations about the
 * outer class for each class scoped class. If that outer class is an outer
 * class again, there must be information about it, too. This particular chain
 * of InnerClassInfos is returned by the getOuterClasses() method; for
 * convenience in reverse order, i.e. current class first, then the outer
 * classes from innermost to outermost. <br>
 * <p/>
 * A valid bytecode must also contain InnerClass infos for each inner classes it
 * declares. These information are returned by the getInnerClasses() method. The
 * order of these classes is the same as in the bytecode attribute.
 * <p/>
 * All remaining attributes are returned by getExtraClasses() in the same order
 * as in the bytecode attribute.
 * 
 * @author Jochen Hoenicke
 */
public class ClassInfo extends BinaryInfo {

	private static SearchPath classpath;

	private static final UnifyHash classes = new UnifyHash();

	private int status = 0;

	private boolean modified = false;

	private int modifiers = -1;
	private boolean deprecatedFlag;
	private String name;
	private ClassInfo superclass;
	private ClassInfo[] interfaces;
	private FieldInfo[] fields;
	private MethodInfo[] methods;
	private InnerClassInfo[] outerClasses;
	private InnerClassInfo[] innerClasses;
	private InnerClassInfo[] extraClasses;
	private String sourceFile;

	public final static ClassInfo javaLangObject = forName("java.lang.Object");

	public static void setClassPath(String path) {
		setClassPath(new SearchPath(path));
	}

	public static void setClassPath(SearchPath path) {
		if (classpath != path) {
			classpath = path;
			Iterator i = classes.iterator();
			while (i.hasNext()) {
				ClassInfo ci = (ClassInfo) i.next();
				ci.status = 0;
				ci.superclass = null;
				ci.fields = null;
				ci.interfaces = null;
				ci.methods = null;
				ci.removeAllAttributes();
			}
		}
	}

	public static boolean exists(String name) {
		return classpath.exists(name.replace('.', '/') + ".class");
	}

	public static boolean isPackage(String name) {
		return classpath.isDirectory(name.replace('.', '/'));
	}

	public static Enumeration getClassesAndPackages(final String packageName) {
		final Enumeration enum_ = classpath.listFiles(packageName.replace('.',
				'/'));
		return new Enumeration() {
			public boolean hasMoreElements() {
				return enum_.hasMoreElements();
			}

			public Object nextElement() {
				String name = (String) enum_.nextElement();
				if (!name.endsWith(".class"))
					// This is a package
					return name;
				return name.substring(0, name.length() - 6);
			}
		};
	}

	public static ClassInfo forName(String name) {
		if (name == null || name.indexOf(';') != -1 || name.indexOf('[') != -1
				|| name.indexOf('/') != -1)
			throw new IllegalArgumentException("Illegal class name: " + name);

		int hash = name.hashCode();
		Iterator iter = classes.iterateHashCode(hash);
		while (iter.hasNext()) {
			ClassInfo clazz = (ClassInfo) iter.next();
			if (clazz.name.equals(name))
				return clazz;
		}
		ClassInfo clazz = new ClassInfo(name);
		classes.put(hash, clazz);
		return clazz;
	}

	private ClassInfo(String name) {
		this.name = name;
	}

	protected void readAttribute(String name, int length, ConstantPool cp,
			DataInputStream input, int howMuch) throws IOException {
		if ((howMuch & KNOWNATTRIBS) != 0 && name.equals("SourceFile")) {
			if (length != 2)
				throw new ClassFormatException("SourceFile attribute"
						+ " has wrong length");
			sourceFile = cp.getUTF8(input.readUnsignedShort());
		} else if ((howMuch & (OUTERCLASSES | INNERCLASSES)) != 0
				&& name.equals("InnerClasses")) {
			int count = input.readUnsignedShort();
			if (length != 2 + 8 * count)
				throw new ClassFormatException(
						"InnerClasses attribute has wrong length");
			int innerCount = 0, outerCount = 0, extraCount = 0;
			InnerClassInfo[] innerClassInfo = new InnerClassInfo[count];
			for (int i = 0; i < count; i++) {
				int innerIndex = input.readUnsignedShort();
				int outerIndex = input.readUnsignedShort();
				int nameIndex = input.readUnsignedShort();
				String inner = cp.getClassName(innerIndex);
				String outer = outerIndex != 0 ? cp.getClassName(outerIndex)
						: null;
				String innername = nameIndex != 0 ? cp.getUTF8(nameIndex)
						: null;
				int access = input.readUnsignedShort();
				if (innername != null && innername.length() == 0)
					innername = null;

				/*
				 * Some compilers give method scope classes a valid outer field,
				 * but we mustn't handle them as inner classes. The best way to
				 * distinguish this case is by the class name.
				 */
				if (outer != null
						&& innername != null
						&& inner.length() > outer.length() + 2
								+ innername.length()
						&& inner.startsWith(outer + "$")
						&& inner.endsWith("$" + innername)
						&& Character.isDigit(inner.charAt(outer.length() + 1)))
					outer = null;

				InnerClassInfo ici = new InnerClassInfo(inner, outer,
						innername, access);

				if (outer != null && outer.equals(getName())
						&& innername != null)
					innerClassInfo[innerCount++] = ici;
				else
					innerClassInfo[count - (++extraCount)] = ici;
			}
			/*
			 * Now innerClasses are at the front of innerClassInfo array in
			 * correct order. The other InnerClassInfos are in reverse order in
			 * the rest of the innerClassInfo array.
			 */

			/*
			 * We now count the outerClasses. The reverse order is the right
			 * thing for us.
			 */
			{
				String lastOuterName = getName();
				for (int i = count - extraCount; i < count
						&& lastOuterName != null; i++) {
					InnerClassInfo ici = innerClassInfo[i];
					if (ici.inner.equals(lastOuterName)) {
						outerCount++;
						extraCount--;
						lastOuterName = ici.outer;
					}
				}
			}
			if (innerCount > 0) {
				innerClasses = new InnerClassInfo[innerCount];
				System.arraycopy(innerClassInfo, 0, innerClasses, 0, innerCount);
			} else
				innerClasses = null;

			if (outerCount > 0) {
				outerClasses = new InnerClassInfo[outerCount];
			} else
				outerClasses = null;

			if (extraCount > 0) {
				extraClasses = new InnerClassInfo[extraCount];
			} else
				extraClasses = null;

			/*
			 * The last part: We split between outer and extra classes. In this
			 * step we will also revert the order of the extra classes.
			 */
			{
				int outerPtr = 0;
				String lastOuterName = getName();
				for (int i = count - extraCount - outerCount; i < count; i++) {
					InnerClassInfo ici = innerClassInfo[i];

					/*
					 * If we counted correctly there is no NullPointer or
					 * ArrayIndexOutOfBoundsException here
					 */
					if (ici.inner.equals(lastOuterName)) {
						outerClasses[outerPtr++] = ici;
						lastOuterName = ici.outer;
					} else
						extraClasses[--extraCount] = ici;
				}
			}
		} else if (name.equals("Deprecated")) {
			deprecatedFlag = true;
			if (length != 0)
				throw new ClassFormatException(
						"Deprecated attribute has wrong length");
		} else
			super.readAttribute(name, length, cp, input, howMuch);
	}

	public void read(DataInputStream input, int howMuch) throws IOException {
		/*
		 * Since we have to read the whole class anyway, we load all info, that
		 * we may need later and that does not take much memory.
		 */
		howMuch |= HIERARCHY | INNERCLASSES | OUTERCLASSES;
		howMuch &= ~status;
		/* header */
		if (input.readInt() != 0xcafebabe)
			throw new ClassFormatException("Wrong magic");
		int version = input.readUnsignedShort();
		version |= input.readUnsignedShort() << 16;
		if (version < (45 << 16 | 0))
			throw new ClassFormatException("Wrong class version");

		/* constant pool */
		ConstantPool cpool = new ConstantPool();
		cpool.read(input);

		/* always read modifiers, name, super, ifaces */
		{
			modifiers = input.readUnsignedShort();
			String className = cpool.getClassName(input.readUnsignedShort());
			if (!name.equals(className))
				throw new ClassFormatException("wrong name " + className);
			String superName = cpool.getClassName(input.readUnsignedShort());
			superclass = superName != null ? ClassInfo.forName(superName)
					: null;
			int count = input.readUnsignedShort();
			interfaces = new ClassInfo[count];
			for (int i = 0; i < count; i++) {
				interfaces[i] = ClassInfo.forName(cpool.getClassName(input
						.readUnsignedShort()));
			}
			status |= HIERARCHY;
		}

		/* fields */
		if ((howMuch & (FIELDS | KNOWNATTRIBS | UNKNOWNATTRIBS)) != 0) {
			int count = input.readUnsignedShort();
			if ((status & FIELDS) == 0)
				fields = new FieldInfo[count];
			for (int i = 0; i < count; i++) {
				if ((status & FIELDS) == 0)
					fields[i] = new FieldInfo(this);
				fields[i].read(cpool, input, howMuch);
			}
		} else {
			byte[] skipBuf = new byte[6];
			int count = input.readUnsignedShort();
			for (int i = 0; i < count; i++) {
				input.readFully(skipBuf); // modifier, name, type
				skipAttributes(input);
			}
		}

		/* methods */
		if ((howMuch & (METHODS | KNOWNATTRIBS | UNKNOWNATTRIBS)) != 0) {
			int count = input.readUnsignedShort();
			if ((status & METHODS) == 0)
				methods = new MethodInfo[count];
			for (int i = 0; i < count; i++) {
				if ((status & METHODS) == 0)
					methods[i] = new MethodInfo(this);
				methods[i].read(cpool, input, howMuch);
			}
		} else {
			byte[] skipBuf = new byte[6];
			int count = input.readUnsignedShort();
			for (int i = 0; i < count; i++) {
				input.readFully(skipBuf); // modifier, name, type
				skipAttributes(input);
			}
		}

		/* attributes */
		readAttributes(cpool, input, howMuch);
		status |= howMuch;
	}

	public void reserveSmallConstants(GrowableConstantPool gcp) {
		for (int i = 0; i < fields.length; i++)
			fields[i].reserveSmallConstants(gcp);

		for (int i = 0; i < methods.length; i++)
			methods[i].reserveSmallConstants(gcp);
	}

	public void prepareWriting(GrowableConstantPool gcp) {
		gcp.putClassName(name);
		gcp.putClassName(superclass.getName());
		for (int i = 0; i < interfaces.length; i++)
			gcp.putClassName(interfaces[i].getName());

		for (int i = 0; i < fields.length; i++)
			fields[i].prepareWriting(gcp);

		for (int i = 0; i < methods.length; i++)
			methods[i].prepareWriting(gcp);

		if (sourceFile != null) {
			gcp.putUTF8("SourceFile");
			gcp.putUTF8(sourceFile);
		}
		if (outerClasses != null || innerClasses != null
				|| extraClasses != null) {
			gcp.putUTF8("InnerClasses");
			int outerCount = outerClasses != null ? outerClasses.length : 0;
			for (int i = outerCount; i-- > 0;) {
				gcp.putClassName(outerClasses[i].inner);
				if (outerClasses[i].outer != null)
					gcp.putClassName(outerClasses[i].outer);
				if (outerClasses[i].name != null)
					gcp.putUTF8(outerClasses[i].name);
			}
			int innerCount = innerClasses != null ? innerClasses.length : 0;
			for (int i = 0; i < innerCount; i++) {
				gcp.putClassName(innerClasses[i].inner);
				if (innerClasses[i].outer != null)
					gcp.putClassName(innerClasses[i].outer);
				if (innerClasses[i].name != null)
					gcp.putUTF8(innerClasses[i].name);
			}
			int extraCount = extraClasses != null ? extraClasses.length : 0;
			for (int i = 0; i < extraCount; i++) {
				gcp.putClassName(extraClasses[i].inner);
				if (extraClasses[i].outer != null)
					gcp.putClassName(extraClasses[i].outer);
				if (extraClasses[i].name != null)
					gcp.putUTF8(extraClasses[i].name);
			}
		}
		if (deprecatedFlag)
			gcp.putUTF8("Deprecated");
		prepareAttributes(gcp);
	}

	protected int getKnownAttributeCount() {
		int count = 0;
		if (sourceFile != null)
			count++;
		if (innerClasses != null || outerClasses != null
				|| extraClasses != null)
			count++;
		return count;
	}

	public void writeKnownAttributes(GrowableConstantPool gcp,
			DataOutputStream output) throws IOException {
		if (sourceFile != null) {
			output.writeShort(gcp.putUTF8("SourceFile"));
			output.writeInt(2);
			output.writeShort(gcp.putUTF8(sourceFile));
		}
		if (outerClasses != null || innerClasses != null
				|| extraClasses != null) {
			output.writeShort(gcp.putUTF8("InnerClasses"));
			int outerCount = (outerClasses != null) ? outerClasses.length : 0;
			int innerCount = (innerClasses != null) ? innerClasses.length : 0;
			int extraCount = (extraClasses != null) ? extraClasses.length : 0;
			int count = outerCount + innerCount + extraCount;
			output.writeInt(2 + count * 8);
			output.writeShort(count);
			for (int i = outerCount; i-- > 0;) {
				output.writeShort(gcp.putClassName(outerClasses[i].inner));
				output.writeShort(outerClasses[i].outer != null ? gcp
						.putClassName(outerClasses[i].outer) : 0);
				output.writeShort(outerClasses[i].name != null ? gcp
						.putUTF8(outerClasses[i].name) : 0);
				output.writeShort(outerClasses[i].modifiers);
			}
			for (int i = 0; i < innerCount; i++) {
				output.writeShort(gcp.putClassName(innerClasses[i].inner));
				output.writeShort(innerClasses[i].outer != null ? gcp
						.putClassName(innerClasses[i].outer) : 0);
				output.writeShort(innerClasses[i].name != null ? gcp
						.putUTF8(innerClasses[i].name) : 0);
				output.writeShort(innerClasses[i].modifiers);
			}
			for (int i = 0; i < extraCount; i++) {
				output.writeShort(gcp.putClassName(extraClasses[i].inner));
				output.writeShort(extraClasses[i].outer != null ? gcp
						.putClassName(extraClasses[i].outer) : 0);
				output.writeShort(extraClasses[i].name != null ? gcp
						.putUTF8(extraClasses[i].name) : 0);
				output.writeShort(extraClasses[i].modifiers);
			}
		}
		if (deprecatedFlag) {
			output.writeShort(gcp.putUTF8("Deprecated"));
			output.writeInt(0);
		}
	}

	public void write(DataOutputStream out) throws IOException {
		GrowableConstantPool gcp = new GrowableConstantPool();
		reserveSmallConstants(gcp);
		prepareWriting(gcp);

		out.writeInt(0xcafebabe);
		out.writeShort(3);
		out.writeShort(45);
		gcp.write(out);

		out.writeShort(modifiers);
		out.writeShort(gcp.putClassName(name));
		out.writeShort(gcp.putClassName(superclass.getName()));
		out.writeShort(interfaces.length);
		for (int i = 0; i < interfaces.length; i++)
			out.writeShort(gcp.putClassName(interfaces[i].getName()));

		out.writeShort(fields.length);
		for (int i = 0; i < fields.length; i++)
			fields[i].write(gcp, out);

		out.writeShort(methods.length);
		for (int i = 0; i < methods.length; i++)
			methods[i].write(gcp, out);

		writeAttributes(gcp, out);
	}

	public void loadInfoReflection(Class clazz, int howMuch)
			throws SecurityException {
		if ((howMuch & HIERARCHY) != 0) {
			modifiers = clazz.getModifiers();
			if (clazz.getSuperclass() == null)
				superclass = clazz == Object.class ? null : javaLangObject;
			else
				superclass = ClassInfo.forName(clazz.getSuperclass().getName());
			Class[] ifaces = clazz.getInterfaces();
			interfaces = new ClassInfo[ifaces.length];
			for (int i = 0; i < ifaces.length; i++)
				interfaces[i] = ClassInfo.forName(ifaces[i].getName());
			status |= HIERARCHY;
		}
		if ((howMuch & FIELDS) != 0 && fields == null) {
			Field[] fs;
			try {
				fs = clazz.getDeclaredFields();
			} catch (SecurityException ex) {
				fs = clazz.getFields();
				GlobalOptions.err
						.println("Could only get public fields of class "
								+ name + ".");
			}
			fields = new FieldInfo[fs.length];
			for (int i = fs.length; --i >= 0;) {
				String type = TypeSignature.getSignature(fs[i].getType());
				fields[i] = new FieldInfo(this, fs[i].getName(), type,
						fs[i].getModifiers());
			}
		}
		if ((howMuch & METHODS) != 0 && methods == null) {
			Constructor[] cs;
			Method[] ms;
			try {
				cs = clazz.getDeclaredConstructors();
				ms = clazz.getDeclaredMethods();
			} catch (SecurityException ex) {
				cs = clazz.getConstructors();
				ms = clazz.getMethods();
				GlobalOptions.err
						.println("Could only get public methods of class "
								+ name + ".");
			}
			methods = new MethodInfo[cs.length + ms.length];
			for (int i = cs.length; --i >= 0;) {
				String type = TypeSignature.getSignature(
						cs[i].getParameterTypes(), void.class);
				methods[i] = new MethodInfo(this, "<init>", type,
						cs[i].getModifiers());
			}
			for (int i = ms.length; --i >= 0;) {
				String type = TypeSignature.getSignature(
						ms[i].getParameterTypes(), ms[i].getReturnType());
				methods[cs.length + i] = new MethodInfo(this, ms[i].getName(),
						type, ms[i].getModifiers());
			}
		}
		if ((howMuch & INNERCLASSES) != 0 && innerClasses == null) {
			Class[] is;
			try {
				is = clazz.getDeclaredClasses();
			} catch (SecurityException ex) {
				is = clazz.getClasses();
				GlobalOptions.err
						.println("Could only get public inner classes of class "
								+ name + ".");
			}
			if (is.length > 0) {
				innerClasses = new InnerClassInfo[is.length];
				for (int i = is.length; --i >= 0;) {
					String inner = is[i].getName();
					int dollar = inner.lastIndexOf('$');
					String name = inner.substring(dollar + 1);
					innerClasses[i] = new InnerClassInfo(inner, getName(),
							name, is[i].getModifiers());
				}
			}
		}
		if ((howMuch & OUTERCLASSES) != 0 && outerClasses == null) {
			int count = 0;
			Class declarer = clazz.getDeclaringClass();
			while (declarer != null) {
				count++;
				declarer = declarer.getDeclaringClass();
			}
			if (count > 0) {
				outerClasses = new InnerClassInfo[count];
				Class current = clazz;
				for (int i = 0; i < count; i++) {
					declarer = current.getDeclaringClass();
					String name = current.getName();
					int dollar = name.lastIndexOf('$');
					outerClasses[i] = new InnerClassInfo(name,
							declarer.getName(), name.substring(dollar + 1),
							current.getModifiers());
					current = declarer;
				}
			}
		}
		status |= howMuch;
	}

	public void loadInfo(int howMuch) {
		if ((status & howMuch) == howMuch)
			return;
		if (modified) {
			System.err.println("Allocating info 0x"
					+ Integer.toHexString(howMuch) + " (status 0x"
					+ Integer.toHexString(status) + ") in class " + this);
			Thread.dumpStack();
			return;
		}
		try {
			DataInputStream input = new DataInputStream(
					new BufferedInputStream(classpath.getFile(name.replace('.',
							'/') + ".class")));
			read(input, howMuch);
		} catch (IOException ex) {
			String message = ex.getMessage();
			if ((howMuch & ~(FIELDS | METHODS | HIERARCHY | INNERCLASSES | OUTERCLASSES)) != 0) {
				ex.printStackTrace();

				System.out.println("fields: " + ((howMuch & FIELDS) != 0));
				System.out.println("methods: " + ((howMuch & METHODS) != 0));
				System.out
						.println("hierarchy: " + ((howMuch & HIERARCHY) != 0));
				System.out.println("innerclasses: "
						+ ((howMuch & INNERCLASSES) != 0));
				System.out.println("outerclasses: "
						+ ((howMuch & OUTERCLASSES) != 0));
				throw new NoClassDefFoundError(name);
			}
			// Try getting the info through the reflection interface
			// instead.
			Class clazz = null;
			try {
				clazz = Class.forName(name);
			} catch (ClassNotFoundException ex2) {
			} catch (NoClassDefFoundError ex2) {
			}
			try {
				if (clazz != null) {
					loadInfoReflection(clazz, howMuch);
					return;
				}
			} catch (SecurityException ex2) {
				GlobalOptions.err.println(ex2
						+ " while collecting info about class " + name + ".");
			}

			// Give a warning and ``guess'' the hierarchie, methods etc.
			GlobalOptions.err.println("Can't read class " + name
					+ ", types may be incorrect. (" + ex.getClass().getName()
					+ (message != null ? ": " + message : "") + ")");
			ex.printStackTrace(GlobalOptions.err);

			if ((howMuch & HIERARCHY) != 0) {
				modifiers = Modifier.PUBLIC;
				if (name.equals("java.lang.Object"))
					superclass = null;
				else
					superclass = javaLangObject;
				interfaces = new ClassInfo[0];
			}
			if ((howMuch & METHODS) != 0)
				methods = new MethodInfo[0];
			if ((howMuch & FIELDS) != 0)
				fields = new FieldInfo[0];
			status |= howMuch;
		}
	}

	/**
	 * This is the counter part to loadInfo. It will drop all info specified in
	 * howMuch and clean up the memory.
	 * 
	 * @param howMuch
	 *            tells how much info we should drop
	 */
	public void dropInfo(int howMuch) {
		if ((status & howMuch) == 0)
			return;
		if (modified) {
			System.err.println("Dropping info 0x"
					+ Integer.toHexString(howMuch) + " (status 0x"
					+ Integer.toHexString(status) + ") in class " + this);
			Thread.dumpStack();
			return;
		}
		howMuch &= status;

		if ((howMuch & FIELDS) != 0) {
			fields = null;
		} else if ((status & FIELDS) != 0
				&& (howMuch & (KNOWNATTRIBS | UNKNOWNATTRIBS)) != 0) {
			for (int i = 0; i < fields.length; i++)
				fields[i].dropInfo(howMuch);
		}

		if ((howMuch & METHODS) != 0) {
			methods = null;
		} else if ((status & METHODS) != 0
				&& (howMuch & (KNOWNATTRIBS | UNKNOWNATTRIBS)) != 0) {
			for (int i = 0; i < methods.length; i++)
				methods[i].dropInfo(howMuch);
		}
		if ((howMuch & KNOWNATTRIBS) != 0)
			sourceFile = null;
		if ((howMuch & OUTERCLASSES) != 0)
			outerClasses = null;
		if ((howMuch & INNERCLASSES) != 0) {
			innerClasses = null;
			extraClasses = null;
		}
		super.dropInfo(howMuch);
		status &= ~howMuch;
	}

	public String getName() {
		return name;
	}

	public String getJavaName() {
		/*
		 * Don't load attributes for class names not containing a dollar sign.
		 */
		if (name.indexOf('$') == -1)
			return getName();
		if (getOuterClasses() != null) {
			int last = outerClasses.length - 1;
			StringBuffer sb = new StringBuffer(
					outerClasses[last].outer != null ? outerClasses[last].outer
							: "METHOD");
			for (int i = last; i >= 0; i--)
				sb.append(".").append(
						outerClasses[i].name != null ? outerClasses[i].name
								: "ANONYMOUS");
			return sb.toString();
		}
		return getName();
	}

	public ClassInfo getSuperclass() {
		if ((status & HIERARCHY) == 0)
			loadInfo(HIERARCHY);
		return superclass;
	}

	public ClassInfo[] getInterfaces() {
		if ((status & HIERARCHY) == 0)
			loadInfo(HIERARCHY);
		return interfaces;
	}

	public int getModifiers() {
		if ((status & HIERARCHY) == 0)
			loadInfo(HIERARCHY);
		return modifiers;
	}

	public boolean isInterface() {
		return Modifier.isInterface(getModifiers());
	}

	public boolean isDeprecated() {
		return deprecatedFlag;
	}

	public FieldInfo findField(String name, String typeSig) {
		if ((status & FIELDS) == 0)
			loadInfo(FIELDS);
		for (int i = 0; i < fields.length; i++)
			if (fields[i].getName().equals(name)
					&& fields[i].getType().equals(typeSig))
				return fields[i];
		return null;
	}

	public MethodInfo findMethod(String name, String typeSig) {
		if ((status & METHODS) == 0)
			loadInfo(METHODS);
		for (int i = 0; i < methods.length; i++)
			if (methods[i].getName().equals(name)
					&& methods[i].getType().equals(typeSig))
				return methods[i];
		return null;
	}

	public MethodInfo[] getMethods() {
		if ((status & METHODS) == 0)
			loadInfo(METHODS);
		return methods;
	}

	public FieldInfo[] getFields() {
		if ((status & FIELDS) == 0)
			loadInfo(FIELDS);
		return fields;
	}

	public InnerClassInfo[] getOuterClasses() {
		if ((status & OUTERCLASSES) == 0)
			loadInfo(OUTERCLASSES);
		return outerClasses;
	}

	public InnerClassInfo[] getInnerClasses() {
		if ((status & INNERCLASSES) == 0)
			loadInfo(INNERCLASSES);
		return innerClasses;
	}

	public InnerClassInfo[] getExtraClasses() {
		if ((status & INNERCLASSES) == 0)
			loadInfo(INNERCLASSES);
		return extraClasses;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public void setName(String newName) {
		name = newName;
		modified = true;
	}

	public void setSuperclass(ClassInfo newSuper) {
		superclass = newSuper;
		modified = true;
	}

	public void setInterfaces(ClassInfo[] newIfaces) {
		interfaces = newIfaces;
		modified = true;
	}

	public void setModifiers(int newModifiers) {
		modifiers = newModifiers;
		modified = true;
	}

	public void setDeprecated(boolean flag) {
		deprecatedFlag = flag;
	}

	public void setMethods(MethodInfo[] mi) {
		methods = mi;
		modified = true;
	}

	public void setFields(FieldInfo[] fi) {
		fields = fi;
		modified = true;
	}

	public void setOuterClasses(InnerClassInfo[] oc) {
		outerClasses = oc;
		modified = true;
	}

	public void setInnerClasses(InnerClassInfo[] ic) {
		innerClasses = ic;
		modified = true;
	}

	public void setExtraClasses(InnerClassInfo[] ec) {
		extraClasses = ec;
		modified = true;
	}

	public void setSourceFile(String newSource) {
		sourceFile = newSource;
		modified = true;
	}

	public boolean superClassOf(ClassInfo son) {
		while (son != this && son != null) {
			son = son.getSuperclass();
		}
		return son == this;
	}

	public boolean implementedBy(ClassInfo clazz) {
		while (clazz != this && clazz != null) {
			ClassInfo[] ifaces = clazz.getInterfaces();
			for (int i = 0; i < ifaces.length; i++) {
				if (implementedBy(ifaces[i]))
					return true;
			}
			clazz = clazz.getSuperclass();
		}
		return clazz == this;
	}

	public String toString() {
		return name;
	}
}
