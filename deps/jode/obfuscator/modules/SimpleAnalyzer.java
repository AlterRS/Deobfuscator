/* SimpleAnalyzer Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: SimpleAnalyzer.java.in,v 1.1.2.2 2002/05/28 17:34:17 hoenicke Exp $
 */

package jode.obfuscator.modules;

import java.util.Iterator;
import java.util.ListIterator;

import jode.GlobalOptions;
import jode.bytecode.BytecodeInfo;
import jode.bytecode.ClassInfo;
import jode.bytecode.Handler;
import jode.bytecode.Instruction;
import jode.bytecode.Opcodes;
import jode.bytecode.Reference;
import jode.bytecode.TypeSignature;
import jode.obfuscator.ClassIdentifier;
import jode.obfuscator.CodeAnalyzer;
import jode.obfuscator.FieldIdentifier;
import jode.obfuscator.Identifier;
import jode.obfuscator.Main;
import jode.obfuscator.MethodIdentifier;


public class SimpleAnalyzer implements CodeAnalyzer, Opcodes {

	private ClassInfo canonizeIfaceRef(ClassInfo clazz, Reference ref) {
		while (clazz != null) {
			if (clazz.findMethod(ref.getName(), ref.getType()) != null)
				return clazz;
			ClassInfo[] ifaces = clazz.getInterfaces();
			for (int i = 0; i < ifaces.length; i++) {
				ClassInfo realClass = canonizeIfaceRef(ifaces[i], ref);
				if (realClass != null)
					return realClass;
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	public Identifier canonizeReference(Instruction instr) {
		Reference ref = instr.getReference();
		Identifier ident = Main.getClassBundle().getIdentifier(ref);
		String clName = ref.getClazz();
		String realClazzName;
		if (ident != null) {
			ClassIdentifier clazz = (ClassIdentifier) ident.getParent();
			realClazzName = "L" + (clazz.getFullName().replace('.', '/')) + ";";
		} else {
			/*
			 * We have to look at the ClassInfo's instead, to point to the right
			 * method.
			 */
			ClassInfo clazz;
			if (clName.charAt(0) == '[') {
				/*
				 * Arrays don't define new methods (well clone(), but that can
				 * be ignored).
				 */
				clazz = ClassInfo.javaLangObject;
			} else {
				clazz = ClassInfo.forName(clName.substring(1,
						clName.length() - 1).replace('/', '.'));
			}
			if (instr.getOpcode() == opc_invokeinterface) {
				clazz = canonizeIfaceRef(clazz, ref);
			} else if (instr.getOpcode() >= opc_invokevirtual) {
				while (clazz != null
						&& clazz.findMethod(ref.getName(), ref.getType()) == null)
					clazz = clazz.getSuperclass();
			} else {
				while (clazz != null
						&& clazz.findField(ref.getName(), ref.getType()) == null)
					clazz = clazz.getSuperclass();
			}

			if (clazz == null) {
				GlobalOptions.err.println("WARNING: Can't find reference: "
						+ ref);
				realClazzName = clName;
			} else
				realClazzName = "L" + clazz.getName().replace('.', '/') + ";";
		}
		if (!realClazzName.equals(ref.getClazz())) {
			ref = Reference.getReference(realClazzName, ref.getName(),
					ref.getType());
			instr.setReference(ref);
		}
		return ident;
	}

	/**
	 * Reads the opcodes out of the code info and determine its references
	 * 
	 * @return an enumeration of the references.
	 */
	public void analyzeCode(MethodIdentifier m, BytecodeInfo bytecode) {
		for (Iterator iter = bytecode.getInstructions().iterator(); iter
				.hasNext();) {
			Instruction instr = (Instruction) iter.next();
			switch (instr.getOpcode()) {
			case opc_checkcast:
			case opc_instanceof:
			case opc_multianewarray: {
				String clName = instr.getClazzType();
				int i = 0;
				while (i < clName.length() && clName.charAt(i) == '[')
					i++;
				if (i < clName.length() && clName.charAt(i) == 'L') {
					clName = clName.substring(i + 1, clName.length() - 1)
							.replace('/', '.');
					Main.getClassBundle().reachableClass(clName);
				}
				break;
			}
			case opc_invokespecial:
			case opc_invokestatic:
			case opc_invokeinterface:
			case opc_invokevirtual:
			case opc_putstatic:
			case opc_putfield:
				m.setGlobalSideEffects();
				/* fall through */
			case opc_getstatic:
			case opc_getfield: {
				Identifier ident = canonizeReference(instr);
				if (ident != null) {
					if (instr.getOpcode() == opc_putstatic
							|| instr.getOpcode() == opc_putfield) {
						FieldIdentifier fi = (FieldIdentifier) ident;
						if (fi != null && !fi.isNotConstant())
							fi.setNotConstant();
					} else if (instr.getOpcode() == opc_invokevirtual
							|| instr.getOpcode() == opc_invokeinterface) {
						((ClassIdentifier) ident.getParent())
								.reachableReference(instr.getReference(), true);
					} else {
						ident.setReachable();
					}
				}
				break;
			}
			}
		}

		Handler[] handlers = bytecode.getExceptionHandlers();
		for (int i = 0; i < handlers.length; i++) {
			if (handlers[i].type != null)
				Main.getClassBundle().reachableClass(handlers[i].type);
		}
	}

	public void transformCode(BytecodeInfo bytecode) {
		for (ListIterator iter = bytecode.getInstructions().listIterator(); iter
				.hasNext();) {
			Instruction instr = (Instruction) iter.next();
			if (instr.getOpcode() == opc_putstatic
					|| instr.getOpcode() == opc_putfield) {
				Reference ref = instr.getReference();
				FieldIdentifier fi = (FieldIdentifier) Main.getClassBundle()
						.getIdentifier(ref);
				if (fi != null && (Main.stripping & Main.STRIP_UNREACH) != 0
						&& !fi.isReachable()) {
					/* Replace instruction with pop opcodes. */
					int stacksize = (instr.getOpcode() == Instruction.opc_putstatic) ? 0
							: 1;
					stacksize += TypeSignature.getTypeSize(ref.getType());
					switch (stacksize) {
					case 1:
						iter.set(new Instruction(Instruction.opc_pop));
						break;
					case 2:
						iter.set(new Instruction(Instruction.opc_pop2));
						break;
					case 3:
						iter.set(new Instruction(Instruction.opc_pop2));
						iter.add(new Instruction(Instruction.opc_pop));
						break;
					}
				}
			}
		}
	}
}
