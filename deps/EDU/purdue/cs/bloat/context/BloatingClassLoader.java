/**
 * All files in the distribution of BLOAT (Bytecode Level Optimization and
 * Analysis tool for Java(tm)) are Copyright 1997-2001 by the Purdue
 * Research Foundation of Purdue University.  All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package EDU.purdue.cs.bloat.context;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import EDU.purdue.cs.bloat.editor.ClassEditor;
import EDU.purdue.cs.bloat.editor.EditorContext;
import EDU.purdue.cs.bloat.file.ClassFile;
import EDU.purdue.cs.bloat.reflect.ClassInfo;
import EDU.purdue.cs.bloat.reflect.ClassInfoLoader;
import EDU.purdue.cs.bloat.util.Assert;

/**
 * <code>BloatingClassLoader</code> is a Java class loader that BLOATs a class
 * before it is loader into a Java Virtual Machine. It loads its classes from a
 * set of {@link URL}s.
 */
public abstract class BloatingClassLoader extends URLClassLoader {

	/**
	 * A ClassInfoLoader that loads classes from the same locations as this
	 * class loader.
	 */
	ClassInfoLoader loader = new BloatingClassInfoLoader();

	/** The context that is used to edit classes, etc. */
	private final EditorContext context = new PersistentBloatContext(loader,
			false);

	/**
	 * Maps ClassInfos to their committed bytes (as a ByteArrayOutputStream)
	 */
	private final Map classBytes = new HashMap();

	// //////////////////// Constructors /////////////////////////

	/**
	 * Creates a new <code>BloatingClassLoader</code> that loads its classes
	 * from a given set of URLs.
	 */
	public BloatingClassLoader(final URL[] urls) {
		super(urls);
	}

	/**
	 * Creates a new <code>BloatingClassLoader</code> that loads its classes
	 * from a given set of URLs. Before attempting to load a class, this
	 * <code>BloatingClassLoader</code> will delegate to its parent class
	 * loader.
	 */
	public BloatingClassLoader(final URL[] urls, final ClassLoader parent) {
		super(urls);
	}

	/**
	 * Before the <code>Class</code> is created, invoke {@link
	 * #bloat(ClassEditor)}.
	 */
	protected Class findClass(final String name) throws ClassNotFoundException {

		final ClassInfo info = this.loader.loadClass(name);
		final ClassEditor ce = this.context.editClass(info);

		this.bloat(ce);

		ce.commit();
		final ByteArrayOutputStream baos = (ByteArrayOutputStream) this.classBytes
				.get(info);
		Assert.isNotNull(baos, "No bytes for " + name);

		final byte[] bytes = baos.toByteArray();
		return super.defineClass(name, bytes, 0, bytes.length);
	}

	/**
	 * Returns a <code>ClassInfoLoader</code> that loads classes from the same
	 * place as this <code>ClassLoader</code>.
	 */
	public ClassInfoLoader getClassInfoLoader() {
		return this.loader;
	}

	/**
	 * This method is invoked as a class is being loaded.
	 */
	protected abstract void bloat(ClassEditor ce);

	/**
	 * This inner class is a ClassInfoLoader that loads classes from the same
	 * locations as the outer BloatClassLoader. The primary reason that we have
	 * this class is because the loadClass method of ClassInfoLoader has a
	 * different signature from ClassLoader. Hence, a ClassLoader cannot be a
	 * ClassInfoLoader.
	 */
	class BloatingClassInfoLoader implements ClassInfoLoader {

		public ClassInfo loadClass(final String name)
				throws ClassNotFoundException {

			final String classFileName = name.replace('.', '/') + ".class";
			final InputStream is = BloatingClassLoader.this
					.getResourceAsStream(classFileName);
			if (is == null) {
				throw new ClassNotFoundException("Could not find class " + name);
			}

			final DataInputStream dis = new DataInputStream(is);
			return new ClassFile(null, this, dis);
		}

		public ClassInfo newClass(final int modifiers, final int classIndex,
				final int superClassIndex, final int[] interfaceIndexes,
				final java.util.List constants) {

			return new ClassFile(modifiers, classIndex, superClassIndex,
					interfaceIndexes, constants, this);
		}

		public OutputStream outputStreamFor(final ClassInfo info)
				throws IOException {

			// Maintain a mapping between ClassInfos and their committed bytes
			final OutputStream os = new ByteArrayOutputStream();
			classBytes.put(info, os);
			return (os);
		}
	}
}
