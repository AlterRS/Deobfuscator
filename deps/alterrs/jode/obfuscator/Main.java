/* Main Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: Main.java.in,v 1.4.2.3 2002/05/28 17:34:14 hoenicke Exp $
 */

package alterrs.jode.obfuscator;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Random;

import alterrs.gnu.getopt.Getopt;
import alterrs.gnu.getopt.LongOpt;
import alterrs.jode.GlobalOptions;

public class Main {
	public static boolean swapOrder = false;

	public static final int OPTION_STRONGOVERLOAD = 0x0001;
	public static final int OPTION_PRESERVESERIAL = 0x0002;
	public static int options = OPTION_PRESERVESERIAL;

	private static final LongOpt[] longOptions = new LongOpt[] {
			new LongOpt("cp", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
			new LongOpt("classpath", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
			new LongOpt("destpath", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
			new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
			new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'V'),
			new LongOpt("verbose", LongOpt.OPTIONAL_ARGUMENT, null, 'v'),
			new LongOpt("debug", LongOpt.OPTIONAL_ARGUMENT, null, 'D'), };

	public static final String[] stripNames = { "unreach", "inner", "lvt",
			"lnt", "source" };
	public static final int STRIP_UNREACH = 0x0001;
	public static final int STRIP_INNERINFO = 0x0002;
	public static final int STRIP_LVT = 0x0004;
	public static final int STRIP_LNT = 0x0008;
	public static final int STRIP_SOURCE = 0x0010;
	public static int stripping = 0;
	/**
	 * A random pool used to destroy order of method identifiers and classes in
	 * packages. <br>
	 * <p/>
	 * A pseudo random is enough, no need to generate the seed securely. This
	 * makes obfuscating errors reproducable.
	 */
	public static Random rand = new Random(123456);

	private static ClassBundle bundle;

	public static void usage() {
		PrintWriter err = GlobalOptions.err;
		err.println("usage: jode.Obfuscator flags* script");
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
		err.println("  -D, --debug=...      "
				+ "use --debug=help for more information.");
	}

	public static ClassBundle getClassBundle() {
		return bundle;
	}

	public static void main(String[] params) {
		if (params.length == 0) {
			usage();
			return;
		}
		String cp = null, dest = null;

		GlobalOptions.err.println(GlobalOptions.copyright);
		bundle = new ClassBundle();
		boolean errorInParams = false;
		Getopt g = new Getopt("jode.obfuscator.Main", params, "hVvc:d:D:",
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
				cp = g.getOptarg();
				break;
			case 'd':
				dest = g.getOptarg();
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
								.println("jode.obfuscator.Main: Argument `"
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
			default:
				errorInParams = true;
				break;
			}
		}
		if (errorInParams)
			return;

		if (g.getOptind() != params.length - 1) {
			GlobalOptions.err.println("You must specify exactly one script.");
			return;
		}

		try {
			String filename = params[g.getOptind()];
			ScriptParser parser = new ScriptParser(
					filename.equals("-") ? new InputStreamReader(System.in)
							: new FileReader(filename));
			parser.parseOptions(bundle);
		} catch (IOException ex) {
			GlobalOptions.err.println("IOException while reading script file.");
			ex.printStackTrace(GlobalOptions.err);
			return;
		} catch (ParseException ex) {
			GlobalOptions.err.println("Syntax error in script file: ");
			GlobalOptions.err.println(ex.getMessage());
			if (GlobalOptions.verboseLevel > 5)
				ex.printStackTrace(GlobalOptions.err);
			return;
		}

		// Command Line overwrites script options:
		if (cp != null)
			bundle.setOption("classpath", Collections.singleton(cp));
		if (dest != null)
			bundle.setOption("dest", Collections.singleton(dest));

		bundle.run();
	}
}
