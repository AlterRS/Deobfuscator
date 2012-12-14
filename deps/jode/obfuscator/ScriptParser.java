/* ScriptParser Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: ScriptParser.java.in,v 1.4.2.1 2002/05/28 17:34:14 hoenicke Exp $
 */

package jode.obfuscator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;

public class ScriptParser {
	static int NO_TOKEN = -2;
	static int EOF_TOKEN = -1;
	static int STRING_TOKEN = 0;
	static int NEW_TOKEN = 1;
	static int EQUALS_TOKEN = 2;
	static int COMMA_TOKEN = 3;
	static int OPENBRACE_TOKEN = 4;
	static int CLOSEBRACE_TOKEN = 5;
	static int IDENTIFIER_TOKEN = 6;
	static int NUMBER_TOKEN = 7;
	Scanner scanner;

	class Scanner {
		BufferedReader input;
		String value;
		String line;
		int column;
		int linenr;
		int pushback = NO_TOKEN;

		public Scanner(Reader i) {
			input = new BufferedReader(i);
		}

		public void readString() throws ParseException {
			StringBuffer val = new StringBuffer();
			while (column < line.length()) {
				char c = line.charAt(column++);
				if (c == '"') {
					value = val.toString();
					return;
				}
				if (c == '\\') {
					c = line.charAt(column++);
					switch (c) {
					case 'n':
						val.append('\n');
						break;
					case 't':
						val.append('\t');
						break;
					case 'r':
						val.append('\r');
						break;
					case 'u':
						if (column + 4 <= line.length()) {
							try {
								char uni = (char) Integer.parseInt(
										line.substring(column, column + 4), 16);
								column += 4;
								val.append(uni);
							} catch (NumberFormatException ex) {
								throw new ParseException(linenr,
										"Invalid unicode escape character");
							}
						} else
							throw new ParseException(linenr,
									"Invalid unicode escape character");
						break;
					default:
						val.append(c);
					}
				} else
					val.append(c);
			}
			throw new ParseException(linenr, "String spans over multiple lines");
		}

		public void readIdentifier() {
			int start = column - 1;
			while (column < line.length()
					&& Character.isUnicodeIdentifierPart(line.charAt(column)))
				column++;
			value = line.substring(start, column);
		}

		public void readNumber() {
			boolean hex = false;
			int start = column - 1;
			/* special case for hex numbers */
			if (line.charAt(start) == '0' && line.charAt(column) == 'x') {
				column++;
				hex = true;
			}
			while (column < line.length()) {
				char c = line.charAt(column);
				if (!Character.isDigit(c)) {
					if (!hex)
						break;
					if ((c < 'A' || c > 'F') && (c < 'a' || c > 'f'))
						break;
				}
				column++;
			}
			value = line.substring(start, column);
		}

		public void pushbackToken(int token) {
			if (pushback != NO_TOKEN)
				throw new IllegalStateException("Can only handle one pushback");
			pushback = token;
		}

		public int getToken() throws ParseException, IOException {
			if (pushback != NO_TOKEN) {
				int result = pushback;
				pushback = NO_TOKEN;
				return result;
			}
			value = null;
			while (true) {
				if (line == null) {
					line = input.readLine();
					if (line == null)
						return EOF_TOKEN;
					linenr++;
					column = 0;
				}
				while (column < line.length()) {
					char c = line.charAt(column++);
					if (Character.isWhitespace(c))
						continue;
					if (c == '#')
						// this is a comment, skip this line
						break;
					if (c == '=')
						return EQUALS_TOKEN;
					if (c == ',')
						return COMMA_TOKEN;
					if (c == '{')
						return OPENBRACE_TOKEN;
					if (c == '}')
						return CLOSEBRACE_TOKEN;
					if (c == '"') {
						readString();
						return STRING_TOKEN;
					}
					if (Character.isDigit(c) || c == '+' || c == '-') {
						readNumber();
						return NUMBER_TOKEN;
					}
					if (Character.isUnicodeIdentifierStart(c)) {
						readIdentifier();
						if (value.equals("new"))
							return NEW_TOKEN;
						return IDENTIFIER_TOKEN;
					}
					throw new ParseException(linenr, "Illegal character `" + c
							+ "'");
				}
				line = null;
			}
		}

		public String getValue() {
			return value;
		}

		public int getLineNr() {
			return linenr;
		}
	}

	public ScriptParser(Reader reader) {
		this.scanner = new Scanner(reader);
	}

	public Object parseClass() throws ParseException, IOException {
		int linenr = scanner.getLineNr();
		int token = scanner.getToken();
		if (token != IDENTIFIER_TOKEN)
			throw new ParseException(linenr, "Class name expected");
		Object instance;
		try {
			Class clazz = Class.forName("jode.obfuscator.modules."
					+ scanner.getValue());
			instance = clazz.newInstance();
		} catch (ClassNotFoundException ex) {
			throw new ParseException(scanner.getLineNr(), "Class `"
					+ scanner.getValue() + "' not found");
		} catch (Exception ex) {
			throw new ParseException(scanner.getLineNr(), "Class `"
					+ scanner.getValue() + "' not valid: " + ex.getMessage());
		}

		token = scanner.getToken();
		if (token == OPENBRACE_TOKEN) {
			if (!(instance instanceof OptionHandler))
				throw new ParseException(scanner.getLineNr(), "Class `"
						+ instance.getClass().getName()
						+ "' doesn't handle options.");
			parseOptions((OptionHandler) instance);
			if (scanner.getToken() != CLOSEBRACE_TOKEN)
				throw new ParseException(scanner.getLineNr(), "`}' expected");
		} else
			scanner.pushbackToken(token);
		return instance;
	}

	public void parseOptions(OptionHandler optionHandler)
			throws ParseException, IOException {
		int token = scanner.getToken();
		while (true) {
			if (token == EOF_TOKEN || token == CLOSEBRACE_TOKEN) {
				scanner.pushbackToken(token);
				return;
			}
			if (token != IDENTIFIER_TOKEN)
				throw new ParseException(scanner.getLineNr(),
						"identifier expected");
			String ident = scanner.getValue();
			if (scanner.getToken() != EQUALS_TOKEN)
				throw new ParseException(scanner.getLineNr(),
						"equal sign expected");

			int linenr = scanner.getLineNr();
			Collection values = new LinkedList();
			do {
				token = scanner.getToken();
				if (token == NEW_TOKEN) {
					values.add(parseClass());
				} else if (token == STRING_TOKEN) {
					values.add(scanner.getValue());
				} else if (token == NUMBER_TOKEN) {
					values.add(new Integer(scanner.getValue()));
				}
				token = scanner.getToken();
			} while (token == COMMA_TOKEN);
			try {
				optionHandler.setOption(ident, values);
			} catch (IllegalArgumentException ex) {
				throw new ParseException(linenr, optionHandler.getClass()
						.getName() + ": " + ex.getMessage());
			} catch (RuntimeException ex) {
				throw new ParseException(linenr, optionHandler.getClass()
						.getName()
						+ ": Illegal value: "
						+ ex.getClass().getName() + ": " + ex.getMessage());
			}
		}
	}
}
