/* RemovePopAnalyzer Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: RemovePopAnalyzer.java.in,v 1.1.2.1 2002/05/28 17:34:17 hoenicke Exp $
 */

package alterrs.jode.obfuscator.modules;

import java.util.ListIterator;

import alterrs.jode.AssertError;
import alterrs.jode.bytecode.BytecodeInfo;
import alterrs.jode.bytecode.Handler;
import alterrs.jode.bytecode.Instruction;
import alterrs.jode.bytecode.Opcodes;
import alterrs.jode.bytecode.Reference;
import alterrs.jode.bytecode.TypeSignature;
import alterrs.jode.obfuscator.CodeTransformer;

public class RemovePopAnalyzer implements CodeTransformer, Opcodes {
	public RemovePopAnalyzer() {
	}

	public void transformCode(BytecodeInfo bytecode) {
		int poppush[] = new int[2];
		ListIterator iter = bytecode.getInstructions().listIterator();
		next_pop: while (iter.hasNext()) {
			Instruction popInstr = (Instruction) iter.next();
			boolean isPop2 = false;
			switch (popInstr.getOpcode()) {
			case opc_nop: {
				iter.remove();
				continue;
			}

			case opc_pop2:
				isPop2 = true;
			case opc_pop:
				if (popInstr.getPreds() != null)
					// Can't handle pop with multiple predecessors
					continue next_pop;
				Handler[] handlers = bytecode.getExceptionHandlers();
				for (int i = 0; i < handlers.length; i++)
					if (handlers[i].catcher == popInstr)
						continue next_pop;

				// remove pop, we will insert it again if something
				// bad happened.
				iter.remove();

				// remember position of pop, so we can insert it again.
				Instruction popPrevious = (Instruction) iter.previous();
				Instruction instr = popPrevious;
				int count = 0;
				while (true) {
					if (instr.getSuccs() != null || instr.doesAlwaysJump()) {
						instr = null;
						break;
					}
					instr.getStackPopPush(poppush);

					if (count < poppush[1]) {
						if (count == 0)
							break;

						int opcode = instr.getOpcode();
						/*
						 * If this is a dup and the instruction popped is the
						 * duplicated element, remove the dup and the pop
						 */
						if (count <= 3 && opcode == (opc_dup + count - 1)) {
							iter.remove();
							if (!isPop2)
								continue next_pop;

							// We have to consider a pop instead of a
							// pop2 now.
							popInstr = new Instruction(opc_pop);
							isPop2 = false;
							instr = (Instruction) iter.previous();
							continue;
						}

						if (isPop2 && count > 1 && count <= 4
								&& opcode == (opc_dup2 + count - 2)) {
							iter.remove();
							continue next_pop;
						}
						/* Otherwise popping is not possible */
						instr = null;
						break;
					}
					count += poppush[0] - poppush[1];
					instr = (Instruction) iter.previous();
				}

				if (instr == null) {
					// We insert the pop at the previous position
					while (iter.next() != popPrevious) {
					}
					if (!isPop2 && popPrevious.getOpcode() == opc_pop) {
						// merge pop with popPrevious
						iter.set(new Instruction(opc_pop2));
					} else
						iter.add(popInstr);
					continue;
				}
				int opcode = instr.getOpcode();
				switch (opcode) {
				case opc_ldc2_w:
				case opc_lload:
				case opc_dload:
					if (!isPop2)
						throw new AssertError("pop on long");
					iter.remove();
					continue;
				case opc_ldc:
				case opc_iload:
				case opc_fload:
				case opc_aload:
				case opc_dup:
				case opc_new:
					if (isPop2)
						iter.set(new Instruction(opc_pop));
					else
						iter.remove();
					continue;
				case opc_iaload:
				case opc_faload:
				case opc_aaload:
				case opc_baload:
				case opc_caload:
				case opc_saload:
				case opc_iadd:
				case opc_fadd:
				case opc_isub:
				case opc_fsub:
				case opc_imul:
				case opc_fmul:
				case opc_idiv:
				case opc_fdiv:
				case opc_irem:
				case opc_frem:
				case opc_iand:
				case opc_ior:
				case opc_ixor:
				case opc_ishl:
				case opc_ishr:
				case opc_iushr:
				case opc_fcmpl:
				case opc_fcmpg:
					/* We have to pop one entry more. */
					iter.next();
					iter.add(popInstr);
					iter.previous();
					iter.previous();
					iter.set(new Instruction(opc_pop));
					continue;

				case opc_dup_x1:
					iter.set(new Instruction(opc_swap));
					iter.next();
					if (isPop2)
						iter.add(new Instruction(opc_pop));
					continue;

				case opc_dup2:
					if (isPop2) {
						iter.remove();
						continue;
					}
					break;
				case opc_swap:
					if (isPop2) {
						iter.set(popInstr);
						continue;
					}
					break;

				case opc_lneg:
				case opc_dneg:
				case opc_l2d:
				case opc_d2l:
				case opc_laload:
				case opc_daload:
					if (!isPop2)
						throw new AssertError("pop on long");
					/* fall through */
				case opc_ineg:
				case opc_fneg:
				case opc_i2f:
				case opc_f2i:
				case opc_i2b:
				case opc_i2c:
				case opc_i2s:
				case opc_newarray:
				case opc_anewarray:
				case opc_arraylength:
				case opc_instanceof:
					iter.set(popInstr);
					continue;

				case opc_l2i:
				case opc_l2f:
				case opc_d2i:
				case opc_d2f:
					if (isPop2) {
						iter.next();
						iter.add(new Instruction(opc_pop));
						iter.previous();
						iter.previous();
					}
					iter.set(new Instruction(opc_pop2));
					continue;

				case opc_ladd:
				case opc_dadd:
				case opc_lsub:
				case opc_dsub:
				case opc_lmul:
				case opc_dmul:
				case opc_ldiv:
				case opc_ddiv:
				case opc_lrem:
				case opc_drem:
				case opc_land:
				case opc_lor:
				case opc_lxor:
					if (!isPop2)
						throw new AssertError("pop on long");
					iter.next();
					iter.add(popInstr);
					iter.previous();
					iter.previous();
					iter.set(new Instruction(opc_pop2));
					continue;
				case opc_lshl:
				case opc_lshr:
				case opc_lushr:
					if (!isPop2)
						throw new AssertError("pop on long");
					iter.next();
					iter.add(popInstr);
					iter.previous();
					iter.previous();
					iter.set(new Instruction(opc_pop));
					continue;

				case opc_i2l:
				case opc_i2d:
				case opc_f2l:
				case opc_f2d:
					if (!isPop2)
						throw new AssertError("pop on long");
					iter.set(new Instruction(opc_pop));
					continue;

				case opc_lcmp:
				case opc_dcmpl:
				case opc_dcmpg:
					iter.next();
					iter.add(new Instruction(opc_pop2));
					if (isPop2) {
						iter.add(new Instruction(opc_pop));
						iter.previous();
					}
					iter.previous();
					iter.previous();
					iter.set(new Instruction(opc_pop2));
					continue;

				case opc_getstatic:
				case opc_getfield: {
					Reference ref = instr.getReference();
					int size = TypeSignature.getTypeSize(ref.getType());
					if (size == 2 && !isPop2)
						throw new AssertError("pop on long");
					if (opcode == opc_getfield)
						size--;
					switch (size) {
					case 0:
						iter.set(popInstr);
						break;
					case 1:
						if (isPop2) {
							iter.set(new Instruction(opc_pop));
							break;
						}
						/* fall through */
					case 2:
						iter.remove();
					}
					continue;
				}

				case opc_multianewarray: {
					int dims = instr.getDimensions();
					if (--dims > 0) {
						iter.next();
						while (dims-- > 0) {
							iter.add(new Instruction(opc_pop));
							iter.previous();
						}
						iter.previous();
					}
					iter.set(popInstr);
					continue;
				}

				case opc_invokevirtual:
				case opc_invokespecial:
				case opc_invokestatic:
				case opc_invokeinterface:
					if (TypeSignature.getReturnSize(instr.getReference()
							.getType()) != 1)
						break;
					/* fall through */
				case opc_checkcast:
					if (isPop2) {
						/*
						 * This is/may be a double pop on a single value split
						 * it and continue with second half
						 */
						iter.next();
						iter.add(new Instruction(opc_pop));
						iter.add(new Instruction(opc_pop));
						iter.previous();
						continue;
					}
				}
				// append the pop behind the unresolvable opcode.
				iter.next();
				iter.add(popInstr);
				continue;
			}
		}
	}
}
