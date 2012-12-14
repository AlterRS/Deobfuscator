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

package EDU.purdue.cs.bloat.benchmark;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import EDU.purdue.cs.bloat.context.BloatContext;
import EDU.purdue.cs.bloat.context.CachingBloatContext;
import EDU.purdue.cs.bloat.editor.ClassEditor;
import EDU.purdue.cs.bloat.editor.ClassHierarchy;
import EDU.purdue.cs.bloat.editor.EditorContext;
import EDU.purdue.cs.bloat.editor.Instruction;
import EDU.purdue.cs.bloat.editor.MemberRef;
import EDU.purdue.cs.bloat.editor.MethodEditor;
import EDU.purdue.cs.bloat.editor.NameAndType;
import EDU.purdue.cs.bloat.editor.Opcode;
import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.file.ClassFile;
import EDU.purdue.cs.bloat.file.ClassFileLoader;
import EDU.purdue.cs.bloat.reflect.ClassFormatException;
import EDU.purdue.cs.bloat.reflect.ClassInfo;
import EDU.purdue.cs.bloat.reflect.MethodInfo;

public class CounterDecorate implements Opcode {
	private static final String COUNTER_TYPE = "I";

	private static final String COUNTER_RCNAME = "rcCount";

	private static final String COUNTER_AUNAME = "auCount";

	private static final String COUNTER_SUNAME = "suCount";

	private static final String COUNTER_MAIN = "LEDU/purdue/cs/bloat/benchmark/Counter;";

	private static int VERBOSE = 0;

	private static boolean FORCE = false;

	private static boolean CLOSURE = false;

	private static final List SKIP = new ArrayList();

	private static final List ONLY = new ArrayList();

	public static void main(final String[] args) {
		final ClassFileLoader loader = new ClassFileLoader();
		List classes = new ArrayList();
		boolean gotdir = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-v") || args[i].equals("-verbose")) {
				CounterDecorate.VERBOSE++;
			} else if (args[i].equals("-help")) {
				CounterDecorate.usage();
			} else if (args[i].equals("-classpath")) {
				if (++i >= args.length) {
					CounterDecorate.usage();
				}

				final String classpath = args[i];
				loader.setClassPath(classpath);
			} else if (args[i].equals("-skip")) {
				if (++i >= args.length) {
					CounterDecorate.usage();
				}

				final String pkg = args[i].replace('.', '/');
				CounterDecorate.SKIP.add(pkg);
			} else if (args[i].equals("-only")) {
				if (++i >= args.length) {
					CounterDecorate.usage();
				}

				final String pkg = args[i].replace('.', '/');
				CounterDecorate.ONLY.add(pkg);
			} else if (args[i].equals("-closure")) {
				CounterDecorate.CLOSURE = true;
			} else if (args[i].equals("-relax-loading")) {
				ClassHierarchy.RELAX = true;
			} else if (args[i].equals("-f")) {
				CounterDecorate.FORCE = true;
			} else if (args[i].startsWith("-")) {
				CounterDecorate.usage();
			} else if (i == args.length - 1) {
				final File f = new File(args[i]);

				if (f.exists() && !f.isDirectory()) {
					System.err.println("No such directory: " + f.getPath());
					System.exit(2);
				}

				loader.setOutputDir(f);
				gotdir = true;
			} else {
				classes.add(args[i]);
			}
		}

		if (!gotdir) {
			CounterDecorate.usage();
		}

		if (classes.size() == 0) {
			CounterDecorate.usage();
		}

		if (CounterDecorate.VERBOSE > 3) {
			ClassFileLoader.DEBUG = true;
			ClassEditor.DEBUG = true;
		}

		boolean errors = false;

		final Iterator iter = classes.iterator();

		while (iter.hasNext()) {
			final String name = (String) iter.next();

			try {
				loader.loadClass(name);
			} catch (final ClassNotFoundException ex) {
				System.err.println("Couldn't find class: " + ex.getMessage());
				errors = true;
			}
		}

		if (errors) {
			System.exit(1);
		}

		final BloatContext context = new CachingBloatContext(loader, classes,
				CounterDecorate.CLOSURE);

		if (!CounterDecorate.CLOSURE) {
			final Iterator e = classes.iterator();

			while (e.hasNext()) {
				final String name = (String) e.next();
				try {
					final ClassInfo info = loader.loadClass(name);
					CounterDecorate.decorateClass(context, info);
				} catch (final ClassNotFoundException ex) {
					System.err.println("Couldn't find class: "
							+ ex.getMessage());
					System.exit(1);
				}
			}
		} else {
			classes = null;

			final ClassHierarchy hier = context.getHierarchy();

			final Iterator e = hier.classes().iterator();

			while (e.hasNext()) {
				final Type t = (Type) e.next();

				if (t.isObject()) {
					try {
						final ClassInfo info = loader.loadClass(t.className());
						CounterDecorate.decorateClass(context, info);
					} catch (final ClassNotFoundException ex) {
						System.err.println("Couldn't find class: "
								+ ex.getMessage());
						System.exit(1);
					}
				}
			}
		}
	}

	private static void usage() {
		System.err
				.println("Usage: java EDU.purdue.cs.bloat.count.Main "
						+ "\n            [-options] classes output_dir"
						+ "\n"
						+ "\nwhere options include:"
						+ "\n    -help             print out this message"
						+ "\n    -v -verbose       turn on verbose mode "
						+ "(can be given multiple times)"
						+ "\n    -classpath <directories separated by colons>"
						+ "\n                      list directories in which to look for classes"
						+ "\n    -f                decorate files even if up-to-date"
						+ "\n    -closure          recursively decorate referenced classes"
						+ "\n    -relax-loading    don't report errors if a class is not found"
						+ "\n    -skip <class|package.*>"
						+ "\n                      skip the given class or package"
						+ "\n                      (this option can be given more than once)"
						+ "\n    -only <class|package.*>"
						+ "\n                      skip all but the given class or package"
						+ "\n                      (this option can be given more than once)");
		System.exit(0);
	}

	private static void decorateClass(final EditorContext editor,
			final ClassInfo info) {
		final ClassFile classFile = (ClassFile) info;

		if (!CounterDecorate.FORCE) {
			final File source = classFile.file();
			final File target = classFile.outputFile();

			if ((source != null) && (target != null) && source.exists()
					&& target.exists()
					&& (source.lastModified() < target.lastModified())) {

				if (CounterDecorate.VERBOSE > 1) {
					System.out.println(classFile.name() + " is up to date");
				}

				return;
			}
		}

		if (CounterDecorate.VERBOSE > 2) {
			classFile.print(System.out);
		}

		final ClassEditor c = editor.editClass(info);

		boolean skip = false;

		final String name = c.type().className();
		final String qual = c.type().qualifier() + "/*";

		// Edit only classes explicitly mentioned.
		if (CounterDecorate.ONLY.size() > 0) {
			skip = true;

			// Only edit classes we explicitly don't name.
			for (int i = 0; i < CounterDecorate.ONLY.size(); i++) {
				final String pkg = (String) CounterDecorate.ONLY.get(i);

				if (name.equals(pkg) || qual.equals(pkg)) {
					skip = false;
					break;
				}
			}
		}

		// Don't edit classes we explicitly skip.
		if (!skip) {
			for (int i = 0; i < CounterDecorate.SKIP.size(); i++) {
				final String pkg = (String) CounterDecorate.SKIP.get(i);

				if (name.equals(pkg) || qual.equals(pkg)) {
					skip = true;
					break;
				}
			}
		}

		if (skip) {
			if (CounterDecorate.VERBOSE > 0) {
				System.out.println("Skipping " + c.type().className());
			}

			editor.release(info);
			return;
		}

		if (CounterDecorate.VERBOSE > 0) {
			System.out.println("Decorating class " + c.type().className());
		}

		if (CounterDecorate.VERBOSE > 2) {
			((ClassFile) info).print(System.out);
		}

		final MethodInfo[] methods = c.methods();

		for (int j = 0; j < methods.length; j++) {
			MethodEditor m;

			try {
				m = editor.editMethod(methods[j]);
			} catch (final ClassFormatException ex) {
				System.err.println(ex.getMessage());
				continue;
			}

			CounterDecorate.transform(m);
			editor.commit(methods[j]);
		}

		editor.commit(info);
	}

	private static void transform(final MethodEditor method) {
		if (CounterDecorate.VERBOSE > 1) {
			System.out.println("Decorating method " + method);
		}

		final MemberRef rcfield = new MemberRef(Type
				.getType(CounterDecorate.COUNTER_MAIN), new NameAndType(
				CounterDecorate.COUNTER_RCNAME, Type
						.getType(CounterDecorate.COUNTER_TYPE)));
		final MemberRef aufield = new MemberRef(Type
				.getType(CounterDecorate.COUNTER_MAIN), new NameAndType(
				CounterDecorate.COUNTER_AUNAME, Type
						.getType(CounterDecorate.COUNTER_TYPE)));
		final MemberRef sufield = new MemberRef(Type
				.getType(CounterDecorate.COUNTER_MAIN), new NameAndType(
				CounterDecorate.COUNTER_SUNAME, Type
						.getType(CounterDecorate.COUNTER_TYPE)));

		final ListIterator iter = method.code().listIterator();

		while (iter.hasNext()) {
			final Object ce = iter.next();

			if (CounterDecorate.VERBOSE > 2) {
				System.out.println("Examining " + ce);
			}

			if (ce instanceof Instruction) {
				final Instruction inst = (Instruction) ce;

				if (inst.opcodeClass() == Opcode.opcx_aupdate) {
					iter.remove();
					iter.add(new Instruction(Opcode.opcx_getstatic, aufield));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_ldc, new Integer(1)));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_iadd));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_putstatic, aufield));
					iter.next();
				}
				if (inst.opcodeClass() == Opcode.opcx_supdate) {
					iter.remove();
					iter.add(new Instruction(Opcode.opcx_getstatic, sufield));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_ldc, new Integer(1)));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_iadd));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_putstatic, sufield));
					iter.next();
				} else if (inst.opcodeClass() == Opcode.opcx_rc) {
					iter.remove();
					iter.add(new Instruction(Opcode.opcx_getstatic, rcfield));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_ldc, new Integer(1)));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_iadd));
					iter.next();
					iter.add(new Instruction(Opcode.opcx_putstatic, rcfield));
					iter.next();
				}
			}
		}
	}
}
