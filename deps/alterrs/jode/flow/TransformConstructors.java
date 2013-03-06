/* TransformConstructors Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: TransformConstructors.java,v 1.27.2.5 2002/05/28 17:34:09 hoenicke Exp $
 */

package alterrs.jode.flow;

import java.lang.reflect.Modifier;
import java.util.Vector;

import alterrs.jode.GlobalOptions;
import alterrs.jode.bytecode.ClassInfo;
import alterrs.jode.bytecode.InnerClassInfo;
import alterrs.jode.bytecode.MethodInfo;
import alterrs.jode.decompiler.ClassAnalyzer;
import alterrs.jode.decompiler.FieldAnalyzer;
import alterrs.jode.decompiler.MethodAnalyzer;
import alterrs.jode.decompiler.Options;
import alterrs.jode.decompiler.OuterValueListener;
import alterrs.jode.decompiler.OuterValues;
import alterrs.jode.expr.Expression;
import alterrs.jode.expr.FieldOperator;
import alterrs.jode.expr.IIncOperator;
import alterrs.jode.expr.InvokeOperator;
import alterrs.jode.expr.LocalLoadOperator;
import alterrs.jode.expr.LocalStoreOperator;
import alterrs.jode.expr.LocalVarOperator;
import alterrs.jode.expr.Operator;
import alterrs.jode.expr.PutFieldOperator;
import alterrs.jode.expr.StoreInstruction;
import alterrs.jode.expr.ThisOperator;
import alterrs.jode.type.MethodType;
import alterrs.jode.type.Type;

/**
 * This class will transform the constructors. We differ three types of
 * constructors:
 * <dl>
 * <dt>type0</dt>
 * <dd>are constructors, that call no constructors or whose default super call
 * was already removed. <code>java.lang.Object.&lt;init&gt;</code> and static
 * constructors are examples for the first kind.</dd>
 * <dt>type1</dt>
 * <dd>are constructors, that call the constructor of super class</dd>
 * <dt>type2</dt>
 * <dd>are constructors, that call another constructor of the same class</dd>
 * </dl>
 * <p/>
 * The transformation involves several steps:
 * <p/>
 * The first step is done by removeSynthInitializers, which does the following:
 * <ul>
 * <li>For inner classes check if the this$0 field(s) is/are initialized
 * corectly, remove the initializer and mark that field.</li>
 * <li>For method scope classes also check the val$xx fields.</li>
 * </ul>
 * <br>
 * <p/>
 * In the last analyze phase (makeDeclaration) the rest is done:
 * <ul>
 * <li>remove implicit super() call</li>
 * <li>move constant field initializations that occur in all constructors
 * (except those that start with a this() call) to the fields.</li>
 * <li>For jikes class check for a constructor$xx call, and mark that as the
 * real constructor, moving the super call of the original constructor</li>
 * <li>For anonymous classes check that the constructor only contains a super
 * call and mark it as default</li>
 * </ul>
 * <p/>
 * It will make use of the <code>outerValues</code> expression, that tell which
 * parameters (this and final method variables) are always given to the
 * constructor.
 * <p/>
 * You can debug this class with the <code>--debug=constructors</code> switch.
 * 
 * @author Jochen Hoenicke
 * @see alterrs.jode.decompiler.FieldAnalyzer#setInitializer
 * @see alterrs.jode.decompiler.ClassAnalyzer#getOuterValues
 */
public class TransformConstructors {
	/*
	 * What is sometimes confusing is the distinction between slot and
	 * parameter. Most times parameter nr = slot nr, but double and long
	 * parameters take two slots, so the remaining parameters will shift.
	 */

	ClassAnalyzer clazzAnalyzer;
	boolean isStatic;

	/*
	 * The method analyzers of the constructors: type0 constructors come first,
	 * then type1, then type2.
	 */MethodAnalyzer[] cons;
	int type0Count;
	int type01Count;

	OuterValues outerValues;

	public TransformConstructors(ClassAnalyzer clazzAnalyzer, boolean isStatic,
			MethodAnalyzer[] cons) {
		this.clazzAnalyzer = clazzAnalyzer;
		this.isStatic = isStatic;
		this.cons = cons;
		if (!isStatic)
			this.outerValues = clazzAnalyzer.getOuterValues();
		lookForConstructorCall();
	}

	/**
	 * Returns the type of the constructor. We differ three types of
	 * constructors:
	 * <dl>
	 * <dt>type1</dt>
	 * <dd>are constructors, that call the constructor of super class</dd>
	 * <dt>type2</dt>
	 * <dd>are constructors, that call another constructor of the same class</dd>
	 * <dt>type0</dt>
	 * <dd>are constructors, that call no constructors.
	 * <code>java.lang.Object.&lt;init&gt;</code> and static constructors are
	 * examples for this</dd>
	 * </dl>
	 * 
	 * @param body
	 *            the content of the constructor.
	 * @return the type of the constructor.
	 */
	private int getConstructorType(StructuredBlock body) {
		/*
		 * A non static constructor must begin with a call to another
		 * constructor. Either to a constructor of the same class or to the
		 * super class
		 */
		InstructionBlock ib;
		if (body instanceof InstructionBlock)
			ib = (InstructionBlock) body;
		else if (body instanceof SequentialBlock
				&& (body.getSubBlocks()[0] instanceof InstructionBlock))
			ib = (InstructionBlock) body.getSubBlocks()[0];
		else
			return 0;

		Expression superExpr = ib.getInstruction().simplify();
		if (!(superExpr instanceof InvokeOperator)
				|| superExpr.getFreeOperandCount() != 0)
			return 0;
		InvokeOperator superInvoke = (InvokeOperator) superExpr;
		if (!superInvoke.isConstructor() || !superInvoke.isSuperOrThis())
			return 0;
		Expression thisExpr = superInvoke.getSubExpressions()[0];
		if (!isThis(thisExpr, clazzAnalyzer.getClazz()))
			return 0;

		if (superInvoke.isThis())
			return 2;
		else
			return 1;
	}

	public void lookForConstructorCall() {
		type01Count = cons.length;
		for (int i = 0; i < type01Count;) {
			MethodAnalyzer current = cons[i];
			FlowBlock header = cons[i].getMethodHeader();
			/* Check that code block is fully analyzed */
			if (header == null || !header.hasNoJumps())
				return;

			StructuredBlock body = cons[i].getMethodHeader().block;
			int type = isStatic ? 0 : getConstructorType(body);
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("constr " + i + ": type" + type + " "
						+ body);

			switch (type) {
			case 0:
				// type0 are moved to the beginning.
				cons[i] = cons[type0Count];
				cons[type0Count++] = current;
				/* fall through */
			case 1:
				// type1 are not moved at all.
				i++;
				break;
			case 2:
				// type2 are moved to the end.
				cons[i] = cons[--type01Count];
				cons[type01Count] = current;
				break;
			}
		}
	}

	public static boolean isThis(Expression thisExpr, ClassInfo clazz) {
		return ((thisExpr instanceof ThisOperator) && (((ThisOperator) thisExpr)
				.getClassInfo() == clazz));
	}

	/**
	 * Check if this is a single anonymous constructor and mark it as such. We
	 * only check if the super() call is correctly formed and ignore the rest of
	 * the body.
	 * <p/>
	 * This method also marks the jikesAnonymousInner.
	 */
	private void checkAnonymousConstructor() {
		if (isStatic || cons.length != 1 || type01Count - type0Count != 1
				|| clazzAnalyzer.getName() != null)
			return;

		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("checkAnonymousConstructor of "
					+ clazzAnalyzer.getClazz());

		StructuredBlock sb = cons[0].getMethodHeader().block;
		if (sb instanceof SequentialBlock)
			sb = sb.getSubBlocks()[0];

		InstructionBlock superBlock = (InstructionBlock) sb;
		/**
		 * Situation: constructor(outerValues, params) { super(someOuters,
		 * params); }
		 * 
		 * For jikes anonymous classes that extends class or method scoped
		 * classes the situation is more unusal for type1. We check if this is
		 * the case and mark the class as jikesAnonymousInner:
		 * 
		 * constructor(outerValues, params, outerClass) {
		 * outerClass.super(someOuters, params); constructor$?(outerValues[0],
		 * params); }
		 * 
		 * Mark constructor as anonymous constructor.
		 */
		Expression expr = superBlock.getInstruction().simplify();
		InvokeOperator superCall = (InvokeOperator) expr;
		Expression[] subExpr = superCall.getSubExpressions();

		/*
		 * An anonymous constructor may only give locals to its super
		 * constructor.
		 */
		for (int i = 1; i < subExpr.length; i++) {
			if (!(subExpr[i] instanceof LocalLoadOperator))
				return;
		}

		Type[] params = cons[0].getType().getParameterTypes();
		boolean jikesAnon = false;

		int minOuter = params.length;

		int slot = 1;
		for (int i = 0; i < params.length - 1; i++)
			slot += params[i].stackSize();

		/* slot counts from last slot down. */

		int start = 1;
		if (subExpr.length > 2) {
			LocalLoadOperator llop = (LocalLoadOperator) subExpr[1];

			if (llop.getLocalInfo().getSlot() == slot) {
				jikesAnon = true;
				start++;
				// This is not an outer value.
				minOuter--;
				slot -= params[minOuter - 1].stackSize();
			}
		}

		int sub = subExpr.length - 1;
		/* Check how many parameters are passed correctly. */
		while (sub >= start) {
			LocalLoadOperator llop = (LocalLoadOperator) subExpr[sub];

			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("  pos " + sub + ": " + slot + ","
						+ llop.getLocalInfo().getSlot() + "; " + minOuter);

			if (llop.getLocalInfo().getSlot() != slot) {
				// restore the slot.
				slot += params[minOuter - 1].stackSize();
				break;
			}
			sub--;
			/* This parameter is not forced to be an outer value */
			minOuter--;
			if (minOuter == 0)
				break;
			slot -= params[minOuter - 1].stackSize();
		}
		ClassAnalyzer superAna = superCall.getClassAnalyzer();
		OuterValues superOV = null;
		if (superAna != null && superAna.getParent() instanceof MethodAnalyzer) {
			// super is a method scope class.
			superOV = superAna.getOuterValues();
		}
		int minSuperOuter = sub - start + 1;
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("  super outer: " + superOV);

		/* The remaining sub expressions must be outerValues. */
		for (; sub >= start; sub--) {
			LocalLoadOperator llop = (LocalLoadOperator) subExpr[sub];
			if (llop.getLocalInfo().getSlot() >= slot) {
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
					GlobalOptions.err.println("  Illegal slot at " + sub + ":"
							+ llop.getLocalInfo().getSlot());
				return;
			}
		}

		if (minSuperOuter == 1 && superAna.getParent() instanceof ClassAnalyzer) {
			/* Check if this is the implicit Outer Class */
			LocalLoadOperator llop = (LocalLoadOperator) subExpr[start];
			if (outerValues.getValueBySlot(llop.getLocalInfo().getSlot()) instanceof ThisOperator) {
				minSuperOuter = 0;
				outerValues.setImplicitOuterClass(true);
			}
		}

		if (minSuperOuter > 0) {
			if (superOV == null || superOV.getCount() < minSuperOuter) {
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
					GlobalOptions.err.println("  super outer doesn't match: "
							+ minSuperOuter);
				return;
			}
			superOV.setMinCount(minSuperOuter);
		}

		outerValues.setMinCount(minOuter);
		if (superOV != null) {
			final int ovdiff = minOuter - minSuperOuter;
			outerValues.setCount(superOV.getCount() + ovdiff);
			superOV.addOuterValueListener(new OuterValueListener() {
				public void shrinkingOuterValues(OuterValues other, int newCount) {
					outerValues.setCount(newCount + ovdiff);
				}
			});
		} else
			outerValues.setCount(minOuter);

		if (jikesAnon)
			outerValues.setJikesAnonymousInner(true);
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("  succeeded: " + outerValues);
		cons[0].setAnonymousConstructor(true);
		superBlock.removeBlock();
		type0Count++;
	}

	private boolean checkJikesSuper(Expression expr) {
		if (expr instanceof LocalStoreOperator || expr instanceof IIncOperator)
			return false;
		if (expr instanceof Operator) {
			Expression subExpr[] = ((Operator) expr).getSubExpressions();
			for (int i = 0; i < subExpr.length; i++) {
				if (!checkJikesSuper(subExpr[i]))
					return false;
			}
		}
		return true;
	}

	private Expression renameJikesSuper(Expression expr,
			MethodAnalyzer methodAna, int firstOuterSlot, int firstParamSlot) {
		if (expr instanceof LocalLoadOperator) {
			LocalLoadOperator llop = (LocalLoadOperator) expr;
			int slot = llop.getLocalInfo().getSlot();
			if (slot >= firstOuterSlot && slot < firstParamSlot)
				return outerValues.getValueBySlot(slot);
			else {
				Type[] paramTypes = methodAna.getType().getParameterTypes();
				int param;
				/* Adjust the slot */
				if (slot >= firstParamSlot)
					slot -= firstParamSlot - firstOuterSlot;
				for (param = 0; slot > 1 && param < paramTypes.length; param++)
					slot -= paramTypes[param].stackSize();
				llop.setLocalInfo(methodAna.getParamInfo(1 + param));
				llop.setMethodAnalyzer(methodAna);
				return llop;
			}
		}
		if (expr instanceof Operator) {
			Expression subExpr[] = ((Operator) expr).getSubExpressions();
			for (int i = 0; i < subExpr.length; i++) {
				Expression newSubExpr = renameJikesSuper(subExpr[i], methodAna,
						firstOuterSlot, firstParamSlot);
				if (newSubExpr != subExpr[i])
					((Operator) expr).setSubExpressions(i, newSubExpr);
			}
		}
		return expr;
	}

	public void checkJikesContinuation() {
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			System.err.println("checkJikesContinuation: " + outerValues);
		constr_loop: for (int i = 0; i < cons.length; i++) {
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("constr " + i + " type"
						+ (i < type0Count ? 0 : i < type01Count ? 1 : 2)
						+ " : " + cons[i].getMethodHeader());

			MethodAnalyzer constr = cons[i];
			MethodType constrType = constr.getType();

			/*
			 * constructor(outerValues, params, opt. jikesAnonInner param) {
			 * optional super/this(expressions); constructor$?(optional
			 * outerValues[0], params); }
			 * 
			 * The outerValues[0] parameter is the this local in the surrounding
			 * method. But we can't be sure, what the surrounding method is,
			 * since it could be either the method that uses the class, or a
			 * method that declares the class, that contains the method that
			 * uses the class.<br>
			 * 
			 * If the surrounding method is static, the outerValues[0] parameter
			 * disappears.
			 * 
			 * Move optional super to method constructor$? (renaming local
			 * variables) and mark constructor and constructor$? as Jikes
			 * constructor.
			 */
			StructuredBlock sb = constr.getMethodHeader().block;

			Vector localLoads = null;
			InstructionBlock superBlock = null;
			if (i >= type0Count) {
				/*
				 * Extract the super() or this() call at the beginning of the
				 * constructor
				 */
				if (!(sb instanceof SequentialBlock)
						|| !(sb.getSubBlocks()[1] instanceof InstructionBlock))
					continue constr_loop;

				superBlock = (InstructionBlock) sb.getSubBlocks()[0];
				sb = sb.getSubBlocks()[1];

				Expression superExpr = superBlock.getInstruction().simplify();
				InvokeOperator superInvoke = (InvokeOperator) superExpr;
				superBlock.setInstruction(superInvoke);

				Expression[] subExpr = superInvoke.getSubExpressions();
				for (int j = 1; j < subExpr.length; j++) {
					if (!checkJikesSuper(subExpr[j]))
						continue constr_loop;
				}
			}

			if (!(sb instanceof InstructionBlock))
				continue constr_loop;

			/* Now check the constructor$? invocation */
			Expression lastExpr = ((InstructionBlock) sb).getInstruction()
					.simplify();
			if (!(lastExpr instanceof InvokeOperator))
				continue constr_loop;

			InvokeOperator invoke = (InvokeOperator) lastExpr;
			if (!invoke.isThis() || invoke.getFreeOperandCount() != 0)
				continue constr_loop;
			MethodAnalyzer methodAna = invoke.getMethodAnalyzer();
			if (methodAna == null)
				continue constr_loop;
			MethodType methodType = methodAna.getType();
			Expression[] methodParams = invoke.getSubExpressions();

			if (!methodAna.getName().startsWith("constructor$")
					|| methodType.getReturnType() != Type.tVoid)
				continue constr_loop;

			if (!isThis(methodParams[0], clazzAnalyzer.getClazz()))
				continue constr_loop;
			for (int j = 1; j < methodParams.length; j++) {
				if (!(methodParams[j] instanceof LocalLoadOperator))
					continue constr_loop;
			}

			Type[] paramTypes = constr.getType().getParameterTypes();
			int paramCount = paramTypes.length;
			if (outerValues.isJikesAnonymousInner())
				paramCount--;

			int maxOuterCount = paramCount - methodParams.length + 2;
			int minOuterCount = maxOuterCount - 1;
			int slot = 1;
			int firstParam = 1;
			Expression outer0 = null;

			if (maxOuterCount > 0 && methodParams.length > 1
					&& outerValues.getCount() > 0) {
				/*
				 * Check if the outerValues[0] param is present. we can't be
				 * sure if maxOuterCount equals 1, but we assume so, since at
				 * this time all possible info about outers have been collected.
				 */
				if (((LocalLoadOperator) methodParams[firstParam])
						.getLocalInfo().getSlot() == 1) {
					minOuterCount = maxOuterCount;
					outer0 = outerValues.getValue(0);
					firstParam++;
				} else
					maxOuterCount--;
				for (int j = 0; j < maxOuterCount; j++)
					slot += paramTypes[j].stackSize();
			}

			if (minOuterCount > outerValues.getCount())
				continue constr_loop;

			int firstParamSlot = slot;
			int firstOuterSlot = firstParam;
			int slotDist = firstParamSlot - firstOuterSlot;
			/*
			 * check the remaining parameters.
			 */
			for (int j = firstParam; j < methodParams.length; j++) {
				if (((LocalLoadOperator) methodParams[j]).getLocalInfo()
						.getSlot() != slot)
					continue constr_loop;
				slot += methodParams[j].getType().stackSize();
			}

			outerValues.setMinCount(minOuterCount);
			outerValues.setCount(maxOuterCount);

			/*
			 * Now move the constructor call.
			 */
			if (superBlock != null) {
				Expression newExpr = renameJikesSuper(
						superBlock.getInstruction(), methodAna, firstOuterSlot,
						firstParamSlot);
				superBlock.removeBlock();
				methodAna.insertStructuredBlock(superBlock);
			}
			if (outer0 != null) {
				methodAna.getParamInfo(1).setExpression(outer0);
				methodAna.getMethodHeader().simplify();
			}

			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("  succeeded");

			constr.setJikesConstructor(constr);
			methodAna.setJikesConstructor(constr);
			methodAna.setHasOuterValue(firstOuterSlot == 2);
			if (constr.isAnonymousConstructor())
				methodAna.setAnonymousConstructor(true);
		}
	}

	/**
	 * This methods checks if expr is a valid field initializer. It will also
	 * merge outerValues, that occur in expr.
	 * 
	 * @param expr
	 *            the initializer to check
	 * @return the transformed initializer or null if expr is not valid.
	 */
	private Expression transformFieldInitializer(int fieldSlot, Expression expr) {
		if (expr instanceof LocalVarOperator) {
			if (!(expr instanceof LocalLoadOperator)) {
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
					GlobalOptions.err.println("illegal local op: " + expr);
				return null;
			}
			if (outerValues != null
					&& (Options.options & Options.OPTION_CONTRAFO) != 0) {
				int slot = ((LocalLoadOperator) expr).getLocalInfo().getSlot();
				Expression outExpr = outerValues.getValueBySlot(slot);
				if (outExpr != null)
					return outExpr;
			}
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("not outerValue: " + expr + " "
						+ outerValues);
			return null;
		}
		if (expr instanceof FieldOperator) {
			if (expr instanceof PutFieldOperator)
				return null;
			FieldOperator fo = (FieldOperator) expr;
			if (fo.getClassInfo() == clazzAnalyzer.getClazz()
					&& clazzAnalyzer.getFieldIndex(fo.getFieldName(),
							fo.getFieldType()) >= fieldSlot)
				return null;
		}
		if (expr instanceof InvokeOperator) {
			/*
			 * Don't allow method invocations that can throw a checked exception
			 * to leave the constructor.
			 */
			MethodInfo method = ((InvokeOperator) expr).getMethodInfo();
			String[] excs = method == null ? null : method.getExceptions();
			if (excs != null) {
				ClassInfo runtimeException = ClassInfo
						.forName("java.lang.RuntimeException");
				ClassInfo error = ClassInfo.forName("java.lang.Error");
				for (int i = 0; i < excs.length; i++) {
					ClassInfo exClass = ClassInfo.forName(excs[i]);
					if (!runtimeException.superClassOf(exClass)
							&& !error.superClassOf(exClass))
						return null;
				}
			}
		}
		if (expr instanceof Operator) {
			Operator op = (Operator) expr;
			Expression[] subExpr = op.getSubExpressions();
			for (int i = 0; i < subExpr.length; i++) {
				Expression transformed = transformFieldInitializer(fieldSlot,
						subExpr[i]);
				if (transformed == null)
					return null;
				if (transformed != subExpr[i])
					op.setSubExpressions(i, transformed);
			}
		}
		return expr;
	}

	public void removeSynthInitializers() {
		if ((Options.options & Options.OPTION_CONTRAFO) == 0 || isStatic
				|| type01Count == 0)
			return;

		checkAnonymousConstructor();

		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("removeSynthInitializers of "
					+ clazzAnalyzer.getClazz());

		/* sb will iterate the instructions of the constructor. */
		StructuredBlock[] sb = new StructuredBlock[type01Count];
		for (int i = 0; i < type01Count; i++) {
			sb[i] = cons[i].getMethodHeader().block;
			if (i >= type0Count) {
				if (sb[i] instanceof SequentialBlock)
					sb[i] = sb[i].getSubBlocks()[1];
				else
					/* One constructor is done. There is no field */
					return;
			}
		}

		big_loop: for (;;) {
			StructuredBlock ib = (sb[0] instanceof SequentialBlock) ? sb[0]
					.getSubBlocks()[0] : sb[0];

			if (!(ib instanceof InstructionBlock))
				break big_loop;

			Expression instr = ((InstructionBlock) ib).getInstruction()
					.simplify();

			if (!(instr instanceof StoreInstruction)
					|| instr.getFreeOperandCount() != 0)
				break big_loop;

			StoreInstruction store = (StoreInstruction) instr;
			if (!(store.getLValue() instanceof PutFieldOperator))
				break big_loop;

			PutFieldOperator pfo = (PutFieldOperator) store.getLValue();
			if (pfo.isStatic() != isStatic
					|| pfo.getClassInfo() != clazzAnalyzer.getClazz())
				break big_loop;

			if (!isThis(pfo.getSubExpressions()[0], clazzAnalyzer.getClazz()))
				break big_loop;

			int field = clazzAnalyzer.getFieldIndex(pfo.getFieldName(),
					pfo.getFieldType());
			if (field < 0)
				break big_loop;
			FieldAnalyzer fieldAna = clazzAnalyzer.getField(field);

			/*
			 * Don't check for final. Jikes sometimes omits this attribute.
			 */
			if (!fieldAna.isSynthetic())
				break big_loop;

			Expression expr = store.getSubExpressions()[1];
			expr = transformFieldInitializer(field, expr);
			if (expr == null)
				break big_loop;

			for (int i = 1; i < type01Count; i++) {
				ib = (sb[i] instanceof SequentialBlock) ? sb[i].getSubBlocks()[0]
						: sb[i];
				if (!(ib instanceof InstructionBlock)
						|| !(((InstructionBlock) ib).getInstruction()
								.simplify().equals(instr))) {
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
						GlobalOptions.err.println("  constr 0 and " + i
								+ " differ: " + instr + "<-/->" + ib);
					break big_loop;
				}
			}

			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("  field " + pfo.getFieldName()
						+ " = " + expr);

			if (!(fieldAna.setInitializer(expr))) {
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
					GlobalOptions.err.println("    setField failed");
				break big_loop;
			}

			boolean done = false;
			for (int i = 0; i < type01Count; i++) {
				if (sb[i] instanceof SequentialBlock) {
					StructuredBlock next = sb[i].getSubBlocks()[1];
					next.replace(sb[i]);
					sb[i] = next;
				} else {
					sb[i].removeBlock();
					sb[i] = null;
					done = true;
				}
			}

			if (done) {
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
					GlobalOptions.err.println("one constr is over");
				break;
			}
		}
	}

	public int transformOneField(int lastField, StructuredBlock ib) {

		if (!(ib instanceof InstructionBlock))
			return -1;

		Expression instr = ((InstructionBlock) ib).getInstruction().simplify();

		if (!(instr instanceof StoreInstruction)
				|| instr.getFreeOperandCount() != 0)
			return -1;

		StoreInstruction store = (StoreInstruction) instr;
		if (!(store.getLValue() instanceof PutFieldOperator))
			return -1;

		PutFieldOperator pfo = (PutFieldOperator) store.getLValue();
		if (pfo.isStatic() != isStatic
				|| pfo.getClassInfo() != clazzAnalyzer.getClazz())
			return -1;

		if (!isStatic) {
			if (!isThis(pfo.getSubExpressions()[0], clazzAnalyzer.getClazz())) {
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
					GlobalOptions.err.println("  not this: " + instr);
				return -1;
			}
		}

		int field = clazzAnalyzer.getFieldIndex(pfo.getFieldName(),
				pfo.getFieldType());

		if (field <= lastField)
			return -1;

		Expression expr = store.getSubExpressions()[1];
		expr = transformFieldInitializer(field, expr);
		if (expr == null)
			return -1;

		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("  field " + pfo.getFieldName() + " = "
					+ expr);

		// if field does not exists: -1 <= lastField.
		if (field <= lastField
				|| !(clazzAnalyzer.getField(field).setInitializer(expr))) {
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("set field failed");
			return -1;
		}
		return field;
	}

	public void transformBlockInitializer(StructuredBlock block) {
		StructuredBlock start = null;
		StructuredBlock tail = null;
		int lastField = -1;
		while (block instanceof SequentialBlock) {
			StructuredBlock ib = block.getSubBlocks()[0];
			int field = transformOneField(lastField, ib);
			if (field < 0)
				clazzAnalyzer.addBlockInitializer(lastField + 1, ib);
			else
				lastField = field;
			block = block.getSubBlocks()[1];
		}
		if (transformOneField(lastField, block) < 0)
			clazzAnalyzer.addBlockInitializer(lastField + 1, block);
	}

	public boolean checkBlockInitializer(InvokeOperator invoke) {
		if (!invoke.isThis() || invoke.getFreeOperandCount() != 0)
			return false;
		MethodAnalyzer methodAna = invoke.getMethodAnalyzer();
		if (methodAna == null)
			return false;
		FlowBlock flow = methodAna.getMethodHeader();
		MethodType methodType = methodAna.getType();
		if (!methodAna.getName().startsWith("block$")
				|| methodType.getParameterTypes().length != 0
				|| methodType.getReturnType() != Type.tVoid)
			return false;
		if (flow == null || !flow.hasNoJumps())
			return false;

		if (!isThis(invoke.getSubExpressions()[0], clazzAnalyzer.getClazz()))
			return false;

		methodAna.setJikesBlockInitializer(true);
		transformBlockInitializer(flow.block);
		return true;
	}

	private void removeDefaultSuper() {
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("removeDefaultSuper of "
					+ clazzAnalyzer.getClazz());
		/*
		 * Check if we can remove the super() call of type1 constructors. This
		 * transforms a type1 constructor in a type0 constructor.
		 */
		for (int i = type0Count; i < type01Count; i++) {
			MethodAnalyzer current = cons[i];
			FlowBlock header = cons[i].getMethodHeader();
			StructuredBlock body = header.block;

			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("constr " + i + ": " + body);

			InstructionBlock ib;
			if (body instanceof InstructionBlock)
				ib = (InstructionBlock) body;
			else
				ib = (InstructionBlock) body.getSubBlocks()[0];

			InvokeOperator superInvoke = (InvokeOperator) ib.getInstruction()
					.simplify();
			ClassInfo superClazz = superInvoke.getClassInfo();
			InnerClassInfo[] outers = superClazz.getOuterClasses();
			int superParamCount = superInvoke.getSubExpressions().length - 1;

			if ((Options.options & Options.OPTION_INNER) != 0 && outers != null
					&& outers[0].outer != null && outers[0].name != null
					&& !Modifier.isStatic(outers[0].modifiers)) {

				if (superParamCount != 1
						|| !(superInvoke.getSubExpressions()[1] instanceof ThisOperator))
					continue;
			} else {
				/*
				 * If the super() has no parameters (or only default outerValue
				 * parameter for inner/anonymous classes), we can remove it
				 */
				ClassAnalyzer superClazzAna = superInvoke.getClassAnalyzer();
				OuterValues superOV = null;
				if (superClazzAna != null)
					superOV = superClazzAna.getOuterValues();
				if (superParamCount > 0
						&& (superOV == null || superParamCount > superOV
								.getCount()))
					continue;
			}
			ib.removeBlock();
			if (i > type0Count) {
				cons[i] = cons[type0Count];
				cons[type0Count] = current;
			}
			type0Count++;
		}
	}

	private void removeInitializers() {
		if (type01Count == 0)
			return;

		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("removeInitializers");

		StructuredBlock[] sb = new StructuredBlock[type01Count];
		for (int i = 0; i < type01Count; i++) {
			FlowBlock header = cons[i].getMethodHeader();
			/* sb[i] will iterate the instructions of the constructor. */
			sb[i] = header.block;
			if (i >= type0Count) {
				if (sb[i] instanceof SequentialBlock)
					sb[i] = sb[i].getSubBlocks()[1];
				else {
					sb[i] = null;
					return;
				}
			}
		}
		int lastField = -1;
		big_loop: for (;;) {
			StructuredBlock ib = (sb[0] instanceof SequentialBlock) ? sb[0]
					.getSubBlocks()[0] : sb[0];

			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
				GlobalOptions.err.println("Instruction: " + ib);

			if (!(ib instanceof InstructionBlock))
				break big_loop;

			Expression instr = ((InstructionBlock) ib).getInstruction()
					.simplify();

			for (int i = 1; i < type01Count; i++) {
				ib = (sb[i] instanceof SequentialBlock) ? sb[i].getSubBlocks()[0]
						: sb[i];
				if (!(ib instanceof InstructionBlock)
						|| !(((InstructionBlock) ib).getInstruction()
								.simplify().equals(instr))) {
					if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
						GlobalOptions.err.println("constr " + i + " differs: "
								+ ib);
					break big_loop;
				}
			}

			if (instr instanceof InvokeOperator
					&& checkBlockInitializer((InvokeOperator) instr)) {
				for (int i = 0; i < type01Count; i++) {
					if (sb[i] instanceof SequentialBlock) {
						StructuredBlock next = sb[i].getSubBlocks()[1];
						next.replace(sb[i]);
						sb[i] = next;
					} else {
						sb[i].removeBlock();
						sb[i] = null;
					}
				}
				break big_loop;
			}

			int field = transformOneField(lastField, ib);
			if (field < 0)
				break big_loop;

			lastField = field;

			boolean done = false;
			for (int i = 0; i < type01Count; i++) {
				if (sb[i] instanceof SequentialBlock) {
					StructuredBlock next = sb[i].getSubBlocks()[1];
					next.replace(sb[i]);
					sb[i] = next;
				} else {
					sb[i].removeBlock();
					sb[i] = null;
					done = true;
				}
			}

			if (done) {
				if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_CONSTRS) != 0)
					GlobalOptions.err.println("one constr is over");
				break;
			}
		}
	}

	/**
	 * This does the normal constructor transformations.
	 * <p/>
	 * javac copies the field initializers to each constructor. This will undo
	 * the transformation: it will tell the fields about the initial value and
	 * removes the initialization from all constructors.
	 * <p/>
	 * There are of course many checks necessary: All field initializers must be
	 * equal in all constructors, and there mustn't be locals that used in field
	 * initialization (except outerValue - locals).
	 */
	public void transform() {
		if ((Options.options & Options.OPTION_CONTRAFO) == 0
				|| cons.length == 0)
			return;

		removeDefaultSuper();
		removeInitializers();
		checkJikesContinuation();

		if (outerValues != null) {
			/*
			 * Now tell all constructors the value of outerValues parameters and
			 * simplify them again.
			 */
			for (int i = 0; i < cons.length; i++) {
				for (int j = 0; j < outerValues.getCount(); j++)
					cons[i].getParamInfo(j + 1).setExpression(
							outerValues.getValue(j));
				// if (outerValues.isJikesAnonymousConstructor()) {
				// /*XXX???*/
				// }
				cons[i].getMethodHeader().simplify();
			}
		}
	}
}
