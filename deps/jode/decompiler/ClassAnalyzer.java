/* ClassAnalyzer Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ClassAnalyzer.java.in,v 4.5.2.3 2002/05/28 17:34:03 hoenicke Exp $
 */

package jode.decompiler;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;
import java.util.Vector;

import jode.GlobalOptions;
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.InnerClassInfo;
import jode.bytecode.MethodInfo;
import jode.expr.Expression;
import jode.expr.ThisOperator;
import jode.flow.StructuredBlock;
import jode.flow.TransformConstructors;
import jode.type.MethodType;
import jode.type.Type;
import jode.util.SimpleSet;


public class ClassAnalyzer implements Scope, Declarable, ClassDeclarer {
	ImportHandler imports;
	ClassInfo clazz;
	ClassDeclarer parent;
	ProgressListener progressListener;

	/**
	 * The complexity for initi#alizing a class.
	 */
	private static double INITIALIZE_COMPLEXITY = 0.03;
	/**
	 * The minimal visible complexity.
	 */
	private static double STEP_COMPLEXITY = 0.03;
	/**
	 * The value of the strictfp modifier. JDK1.1 doesn't define it.
	 */
	private static int STRICTFP = 0x800;

	double methodComplexity = 0.0;
	double innerComplexity = 0.0;

	String name;
	StructuredBlock[] blockInitializers;
	FieldAnalyzer[] fields;
	MethodAnalyzer[] methods;
	ClassAnalyzer[] inners;
	int modifiers;

	TransformConstructors constrAna;
	MethodAnalyzer staticConstructor;
	MethodAnalyzer[] constructors;

	OuterValues outerValues;

	public ClassAnalyzer(ClassDeclarer parent, ClassInfo clazz,
			ImportHandler imports, Expression[] outerValues) {
		clazz.loadInfo(clazz.MOSTINFO);
		this.parent = parent;
		this.clazz = clazz;
		this.imports = imports;
		if (outerValues != null)
			this.outerValues = new OuterValues(this, outerValues);
		modifiers = clazz.getModifiers();

		if (parent != null) {
			InnerClassInfo[] outerInfos = clazz.getOuterClasses();
			if (outerInfos[0].outer == null || outerInfos[0].name == null) {
				if (parent instanceof ClassAnalyzer)
					throw new jode.AssertError(
							"ClassInfo Attributes are inconsistent: "
									+ clazz.getName());
			} else {
				if (!(parent instanceof ClassAnalyzer)
						|| !(((ClassAnalyzer) parent).clazz.getName()
								.equals(outerInfos[0].outer))
						|| outerInfos[0].name == null)
					throw new jode.AssertError(
							"ClassInfo Attributes are inconsistent: "
									+ clazz.getName());
			}
			name = outerInfos[0].name;
			modifiers = outerInfos[0].modifiers;
		} else {
			name = clazz.getName();
			int dot = name.lastIndexOf('.');
			if (dot >= 0)
				name = name.substring(dot + 1);
		}
	}

	public ClassAnalyzer(ClassDeclarer parent, ClassInfo clazz,
			ImportHandler imports) {
		this(parent, clazz, imports, null);
	}

	public ClassAnalyzer(ClassInfo clazz, ImportHandler imports) {
		this(null, clazz, imports);
	}

	public final boolean isStatic() {
		return Modifier.isStatic(modifiers);
	}

	public final boolean isStrictFP() {
		return (modifiers & STRICTFP) != 0;
	}

	public FieldAnalyzer getField(int index) {
		return fields[index];
	}

	public int getFieldIndex(String fieldName, Type fieldType) {
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getName().equals(fieldName)
					&& fields[i].getType().equals(fieldType))
				return i;
		}
		return -1;
	}

	public MethodAnalyzer getMethod(String methodName, MethodType methodType) {
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(methodName)
					&& methods[i].getType().equals(methodType))
				return methods[i];
		}
		return null;
	}

	public int getModifiers() {
		return modifiers;
	}

	public ClassDeclarer getParent() {
		return parent;
	}

	public void setParent(ClassDeclarer newParent) {
		this.parent = newParent;
	}

	public ClassInfo getClazz() {
		return clazz;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public OuterValues getOuterValues() {
		return outerValues;
	}

	public void addBlockInitializer(int index, StructuredBlock initializer) {
		if (blockInitializers[index] == null)
			blockInitializers[index] = initializer;
		else
			blockInitializers[index].appendBlock(initializer);
	}

	public void initialize() {
		FieldInfo[] finfos = clazz.getFields();
		MethodInfo[] minfos = clazz.getMethods();
		InnerClassInfo[] innerInfos = clazz.getInnerClasses();

		if (finfos == null) {
			/*
			 * This means that the class could not be loaded. give up.
			 */
			return;
		}

		if ((Options.options & Options.OPTION_INNER) != 0 && innerInfos != null) {
			/* Create inner classes */
			Expression[] outerThis = new Expression[] { new ThisOperator(clazz) };

			int innerCount = innerInfos.length;
			inners = new ClassAnalyzer[innerCount];
			for (int i = 0; i < innerCount; i++) {
				ClassInfo ci = ClassInfo.forName(innerInfos[i].inner);
				inners[i] = new ClassAnalyzer(this, ci, imports,
						Modifier.isStatic(innerInfos[i].modifiers) ? null
								: outerThis);
			}
		} else
			inners = new ClassAnalyzer[0];

		fields = new FieldAnalyzer[finfos.length];
		methods = new MethodAnalyzer[minfos.length];
		blockInitializers = new StructuredBlock[finfos.length + 1];
		for (int j = 0; j < finfos.length; j++)
			fields[j] = new FieldAnalyzer(this, finfos[j], imports);

		staticConstructor = null;
		Vector constrVector = new Vector();
		for (int j = 0; j < methods.length; j++) {
			methods[j] = new MethodAnalyzer(this, minfos[j], imports);

			if (methods[j].isConstructor()) {
				if (methods[j].isStatic())
					staticConstructor = methods[j];
				else
					constrVector.addElement(methods[j]);

				/*
				 * Java bytecode can't have strictfp modifier for classes, while
				 * java can't have strictfp modifier for constructors. We handle
				 * the difference here.
				 * 
				 * If only a few constructors are strictfp and the methods
				 * aren't this would add too much strictfp, but that isn't
				 * really dangerous.
				 */
				if (methods[j].isStrictFP())
					modifiers |= STRICTFP;
			}
			methodComplexity += methods[j].getComplexity();
		}

		constructors = new MethodAnalyzer[constrVector.size()];
		constrVector.copyInto(constructors);

		// initialize the inner classes.
		for (int j = 0; j < inners.length; j++) {
			inners[j].initialize();
			innerComplexity += inners[j].getComplexity();
		}
	}

	/**
	 * Gets the complexity of this class. Must be called after it has been
	 * initialized. This is used for a nice progress bar.
	 */
	public double getComplexity() {
		return (methodComplexity + innerComplexity);
	}

	public void analyze(ProgressListener pl, double done, double scale) {
		if (GlobalOptions.verboseLevel > 0)
			GlobalOptions.err.println("Class " + name);
		double subScale = scale / methodComplexity;
		if (pl != null)
			pl.updateProgress(done, name);

		imports.useClass(clazz);
		if (clazz.getSuperclass() != null)
			imports.useClass(clazz.getSuperclass());
		ClassInfo[] interfaces = clazz.getInterfaces();
		for (int j = 0; j < interfaces.length; j++)
			imports.useClass(interfaces[j]);

		if (fields == null) {
			/*
			 * This means that the class could not be loaded. give up.
			 */
			return;
		}

		// First analyze constructors and synthetic fields:
		constrAna = null;
		if (constructors.length > 0) {
			for (int j = 0; j < constructors.length; j++) {
				if (pl != null) {
					double constrCompl = constructors[j].getComplexity()
							* subScale;
					if (constrCompl > STEP_COMPLEXITY)
						constructors[j].analyze(pl, done, constrCompl);
					else {
						pl.updateProgress(done, name);
						constructors[j].analyze(null, 0.0, 0.0);
					}
					done += constrCompl;
				} else
					constructors[j].analyze(null, 0.0, 0.0);
			}
			constrAna = new TransformConstructors(this, false, constructors);
			constrAna.removeSynthInitializers();
		}
		if (staticConstructor != null) {
			if (pl != null) {
				double constrCompl = staticConstructor.getComplexity()
						* subScale;
				if (constrCompl > STEP_COMPLEXITY)
					staticConstructor.analyze(pl, done, constrCompl);
				else {
					pl.updateProgress(done, name);
					staticConstructor.analyze(null, 0.0, 0.0);
				}
				done += constrCompl;
			} else
				staticConstructor.analyze(null, 0.0, 0.0);
		}

		// If output should be immediate, we delay analyzation to output.
		// Note that this may break anonymous classes, but the user
		// has been warned.
		if ((Options.options & Options.OPTION_IMMEDIATE) != 0)
			return;

		// Analyze fields
		for (int j = 0; j < fields.length; j++)
			fields[j].analyze();

		// Now analyze remaining methods.
		for (int j = 0; j < methods.length; j++) {
			if (!methods[j].isConstructor())
				if (pl != null) {
					double methodCompl = methods[j].getComplexity() * subScale;
					if (methodCompl > STEP_COMPLEXITY)
						methods[j].analyze(pl, done, methodCompl);
					else {
						pl.updateProgress(done, methods[j].getName());
						methods[j].analyze(null, 0.0, 0.0);
					}
					done += methodCompl;
				} else
					methods[j].analyze(null, 0.0, 0.0);
		}
	}

	public void analyzeInnerClasses(ProgressListener pl, double done,
			double scale) {
		double subScale = scale / innerComplexity;
		// If output should be immediate, we delay analyzation to output.
		// Note that this may break anonymous classes, but the user
		// has been warned.
		if ((Options.options & Options.OPTION_IMMEDIATE) != 0)
			return;

		// Now analyze the inner classes.
		for (int j = 0; j < inners.length; j++) {
			if (pl != null) {
				double innerCompl = inners[j].getComplexity() * subScale;
				if (innerCompl > STEP_COMPLEXITY) {
					double innerscale = subScale * inners[j].methodComplexity;
					inners[j].analyze(pl, done, innerscale);
					inners[j].analyzeInnerClasses(null, done + innerscale,
							innerCompl - innerscale);
				} else {
					pl.updateProgress(done, inners[j].name);
					inners[j].analyze(null, 0.0, 0.0);
					inners[j].analyzeInnerClasses(null, 0.0, 0.0);
				}
				done += innerCompl;
			} else {
				inners[j].analyze(null, 0.0, 0.0);
				inners[j].analyzeInnerClasses(null, 0.0, 0.0);
			}
		}

		// Now analyze the method scoped classes.
		for (int j = 0; j < methods.length; j++)
			methods[j].analyzeInnerClasses();
	}

	public void makeDeclaration(Set done) {
		if (constrAna != null)
			constrAna.transform();
		if (staticConstructor != null) {
			new TransformConstructors(this, true,
					new MethodAnalyzer[] { staticConstructor }).transform();
		}

		// If output should be immediate, we delay analyzation to output.
		// Note that this may break anonymous classes, but the user
		// has been warned.
		if ((Options.options & Options.OPTION_IMMEDIATE) != 0)
			return;

		for (int j = 0; j < fields.length; j++)
			fields[j].makeDeclaration(done);
		for (int j = 0; j < inners.length; j++)
			inners[j].makeDeclaration(done);
		for (int j = 0; j < methods.length; j++)
			methods[j].makeDeclaration(done);
	}

	public void dumpDeclaration(TabbedPrintWriter writer) throws IOException {
		dumpDeclaration(writer, null, 0.0, 0.0);
	}

	public void dumpDeclaration(TabbedPrintWriter writer, ProgressListener pl,
			double done, double scale) throws IOException {
		if (fields == null) {
			/*
			 * This means that the class could not be loaded. give up.
			 */
			return;
		}

		writer.startOp(writer.NO_PAREN, 0);
		/* Clear the SUPER bit, which is also used as SYNCHRONIZED bit. */
		int modifiedModifiers = modifiers & ~(Modifier.SYNCHRONIZED | STRICTFP);
		if (clazz.isInterface())
			/* interfaces are implicitily abstract */
			modifiedModifiers &= ~Modifier.ABSTRACT;
		if (parent instanceof MethodAnalyzer) {
			/* method scope classes are implicitly private */
			modifiedModifiers &= ~Modifier.PRIVATE;
			/* anonymous classes are implicitly final */
			if (name == null)
				modifiedModifiers &= ~Modifier.FINAL;
		}
		String modif = Modifier.toString(modifiedModifiers);
		if (modif.length() > 0)
			writer.print(modif + " ");
		if (isStrictFP()) {
			/*
			 * The STRICTFP modifier is set. We handle it, since
			 * java.lang.reflect.Modifier is too dumb.
			 */
			writer.print("strictfp ");
		}
		/* interface is in modif */
		if (!clazz.isInterface())
			writer.print("class ");
		writer.print(name);
		ClassInfo superClazz = clazz.getSuperclass();
		if (superClazz != null && superClazz != ClassInfo.javaLangObject) {
			writer.breakOp();
			writer.print(" extends "
					+ (writer.getClassString(superClazz, Scope.CLASSNAME)));
		}
		ClassInfo[] interfaces = clazz.getInterfaces();
		if (interfaces.length > 0) {
			writer.breakOp();
			writer.print(clazz.isInterface() ? " extends " : " implements ");
			writer.startOp(writer.EXPL_PAREN, 1);
			for (int i = 0; i < interfaces.length; i++) {
				if (i > 0) {
					writer.print(", ");
					writer.breakOp();
				}
				writer.print(writer.getClassString(interfaces[i],
						Scope.CLASSNAME));
			}
			writer.endOp();
		}
		writer.println();

		writer.openBraceClass();
		writer.tab();
		dumpBlock(writer, pl, done, scale);
		writer.untab();
		writer.closeBraceClass();
	}

	public void dumpBlock(TabbedPrintWriter writer) throws IOException {
		dumpBlock(writer, null, 0.0, 0.0);
	}

	public void dumpBlock(TabbedPrintWriter writer, ProgressListener pl,
			double done, double scale) throws IOException {
		double subScale = scale / getComplexity();
		writer.pushScope(this);
		boolean needFieldNewLine = false;
		boolean needNewLine = false;
		Set declared = null;
		if ((Options.options & Options.OPTION_IMMEDIATE) != 0)
			declared = new SimpleSet();
		for (int i = 0; i < fields.length; i++) {
			if (blockInitializers[i] != null) {
				if (needNewLine)
					writer.println();
				writer.openBrace();
				writer.tab();
				blockInitializers[i].dumpSource(writer);
				writer.untab();
				writer.closeBrace();
				needFieldNewLine = needNewLine = true;
			}
			if ((Options.options & Options.OPTION_IMMEDIATE) != 0) {
				// We now do the analyzation we skipped before.
				fields[i].analyze();
				fields[i].makeDeclaration(declared);
			}
			if (fields[i].skipWriting())
				continue;
			if (needFieldNewLine)
				writer.println();
			fields[i].dumpSource(writer);
			needNewLine = true;
		}
		if (blockInitializers[fields.length] != null) {
			if (needNewLine)
				writer.println();
			writer.openBrace();
			writer.tab();
			blockInitializers[fields.length].dumpSource(writer);
			writer.untab();
			writer.closeBrace();
			needNewLine = true;
		}
		for (int i = 0; i < inners.length; i++) {
			if (needNewLine)
				writer.println();

			if ((Options.options & Options.OPTION_IMMEDIATE) != 0) {
				// We now do the analyzation we skipped before.
				inners[i].analyze(null, 0.0, 0.0);
				inners[i].analyzeInnerClasses(null, 0.0, 0.0);
				inners[i].makeDeclaration(declared);
			}

			if (pl != null) {
				double innerCompl = inners[i].getComplexity() * subScale;
				if (innerCompl > STEP_COMPLEXITY)
					inners[i].dumpSource(writer, pl, done, innerCompl);
				else {
					pl.updateProgress(done, name);
					inners[i].dumpSource(writer);
				}
				done += innerCompl;
			} else
				inners[i].dumpSource(writer);
			needNewLine = true;
		}
		for (int i = 0; i < methods.length; i++) {
			if ((Options.options & Options.OPTION_IMMEDIATE) != 0) {
				// We now do the analyzation we skipped before.
				if (!methods[i].isConstructor())
					methods[i].analyze(null, 0.0, 0.0);
				methods[i].analyzeInnerClasses();
				methods[i].makeDeclaration(declared);
			}

			if (methods[i].skipWriting())
				continue;
			if (needNewLine)
				writer.println();

			if (pl != null) {
				double methodCompl = methods[i].getComplexity() * subScale;
				pl.updateProgress(done, methods[i].getName());
				methods[i].dumpSource(writer);
				done += methodCompl;
			} else
				methods[i].dumpSource(writer);
			needNewLine = true;
		}
		writer.popScope();
		clazz.dropInfo(clazz.KNOWNATTRIBS | clazz.UNKNOWNATTRIBS);
	}

	public void dumpSource(TabbedPrintWriter writer) throws IOException {
		dumpSource(writer, null, 0.0, 0.0);
	}

	public void dumpSource(TabbedPrintWriter writer, ProgressListener pl,
			double done, double scale) throws IOException {
		dumpDeclaration(writer, pl, done, scale);
		writer.println();
	}

	public void dumpJavaFile(TabbedPrintWriter writer) throws IOException {
		dumpJavaFile(writer, null);
	}

	public void dumpJavaFile(TabbedPrintWriter writer, ProgressListener pl)
			throws IOException {
		imports.init(clazz.getName());
		LocalInfo.init();
		initialize();
		double done = 0.05;
		double scale = (0.75) * methodComplexity
				/ (methodComplexity + innerComplexity);
		analyze(pl, INITIALIZE_COMPLEXITY, scale);
		done += scale;
		analyzeInnerClasses(pl, done, 0.8 - done);
		makeDeclaration(new SimpleSet());
		imports.dumpHeader(writer);
		dumpSource(writer, pl, 0.8, 0.2);
		if (pl != null)
			pl.updateProgress(1.0, name);
	}

	public boolean isScopeOf(Object obj, int scopeType) {
		if (clazz.equals(obj) && scopeType == CLASSSCOPE)
			return true;
		return false;
	}

	static int serialnr = 0;

	public void makeNameUnique() {
		name = name + "_" + serialnr++ + "_";
	}

	public boolean conflicts(String name, int usageType) {
		return conflicts(clazz, name, usageType);
	}

	private static boolean conflicts(ClassInfo info, String name, int usageType) {
		while (info != null) {
			if (usageType == NOSUPERMETHODNAME || usageType == METHODNAME) {
				MethodInfo[] minfos = info.getMethods();
				for (int i = 0; i < minfos.length; i++)
					if (minfos[i].getName().equals(name))
						return true;
			}
			if (usageType == NOSUPERFIELDNAME || usageType == FIELDNAME
					|| usageType == AMBIGUOUSNAME) {
				FieldInfo[] finfos = info.getFields();
				for (int i = 0; i < finfos.length; i++) {
					if (finfos[i].getName().equals(name))
						return true;
				}
			}
			if (usageType == CLASSNAME || usageType == AMBIGUOUSNAME) {
				InnerClassInfo[] iinfos = info.getInnerClasses();
				if (iinfos != null) {
					for (int i = 0; i < iinfos.length; i++) {
						if (iinfos[i].name.equals(name))
							return true;
					}
				}
			}
			if (usageType == NOSUPERFIELDNAME || usageType == NOSUPERMETHODNAME)
				return false;

			ClassInfo[] ifaces = info.getInterfaces();
			for (int i = 0; i < ifaces.length; i++)
				if (conflicts(ifaces[i], name, usageType))
					return true;
			info = info.getSuperclass();
		}
		return false;
	}

	/**
	 * Get the class analyzer for the given class info. This searches the method
	 * scoped/anonymous classes in this method and all outer methods and the
	 * outer classes for the class analyzer.
	 * 
	 * @param cinfo
	 *            the classinfo for which the analyzer is searched.
	 * @return the class analyzer, or null if there is not an outer class that
	 *         equals cinfo, and not a method scope/inner class in an outer
	 *         method.
	 */
	public ClassAnalyzer getClassAnalyzer(ClassInfo cinfo) {
		if (cinfo == getClazz())
			return this;
		if (parent == null)
			return null;
		return getParent().getClassAnalyzer(cinfo);
	}

	/**
	 * Get the class analyzer for the given inner class.
	 * 
	 * @param name
	 *            the short name of the inner class
	 * @return the class analyzer, or null if there is no inner class with the
	 *         given name.
	 */
	public ClassAnalyzer getInnerClassAnalyzer(String name) {
		/** require name != null; **/
		int innerCount = inners.length;
		for (int i = 0; i < innerCount; i++) {
			if (inners[i].name.equals(name))
				return inners[i];
		}
		return null;
	}

	/**
	 * We add the named method scoped classes to the declarables.
	 */
	public void fillDeclarables(Collection used) {
		for (int j = 0; j < methods.length; j++)
			methods[j].fillDeclarables(used);
	}

	public void addClassAnalyzer(ClassAnalyzer clazzAna) {
		if (parent != null)
			parent.addClassAnalyzer(clazzAna);
	}

	public String toString() {
		return getClass().getName() + "[" + getClazz() + "]";
	}
}
