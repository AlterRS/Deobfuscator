/* MethodIdentifier Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: MethodIdentifier.java.in,v 1.7.2.2 2002/05/28 17:34:14 hoenicke Exp $
 */

package jode.obfuscator;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;

import jode.GlobalOptions;
import jode.bytecode.BytecodeInfo;
import jode.bytecode.Handler;
import jode.bytecode.Instruction;
import jode.bytecode.MethodInfo;
import jode.bytecode.Opcodes;


public class MethodIdentifier extends Identifier implements Opcodes {
	ClassIdentifier clazz;
	MethodInfo info;
	String name;
	String type;

	boolean globalSideEffects;
	BitSet localSideEffects;

	/**
	 * The code analyzer of this method, or null if there isn't any.
	 */
	CodeAnalyzer codeAnalyzer;

	public MethodIdentifier(ClassIdentifier clazz, MethodInfo info) {
		super(info.getName());
		this.name = info.getName();
		this.type = info.getType();
		this.clazz = clazz;
		this.info = info;

		BytecodeInfo bytecode = info.getBytecode();
		if (bytecode != null) {
			if ((Main.stripping & Main.STRIP_LVT) != 0)
				info.getBytecode().setLocalVariableTable(null);
			if ((Main.stripping & Main.STRIP_LNT) != 0)
				info.getBytecode().setLineNumberTable(null);
			codeAnalyzer = Main.getClassBundle().getCodeAnalyzer();

			CodeTransformer[] trafos = Main.getClassBundle()
					.getPreTransformers();
			for (int i = 0; i < trafos.length; i++) {
				trafos[i].transformCode(bytecode);
			}
			info.setBytecode(bytecode);
		}
	}

	public Iterator getChilds() {
		return Collections.EMPTY_LIST.iterator();
	}

	public void setSingleReachable() {
		super.setSingleReachable();
		Main.getClassBundle().analyzeIdentifier(this);
	}

	public void analyze() {
		if (GlobalOptions.verboseLevel > 1)
			GlobalOptions.err.println("Analyze: " + this);

		String type = getType();
		int index = type.indexOf('L');
		while (index != -1) {
			int end = type.indexOf(';', index);
			Main.getClassBundle().reachableClass(
					type.substring(index + 1, end).replace('/', '.'));
			index = type.indexOf('L', end);
		}

		String[] exceptions = info.getExceptions();
		if (exceptions != null) {
			for (int i = 0; i < exceptions.length; i++)
				Main.getClassBundle().reachableClass(exceptions[i]);
		}

		BytecodeInfo code = info.getBytecode();
		if (code != null)
			codeAnalyzer.analyzeCode(this, code);
	}

	public Identifier getParent() {
		return clazz;
	}

	public String getFullName() {
		return clazz.getFullName() + "." + getName() + "." + getType();
	}

	public String getFullAlias() {
		return clazz.getFullAlias() + "." + getAlias() + "."
				+ Main.getClassBundle().getTypeAlias(getType());
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public int getModifiers() {
		return info.getModifiers();
	}

	public boolean conflicting(String newAlias) {
		return clazz.methodConflicts(this, newAlias);
	}

	public String toString() {
		return "MethodIdentifier " + getFullName();
	}

	public boolean hasGlobalSideEffects() {
		return globalSideEffects;
	}

	public boolean getLocalSideEffects(int paramNr) {
		return globalSideEffects || localSideEffects.get(paramNr);
	}

	public void setGlobalSideEffects() {
		globalSideEffects = true;
	}

	public void setLocalSideEffects(int paramNr) {
		localSideEffects.set(paramNr);
	}

	/**
	 * This method does the code transformation. This include
	 * <ul>
	 * <li>new slot distribution for locals</li>
	 * <li>obfuscating transformation of flow</li>
	 * <li>renaming field, method and class references</li>
	 * </ul>
	 */
	boolean wasTransformed = false;

	public void doTransformations() {
		if (wasTransformed)
			throw new jode.AssertError(
					"doTransformation called on transformed method");
		wasTransformed = true;
		info.setName(getAlias());
		ClassBundle bundle = Main.getClassBundle();
		info.setType(bundle.getTypeAlias(type));
		if (codeAnalyzer != null) {
			BytecodeInfo bytecode = info.getBytecode();
			try {
				/*
				 * Only run analyzer on reachable methods. Other methods weren't
				 * analyzed, so running analyzer on them is wrong.
				 */
				if (isReachable())
					codeAnalyzer.transformCode(bytecode);
				CodeTransformer[] trafos = bundle.getPostTransformers();
				for (int i = 0; i < trafos.length; i++) {
					trafos[i].transformCode(bytecode);
				}
			} catch (RuntimeException ex) {
				ex.printStackTrace(GlobalOptions.err);
				bytecode.dumpCode(GlobalOptions.err);
			}

			for (Iterator iter = bytecode.getInstructions().iterator(); iter
					.hasNext();) {
				Instruction instr = (Instruction) iter.next();
				switch (instr.getOpcode()) {
				case opc_invokespecial:
				case opc_invokestatic:
				case opc_invokeinterface:
				case opc_invokevirtual: {
					instr.setReference(Main.getClassBundle().getReferenceAlias(
							instr.getReference()));
					break;
				}
				case opc_putstatic:
				case opc_putfield:
				case opc_getstatic:
				case opc_getfield: {
					instr.setReference(Main.getClassBundle().getReferenceAlias(
							instr.getReference()));
					break;
				}
				case opc_new:
				case opc_checkcast:
				case opc_instanceof:
				case opc_multianewarray: {
					instr.setClazzType(Main.getClassBundle().getTypeAlias(
							instr.getClazzType()));
					break;
				}
				}
			}

			Handler[] handlers = bytecode.getExceptionHandlers();
			for (int i = 0; i < handlers.length; i++) {
				if (handlers[i].type != null) {
					ClassIdentifier ci = Main.getClassBundle()
							.getClassIdentifier(handlers[i].type);
					if (ci != null)
						handlers[i].type = ci.getFullAlias();
				}
			}
			info.setBytecode(bytecode);
		}

		String[] exceptions = info.getExceptions();
		if (exceptions != null) {
			for (int i = 0; i < exceptions.length; i++) {
				ClassIdentifier ci = Main.getClassBundle().getClassIdentifier(
						exceptions[i]);
				if (ci != null)
					exceptions[i] = ci.getFullAlias();
			}
		}
	}
}
