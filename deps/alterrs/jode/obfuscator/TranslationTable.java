/* TranslationTable Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: TranslationTable.java.in,v 1.1.2.2 2002/05/28 17:34:14 hoenicke Exp $
 */

package alterrs.jode.obfuscator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

///#ifndef JDK12
///import java.util.Comparator;
///#endif

public class TranslationTable extends TreeMap {

	// /#ifndef JDK12
	// / public TranslationTable() {
	// / super(createStringComparator());
	// / }
	// /
	// / private static Comparator createStringComparator() {
	// / return new Comparator() {
	// / public int compare(Object o1, Object o2) {
	// / return ((String) o1).compareTo((String) o2);
	// / }
	// / };
	// / }
	// /#endif

	public void load(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		String line;
		while ((line = reader.readLine()) != null) {
			if (line.charAt(0) == '#')
				continue;
			int delim = line.indexOf('=');
			String key = line.substring(0, delim);
			String value = line.substring(delim + 1);
			put(key, value);
		}
	}

	public void store(OutputStream out) throws IOException {
		PrintWriter writer = new PrintWriter(out);
		for (Iterator i = entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			writer.println(e.getKey() + "=" + e.getValue());
		}
		writer.flush();
	}
}
