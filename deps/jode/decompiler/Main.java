/* Main Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: Main.java,v 4.1.2.7 2003/06/11 09:07:07 hoenicke Exp $
 */

package jode.decompiler;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import jode.GlobalOptions;
import jode.bytecode.ClassInfo;


public class Main extends Options {
	private static int successCount = 0;
	private static Vector failedClasses;

	private static final int OPTION_START = 0x10000;
	private static final int OPTION_END = 0x20000;

	private static final LongOpt[] longOptions = new LongOpt[] {
			new LongOpt("cp", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
			new LongOpt("classpath", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
			new LongOpt("dest", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
			new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
			new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'V'),
			new LongOpt("verbose", LongOpt.OPTIONAL_ARGUMENT, null, 'v'),
			new LongOpt("debug", LongOpt.OPTIONAL_ARGUMENT, null, 'D'),
			new LongOpt("import", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
			new LongOpt("style", LongOpt.REQUIRED_ARGUMENT, null, 's'),
			new LongOpt("lvt", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 0),
			new LongOpt("inner", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 1),
			new LongOpt("anonymous", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 2),
			new LongOpt("push", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 3),
			new LongOpt("pretty", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 4),
			new LongOpt("decrypt", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 5),
			new LongOpt("onetime", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 6),
			new LongOpt("immediate", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 7),
			new LongOpt("verify", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 8),
			new LongOpt("contrafo", LongOpt.OPTIONAL_ARGUMENT, null,
					OPTION_START + 9) };

	public static void usage() {
		PrintWriter err = GlobalOptions.err;
		err.println("Version: " + GlobalOptions.version);
		err.println("Usage: java jode.decompiler.Main [OPTION]* {CLASS|JAR}*");
		err.println("Give a fully qualified CLASS name, e.g. jode.decompiler.Main, if you want to");
		err.println("decompile a single class, or a JAR file containing many classes.");
		err.println("OPTION is any of these:");
		err.println("  -h, --help           " + "show this information.");
		err.println("  -V, --version        "
				+ "output version information and exit.");
		err.println("  -v, --verbose        "
				+ "be verbose (multiple times means more verbose).");
		err.println("  -c, --classpath <path> "
				+ "search for classes in specified classpath.");
		err.println("                       "
				+ "The directories should be separated by ','.");
		err.println("  -d, --dest <dir>     "
				+ "write decompiled files to disk into directory destdir.");
		err.println("  -s, --style {sun|gnu}  " + "specify indentation style");
		err.println("  -i, --import <pkglimit>,<clslimit>");
		err.println("                       "
				+ "import classes used more than clslimit times");
		err.println("                       "
				+ "and packages with more then pkglimit used classes.");
		err.println("                       "
				+ "Limit 0 means never import. Default is 0,1.");
		err.println("  -D, --debug=...      "
				+ "use --debug=help for more information.");

		err.println("NOTE: The following options can be turned on or off with `yes' or `no'.");
		err.println("The options tagged with (default) are normally on.  Omitting the yes/no");
		err.println("argument will toggle the option, e.g. --verify is equivalent to --verify=no.");
		err.println("      --inner          "
				+ "decompile inner classes (default).");
		err.println("      --anonymous      "
				+ "decompile anonymous classes (default).");
		err.println("      --contrafo       "
				+ "transform constructors of inner classes (default).");
		err.println("      --lvt            "
				+ "use the local variable table (default).");
		err.println("      --pretty         "
				+ "use `pretty' names for local variables (default).");
		err.println("      --push           "
				+ "allow PUSH instructions in output.");
		err.println("      --decrypt        "
				+ "decrypt encrypted strings (default).");
		err.println("      --onetime        "
				+ "remove locals, that are used only one time.");
		err.println("      --immediate      "
				+ "output source immediately (may produce buggy code).");
		err.println("      --verify         "
				+ "verify code before decompiling it (default).");
	}

	public static boolean handleOption(int option, int longind, String arg) {
		if (arg == null)
			options ^= 1 << option;
		else if ("yes".startsWith(arg) || arg.equals("on"))
			options |= 1 << option;
		else if ("no".startsWith(arg) || arg.equals("off"))
			options &= ~(1 << option);
		else {
			GlobalOptions.err.println("jode.decompiler.Main: option --"
					+ longOptions[longind].getName()
					+ " takes one of `yes', `no', `on', `off' as parameter");
			return false;
		}
		return true;
	}

	public static void decompileClass(String className,
			ZipOutputStream destZip, String destDir, TabbedPrintWriter writer,
			ImportHandler imports) {
		try {
			ClassInfo clazz;
			try {
				clazz = ClassInfo.forName(className);
			} catch (IllegalArgumentException ex) {
				GlobalOptions.err.println("`" + className
						+ "' is not a class name");
				return;
			}
			if (skipClass(clazz))
				return;

			String filename = className.replace('.', File.separatorChar)
					+ ".java";
			if (destZip != null) {
				writer.flush();
				destZip.putNextEntry(new ZipEntry(filename));
			} else if (destDir != null) {
				File file = new File(destDir, filename);
				File directory = new File(file.getParent());
				if (!directory.exists() && !directory.mkdirs()) {
					GlobalOptions.err.println("Could not create directory "
							+ directory.getPath() + ", check permissions.");
				}
				writer = new TabbedPrintWriter(new BufferedOutputStream(
						new FileOutputStream(file)), imports, false);
			}

			GlobalOptions.err.println(className);

			ClassAnalyzer clazzAna = new ClassAnalyzer(clazz, imports);
			clazzAna.dumpJavaFile(writer);

			if (destZip != null) {
				writer.flush();
				destZip.closeEntry();
			} else if (destDir != null)
				writer.close();
			/* Now is a good time to clean up */
			System.gc();
			successCount++;
		} catch (IOException ex) {
			failedClasses.addElement(className);
			GlobalOptions.err.println("Can't write source of " + className
					+ ".");
			GlobalOptions.err.println("Check the permissions.");
			ex.printStackTrace(GlobalOptions.err);
		} catch (Throwable t) {
			failedClasses.addElement(className);
			GlobalOptions.err.println("Failed to decompile " + className + ".");
			t.printStackTrace(GlobalOptions.err);
		}
	}

	public static void main(String[] params) {
		try {
			decompile(params);
		} catch (ExceptionInInitializerError ex) {
			ex.getException().printStackTrace();
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
		printSummary();
		/*
		 * When AWT applications are compiled with insufficient classpath the
		 * type guessing by reflection code can generate an awt thread that will
		 * prevent normal exiting.
		 */
		System.exit(0);
	}

	private static void printSummary() {
		GlobalOptions.err.println();
		if (failedClasses.size() > 0) {
			GlobalOptions.err.println("Failed to decompile these classes:");
			Enumeration enum_ = failedClasses.elements();
			while (enum_.hasMoreElements()) {
				GlobalOptions.err.println("\t" + enum_.nextElement());
			}
			GlobalOptions.err.println("Failed to decompile "
					+ failedClasses.size() + " classes.");
		}
		GlobalOptions.err.println("Decompiled " + successCount + " classes.");
	}

	public static void decompile(String[] params) {
		if (params.length == 0) {
			usage();
			return;
		}

		failedClasses = new Vector();

		String classPath = System.getProperty("java.class.path").replace(
				File.pathSeparatorChar, Decompiler.altPathSeparatorChar);
		String bootClassPath = System.getProperty("sun.boot.class.path");
		if (bootClassPath != null)
			classPath += Decompiler.altPathSeparatorChar
					+ bootClassPath.replace(File.pathSeparatorChar,
							Decompiler.altPathSeparatorChar);

		String destDir = null;

		int importPackageLimit = ImportHandler.DEFAULT_PACKAGE_LIMIT;
		int importClassLimit = ImportHandler.DEFAULT_CLASS_LIMIT;
		;

		GlobalOptions.err.println(GlobalOptions.copyright);

		boolean errorInParams = false;
		Getopt g = new Getopt("jode.decompiler.Main", params, "hVvc:d:D:i:s:",
				longOptions, true);
		for (int opt = g.getopt(); opt != -1; opt = g.getopt()) {
			switch (opt) {
			case 0:
				break;
			case 'h':
				usage();
				errorInParams = true;
				break;
			case 'V':
				GlobalOptions.err.println(GlobalOptions.version);
				break;
			case 'c':
				classPath = g.getOptarg();
				break;
			case 'd':
				destDir = g.getOptarg();
				break;
			case 'v': {
				String arg = g.getOptarg();
				if (arg == null)
					GlobalOptions.verboseLevel++;
				else {
					try {
						GlobalOptions.verboseLevel = Integer.parseInt(arg);
					} catch (NumberFormatException ex) {
						GlobalOptions.err
								.println("jode.decompiler.Main: Argument `"
										+ arg
										+ "' to --verbose must be numeric:");
						errorInParams = true;
					}
				}
				break;
			}
			case 'D': {
				String arg = g.getOptarg();
				if (arg == null)
					arg = "help";
				errorInParams |= !GlobalOptions.setDebugging(arg);
				break;
			}
			case 's': {
				String arg = g.getOptarg();
				if ("sun".startsWith(arg))
					outputStyle = SUN_STYLE;
				else if ("gnu".startsWith(arg))
					outputStyle = GNU_STYLE;
				else if ("pascal".startsWith(arg))
					outputStyle = Options.PASCAL_STYLE;
				else {
					GlobalOptions.err
							.println("jode.decompiler.Main: Unknown style `"
									+ arg + "'.");
					errorInParams = true;
				}
				break;
			}
			case 'i': {
				String arg = g.getOptarg();
				int comma = arg.indexOf(',');
				try {
					int packLimit = Integer.parseInt(arg.substring(0, comma));
					if (packLimit == 0)
						packLimit = Integer.MAX_VALUE;
					if (packLimit < 0)
						throw new IllegalArgumentException();
					int clazzLimit = Integer.parseInt(arg.substring(comma + 1));
					if (clazzLimit == 0)
						clazzLimit = Integer.MAX_VALUE;
					if (clazzLimit < 0)
						throw new IllegalArgumentException();

					importPackageLimit = packLimit;
					importClassLimit = clazzLimit;
				} catch (RuntimeException ex) {
					GlobalOptions.err
							.println("jode.decompiler.Main: Invalid argument for -i option.");
					errorInParams = true;
				}
				break;
			}
			default:
				if (opt >= OPTION_START && opt <= OPTION_END) {
					errorInParams |= !handleOption(opt - OPTION_START,
							g.getLongind(), g.getOptarg());
				} else
					errorInParams = true;
				break;
			}
		}
		if (errorInParams)
			return;
		ClassInfo.setClassPath(classPath);
		ImportHandler imports = new ImportHandler(importPackageLimit,
				importClassLimit);

		ZipOutputStream destZip = null;
		TabbedPrintWriter writer = null;
		if (destDir == null)
			writer = new TabbedPrintWriter(System.out, imports);
		else if (destDir.toLowerCase().endsWith(".zip")
				|| destDir.toLowerCase().endsWith(".jar")) {
			try {
				destZip = new ZipOutputStream(new FileOutputStream(destDir));
			} catch (IOException ex) {
				GlobalOptions.err.println("Can't open zip file " + destDir);
				ex.printStackTrace(GlobalOptions.err);
				return;
			}
			writer = new TabbedPrintWriter(new BufferedOutputStream(destZip),
					imports, false);
		}
		for (int i = g.getOptind(); i < params.length; i++) {
			try {
				if ((params[i].endsWith(".jar") || params[i].endsWith(".zip"))
						&& new File(params[i]).isFile()) {
					/*
					 * The user obviously wants to decompile a jar/zip file.
					 * Lets do him a pleasure and allow this.
					 */
					ClassInfo.setClassPath(params[i]
							+ Decompiler.altPathSeparatorChar + classPath);
					Enumeration enum_ = new ZipFile(params[i]).entries();
					while (enum_.hasMoreElements()) {
						String entry = ((ZipEntry) enum_.nextElement())
								.getName();
						if (entry.endsWith(".class")) {
							entry = entry.substring(0, entry.length() - 6)
									.replace('/', '.');
							decompileClass(entry, destZip, destDir, writer,
									imports);
						}
					}
					ClassInfo.setClassPath(classPath);
				} else
					decompileClass(params[i], destZip, destDir, writer, imports);
			} catch (IOException ex) {
				GlobalOptions.err.println("Can't read zip file " + params[i]
						+ ".");
				ex.printStackTrace(GlobalOptions.err);
			}
		}
		if (destZip != null) {
			try {
				destZip.close();
			} catch (IOException ex) {
				GlobalOptions.err.println("Can't close Zipfile");
				ex.printStackTrace(GlobalOptions.err);
			}
		}
	}
}
