/* OuterValues Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: OuterValues.java,v 4.1.2.2 2002/05/28 17:34:03 hoenicke Exp $
 */

package alterrs.jode.decompiler;

import java.util.Enumeration;
import java.util.Vector;

import alterrs.jode.GlobalOptions;
import alterrs.jode.expr.Expression;
import alterrs.jode.expr.LocalLoadOperator;
import alterrs.jode.expr.OuterLocalOperator;
import alterrs.jode.expr.ThisOperator;
import alterrs.jode.type.Type;

/**
 * OuterValues are used in method scoped classes: If a method scoped class uses
 * a local of the surrounding method, the java compiler adds the locals to the
 * param list of each constructor of the method scoped class. Each invocation of
 * the constructor must give the correct values for these locals.
 * <p/>
 * These extra parameters are the outerValues.
 * <p/>
 * The main problem here is, that we don't know immediately if a parameter is a
 * standard parameter or a local of the outer method. We may shrink this array
 * if we notice a problem later.
 * <p/>
 * Every class interested in outer values, may register itself as
 * OuterValueListener. It will then be notified every time the outer values
 * shrink.
 * <p/>
 * The outer instance of a non static _class_ scoped class is not considered as
 * outer value, mainly because it can be changed. With jikes method scoped
 * classes also have an outer class instance, but that is considered as outer
 * value.
 * <p/>
 * Under jikes anonymous classes that extends class or method scoped classes
 * have as last parameter the outer instance of the parent class. This should
 * really be the first parameter (just after the outerValues), like it is under
 * javac. We mark such classes as jikesAnonymousInner. This is done in the
 * initialize() pass.
 * <p/>
 * Under javac the outer class paramter for anonymous classes that extends class
 * scoped classes is at the right position, just before the other parameters.
 * 
 * @see #shrinkOuterValues
 * @see #addOuterValueListener
 * @since 1.0.93
 */
public class OuterValues {
	private ClassAnalyzer clazzAnalyzer;

	private Expression[] head;
	private Vector ovListeners;
	private boolean jikesAnonymousInner;
	private boolean implicitOuterClass;

	/**
	 * The maximal number of parameters used for outer values.
	 */
	private int headCount;
	/**
	 * The minimal number of parameters used for outer values.
	 */
	private int headMinCount;

	public OuterValues(ClassAnalyzer ca, Expression[] head) {
		this.clazzAnalyzer = ca;
		this.head = head;
		this.headMinCount = 0;
		this.headCount = head.length;
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("Created OuterValues: " + this);
	}

	public Expression getValue(int i) {
		/** require i < getCount() **/
		return head[i];
	}

	public int getCount() {
		return headCount;
	}

	private int getNumberBySlot(int slot) {
		slot--; // skip this parameter (not an outer value)
		for (int i = 0; slot >= 0 && i < headCount; i++) {
			if (slot == 0)
				return i;
			slot -= head[i].getType().stackSize();
		}
		return -1;
	}

	/**
	 * Get the outer value corresponding to a given slot. This will also adjust
	 * the minSlot value. This only considers head slots.
	 * 
	 * @return index into outerValues array or -1, if not matched.
	 */
	public Expression getValueBySlot(int slot) {
		slot--; // skip this parameter (not an outer value)
		for (int i = 0; i < headCount; i++) {
			if (slot == 0) {
				Expression expr = head[i];
				if (i >= headMinCount)
					headMinCount = i;
				return expr;
			}
			slot -= head[i].getType().stackSize();
		}
		return null;
	}

	/**
	 * If li is a local variable of a constructor, and it could be an outer
	 * value, return this outer value and mark ourself as listener. If that
	 * outer value gets invalid later, we shrink ourself to the given nr.
	 * 
	 * @param expr
	 *            The expression to lift.
	 * @param nr
	 *            The nr of outer values we shrink to, if something happens
	 *            later.
	 * @return the outer value if the above conditions are true, null otherwise.
	 */
	private Expression liftOuterValue(LocalInfo li, final int nr) {
		MethodAnalyzer method = li.getMethodAnalyzer();

		if (!method.isConstructor() || method.isStatic())
			return null;
		OuterValues ov = method.getClassAnalyzer().getOuterValues();
		if (ov == null)
			return null;

		int ovNr = ov.getNumberBySlot(li.getSlot());
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("  ovNr " + ovNr + "," + ov);
		if (ovNr < 0 && ov.getCount() >= 1 && ov.isJikesAnonymousInner()) {
			/*
			 * Second chance if this is a jikesAnonInner class: last parameter
			 * is this parameter. XXX
			 */
			Type[] paramTypes = method.getType().getParameterTypes();
			int lastSlot = 1;
			for (int i = 0; i < paramTypes.length - 1; i++)
				lastSlot += paramTypes[i].stackSize();

			/* jikesAnonInner corresponds to the first outer value */
			if (li.getSlot() == lastSlot)
				ovNr = 0;
		}
		if (ovNr < 0)
			return null;
		if (ov != this || ovNr > nr) {
			final int limit = ovNr;
			ov.addOuterValueListener(new OuterValueListener() {
				public void shrinkingOuterValues(OuterValues other, int newCount) {
					if (newCount <= limit)
						setCount(nr);
				}
			});
		}
		return ov.head[ovNr];
	}

	public boolean unifyOuterValues(int nr, Expression otherExpr) {
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("unifyOuterValues: " + this + "," + nr
					+ "," + otherExpr);
		/** require nr < getCount() **/
		Expression expr1 = otherExpr;
		Expression expr2 = head[nr];
		LocalInfo li1;

		/*
		 * Wow, unifying outer values of different constructors in different
		 * methods of different classes can get complicated. We have not
		 * committed the number of OuterValues. So we can't say for sure, if the
		 * local load matches an outer local if this is a constructor. Even
		 * worse: The previous outerValues may be a load of a constructor local,
		 * that should be used as outer value...
		 * 
		 * See MethodScopeTest for examples.
		 * 
		 * We look if there is a way to merge them and register an outer value
		 * listener to lots of classes.
		 */

		if (expr1 instanceof ThisOperator) {
			li1 = null;
		} else if (expr1 instanceof OuterLocalOperator) {
			li1 = ((OuterLocalOperator) expr1).getLocalInfo();
		} else if (expr1 instanceof LocalLoadOperator) {
			li1 = ((LocalLoadOperator) expr1).getLocalInfo();
		} else
			return false;

		/* First lift expr1 until it is a parent of this class */
		while (li1 != null
				&& !li1.getMethodAnalyzer().isMoreOuterThan(clazzAnalyzer)) {
			expr1 = liftOuterValue(li1, nr);
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("  lift1 " + li1 + " in "
						+ li1.getMethodAnalyzer() + "  to " + expr1);

			if (expr1 instanceof ThisOperator) {
				li1 = null;
			} else if (expr1 instanceof OuterLocalOperator) {
				li1 = ((OuterLocalOperator) expr1).getLocalInfo();
			} else
				return false;
		}
		/* Now lift expr2 until expr1 and expr2 are equal */
		while (!expr1.equals(expr2)) {
			if (expr2 instanceof OuterLocalOperator) {
				LocalInfo li2 = ((OuterLocalOperator) expr2).getLocalInfo();

				/*
				 * if expr1 and expr2 point to same local, we have succeeded
				 * (note that expr1 may be an LocalLoadOperator)
				 */
				if (li2.equals(li1))
					break;

				expr2 = liftOuterValue(li2, nr);
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
					GlobalOptions.err.println("  lift2 " + li2 + " in "
							+ li2.getMethodAnalyzer() + "  to " + expr2);
			} else
				return false;
		}

		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("unifyOuterValues succeeded.");
		return true;
	}

	/**
	 * Jikes gives the outer class reference in an unusual place (as last
	 * parameter) for anonymous classes that extends an inner (or method scope)
	 * class. This method tells if this is such a class.
	 */
	public boolean isJikesAnonymousInner() {
		return jikesAnonymousInner;
	}

	/**
	 * Javac 1.3 doesn't give an outer class reference for anonymous classes
	 * that extend inner classes, provided the outer class is the normal this
	 * parameter. Instead it takes a normal outer value parameter for this. This
	 * method tells if this is such a class.
	 */
	public boolean isImplicitOuterClass() {
		return implicitOuterClass;
	}

	public void addOuterValueListener(OuterValueListener l) {
		if (ovListeners == null)
			ovListeners = new Vector();
		ovListeners.addElement(l);
	}

	/**
	 * Jikes gives the outer class reference in an unusual place (as last
	 * parameter) for anonymous classes that extends an inner (or method scope)
	 * class. This method tells if this is such a class.
	 */
	public void setJikesAnonymousInner(boolean value) {
		jikesAnonymousInner = value;
	}

	public void setImplicitOuterClass(boolean value) {
		implicitOuterClass = value;
	}

	private static int countSlots(Expression[] exprs, int length) {
		int slots = 0;
		for (int i = 0; i < length; i++)
			slots += exprs[i].getType().stackSize();
		return slots;
	}

	public void setMinCount(int newMin) {
		if (headCount < newMin) {
			GlobalOptions.err
					.println("WARNING: something got wrong with scoped class "
							+ clazzAnalyzer.getClazz() + ": " + newMin + ","
							+ headCount);
			new Throwable().printStackTrace(GlobalOptions.err);
			headMinCount = headCount;
		} else if (newMin > headMinCount)
			headMinCount = newMin;
	}

	public void setCount(int newHeadCount) {
		if (newHeadCount >= headCount)
			return;
		headCount = newHeadCount;

		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0) {
			GlobalOptions.err.println("setCount: " + this + "," + newHeadCount);
			new Throwable().printStackTrace(GlobalOptions.err);
		}

		if (newHeadCount < headMinCount) {
			GlobalOptions.err
					.println("WARNING: something got wrong with scoped class "
							+ clazzAnalyzer.getClazz() + ": " + headMinCount
							+ "," + headCount);
			new Throwable().printStackTrace(GlobalOptions.err);
			headMinCount = newHeadCount;
		}

		if (ovListeners != null) {
			for (Enumeration enum_ = ovListeners.elements(); enum_
					.hasMoreElements();)
				((OuterValueListener) enum_.nextElement())
						.shrinkingOuterValues(this, newHeadCount);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer().append(clazzAnalyzer.getClazz())
				.append(".OuterValues[");
		String comma = "";
		int slot = 1;
		for (int i = 0; i < headCount; i++) {
			if (i == headMinCount)
				sb.append("<-");
			sb.append(comma).append(slot).append(":").append(head[i]);
			slot += head[i].getType().stackSize();
			comma = ",";
		}
		if (jikesAnonymousInner)
			sb.append("!jikesAnonymousInner");
		if (implicitOuterClass)
			sb.append("!implicitOuterClass");
		return sb.append("]").toString();
	}
}
