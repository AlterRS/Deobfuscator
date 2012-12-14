/* ModifierMatcher Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: ModifierMatcher.java.in,v 1.2.2.2 2002/05/28 17:34:17 hoenicke Exp $
 */

package jode.obfuscator.modules;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;

import jode.obfuscator.ClassIdentifier;
import jode.obfuscator.FieldIdentifier;
import jode.obfuscator.Identifier;
import jode.obfuscator.IdentifierMatcher;
import jode.obfuscator.MethodIdentifier;
import jode.obfuscator.OptionHandler;


public class ModifierMatcher implements IdentifierMatcher, OptionHandler,
		Cloneable {
	static final int PUBLIC = Modifier.PUBLIC;
	static final int PROTECTED = Modifier.PROTECTED;
	static final int PRIVATE = Modifier.PRIVATE;

	int[] andMasks;
	int[] xorMasks;

	public static ModifierMatcher denyAll = new ModifierMatcher(new int[0],
			new int[0]);
	public static ModifierMatcher allowAll = new ModifierMatcher(0, 0);

	/*
	 * Invariants: \forall i: ~andMasks[i] & xorMasks[i] == 0 \forall i: entries
	 * wo. i does not imply entry nr. i
	 */

	public ModifierMatcher() {
		this(0, 0);
	}

	private ModifierMatcher(int[] ands, int[] xors) {
		andMasks = ands;
		xorMasks = xors;
	}

	public ModifierMatcher(int and, int xor) {
		andMasks = new int[] { and };
		xorMasks = new int[] { xor };
	}

	public void setOption(String option, Collection values) {
		ModifierMatcher mm = this;
		if (option.equals("access")) {
			for (Iterator i = values.iterator(); i.hasNext();) {
				String str = (String) i.next();
				boolean less = str.charAt(0) == '<';
				boolean greater = str.charAt(0) == '>';
				if (less || greater)
					str = str.substring(1);
				str = str.toUpperCase();
				if (less) {
					int access = str.equals("PROTECTED") ? PUBLIC : str
							.equals("PACKAGE") ? PROTECTED : str
							.equals("PRIVATE") ? 0 : -1;
					if (access == -1)
						throw new IllegalArgumentException(
								"Unknown access modifier " + str);

					mm = mm.forbidAccess(access, true);
				} else {
					int access = str.equals("PUBLIC") ? PUBLIC : str
							.equals("PROTECTED") ? PROTECTED : str
							.equals("PACKAGE") ? 0
							: str.equals("PRIVATE") ? PRIVATE : -1;
					if (access == -1)
						throw new IllegalArgumentException("Unknown access "
								+ str);
					mm = mm.forceAccess(access, greater);
				}
			}
		} else if (option.equals("modifier")) {
			for (Iterator i = values.iterator(); i.hasNext();) {
				String str = (String) i.next();
				boolean negate = str.charAt(0) == '!';
				if (negate)
					str = str.substring(1);
				str = str.toUpperCase();

				int modif = str.equals("ABSTRACT") ? Modifier.ABSTRACT
						: str.equals("FINAL") ? Modifier.FINAL
								: str.equals("INTERFACE") ? Modifier.INTERFACE
										: str.equals("NATIVE") ? Modifier.NATIVE
												: str.equals("STATIC") ? Modifier.STATIC
														// /#ifdef JDK12
														: str.equals("STRICT") ? Modifier.STRICT
																// /#endif
																: str.equals("SYNCHRONIZED") ? Modifier.SYNCHRONIZED
																		: str.equals("TRANSIENT") ? Modifier.TRANSIENT
																				: str.equals("VOLATILE") ? Modifier.VOLATILE
																						: -1;
				if (modif == -1)
					throw new IllegalArgumentException("Unknown modifier "
							+ str);
				if (negate)
					mm = mm.forbidModifier(modif);
				else
					mm = mm.forceModifier(modif);
			}
		} else
			throw new IllegalArgumentException("Invalid option `" + option
					+ "'.");
		andMasks = mm.andMasks;
		xorMasks = mm.xorMasks;
	}

	private static boolean implies(int and1, int xor1, int and2, int xor2) {
		return ((and1 & and2) == and2 && (xor1 & and2) == xor2);
	}

	private boolean implies(int and, int xor) {
		for (int i = 0; i < andMasks.length; i++) {
			if (!implies(andMasks[i], xorMasks[i], and, xor))
				return false;
		}
		return true;
	}

	private boolean impliedBy(int and, int xor) {
		for (int i = 0; i < andMasks.length; i++) {
			if (implies(and, xor, andMasks[i], xorMasks[i]))
				return true;
		}
		return false;
	}

	private boolean implies(ModifierMatcher mm) {
		for (int i = 0; i < andMasks.length; i++) {
			if (!mm.impliedBy(andMasks[i], xorMasks[i]))
				return false;
		}
		return true;
	}

	public ModifierMatcher and(ModifierMatcher mm) {
		if (implies(mm))
			return this;
		if (mm.implies(this))
			return mm;

		ModifierMatcher result = denyAll;
		for (int i = 0; i < andMasks.length; i++)
			result = result.or(mm.and(andMasks[i], xorMasks[i]));
		return result;
	}

	public ModifierMatcher or(ModifierMatcher mm) {
		if (implies(mm))
			return mm;
		if (mm.implies(this))
			return this;
		ModifierMatcher result = this;
		for (int i = 0; i < mm.andMasks.length; i++)
			result = result.or(mm.andMasks[i], mm.xorMasks[i]);
		return result;
	}

	private ModifierMatcher and(int and, int xor) {
		if (this.implies(and, xor))
			return this;
		int newCount = 0;
		next_i: for (int i = 0; i < andMasks.length; i++) {
			if (implies(and, xor, andMasks[i], xorMasks[i]))
				continue next_i;

			for (int j = 0; j < andMasks.length; j++) {
				if (j != i
						&& implies(and | andMasks[j], xor | xorMasks[j],
								andMasks[i], xorMasks[i]))
					continue next_i;
			}
			newCount++;
		}
		if (newCount == 0)
			return new ModifierMatcher(and, xor);
		int[] ands = new int[newCount];
		int[] xors = new int[newCount];
		int index = 0;
		next_i: for (int i = 0; i < newCount; i++) {
			if (implies(and, xor, andMasks[i], xorMasks[i]))
				continue next_i;

			for (int j = 0; j < andMasks.length; j++) {
				if (j != i
						&& implies(and | andMasks[j], xor | xorMasks[j],
								andMasks[i], xorMasks[i]))
					continue next_i;
			}

			ands[index] = andMasks[i] | and;
			xors[index] = xorMasks[i] | xor;
			index++;
		}
		return new ModifierMatcher(ands, xors);
	}

	private ModifierMatcher or(int and, int xor) {
		int matchIndex = -1;
		if (this == denyAll)
			return new ModifierMatcher(and, xor);
		for (int i = 0; i < andMasks.length; i++) {
			if (implies(and, xor, andMasks[i], xorMasks[i]))
				return this;
			if (implies(andMasks[i], xorMasks[i], and, xor)) {
				matchIndex = i;
				break;
			}
		}
		int[] ands, xors;
		if (matchIndex == -1) {
			matchIndex = andMasks.length;
			ands = new int[matchIndex + 1];
			xors = new int[matchIndex + 1];
			System.arraycopy(andMasks, 0, ands, 0, matchIndex);
			System.arraycopy(xorMasks, 0, xors, 0, matchIndex);
		} else {
			ands = (int[]) andMasks.clone();
			xors = (int[]) xorMasks.clone();
		}
		ands[matchIndex] = and;
		xors[matchIndex] = xor;
		return new ModifierMatcher(ands, xors);
	}

	/**
	 * Creates a new ModifierMatcher, that matches only modifiers, we also match
	 * and also forces the access rights, to be accessModif (or less
	 * restrictive).
	 * 
	 * @param accessModif
	 *            the access modifier. Use 0 for package access, or
	 *            Modifier.PRIVATE/PROTECTED/PUBLIC.
	 * @param andAbove
	 *            allow to be less restrictive.
	 * @return a new modifier matcher that will also use the given accesses.
	 */
	public ModifierMatcher forceAccess(int accessModif, boolean andAbove) {
		if (andAbove) {
			if (accessModif == Modifier.PRIVATE)
				return this;
			if (accessModif == 0)
				return this.and(Modifier.PRIVATE, 0);

			ModifierMatcher result = this.and(Modifier.PUBLIC, PUBLIC);
			if (accessModif == Modifier.PROTECTED)
				return result.or(this.and(Modifier.PROTECTED,
						Modifier.PROTECTED));
			if (accessModif == Modifier.PUBLIC)
				return result;
			throw new IllegalArgumentException("" + accessModif);
		} else {
			if (accessModif == 0)
				return this.and(Modifier.PRIVATE | Modifier.PROTECTED
						| Modifier.PUBLIC, 0);
			else
				return this.and(accessModif, accessModif);
		}
	}

	public ModifierMatcher forbidAccess(int accessModif, boolean andAbove) {
		if (andAbove) {
			if (accessModif == Modifier.PRIVATE)
				// This forbids all access.
				return denyAll;
			if (accessModif == 0)
				return this.and(Modifier.PRIVATE, Modifier.PRIVATE);
			if (accessModif == Modifier.PROTECTED)
				return this.and(Modifier.PROTECTED | Modifier.PUBLIC, 0);
			if (accessModif == Modifier.PUBLIC)
				return this.and(Modifier.PUBLIC, 0);
			throw new IllegalArgumentException("" + accessModif);
		} else {
			if (accessModif == 0) {
				return this.and(Modifier.PRIVATE, Modifier.PRIVATE)
						.or(this.and(Modifier.PROTECTED, Modifier.PROTECTED))
						.or(this.and(Modifier.PUBLIC, Modifier.PUBLIC));
			} else
				return this.and(accessModif, 0);
		}
	}

	public final ModifierMatcher forceModifier(int modifier) {
		return this.and(modifier, modifier);
	}

	public final ModifierMatcher forbidModifier(int modifier) {
		return this.and(modifier, 0);
	}

	public final boolean matches(int modifiers) {
		for (int i = 0; i < andMasks.length; i++)
			if ((modifiers & andMasks[i]) == xorMasks[i])
				return true;

		return false;
	}

	public final boolean matches(Identifier ident) {
		int modifiers;
		/* XXX NEW INTERFACE OR ANOTHER METHOD IN IDENTIFIER? */
		if (ident instanceof ClassIdentifier)
			modifiers = ((ClassIdentifier) ident).getModifiers();
		else if (ident instanceof MethodIdentifier)
			modifiers = ((MethodIdentifier) ident).getModifiers();
		else if (ident instanceof FieldIdentifier)
			modifiers = ((FieldIdentifier) ident).getModifiers();
		else
			return false;
		return matches(modifiers);
	}

	public final boolean matchesSub(Identifier ident, String name) {
		return true;
	}

	public final String getNextComponent(Identifier ident) {
		return null;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new IncompatibleClassChangeError(getClass().getName());
		}
	}
}
