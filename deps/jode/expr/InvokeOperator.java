/* InvokeOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: InvokeOperator.java.in,v 4.10.2.8 2002/05/28 17:34:06 hoenicke Exp $
 */

package jode.expr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jode.GlobalOptions;
import jode.bytecode.BytecodeInfo;
import jode.bytecode.ClassInfo;
import jode.bytecode.InnerClassInfo;
import jode.bytecode.MethodInfo;
import jode.bytecode.Reference;
import jode.bytecode.TypeSignature;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.Options;
import jode.decompiler.OuterValues;
import jode.decompiler.Scope;
import jode.decompiler.TabbedPrintWriter;
import jode.jvm.Interpreter;
import jode.jvm.InterpreterException;
import jode.jvm.SimpleRuntimeEnvironment;
import jode.jvm.SyntheticAnalyzer;
import jode.type.ClassInterfacesType;
import jode.type.IntegerType;
import jode.type.MethodType;
import jode.type.NullType;
import jode.type.Type;
import jode.util.SimpleMap;


public final class InvokeOperator extends Operator implements MatchableOperator {

	public final static int VIRTUAL = 0;
	public final static int SPECIAL = 1;
	public final static int STATIC = 2;
	public final static int CONSTRUCTOR = 3;
	public final static int ACCESSSPECIAL = 4;

	/**
	 * The methodAnalyzer of the method, that contains this invocation. This is
	 * not the method that we should call.
	 */
	MethodAnalyzer methodAnalyzer;
	int methodFlag;
	MethodType methodType;
	String methodName;
	Reference ref;
	int skippedArgs;
	Type classType;
	Type[] hints;

	/**
	 * This hashtable contains hints for every library method. Some library
	 * method take or return an int, but it should be a char instead. We will
	 * remember that here to give them the right hint.
	 * <p/>
	 * The key is the string: methodName + "." + methodType, the value is a map:
	 * It maps base class types for which this hint applies, to an array of hint
	 * types corresponding to the parameters: The first element is the hint type
	 * of the return value, the remaining entries are the hint types of the
	 * parameters. All hint types may be null, if that parameter shouldn't be
	 * hinted.
	 */
	private final static Hashtable hintTypes = new Hashtable();

	static {
		/*
		 * Fill the hint type hashtable. For example, the first parameter of
		 * String.indexOf should be hinted as char, even though the formal
		 * parameter is an int. First hint is hint of return value (even if
		 * void) other hints are that of the parameters in order
		 * 
		 * You only have to hint the base class. Other classes will inherit the
		 * hints.
		 * 
		 * We reuse a lot of objects, since they are all unchangeable this is no
		 * problem. We only hint for chars; it doesn't make much sense to hint
		 * for byte, since its constant representation is more difficult than an
		 * int representation. If you have more hints to suggest, please write
		 * contact me. (see GlobalOptions.EMAIL)
		 */
		Type tCharHint = new IntegerType(IntegerType.IT_I, IntegerType.IT_C);
		Type[] hintC = new Type[] { tCharHint };
		Type[] hint0C = new Type[] { null, tCharHint };
		Type[] hint0C0 = new Type[] { null, tCharHint, null };

		Map hintString0CMap = new SimpleMap(
				Collections.singleton(new SimpleMap.SimpleEntry(Type.tString,
						hint0C)));
		Map hintString0C0Map = new SimpleMap(
				Collections.singleton(new SimpleMap.SimpleEntry(Type.tString,
						hint0C0)));
		hintTypes.put("indexOf.(I)I", hintString0CMap);
		hintTypes.put("lastIndexOf.(I)I", hintString0CMap);
		hintTypes.put("indexOf.(II)I", hintString0C0Map);
		hintTypes.put("lastIndexOf.(II)I", hintString0C0Map);
		hintTypes.put(
				"write.(I)V",
				new SimpleMap(Collections.singleton(new SimpleMap.SimpleEntry(
						Type.tClass("java.io.Writer"), hint0C))));
		hintTypes.put(
				"read.()I",
				new SimpleMap(Collections.singleton(new SimpleMap.SimpleEntry(
						Type.tClass("java.io.Reader"), hintC))));
		hintTypes.put(
				"unread.(I)V",
				new SimpleMap(Collections.singleton(new SimpleMap.SimpleEntry(
						Type.tClass("java.io.PushbackReader"), hint0C))));
	}

	public InvokeOperator(MethodAnalyzer methodAnalyzer, int methodFlag,
			Reference reference) {
		super(Type.tUnknown, 0);
		this.ref = reference;
		this.methodType = Type.tMethod(reference.getType());
		this.methodName = reference.getName();
		this.classType = Type.tType(reference.getClazz());
		this.hints = null;
		Map allHints = (Map) hintTypes.get(methodName + "." + methodType);
		if (allHints != null) {
			for (Iterator i = allHints.entrySet().iterator(); i.hasNext();) {
				Map.Entry e = (Map.Entry) i.next();
				if (classType.isOfType(((Type) e.getKey()).getSubType())) {
					this.hints = (Type[]) e.getValue();
					break;
				}
			}
		}
		if (hints != null && hints[0] != null)
			this.type = hints[0];
		else
			this.type = methodType.getReturnType();
		this.methodAnalyzer = methodAnalyzer;
		this.methodFlag = methodFlag;
		if (methodFlag == STATIC)
			methodAnalyzer.useType(classType);
		skippedArgs = (methodFlag == STATIC ? 0 : 1);
		initOperands(skippedArgs + methodType.getParameterTypes().length);
		checkAnonymousClasses();
	}

	public final boolean isStatic() {
		return methodFlag == STATIC;
	}

	public MethodType getMethodType() {
		return methodType;
	}

	public String getMethodName() {
		return methodName;
	}

	private static MethodInfo getMethodInfo(ClassInfo clazz, String name,
			String type) {
		while (clazz != null) {
			MethodInfo method = clazz.findMethod(name, type);
			if (method != null)
				return method;
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	public MethodInfo getMethodInfo() {
		ClassInfo clazz;
		if (ref.getClazz().charAt(0) == '[')
			clazz = ClassInfo.javaLangObject;
		else
			clazz = TypeSignature.getClassInfo(ref.getClazz());
		return getMethodInfo(clazz, ref.getName(), ref.getType());
	}

	public Type getClassType() {
		return classType;
	}

	public int getPriority() {
		return 950;
	}

	public void checkAnonymousClasses() {
		if (methodFlag != CONSTRUCTOR
				|| (Options.options & Options.OPTION_ANON) == 0)
			return;
		InnerClassInfo outer = getOuterClassInfo(getClassInfo());
		if (outer != null && (outer.outer == null || outer.name == null)) {
			methodAnalyzer.addAnonymousConstructor(this);
		}
	}

	public void updateSubTypes() {
		int offset = 0;
		if (!isStatic()) {
			subExpressions[offset++].setType(Type.tSubType(getClassType()));
		}
		Type[] paramTypes = methodType.getParameterTypes();
		for (int i = 0; i < paramTypes.length; i++) {
			Type pType = (hints != null && hints[i + 1] != null) ? hints[i + 1]
					: paramTypes[i];
			subExpressions[offset++].setType(Type.tSubType(pType));
		}
	}

	public void updateType() {
	}

	/**
	 * Makes a non void expression, in case this is a constructor.
	 */
	public void makeNonVoid() {
		if (type != Type.tVoid)
			throw new jode.AssertError("already non void");
		ClassInfo clazz = getClassInfo();
		InnerClassInfo outer = getOuterClassInfo(clazz);
		if (outer != null && outer.name == null) {
			/* This is an anonymous class */
			if (clazz.getInterfaces().length > 0)
				type = Type.tClass(clazz.getInterfaces()[0]);
			else
				type = Type.tClass(clazz.getSuperclass());
		} else
			type = subExpressions[0].getType();
	}

	public boolean isConstructor() {
		return methodFlag == CONSTRUCTOR;
	}

	public ClassInfo getClassInfo() {
		if (classType instanceof ClassInterfacesType)
			return ((ClassInterfacesType) classType).getClassInfo();
		return null;
	}

	/**
	 * Checks, whether this is a call of a method from this class.
	 */
	public boolean isThis() {
		return getClassInfo() == methodAnalyzer.getClazz();
	}

	public InnerClassInfo getOuterClassInfo(ClassInfo ci) {
		if (ci != null) {
			InnerClassInfo[] outers = ci.getOuterClasses();
			if (outers != null)
				return outers[0];
		}
		return null;
	}

	/**
	 * Tries to locate the class analyzer for the callee class. This is mainly
	 * useful for inner and anonymous classes.
	 * 
	 * @return The class analyzer, if the callee class is declared inside the
	 *         same base class as the caller class, null otherwise.
	 */
	public ClassAnalyzer getClassAnalyzer() {
		if ((Options.options & (Options.OPTION_ANON | Options.OPTION_INNER)) == 0)
			return null;

		ClassInfo callee = getClassInfo();
		if (callee == null)
			return null;

		int nested = 0;
		InnerClassInfo[] outers = callee.getOuterClasses();
		if ((Options.options & Options.OPTION_INNER) != 0 && outers != null) {
			/*
			 * If the callee class is an inner class we take its (outermost)
			 * parent instead. This will assure that we find the callee class
			 * with one inner -> outer pass.
			 */
			nested = outers.length;
			if (outers[nested - 1].outer == null
					|| outers[nested - 1].name == null)
				nested--;

			if (nested > 0)
				callee = ClassInfo.forName(outers[nested - 1].outer);
		}

		/* First check if it is an inner class */
		ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer(callee);

		if (ana == null) {
			/*
			 * Now we iterate the caller analyzer queue to find the class
			 * analyzer for callee
			 */
			ana = methodAnalyzer.getClassAnalyzer();
			while (callee != ana.getClazz()) {
				if (ana.getParent() == null)
					return null;
				if (ana.getParent() instanceof MethodAnalyzer
						&& (Options.options & Options.OPTION_ANON) != 0)
					ana = ((MethodAnalyzer) ana.getParent()).getClassAnalyzer();
				else if (ana.getParent() instanceof ClassAnalyzer
						&& (Options.options & Options.OPTION_INNER) != 0)
					ana = (ClassAnalyzer) ana.getParent();
				else
					throw new jode.AssertError("Unknown parent: " + ana
							+ ": " + ana.getParent());
			}
		}

		/* Now get the ClassAnalyzer of the real callee */
		while (nested > 0) {
			nested--;
			ana = ana.getInnerClassAnalyzer(outers[nested].name);
			if (ana == null)
				return null;
		}
		return ana;
	}

	/**
	 * Checks, whether this is a call of a method from this class or an outer
	 * instance.
	 */
	public boolean isOuter() {
		if (classType instanceof ClassInterfacesType) {
			ClassInfo clazz = ((ClassInterfacesType) classType).getClassInfo();
			ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
			while (true) {
				if (clazz == ana.getClazz())
					return true;
				if (ana.getParent() == null)
					break;
				if (ana.getParent() instanceof MethodAnalyzer
						&& (Options.options & Options.OPTION_ANON) != 0)
					ana = ((MethodAnalyzer) ana.getParent()).getClassAnalyzer();
				else if (ana.getParent() instanceof ClassAnalyzer
						&& (Options.options & Options.OPTION_INNER) != 0)
					ana = (ClassAnalyzer) ana.getParent();
				else
					throw new jode.AssertError("Unknown parent: " + ana
							+ ": " + ana.getParent());
			}
		}
		return false;
	}

	/**
	 * Tries to locate the method analyzer for the callee. This is mainly useful
	 * for inner and anonymous classes.
	 * 
	 * @return The method analyzer, if the callee is declared inside the same
	 *         base class as the caller class, null otherwise.
	 */
	public MethodAnalyzer getMethodAnalyzer() {
		ClassAnalyzer ana = getClassAnalyzer();
		if (ana == null)
			return null;
		return ana.getMethod(methodName, methodType);
	}

	/**
	 * Checks, whether this is a call of a method from the super class.
	 * 
	 * @XXX check, if its the first super class that implements the method.
	 */
	public boolean isSuperOrThis() {
		ClassInfo clazz = getClassInfo();
		if (clazz != null) {
			return clazz.superClassOf(methodAnalyzer.getClazz());
		}
		return false;
	}

	public boolean isConstant() {
		if ((Options.options & Options.OPTION_ANON) == 0)
			return super.isConstant();

		ClassInfo clazz = getClassInfo();
		InnerClassInfo outer = getOuterClassInfo(clazz);
		ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);
		if (clazzAna != null && outer != null && outer.outer == null
				&& outer.name != null && clazzAna.getParent() == methodAnalyzer) {
			/*
			 * This is a named method scope class, it needs declaration. And
			 * therefore can't be moved into a field initializer.
			 */
			return false;
		}
		return super.isConstant();
	}

	/**
	 * Checks if the value of the operator can be changed by this expression.
	 */
	public boolean matches(Operator loadop) {
		return (loadop instanceof InvokeOperator || loadop instanceof GetFieldOperator);
	}

	/**
	 * Checks if the method is the magic class$ method.
	 * 
	 * @return true if this is the magic class$ method, false otherwise.
	 */
	public boolean isGetClass() {
		MethodAnalyzer mana = getMethodAnalyzer();
		if (mana == null)
			return false;
		SyntheticAnalyzer synth = getMethodAnalyzer().getSynthetic();
		return (synth != null && synth.getKind() == SyntheticAnalyzer.GETCLASS);
	}

	class Environment extends SimpleRuntimeEnvironment {

		Interpreter interpreter;
		String classSig;

		public Environment(String interpretedClassSig) {
			classSig = interpretedClassSig.intern();
		}

		public Object invokeMethod(Reference ref, boolean isVirtual,
				Object cls, Object[] params) throws InterpreterException,
				InvocationTargetException {
			if (cls == null && ref.getClazz().equals(classSig)) {
				String clazzName = ref.getClazz();
				clazzName = clazzName.substring(1, ref.getClazz().length() - 1)
						.replace('/', '.');
				BytecodeInfo info = ClassInfo.forName(clazzName)
						.findMethod(ref.getName(), ref.getType()).getBytecode();
				if (info != null)
					return interpreter.interpretMethod(info, null, params);
				throw new InterpreterException(
						"Can't interpret static native method: " + ref);
			} else
				return super.invokeMethod(ref, isVirtual, cls, params);
		}
	}

	public ConstOperator deobfuscateString(ConstOperator op) {
		ClassAnalyzer clazz = methodAnalyzer.getClassAnalyzer();
		MethodAnalyzer ma = clazz.getMethod(methodName, methodType);
		if (ma == null)
			return null;
		Environment env = new Environment("L"
				+ methodAnalyzer.getClazz().getName().replace('.', '/') + ";");
		Interpreter interpreter = new Interpreter(env);
		env.interpreter = interpreter;

		String result;
		try {
			result = (String) interpreter.interpretMethod(ma.getBytecodeInfo(),
					null, new Object[] { op.getValue() });
		} catch (InterpreterException ex) {
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_INTERPRT) != 0) {
				GlobalOptions.err.println("Warning: Can't interpret method "
						+ methodName);
				ex.printStackTrace(GlobalOptions.err);
			}
			return null;
		} catch (InvocationTargetException ex) {
			if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_INTERPRT) != 0) {
				GlobalOptions.err.println("Warning: Interpreted method throws"
						+ " an uncaught exception: ");
				ex.getTargetException().printStackTrace(GlobalOptions.err);
			}
			return null;
		}
		return new ConstOperator(result);
	}

	public Expression simplifyStringBuffer() {
		if (getClassType().equals(Type.tStringBuffer)) {
			if (isConstructor() && subExpressions[0] instanceof NewOperator) {
				if (methodType.getParameterTypes().length == 0)
					return EMPTYSTRING;
				if (methodType.getParameterTypes().length == 1
						&& methodType.getParameterTypes()[0]
								.equals(Type.tString))
					return subExpressions[1].simplifyString();
			}

			if (!isStatic() && getMethodName().equals("append")
					&& getMethodType().getParameterTypes().length == 1) {

				Expression firstOp = subExpressions[0].simplifyStringBuffer();
				if (firstOp == null)
					return null;

				subExpressions[1] = subExpressions[1].simplifyString();

				if (firstOp == EMPTYSTRING
						&& subExpressions[1].getType().isOfType(Type.tString))
					return subExpressions[1];

				if (firstOp instanceof StringAddOperator
						&& (((Operator) firstOp).getSubExpressions()[0] == EMPTYSTRING))
					firstOp = ((Operator) firstOp).getSubExpressions()[1];

				Expression secondOp = subExpressions[1];
				Type[] paramTypes = new Type[] { Type.tStringBuffer,
						secondOp.getType().getCanonic() };
				if (needsCast(1, paramTypes)) {
					Type castType = methodType.getParameterTypes()[0];
					Operator castOp = new ConvertOperator(castType, castType);
					castOp.addOperand(secondOp);
					secondOp = castOp;
				}
				Operator result = new StringAddOperator();
				result.addOperand(secondOp);
				result.addOperand(firstOp);
				return result;
			}
		}
		return null;
	}

	public Expression simplifyString() {
		if (getMethodName().equals("toString") && !isStatic()
				&& getClassType().equals(Type.tStringBuffer)
				&& subExpressions.length == 1) {
			Expression simple = subExpressions[0].simplifyStringBuffer();
			if (simple != null)
				return simple;
		} else if (getMethodName().equals("valueOf") && isStatic()
				&& getClassType().equals(Type.tString)
				&& subExpressions.length == 1) {

			if (subExpressions[0].getType().isOfType(Type.tString))
				return subExpressions[0];

			Operator op = new StringAddOperator();
			op.addOperand(subExpressions[0]);
			op.addOperand(EMPTYSTRING);
		}
		/* The pizza way (pizza is the compiler of kaffe) */
		else if (getMethodName().equals("concat") && !isStatic()
				&& getClassType().equals(Type.tString)) {

			Expression result = new StringAddOperator();
			Expression right = subExpressions[1].simplify();
			if (right instanceof StringAddOperator) {
				Operator op = (Operator) right;
				if (op.subExpressions != null
						&& op.subExpressions[0] == EMPTYSTRING)
					right = op.subExpressions[1];
			}
			result.addOperand(right);
			result.addOperand(subExpressions[0].simplify());
		} else if ((Options.options & Options.OPTION_DECRYPT) != 0 && isThis()
				&& isStatic() && methodType.getParameterTypes().length == 1
				&& methodType.getParameterTypes()[0].equals(Type.tString)
				&& methodType.getReturnType().equals(Type.tString)) {

			Expression expr = subExpressions[0].simplifyString();
			if (expr instanceof ConstOperator) {
				expr = deobfuscateString((ConstOperator) expr);
				if (expr != null)
					return expr;
			}
		}
		return this;
	}

	public Expression simplifyAccess() {
		if (getMethodAnalyzer() != null) {
			SyntheticAnalyzer synth = getMethodAnalyzer().getSynthetic();
			if (synth != null) {
				int unifyParam = synth.getUnifyParam();
				Expression op = null;
				switch (synth.getKind()) {
				case SyntheticAnalyzer.ACCESSGETFIELD:
					op = new GetFieldOperator(methodAnalyzer, false,
							synth.getReference());
					break;
				case SyntheticAnalyzer.ACCESSGETSTATIC:
					op = new GetFieldOperator(methodAnalyzer, true,
							synth.getReference());
					break;
				case SyntheticAnalyzer.ACCESSPUTFIELD:
				case SyntheticAnalyzer.ACCESSDUPPUTFIELD:
					op = new StoreInstruction(new PutFieldOperator(
							methodAnalyzer, false, synth.getReference()));
					if (synth.getKind() == synth.ACCESSDUPPUTFIELD)
						((StoreInstruction) op).makeNonVoid();
					break;
				case SyntheticAnalyzer.ACCESSPUTSTATIC:
				case SyntheticAnalyzer.ACCESSDUPPUTSTATIC:
					op = new StoreInstruction(new PutFieldOperator(
							methodAnalyzer, true, synth.getReference()));
					if (synth.getKind() == synth.ACCESSDUPPUTSTATIC)
						((StoreInstruction) op).makeNonVoid();
					break;
				case SyntheticAnalyzer.ACCESSMETHOD:
					op = new InvokeOperator(methodAnalyzer, ACCESSSPECIAL,
							synth.getReference());
					break;
				case SyntheticAnalyzer.ACCESSSTATICMETHOD:
					op = new InvokeOperator(methodAnalyzer, STATIC,
							synth.getReference());
					break;
				case SyntheticAnalyzer.ACCESSCONSTRUCTOR:
					if (subExpressions[unifyParam] instanceof ConstOperator
							&& ((ConstOperator) subExpressions[unifyParam])
									.getValue() == null) {
						op = new InvokeOperator(methodAnalyzer, CONSTRUCTOR,
								synth.getReference());
					}
					break;
				}

				if (op != null) {
					if (subExpressions != null) {
						for (int i = subExpressions.length; i-- > 0;) {
							if (i == unifyParam
									&& synth.getKind() == SyntheticAnalyzer.ACCESSCONSTRUCTOR)
								// skip the null param.
								continue;
							op = op.addOperand(subExpressions[i]);
							if (subExpressions[i].getFreeOperandCount() > 0)
								break;
						}
					}
					return op;
				}
			}
		}
		return null;
	}

	public boolean needsCast(int param, Type[] paramTypes) {
		Type realClassType;
		if (methodFlag == STATIC)
			realClassType = classType;
		else if (param == 0) {
			if (paramTypes[0] instanceof NullType)
				return true;
			if (!(paramTypes[0] instanceof ClassInterfacesType && classType instanceof ClassInterfacesType))
				return false;

			ClassInfo clazz = ((ClassInterfacesType) classType).getClassInfo();
			ClassInfo parClazz = ((ClassInterfacesType) paramTypes[0])
					.getClassInfo();
			MethodInfo method = getMethodInfo();
			if (method == null)
				/* This is a NoSuchMethodError */
				return false;
			if (Modifier.isPrivate(method.getModifiers()))
				return parClazz != clazz;
			else if ((method.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC)) == 0) {
				/*
				 * Method is protected. We need a cast if parClazz is in other
				 * package than clazz.
				 */
				int lastDot = clazz.getName().lastIndexOf('.');
				if (lastDot != parClazz.getName().lastIndexOf('.')
						|| !(parClazz.getName().startsWith(clazz.getName()
								.substring(0, lastDot + 1))))
					return true;
			}
			return false;
		} else {
			realClassType = paramTypes[0];
		}

		if (!(realClassType instanceof ClassInterfacesType)) {
			/* Arrays don't have overloaded methods, all okay */
			return false;
		}
		ClassInfo clazz = ((ClassInterfacesType) realClassType).getClassInfo();
		int offset = skippedArgs;

		Type[] myParamTypes = methodType.getParameterTypes();
		if (myParamTypes[param - offset].equals(paramTypes[param])) {
			/* Type at param is okay. */
			return false;
		}
		/*
		 * Now check if there is a conflicting method in this class or a
		 * superclass.
		 */
		while (clazz != null) {
			MethodInfo[] methods = clazz.getMethods();
			next_method: for (int i = 0; i < methods.length; i++) {
				if (!methods[i].getName().equals(methodName))
					/* method name doesn't match */
					continue next_method;

				Type[] otherParamTypes = Type.tMethod(methods[i].getType())
						.getParameterTypes();
				if (otherParamTypes.length != myParamTypes.length) {
					/* parameter count doesn't match */
					continue next_method;
				}

				if (myParamTypes[param - offset].isOfType(Type
						.tSubType(otherParamTypes[param - offset]))) {
					/* cast to myParamTypes cannot resolve any conflicts. */
					continue next_method;
				}
				for (int p = offset; p < paramTypes.length; p++) {
					if (!paramTypes[p].isOfType(Type.tSubType(otherParamTypes[p
							- offset]))) {
						/* No conflict here */
						continue next_method;
					}
				}
				/* There is a conflict that can be resolved by a cast. */
				return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	public Expression simplify() {
		Expression expr = simplifyAccess();
		if (expr != null)
			return expr.simplify();
		expr = simplifyString();
		if (expr != this)
			return expr.simplify();
		return super.simplify();
	}

	/**
	 * We add the named method scoped classes to the declarables, and only
	 * fillDeclarables on the parameters we will print.
	 */
	public void fillDeclarables(Collection used) {
		ClassInfo clazz = getClassInfo();
		InnerClassInfo outer = getOuterClassInfo(clazz);
		ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);

		if ((Options.options & Options.OPTION_ANON) != 0 && outer != null
				&& outer.outer == null && outer.name != null
				&& clazzAna != null && clazzAna.getParent() == methodAnalyzer) {

			/*
			 * This is a named method scope class, declare it. But first declare
			 * all method scoped classes, that are used inside; order does
			 * matter.
			 */
			clazzAna.fillDeclarables(used);
			used.add(clazzAna);
		}

		if (!isConstructor() || isStatic()) {
			super.fillDeclarables(used);
			return;
		}
		int arg = 1;
		int length = subExpressions.length;
		boolean jikesAnonymousInner = false;
		boolean implicitOuterClass = false;

		if ((Options.options & Options.OPTION_ANON) != 0 && clazzAna != null
				&& outer != null && (outer.outer == null || outer.name == null)) {

			OuterValues ov = clazzAna.getOuterValues();
			arg += ov.getCount();
			jikesAnonymousInner = ov.isJikesAnonymousInner();
			implicitOuterClass = ov.isImplicitOuterClass();

			for (int i = 1; i < arg; i++) {
				Expression expr = subExpressions[i];
				if (expr instanceof CheckNullOperator) {
					CheckNullOperator cno = (CheckNullOperator) expr;
					expr = cno.subExpressions[0];
				}
				expr.fillDeclarables(used);
			}

			if (outer.name == null) {
				/* This is an anonymous class */
				ClassInfo superClazz = clazz.getSuperclass();
				ClassInfo[] interfaces = clazz.getInterfaces();
				if (interfaces.length == 1
						&& (superClazz == null || superClazz == ClassInfo.javaLangObject)) {
					clazz = interfaces[0];
				} else {
					clazz = (superClazz != null ? superClazz
							: ClassInfo.javaLangObject);
				}
				outer = getOuterClassInfo(clazz);
			}
		}

		if ((Options.options & Options.OPTION_INNER) != 0 && outer != null
				&& outer.outer != null && outer.name != null
				&& !Modifier.isStatic(outer.modifiers) && !implicitOuterClass
				&& arg < length) {

			Expression outerExpr = jikesAnonymousInner ? subExpressions[--length]
					: subExpressions[arg++];
			if (outerExpr instanceof CheckNullOperator) {
				CheckNullOperator cno = (CheckNullOperator) outerExpr;
				outerExpr = cno.subExpressions[0];
			}
			outerExpr.fillDeclarables(used);
		}
		for (int i = arg; i < length; i++)
			subExpressions[i].fillDeclarables(used);
	}

	/**
	 * We add the named method scoped classes to the declarables, and only
	 * fillDeclarables on the parameters we will print.
	 */
	public void makeDeclaration(Set done) {
		super.makeDeclaration(done);

		if (isConstructor() && !isStatic()
				&& (Options.options & Options.OPTION_ANON) != 0) {
			ClassInfo clazz = getClassInfo();
			InnerClassInfo outer = getOuterClassInfo(clazz);
			ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);
			if (clazzAna != null && outer != null && outer.name == null) {

				/*
				 * call makeDeclaration on the anonymous class, since _we_ will
				 * declare the anonymous class.
				 */
				clazzAna.makeDeclaration(done);
			}
		}
	}

	public int getBreakPenalty() {
		return 5;
	}

	/*
	 * Invokes never equals: they may return different values even if they have
	 * the same parameters.
	 */
	public void dumpExpression(TabbedPrintWriter writer)
			throws java.io.IOException {
		int arg = 1;
		int length = subExpressions.length;

		boolean anonymousNew = false;
		ClassInfo clazz = getClassInfo();
		ClassAnalyzer clazzAna = null;

		Type[] paramTypes = new Type[subExpressions.length];
		for (int i = 0; i < subExpressions.length; i++)
			paramTypes[i] = subExpressions[i].getType().getCanonic();

		writer.startOp(writer.NO_PAREN, 0);
		switch (methodFlag) {
		case CONSTRUCTOR: {

			boolean qualifiedNew = false;
			boolean jikesAnonymousInner = false;
			boolean implicitOuterClass = false;

			/*
			 * Check if this is an anonymous constructor. In this case clazz and
			 * outer will be changed to point to the super class and
			 * anonymousNew will be set.
			 */
			InnerClassInfo outer = getOuterClassInfo(clazz);
			if (outer != null && outer.name == null
					&& (Options.options & Options.OPTION_ANON) != 0)
				anonymousNew = true;
			clazzAna = methodAnalyzer.getClassAnalyzer(clazz);
			if ((~Options.options & (Options.OPTION_ANON | Options.OPTION_CONTRAFO)) == 0
					&& clazzAna != null
					&& outer != null
					&& (outer.outer == null || outer.name == null)) {

				/* This is a method scoped class, skip the outerValues */
				OuterValues ov = clazzAna.getOuterValues();
				arg += ov.getCount();
				jikesAnonymousInner = ov.isJikesAnonymousInner();
				implicitOuterClass = ov.isImplicitOuterClass();

				if (outer.name == null) {
					/* This is an anonymous class */
					ClassInfo superClazz = clazz.getSuperclass();
					ClassInfo[] interfaces = clazz.getInterfaces();
					if (interfaces.length == 1
							&& (superClazz == null || superClazz == ClassInfo.javaLangObject)) {
						clazz = interfaces[0];
					} else {
						if (interfaces.length > 0) {
							writer.print("too many supers in ANONYMOUS ");
						}
						clazz = (superClazz != null ? superClazz
								: ClassInfo.javaLangObject);
					}
					outer = getOuterClassInfo(clazz);
					if (jikesAnonymousInner && outer != null
							&& outer.outer == null && outer.name != null) {
						Expression thisExpr = subExpressions[--length];
						if (thisExpr instanceof CheckNullOperator) {
							CheckNullOperator cno = (CheckNullOperator) thisExpr;
							thisExpr = cno.subExpressions[0];
						}
						if (!(thisExpr instanceof ThisOperator)
								|| (((ThisOperator) thisExpr).getClassInfo() != methodAnalyzer
										.getClazz()))
							writer.print("ILLEGAL ANON CONSTR");
					}
				}
			}

			/*
			 * Check if this is an inner class. It will dump the outer class
			 * expression, except if its default.
			 */
			if (outer != null
					&& outer.outer != null
					&& outer.name != null
					&& !Modifier.isStatic(outer.modifiers)
					&& (~Options.options & (Options.OPTION_INNER | Options.OPTION_CONTRAFO)) == 0) {

				if (implicitOuterClass) {
					/*
					 * Outer class is "this" and is not given explicitly. No
					 * need to print something.
					 */
				} else if (arg < length) {
					Expression outerExpr = jikesAnonymousInner ? subExpressions[--length]
							: subExpressions[arg++];
					if (outerExpr instanceof CheckNullOperator) {
						CheckNullOperator cno = (CheckNullOperator) outerExpr;
						outerExpr = cno.subExpressions[0];
					} else {
						/*
						 * We used to complain about MISSING CHECKNULL here
						 * except for ThisOperators. But javac v8 doesn't seem
						 * to create CHECKNULL ops.
						 */
					}

					if (outerExpr instanceof ThisOperator) {
						Scope scope = writer.getScope(
								((ThisOperator) outerExpr).getClassInfo(),
								Scope.CLASSSCOPE);
						if (writer
								.conflicts(outer.name, scope, Scope.CLASSNAME)) {
							qualifiedNew = true;
							outerExpr.dumpExpression(writer, 950);
							writer.breakOp();
							writer.print(".");
						}
					} else {
						qualifiedNew = true;
						if (outerExpr.getType().getCanonic() instanceof NullType) {
							writer.print("(");
							writer.startOp(writer.EXPL_PAREN, 1);
							writer.print("(");
							writer.printType(Type.tClass(ClassInfo
									.forName(outer.outer)));
							writer.print(") ");
							writer.breakOp();
							outerExpr.dumpExpression(writer, 700);
							writer.endOp();
							writer.print(")");
						} else
							outerExpr.dumpExpression(writer, 950);
						writer.breakOp();
						writer.print(".");
					}
				} else
					writer.print("MISSING OUTEREXPR ");
			}

			if (subExpressions[0] instanceof NewOperator
					&& paramTypes[0].equals(classType)) {
				writer.print("new ");
				if (qualifiedNew)
					writer.print(outer.name);
				else
					writer.printType(Type.tClass(clazz));
				break;
			}

			if (subExpressions[0] instanceof ThisOperator
					&& (((ThisOperator) subExpressions[0]).getClassInfo() == methodAnalyzer
							.getClazz())) {
				if (isThis())
					writer.print("this");
				else
					writer.print("super");
				break;
			}

			writer.print("(");
			writer.startOp(writer.EXPL_PAREN, 0);
			writer.print("(UNCONSTRUCTED)");
			writer.breakOp();
			subExpressions[0].dumpExpression(writer, 700);
			writer.endOp();
			writer.print(")");
			writer.breakOp();
			writer.print(".");
			writer.printType(Type.tClass(clazz));
			break;
		}
		case SPECIAL:
			if (isSuperOrThis()
					&& subExpressions[0] instanceof ThisOperator
					&& (((ThisOperator) subExpressions[0]).getClassInfo() == methodAnalyzer
							.getClazz())) {
				if (!isThis()) {
					/*
					 * We don't have to check if this is the real super class,
					 * as long as ACC_SUPER is set.
					 */
					writer.print("super");
					ClassInfo superClazz = getClassInfo().getSuperclass();
					paramTypes[0] = superClazz == null ? Type.tObject : Type
							.tClass(superClazz);
					writer.breakOp();
					writer.print(".");
				} else {
					/* XXX check if this is a private method. */
				}
			} else if (isThis()) {
				/* XXX check if this is a private method. */
				if (needsCast(0, paramTypes)) {
					writer.print("(");
					writer.startOp(writer.EXPL_PAREN, 1);
					writer.print("(");
					writer.printType(classType);
					writer.print(") ");
					writer.breakOp();
					subExpressions[0].dumpExpression(writer, 700);
					writer.endOp();
					writer.print(")");
					paramTypes[0] = classType;
				} else
					subExpressions[0].dumpExpression(writer, 950);
				writer.breakOp();
				writer.print(".");
			} else {
				writer.print("(");
				writer.startOp(writer.EXPL_PAREN, 0);
				writer.print("(NON VIRTUAL ");
				writer.printType(classType);
				writer.print(") ");
				writer.breakOp();
				subExpressions[0].dumpExpression(writer, 700);
				writer.endOp();
				writer.print(")");
				writer.breakOp();
				writer.print(".");
			}
			writer.print(methodName);
			break;

		case ACCESSSPECIAL:
			/*
			 * Calling a private method in another class. (This is allowed for
			 * inner classes.)
			 */
			if (paramTypes[0].equals(classType))
				subExpressions[0].dumpExpression(writer, 950);
			else {
				writer.print("(");
				writer.startOp(writer.EXPL_PAREN, 0);
				writer.print("(");
				writer.printType(classType);
				writer.print(") ");
				writer.breakOp();
				paramTypes[0] = classType;
				subExpressions[0].dumpExpression(writer, 700);
				writer.endOp();
				writer.print(")");
			}
			writer.breakOp();
			writer.print(".");
			writer.print(methodName);
			break;

		case STATIC: {
			arg = 0;
			Scope scope = writer.getScope(getClassInfo(), Scope.CLASSSCOPE);
			if (scope == null
					|| writer.conflicts(methodName, scope, Scope.METHODNAME)) {
				writer.printType(classType);
				writer.breakOp();
				writer.print(".");
			}
			writer.print(methodName);
			break;
		}

		case VIRTUAL:
			if (subExpressions[0] instanceof ThisOperator) {
				ThisOperator thisOp = (ThisOperator) subExpressions[0];
				Scope scope = writer.getScope(thisOp.getClassInfo(),
						Scope.CLASSSCOPE);
				if (writer.conflicts(methodName, scope, Scope.METHODNAME)
						|| (/*
							 * This method is inherited from the parent of an
							 * outer class, or it is inherited from the parent
							 * of this class and there is a conflicting method
							 * in some outer class.
							 */
						getMethodAnalyzer() == null && (!isThis() || writer
								.conflicts(methodName, null,
										Scope.NOSUPERMETHODNAME)))) {
					thisOp.dumpExpression(writer, 950);
					writer.breakOp();
					writer.print(".");
				}
			} else {
				if (needsCast(0, paramTypes)) {
					writer.print("(");
					writer.startOp(writer.EXPL_PAREN, 1);
					writer.print("(");
					writer.printType(classType);
					writer.print(") ");
					writer.breakOp();
					subExpressions[0].dumpExpression(writer, 700);
					writer.endOp();
					writer.print(")");
					paramTypes[0] = classType;
				} else
					subExpressions[0].dumpExpression(writer, 950);
				writer.breakOp();
				writer.print(".");
			}
			writer.print(methodName);
		}

		writer.endOp();
		writer.breakOp();
		if ((Options.outputStyle & Options.GNU_SPACING) != 0)
			writer.print(" ");
		writer.print("(");
		writer.startOp(writer.EXPL_PAREN, 0);
		boolean first = true;
		int offset = skippedArgs;
		while (arg < length) {
			if (!first) {
				writer.print(", ");
				writer.breakOp();
			} else
				first = false;
			int priority = 0;
			if (needsCast(arg, paramTypes)) {
				Type castType = methodType.getParameterTypes()[arg - offset];
				writer.startOp(writer.IMPL_PAREN, 1);
				writer.print("(");
				writer.printType(castType);
				writer.print(") ");
				writer.breakOp();
				paramTypes[arg] = castType;
				priority = 700;
			}
			subExpressions[arg++].dumpExpression(writer, priority);
			if (priority == 700)
				writer.endOp();
		}
		writer.endOp();
		writer.print(")");

		if (anonymousNew) {
			/*
			 * If this was an anonymous constructor call, we must now dump the
			 * source code of the anonymous class.
			 */
			Object state = writer.saveOps();
			writer.openBraceClass();
			writer.tab();
			clazzAna.dumpBlock(writer);
			writer.untab();
			writer.closeBraceClass();
			writer.restoreOps(state);
		}
	}

	public boolean opEquals(Operator o) {
		if (o instanceof InvokeOperator) {
			InvokeOperator i = (InvokeOperator) o;
			return classType.equals(i.classType)
					&& methodName.equals(i.methodName)
					&& methodType.equals(i.methodType)
					&& methodFlag == i.methodFlag;
		}
		return false;
	}
}
