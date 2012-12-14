/* Opcodes Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: Opcodes.java,v 1.18.2.1 2002/05/28 17:34:03 hoenicke Exp $
 */

package jode.decompiler;

import java.io.IOException;

import jode.bytecode.Instruction;
import jode.bytecode.Reference;
import jode.expr.ArrayLengthOperator;
import jode.expr.ArrayLoadOperator;
import jode.expr.ArrayStoreOperator;
import jode.expr.BinaryOperator;
import jode.expr.CheckCastOperator;
import jode.expr.CompareBinaryOperator;
import jode.expr.CompareToIntOperator;
import jode.expr.CompareUnaryOperator;
import jode.expr.ConstOperator;
import jode.expr.ConvertOperator;
import jode.expr.Expression;
import jode.expr.GetFieldOperator;
import jode.expr.IIncOperator;
import jode.expr.InstanceOfOperator;
import jode.expr.InvokeOperator;
import jode.expr.LocalLoadOperator;
import jode.expr.LocalStoreOperator;
import jode.expr.MonitorEnterOperator;
import jode.expr.MonitorExitOperator;
import jode.expr.NewArrayOperator;
import jode.expr.NewOperator;
import jode.expr.NopOperator;
import jode.expr.Operator;
import jode.expr.PutFieldOperator;
import jode.expr.ShiftOperator;
import jode.expr.StoreInstruction;
import jode.expr.UnaryOperator;
import jode.flow.ConditionalBlock;
import jode.flow.EmptyBlock;
import jode.flow.FlowBlock;
import jode.flow.InstructionBlock;
import jode.flow.JsrBlock;
import jode.flow.Jump;
import jode.flow.RetBlock;
import jode.flow.ReturnBlock;
import jode.flow.SpecialBlock;
import jode.flow.StructuredBlock;
import jode.flow.SwitchBlock;
import jode.flow.ThrowBlock;
import jode.type.IntegerType;
import jode.type.Type;


/**
 * This is an abstract class which creates flow blocks for the opcodes in a byte
 * stream.
 */
public abstract class Opcodes implements jode.bytecode.Opcodes {

	private final static Type tIntHint = new IntegerType(IntegerType.IT_I,
			IntegerType.IT_I | IntegerType.IT_B | IntegerType.IT_C
					| IntegerType.IT_S);
	private final static Type tBoolIntHint = new IntegerType(IntegerType.IT_I
			| IntegerType.IT_Z, IntegerType.IT_I | IntegerType.IT_B
			| IntegerType.IT_C | IntegerType.IT_S | IntegerType.IT_Z);

	private final static int LOCAL_TYPES = 0;
	private final static int ARRAY_TYPES = 1;
	private final static int UNARY_TYPES = 2;
	private final static int I2BCS_TYPES = 3;
	private final static int BIN_TYPES = 4;
	private final static int ZBIN_TYPES = 5;

	private final static Type types[][] = {
			// Local types
			{ Type.tBoolUInt, Type.tLong, Type.tFloat, Type.tDouble,
					Type.tUObject },
			// Array types
			{ Type.tInt, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject,
					Type.tBoolByte, Type.tChar, Type.tShort },
			// ifld2ifld and shl types
			{ Type.tInt, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject },
			// i2bcs types
			{ Type.tByte, Type.tChar, Type.tShort },
			// cmp/add/sub/mul/div types
			{ tIntHint, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject },
			// and/or/xor types
			{ tBoolIntHint, Type.tLong, Type.tFloat, Type.tDouble,
					Type.tUObject } };

	private static StructuredBlock createNormal(MethodAnalyzer ma,
			Instruction instr, Expression expr) {
		return new InstructionBlock(expr, new Jump(FlowBlock.NEXT_BY_ADDR));
	}

	private static StructuredBlock createSpecial(MethodAnalyzer ma,
			Instruction instr, int type, int stackcount, int param) {
		return new SpecialBlock(type, stackcount, param, new Jump(
				FlowBlock.NEXT_BY_ADDR));
	}

	private static StructuredBlock createGoto(MethodAnalyzer ma,
			Instruction instr) {
		return new EmptyBlock(new Jump((FlowBlock) instr.getSingleSucc()
				.getTmpInfo()));
	}

	private static StructuredBlock createJsr(MethodAnalyzer ma,
			Instruction instr) {
		return new JsrBlock(new Jump((FlowBlock) instr.getSingleSucc()
				.getTmpInfo()), new Jump(FlowBlock.NEXT_BY_ADDR));
	}

	private static StructuredBlock createIfGoto(MethodAnalyzer ma,
			Instruction instr, Expression expr) {
		return new ConditionalBlock(expr, new Jump((FlowBlock) instr
				.getSingleSucc().getTmpInfo()),
				new Jump(FlowBlock.NEXT_BY_ADDR));
	}

	private static StructuredBlock createSwitch(MethodAnalyzer ma,
			Instruction instr, int[] cases, FlowBlock[] dests) {
		return new SwitchBlock(new NopOperator(Type.tUInt), cases, dests);
	}

	private static StructuredBlock createBlock(MethodAnalyzer ma,
			Instruction instr, StructuredBlock block) {
		return block;
	}

	private static StructuredBlock createRet(MethodAnalyzer ma,
			Instruction instr, LocalInfo local) {
		return new RetBlock(local);
	}

	/**
	 * Read an opcode out of a data input stream containing the bytecode.
	 * 
	 * @param addr
	 *            The current address.
	 * @param stream
	 *            The stream containing the java byte code.
	 * @param ma
	 *            The Method Analyzer (where further information can be get
	 *            from).
	 * @return The FlowBlock representing this opcode or null if the stream is
	 *         empty.
	 * @throws IOException
	 *             if an read error occured.
	 * @throws ClassFormatError
	 *             if an invalid opcode is detected.
	 */
	public static StructuredBlock readOpcode(Instruction instr,
			MethodAnalyzer ma) throws ClassFormatError {
		int opcode = instr.getOpcode();
		switch (opcode) {
		case opc_nop:
			return createBlock(ma, instr, new EmptyBlock(new Jump(
					FlowBlock.NEXT_BY_ADDR)));
		case opc_ldc:
		case opc_ldc2_w:
			return createNormal(ma, instr,
					new ConstOperator(instr.getConstant()));

		case opc_iload:
		case opc_lload:
		case opc_fload:
		case opc_dload:
		case opc_aload:
			return createNormal(
					ma,
					instr,
					new LocalLoadOperator(
							types[LOCAL_TYPES][opcode - opc_iload], ma, ma
									.getLocalInfo(instr.getAddr(),
											instr.getLocalSlot())));
		case opc_iaload:
		case opc_laload:
		case opc_faload:
		case opc_daload:
		case opc_aaload:
		case opc_baload:
		case opc_caload:
		case opc_saload:
			return createNormal(ma, instr, new ArrayLoadOperator(
					types[ARRAY_TYPES][opcode - opc_iaload]));
		case opc_istore:
		case opc_lstore:
		case opc_fstore:
		case opc_dstore:
		case opc_astore:
			return createNormal(
					ma,
					instr,
					new StoreInstruction(new LocalStoreOperator(
							types[LOCAL_TYPES][opcode - opc_istore], ma
									.getLocalInfo(instr.getNextByAddr()
											.getAddr(), instr.getLocalSlot()))));
		case opc_iastore:
		case opc_lastore:
		case opc_fastore:
		case opc_dastore:
		case opc_aastore:
		case opc_bastore:
		case opc_castore:
		case opc_sastore:
			return createNormal(ma, instr, new StoreInstruction(
					new ArrayStoreOperator(types[ARRAY_TYPES][opcode
							- opc_iastore])));
		case opc_pop:
		case opc_pop2:
			return createSpecial(ma, instr, SpecialBlock.POP, opcode - opc_pop
					+ 1, 0);
		case opc_dup:
		case opc_dup_x1:
		case opc_dup_x2:
		case opc_dup2:
		case opc_dup2_x1:
		case opc_dup2_x2:
			return createSpecial(ma, instr, SpecialBlock.DUP,
					(opcode - opc_dup) / 3 + 1, (opcode - opc_dup) % 3);
		case opc_swap:
			return createSpecial(ma, instr, SpecialBlock.SWAP, 1, 0);
		case opc_iadd:
		case opc_ladd:
		case opc_fadd:
		case opc_dadd:
		case opc_isub:
		case opc_lsub:
		case opc_fsub:
		case opc_dsub:
		case opc_imul:
		case opc_lmul:
		case opc_fmul:
		case opc_dmul:
		case opc_idiv:
		case opc_ldiv:
		case opc_fdiv:
		case opc_ddiv:
		case opc_irem:
		case opc_lrem:
		case opc_frem:
		case opc_drem:
			return createNormal(ma, instr, new BinaryOperator(
					types[BIN_TYPES][(opcode - opc_iadd) % 4],
					(opcode - opc_iadd) / 4 + Operator.ADD_OP));
		case opc_ineg:
		case opc_lneg:
		case opc_fneg:
		case opc_dneg:
			return createNormal(ma, instr, new UnaryOperator(
					types[UNARY_TYPES][opcode - opc_ineg], Operator.NEG_OP));
		case opc_ishl:
		case opc_lshl:
		case opc_ishr:
		case opc_lshr:
		case opc_iushr:
		case opc_lushr:
			return createNormal(ma, instr, new ShiftOperator(
					types[UNARY_TYPES][(opcode - opc_ishl) % 2],
					(opcode - opc_ishl) / 2 + Operator.SHIFT_OP));
		case opc_iand:
		case opc_land:
		case opc_ior:
		case opc_lor:
		case opc_ixor:
		case opc_lxor:
			return createNormal(ma, instr, new BinaryOperator(
					types[ZBIN_TYPES][(opcode - opc_iand) % 2],
					(opcode - opc_iand) / 2 + Operator.AND_OP));
		case opc_iinc: {
			int value = instr.getIncrement();
			int operation = Operator.ADD_OP;
			if (value < 0) {
				value = -value;
				operation = Operator.SUB_OP;
			}
			LocalInfo li = ma.getLocalInfo(instr.getAddr(),
					instr.getLocalSlot());
			return createNormal(ma, instr, new IIncOperator(
					new LocalStoreOperator(Type.tInt, li), value, operation
							+ Operator.OPASSIGN_OP));
		}
		case opc_i2l:
		case opc_i2f:
		case opc_i2d:
		case opc_l2i:
		case opc_l2f:
		case opc_l2d:
		case opc_f2i:
		case opc_f2l:
		case opc_f2d:
		case opc_d2i:
		case opc_d2l:
		case opc_d2f: {
			int from = (opcode - opc_i2l) / 3;
			int to = (opcode - opc_i2l) % 3;
			if (to >= from)
				to++;
			return createNormal(ma, instr, new ConvertOperator(
					types[UNARY_TYPES][from], types[UNARY_TYPES][to]));
		}
		case opc_i2b:
		case opc_i2c:
		case opc_i2s:
			return createNormal(ma, instr,
					new ConvertOperator(types[UNARY_TYPES][0],
							types[I2BCS_TYPES][opcode - opc_i2b]));
		case opc_lcmp:
		case opc_fcmpl:
		case opc_fcmpg:
		case opc_dcmpl:
		case opc_dcmpg:
			return createNormal(ma, instr, new CompareToIntOperator(
					types[BIN_TYPES][(opcode - (opc_lcmp - 3)) / 2],
					(opcode == opc_fcmpg || opcode == opc_dcmpg)));
		case opc_ifeq:
		case opc_ifne:
			return createIfGoto(ma, instr, new CompareUnaryOperator(
					Type.tBoolInt, opcode - (opc_ifeq - Operator.COMPARE_OP)));
		case opc_iflt:
		case opc_ifge:
		case opc_ifgt:
		case opc_ifle:
			return createIfGoto(ma, instr, new CompareUnaryOperator(Type.tInt,
					opcode - (opc_ifeq - Operator.COMPARE_OP)));
		case opc_if_icmpeq:
		case opc_if_icmpne:
			return createIfGoto(ma, instr, new CompareBinaryOperator(
					tBoolIntHint, opcode
							- (opc_if_icmpeq - Operator.COMPARE_OP)));
		case opc_if_icmplt:
		case opc_if_icmpge:
		case opc_if_icmpgt:
		case opc_if_icmple:
			return createIfGoto(ma, instr, new CompareBinaryOperator(tIntHint,
					opcode - (opc_if_icmpeq - Operator.COMPARE_OP)));
		case opc_if_acmpeq:
		case opc_if_acmpne:
			return createIfGoto(ma, instr, new CompareBinaryOperator(
					Type.tUObject, opcode
							- (opc_if_acmpeq - Operator.COMPARE_OP)));
		case opc_goto:
			return createGoto(ma, instr);
		case opc_jsr:
			return createJsr(ma, instr);
		case opc_ret:
			return createRet(ma, instr,
					ma.getLocalInfo(instr.getAddr(), instr.getLocalSlot()));
		case opc_lookupswitch: {
			int[] cases = instr.getValues();
			FlowBlock[] dests = new FlowBlock[instr.getSuccs().length];
			for (int i = 0; i < dests.length; i++)
				dests[i] = (FlowBlock) instr.getSuccs()[i].getTmpInfo();
			dests[cases.length] = (FlowBlock) instr.getSuccs()[cases.length]
					.getTmpInfo();
			return createSwitch(ma, instr, cases, dests);
		}
		case opc_ireturn:
		case opc_lreturn:
		case opc_freturn:
		case opc_dreturn:
		case opc_areturn: {
			Type retType = Type.tSubType(ma.getReturnType());
			return createBlock(ma, instr, new ReturnBlock(new NopOperator(
					retType)));
		}
		case opc_return:
			return createBlock(ma, instr, new EmptyBlock(new Jump(
					FlowBlock.END_OF_METHOD)));
		case opc_getstatic:
		case opc_getfield: {
			Reference ref = instr.getReference();
			return createNormal(ma, instr, new GetFieldOperator(ma,
					opcode == opc_getstatic, ref));
		}
		case opc_putstatic:
		case opc_putfield: {
			Reference ref = instr.getReference();
			return createNormal(ma, instr, new StoreInstruction(
					new PutFieldOperator(ma, opcode == opc_putstatic, ref)));
		}
		case opc_invokevirtual:
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface: {
			Reference ref = instr.getReference();
			int flag = (ref.getName().equals("<init>") ? InvokeOperator.CONSTRUCTOR
					: opcode == opc_invokestatic ? InvokeOperator.STATIC
							: opcode == opc_invokespecial ? InvokeOperator.SPECIAL
									: InvokeOperator.VIRTUAL);
			StructuredBlock block = createNormal(ma, instr, new InvokeOperator(
					ma, flag, ref));
			return block;
		}
		case opc_new: {
			Type type = Type.tType(instr.getClazzType());
			ma.useType(type);
			return createNormal(ma, instr, new NewOperator(type));
		}
		case opc_arraylength:
			return createNormal(ma, instr, new ArrayLengthOperator());
		case opc_athrow:
			return createBlock(ma, instr, new ThrowBlock(new NopOperator(
					Type.tUObject)));
		case opc_checkcast: {
			Type type = Type.tType(instr.getClazzType());
			ma.useType(type);
			return createNormal(ma, instr, new CheckCastOperator(type));
		}
		case opc_instanceof: {
			Type type = Type.tType(instr.getClazzType());
			ma.useType(type);
			return createNormal(ma, instr, new InstanceOfOperator(type));
		}
		case opc_monitorenter:
			return createNormal(ma, instr, new MonitorEnterOperator());
		case opc_monitorexit:
			return createNormal(ma, instr, new MonitorExitOperator());
		case opc_multianewarray: {
			Type type = Type.tType(instr.getClazzType());
			ma.useType(type);
			int dimension = instr.getDimensions();
			return createNormal(ma, instr,
					new NewArrayOperator(type, dimension));
		}
		case opc_ifnull:
		case opc_ifnonnull:
			return createIfGoto(ma, instr, new CompareUnaryOperator(
					Type.tUObject, opcode - (opc_ifnull - Operator.COMPARE_OP)));
		default:
			throw new jode.AssertError("Invalid opcode " + opcode);
		}
	}
}
