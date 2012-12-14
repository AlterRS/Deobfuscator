/* Opcodes Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: Opcodes.java,v 4.22.2.1 2002/05/28 17:34:00 hoenicke Exp $
 */

package jode.bytecode;

/**
 * This is an interface containing the constants for the byte code opcodes.
 */
public interface Opcodes {
	public final static int opc_nop = 0;
	public final static int opc_aconst_null = 1;
	public final static int opc_iconst_m1 = 2;
	public final static int opc_iconst_0 = 3;
	public final static int opc_iconst_1 = 4;
	public final static int opc_iconst_2 = 5;
	public final static int opc_iconst_3 = 6;
	public final static int opc_iconst_4 = 7;
	public final static int opc_iconst_5 = 8;
	public final static int opc_lconst_0 = 9;
	public final static int opc_lconst_1 = 10;
	public final static int opc_fconst_0 = 11;
	public final static int opc_fconst_1 = 12;
	public final static int opc_fconst_2 = 13;
	public final static int opc_dconst_0 = 14;
	public final static int opc_dconst_1 = 15;
	public final static int opc_bipush = 16;
	public final static int opc_sipush = 17;
	public final static int opc_ldc = 18;
	public final static int opc_ldc_w = 19;
	public final static int opc_ldc2_w = 20;
	public final static int opc_iload = 21;
	public final static int opc_lload = 22;
	public final static int opc_fload = 23;
	public final static int opc_dload = 24;
	public final static int opc_aload = 25;
	public final static int opc_iload_0 = 26;
	public final static int opc_iload_1 = 27;
	public final static int opc_iload_2 = 28;
	public final static int opc_iload_3 = 29;
	public final static int opc_lload_0 = 30;
	public final static int opc_lload_1 = 31;
	public final static int opc_lload_2 = 32;
	public final static int opc_lload_3 = 33;
	public final static int opc_fload_0 = 34;
	public final static int opc_fload_1 = 35;
	public final static int opc_fload_2 = 36;
	public final static int opc_fload_3 = 37;
	public final static int opc_dload_0 = 38;
	public final static int opc_dload_1 = 39;
	public final static int opc_dload_2 = 40;
	public final static int opc_dload_3 = 41;
	public final static int opc_aload_0 = 42;
	public final static int opc_aload_1 = 43;
	public final static int opc_aload_2 = 44;
	public final static int opc_aload_3 = 45;
	public final static int opc_iaload = 46;
	public final static int opc_laload = 47;
	public final static int opc_faload = 48;
	public final static int opc_daload = 49;
	public final static int opc_aaload = 50;
	public final static int opc_baload = 51;
	public final static int opc_caload = 52;
	public final static int opc_saload = 53;
	public final static int opc_istore = 54;
	public final static int opc_lstore = 55;
	public final static int opc_fstore = 56;
	public final static int opc_dstore = 57;
	public final static int opc_astore = 58;
	public final static int opc_istore_0 = 59;
	public final static int opc_istore_1 = 60;
	public final static int opc_istore_2 = 61;
	public final static int opc_istore_3 = 62;
	public final static int opc_lstore_0 = 63;
	public final static int opc_lstore_1 = 64;
	public final static int opc_lstore_2 = 65;
	public final static int opc_lstore_3 = 66;
	public final static int opc_fstore_0 = 67;
	public final static int opc_fstore_1 = 68;
	public final static int opc_fstore_2 = 69;
	public final static int opc_fstore_3 = 70;
	public final static int opc_dstore_0 = 71;
	public final static int opc_dstore_1 = 72;
	public final static int opc_dstore_2 = 73;
	public final static int opc_dstore_3 = 74;
	public final static int opc_astore_0 = 75;
	public final static int opc_astore_1 = 76;
	public final static int opc_astore_2 = 77;
	public final static int opc_astore_3 = 78;
	public final static int opc_iastore = 79;
	public final static int opc_lastore = 80;
	public final static int opc_fastore = 81;
	public final static int opc_dastore = 82;
	public final static int opc_aastore = 83;
	public final static int opc_bastore = 84;
	public final static int opc_castore = 85;
	public final static int opc_sastore = 86;
	public final static int opc_pop = 87;
	public final static int opc_pop2 = 88;
	public final static int opc_dup = 89;
	public final static int opc_dup_x1 = 90;
	public final static int opc_dup_x2 = 91;
	public final static int opc_dup2 = 92;
	public final static int opc_dup2_x1 = 93;
	public final static int opc_dup2_x2 = 94;
	public final static int opc_swap = 95;
	public final static int opc_iadd = 96;
	public final static int opc_ladd = 97;
	public final static int opc_fadd = 98;
	public final static int opc_dadd = 99;
	public final static int opc_isub = 100;
	public final static int opc_lsub = 101;
	public final static int opc_fsub = 102;
	public final static int opc_dsub = 103;
	public final static int opc_imul = 104;
	public final static int opc_lmul = 105;
	public final static int opc_fmul = 106;
	public final static int opc_dmul = 107;
	public final static int opc_idiv = 108;
	public final static int opc_ldiv = 109;
	public final static int opc_fdiv = 110;
	public final static int opc_ddiv = 111;
	public final static int opc_irem = 112;
	public final static int opc_lrem = 113;
	public final static int opc_frem = 114;
	public final static int opc_drem = 115;
	public final static int opc_ineg = 116;
	public final static int opc_lneg = 117;
	public final static int opc_fneg = 118;
	public final static int opc_dneg = 119;
	public final static int opc_ishl = 120;
	public final static int opc_lshl = 121;
	public final static int opc_ishr = 122;
	public final static int opc_lshr = 123;
	public final static int opc_iushr = 124;
	public final static int opc_lushr = 125;
	public final static int opc_iand = 126;
	public final static int opc_land = 127;
	public final static int opc_ior = 128;
	public final static int opc_lor = 129;
	public final static int opc_ixor = 130;
	public final static int opc_lxor = 131;
	public final static int opc_iinc = 132;
	public final static int opc_i2l = 133;
	public final static int opc_i2f = 134;
	public final static int opc_i2d = 135;
	public final static int opc_l2i = 136;
	public final static int opc_l2f = 137;
	public final static int opc_l2d = 138;
	public final static int opc_f2i = 139;
	public final static int opc_f2l = 140;
	public final static int opc_f2d = 141;
	public final static int opc_d2i = 142;
	public final static int opc_d2l = 143;
	public final static int opc_d2f = 144;
	public final static int opc_i2b = 145;
	public final static int opc_i2c = 146;
	public final static int opc_i2s = 147;
	public final static int opc_lcmp = 148;
	public final static int opc_fcmpl = 149;
	public final static int opc_fcmpg = 150;
	public final static int opc_dcmpl = 151;
	public final static int opc_dcmpg = 152;
	public final static int opc_ifeq = 153;
	public final static int opc_ifne = 154;
	public final static int opc_iflt = 155;
	public final static int opc_ifge = 156;
	public final static int opc_ifgt = 157;
	public final static int opc_ifle = 158;
	public final static int opc_if_icmpeq = 159;
	public final static int opc_if_icmpne = 160;
	public final static int opc_if_icmplt = 161;
	public final static int opc_if_icmpge = 162;
	public final static int opc_if_icmpgt = 163;
	public final static int opc_if_icmple = 164;
	public final static int opc_if_acmpeq = 165;
	public final static int opc_if_acmpne = 166;
	public final static int opc_goto = 167;
	public final static int opc_jsr = 168;
	public final static int opc_ret = 169;
	public final static int opc_tableswitch = 170;
	public final static int opc_lookupswitch = 171;
	public final static int opc_ireturn = 172;
	public final static int opc_lreturn = 173;
	public final static int opc_freturn = 174;
	public final static int opc_dreturn = 175;
	public final static int opc_areturn = 176;
	public final static int opc_return = 177;
	public final static int opc_getstatic = 178;
	public final static int opc_putstatic = 179;
	public final static int opc_getfield = 180;
	public final static int opc_putfield = 181;
	public final static int opc_invokevirtual = 182;
	public final static int opc_invokespecial = 183;
	public final static int opc_invokestatic = 184;
	public final static int opc_invokeinterface = 185;
	public final static int opc_xxxunusedxxx = 186;
	public final static int opc_new = 187;
	public final static int opc_newarray = 188;
	public final static int opc_anewarray = 189;
	public final static int opc_arraylength = 190;
	public final static int opc_athrow = 191;
	public final static int opc_checkcast = 192;
	public final static int opc_instanceof = 193;
	public final static int opc_monitorenter = 194;
	public final static int opc_monitorexit = 195;
	public final static int opc_wide = 196;
	public final static int opc_multianewarray = 197;
	public final static int opc_ifnull = 198;
	public final static int opc_ifnonnull = 199;
	public final static int opc_goto_w = 200;
	public final static int opc_jsr_w = 201;
	public final static int opc_breakpoint = 202;
	public final static int opc_impdep1 = 254;
	public final static int opc_impdep2 = 255;

	public final static String[] opcodeString = { "nop", "aconst_null",
			"iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3",
			"iconst_4", "iconst_5", "lconst_0", "lconst_1", "fconst_0",
			"fconst_1", "fconst_2", "dconst_0", "dconst_1", "bipush", "sipush",
			"ldc", "ldc_w", "ldc2_w", "iload", "lload", "fload", "dload",
			"aload", "iload_0", "iload_1", "iload_2", "iload_3", "lload_0",
			"lload_1", "lload_2", "lload_3", "fload_0", "fload_1", "fload_2",
			"fload_3", "dload_0", "dload_1", "dload_2", "dload_3", "aload_0",
			"aload_1", "aload_2", "aload_3", "iaload", "laload", "faload",
			"daload", "aaload", "baload", "caload", "saload", "istore",
			"lstore", "fstore", "dstore", "astore", "istore_0", "istore_1",
			"istore_2", "istore_3", "lstore_0", "lstore_1", "lstore_2",
			"lstore_3", "fstore_0", "fstore_1", "fstore_2", "fstore_3",
			"dstore_0", "dstore_1", "dstore_2", "dstore_3", "astore_0",
			"astore_1", "astore_2", "astore_3", "iastore", "lastore",
			"fastore", "dastore", "aastore", "bastore", "castore", "sastore",
			"pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1",
			"dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub",
			"fsub", "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv",
			"fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg",
			"fneg", "dneg", "ishl", "lshl", "ishr", "lshr", "iushr", "lushr",
			"iand", "land", "ior", "lor", "ixor", "lxor", "iinc", "i2l", "i2f",
			"i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l",
			"d2f", "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl",
			"dcmpg", "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle",
			"if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge", "if_icmpgt",
			"if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ret",
			"tableswitch", "lookupswitch", "ireturn", "lreturn", "freturn",
			"dreturn", "areturn", "return", "getstatic", "putstatic",
			"getfield", "putfield", "invokevirtual", "invokespecial",
			"invokestatic", "invokeinterface", "xxxunusedxxx", "new",
			"newarray", "anewarray", "arraylength", "athrow", "checkcast",
			"instanceof", "monitorenter", "monitorexit", "wide",
			"multianewarray", "ifnull", "ifnonnull", "goto_w", "jsr_w",
			"breakpoint" };

	public final static String newArrayTypes = "ZCFDBSIJ";
}
