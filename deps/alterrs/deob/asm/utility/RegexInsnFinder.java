package alterrs.deob.asm.utility;

/**
 * @author Lazaro
 */

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import alterrs.asm.tree.AbstractInsnNode;
import alterrs.asm.tree.FieldInsnNode;
import alterrs.asm.tree.IincInsnNode;
import alterrs.asm.tree.IntInsnNode;
import alterrs.asm.tree.LdcInsnNode;
import alterrs.asm.tree.MethodInsnNode;
import alterrs.asm.tree.MethodNode;
import alterrs.asm.tree.MultiANewArrayInsnNode;
import alterrs.asm.tree.TypeInsnNode;
import alterrs.asm.tree.VarInsnNode;

/**
 * An instruction finder that finds regex patterns in a method's instruction
 * list and returns an array with the found instructions.
 * 
 * @author Frederic Hannes
 */

public class RegexInsnFinder {
	private static String[] opcodes = new String[] { "NOP", "ACONST_NULL",
			"ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3",
			"ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0",
			"FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1", "BIPUSH", "SIPUSH",
			"LDC", "LDC_W", "LDC2_W", "ILOAD", "LLOAD", "FLOAD", "DLOAD",
			"ALOAD", "ILOAD_0", "ILOAD_1", "ILOAD_2", "ILOAD_3", "LLOAD_0",
			"LLOAD_1", "LLOAD_2", "LLOAD_3", "FLOAD_0", "FLOAD_1", "FLOAD_2",
			"FLOAD_3", "DLOAD_0", "DLOAD_1", "DLOAD_2", "DLOAD_3", "ALOAD_0",
			"ALOAD_1", "ALOAD_2", "ALOAD_3", "IALOAD", "LALOAD", "FALOAD",
			"DALOAD", "AALOAD", "BALOAD", "CALOAD", "SALOAD", "ISTORE",
			"LSTORE", "FSTORE", "DSTORE", "ASTORE", "ISTORE_0", "ISTORE_1",
			"ISTORE_2", "ISTORE_3", "LSTORE_0", "LSTORE_1", "LSTORE_2",
			"LSTORE_3", "FSTORE_0", "FSTORE_1", "FSTORE_2", "FSTORE_3",
			"DSTORE_0", "DSTORE_1", "DSTORE_2", "DSTORE_3", "ASTORE_0",
			"ASTORE_1", "ASTORE_2", "ASTORE_3", "IASTORE", "LASTORE",
			"FASTORE", "DASTORE", "AASTORE", "BASTORE", "CASTORE", "SASTORE",
			"POP", "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1",
			"DUP2_X2", "SWAP", "IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB",
			"FSUB", "DSUB", "IMUL", "LMUL", "FMUL", "DMUL", "IDIV", "LDIV",
			"FDIV", "DDIV", "IREM", "LREM", "FREM", "DREM", "INEG", "LNEG",
			"FNEG", "DNEG", "ISHL", "LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR",
			"IAND", "LAND", "IOR", "LOR", "IXOR", "LXOR", "IINC", "I2L", "I2F",
			"I2D", "L2I", "L2F", "L2D", "F2I", "F2L", "F2D", "D2I", "D2L",
			"D2F", "I2B", "I2C", "I2S", "LCMP", "FCMPL", "FCMPG", "DCMPL",
			"DCMPG", "IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE",
			"IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT",
			"IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO", "JSR", "RET",
			"TABLESWITCH", "LOOKUPSWITCH", "IRETURN", "LRETURN", "FRETURN",
			"DRETURN", "ARETURN", "RETURN", "GETSTATIC", "PUTSTATIC",
			"GETFIELD", "PUTFIELD", "INVOKEVIRTUAL", "INVOKESPECIAL",
			"INVOKESTATIC", "INVOKEINTERFACE", "INVOKEDYNAMIC", "NEW",
			"NEWARRAY", "ANEWARRAY", "ARRAYLENGTH", "ATHROW", "CHECKCAST",
			"INSTANCEOF", "MONITORENTER", "MONITOREXIT", "WIDE",
			"MULTIANEWARRAY", "IFNULL", "IFNONNULL", "GOTO_W", "JSR_W" };

	private static String[] opcodesVar = new String[] { "ILOAD", "LLOAD",
			"FLOAD", "DLOAD", "ALOAD", "ISTORE", "LSTORE", "FSTORE", "DSTORE",
			"ASTORE", "RET" };
	private static String opcodeVars = buildRegexItems(opcodesVar);

	private static String[] opcodesInt = new String[] { "BIPUSH", "SIPUSH",
			"NEWARRAY" };
	private static String opcodesInts = buildRegexItems(opcodesInt);

	private static String[] opcodesField = new String[] { "GETSTATIC",
			"PUTSTATIC", "GETFIELD", "PUTFIELD" };
	private static String opcodesFields = buildRegexItems(opcodesField);

	private static String[] opcodesMethod = new String[] { "INVOKEVIRTUAL",
			"INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE", "INVOKEDYNAMIC" };
	private static String opcodesMethods = buildRegexItems(opcodesMethod);

	private static String[] opcodesType = new String[] { "NEW", "ANEWARRAY",
			"ARRAYLENGTH", "CHECKCAST", "INSTANCEOF" };
	private static String opcodesTypes = buildRegexItems(opcodesType);

	private static String[] opcodesIf = new String[] { "IFEQ", "IFNE", "IFLT",
			"IFGE", "IFGT", "IFLE", "IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT",
			"IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE" };
	private static String opcodesIfs = buildRegexItems(opcodesIf, false, false);

	private static String[] opcodesAny = new String[] { "NOP", "ACONST_NULL",
			"ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3",
			"ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0",
			"FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1", "BIPUSH", "SIPUSH",
			"LDC", "LDC_W", "LDC2_W", "ILOAD", "LLOAD", "FLOAD", "DLOAD",
			"ALOAD", "IALOAD", "LALOAD", "FALOAD", "DALOAD", "AALOAD",
			"BALOAD", "CALOAD", "SALOAD", "ISTORE", "LSTORE", "FSTORE",
			"DSTORE", "ASTORE", "IASTORE", "LASTORE", "FASTORE", "DASTORE",
			"AASTORE", "BASTORE", "CASTORE", "SASTORE", "POP", "POP2", "DUP",
			"DUP_X1", "DUP_X2", "DUP2", "DUP2_X1", "DUP2_X2", "SWAP", "IADD",
			"LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB", "DSUB", "IMUL",
			"LMUL", "FMUL", "DMUL", "IDIV", "LDIV", "FDIV", "DDIV", "IREM",
			"LREM", "FREM", "DREM", "INEG", "LNEG", "FNEG", "DNEG", "ISHL",
			"LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND", "IOR",
			"LOR", "IXOR", "LXOR", "IINC", "I2L", "I2F", "I2D", "L2I", "L2F",
			"L2D", "F2I", "F2L", "F2D", "D2I", "D2L", "D2F", "I2B", "I2C",
			"I2S", "LCMP", "FCMPL", "FCMPG", "DCMPL", "DCMPG", "IFEQ", "IFNE",
			"IFLT", "IFGE", "IFGT", "IFLE", "IF_ICMPEQ", "IF_ICMPNE",
			"IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE", "IF_ACMPEQ",
			"IF_ACMPNE", "GOTO", "JSR", "RET", "TABLESWITCH", "LOOKUPSWITCH",
			"IRETURN", "LRETURN", "FRETURN", "DRETURN", "ARETURN", "RETURN",
			"GETSTATIC", "PUTSTATIC", "GETFIELD", "PUTFIELD", "INVOKEVIRTUAL",
			"INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE",
			"INVOKEDYNAMIC", "NEW", "NEWARRAY", "ANEWARRAY", "ARRAYLENGTH",
			"ATHROW", "CHECKCAST", "INSTANCEOF", "MONITORENTER", "MONITOREXIT",
			"MULTIANEWARRAY", "IFNULL", "IFNONNULL" };
	private static String opcodesAnys = buildRegexItems(opcodesAny, false,
			false);

	private static String buildRegexItems(String[] items, boolean capture,
			boolean stdRepl) {
		if (items.length == 0)
			return "()";
		String result = (stdRepl ? "\\b" : "") + "(" + (capture ? "" : "?:")
				+ items[0];
		for (int i = 1; i < items.length; i++)
			result += "|" + items[i];
		result += ")";
		return result;
	}

	private static String buildRegexItems(String[] items) {
		return buildRegexItems(items, true, true);
	}

	private static String processRegex(String regex) {
		String result = regex.trim();
		result = result.replaceAll("\\bANYINSN *", opcodesAnys);
		result = result.replaceAll(opcodesInts
				+ "\\\\\\{\\s*(\\d+)\\s*\\\\\\} *", "$1\\\\{$2\\\\} ");
		result = result.replaceAll(opcodesInts + " *", "$1\\\\{\\\\d+\\\\} ");
		result = result.replaceAll(
				"\\bLDC\\\\\\{(.*?)\\\\\\}(?<!\\\\\\\\\\}) *",
				"LDC\\\\{$1\\\\}(?<!\\\\\\\\\\\\}) ");
		result = result.replaceAll("\\bLDC *",
				"LDC\\\\{.*?\\\\}(?<!\\\\\\\\\\\\}) ");
		result = result.replaceAll(opcodeVars + "(_\\d+) *", "$1$2 ");
		result = result.replaceAll(opcodeVars + "(?!_) *", "$1_\\\\d+ ");
		result = result.replaceAll(
				"\\bIINC\\\\\\{\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\\\\\} *",
				"IINC\\\\{$1,$2\\\\} ");
		result = result.replaceAll("\\bIINC\\\\\\{\\s*(\\d+)\\s*\\\\\\} *",
				"IINC\\\\{\\d+,$1\\\\} ");
		result = result.replaceAll("\\bIINC *", "IINC\\\\{\\d+,\\d+\\\\} ");
		result = result.replaceAll(opcodesFields
				+ "\\\\\\{\\s*(.*?)\\s*,\\s*(.*?)\\s*,\\s*(.*?)\\s*\\\\\\} *",
				"$1\\\\{$2,$3,$4\\\\} ");
		result = result.replaceAll(opcodesFields
				+ "\\\\\\{((?:.(?!,))*)\\\\\\} *", "$1\\\\{$2,.*?,.*?\\\\} ");
		result = result.replaceAll(opcodesFields + " *", "$1\\\\{.*?\\\\} ");
		result = result.replaceAll(opcodesMethods
				+ "\\\\\\{\\s*(.*?)\\s*,\\s*(.*?)\\s*,\\s*(.*?)\\s*\\\\\\} *",
				"$1\\\\{$2,$3,$4\\\\} ");
		result = result.replaceAll(opcodesMethods
				+ "\\\\\\{((?:.(?!,))*)\\\\\\} *", "$1\\\\{$2,.*?,.*?\\\\} ");
		result = result.replaceAll(opcodesMethods + " *",
				"$1\\\\{.*?,.*?,.*?\\\\} ");
		result = result.replaceAll(opcodesTypes
				+ "\\\\\\{\\s*(.*?)\\s*\\\\\\} +", "$1\\\\{$2\\\\} ");
		result = result.replaceAll(opcodesTypes + " +", "$1\\\\{\\\\.*?\\\\} ");
		result = result
				.replaceAll(
						"\\bMULTIANEWARRAY\\\\\\{\\s*(\\d+)\\s*,\\s*(.*?)\\s*\\\\\\} *",
						"MULTIANEWARRAY\\\\{$1,$2\\\\} ");
		result = result.replaceAll(
				"\\bMULTIANEWARRAY\\\\\\{\\s*(.*?)\\s*\\\\\\} *",
				"MULTIANEWARRAY\\\\{\\d+,$1\\\\} ");
		result = result.replaceAll("\\bMULTIANEWARRAY *",
				"MULTIANEWARRAY\\\\{\\\\\\d+,.*?\\\\} ");
		result = result.replaceAll("\\bIFINSN *", opcodesIfs + " ");
		return result;
	}

	private MethodNode m;
	private AbstractInsnNode[] instructions;
	private int[] offsets;
	private String representation;

	public RegexInsnFinder(MethodNode m) {
		setMethod(m);
	}

	private AbstractInsnNode[] cleanInsn(AbstractInsnNode[] rawInstructions) {
		List<AbstractInsnNode> cleanedInstructions = new ArrayList<AbstractInsnNode>();
		for (AbstractInsnNode n : rawInstructions)
			if (n.getOpcode() >= 0)
				cleanedInstructions.add(n);
		return cleanedInstructions
				.toArray(new AbstractInsnNode[cleanedInstructions.size()]);
	}

	/**
	 * Refreshes the internal instruction list when you have made changes to the
	 * method.
	 */
	public void refresh() {
		instructions = cleanInsn(m.instructions.toArray());
		offsets = new int[instructions.length];
		representation = "";
		for (int i = 0; i < instructions.length; i++) {
			offsets[i] = -1;
			AbstractInsnNode n = instructions[i];
			if (n.getOpcode() >= 0) {
				if (n.getOpcode() >= opcodes.length)
					try {
						throw new UnexpectedException(
								"Unknown opcode encountered: " + n.getOpcode());
					} catch (UnexpectedException e) {
						e.printStackTrace();
					}
				offsets[i] = representation.length();
				representation += opcodes[n.getOpcode()];
				switch (n.getType()) {
				case AbstractInsnNode.INT_INSN:
					IntInsnNode iin = (IntInsnNode) n;
					representation += "{" + iin.operand + "}";
					break;
				case AbstractInsnNode.LDC_INSN:
					LdcInsnNode lin = (LdcInsnNode) n;
					representation += "{"
							+ lin.cst.toString().replace("}", "\\}") + "}";
					break;
				case AbstractInsnNode.VAR_INSN:
					VarInsnNode vin = (VarInsnNode) n;
					representation += "_" + vin.var;
					break;
				case AbstractInsnNode.IINC_INSN:
					IincInsnNode iiin = (IincInsnNode) n;
					representation += "{" + iiin.var + "," + iiin.incr + "}";
					break;
				case AbstractInsnNode.FIELD_INSN:
					FieldInsnNode fin = (FieldInsnNode) n;
					representation += "{" + fin.desc + "," + fin.owner + ","
							+ fin.name + "}";
					break;
				case AbstractInsnNode.METHOD_INSN:
					MethodInsnNode min = (MethodInsnNode) n;
					representation += "{" + min.desc + "," + min.owner + ","
							+ min.name + "}";
					break;
				case AbstractInsnNode.TYPE_INSN:
					TypeInsnNode tin = (TypeInsnNode) n;
					representation += "{" + tin.desc + "}";
					break;
				case AbstractInsnNode.MULTIANEWARRAY_INSN:
					MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) n;
					representation += "{" + manain.dims + "," + manain.desc
							+ "}";
					break;
				}
				representation += " ";
			}
		}
	}

	public MethodNode getMethod() {
		return m;
	}

	public void setMethod(MethodNode m) {
		this.m = m;
		refresh();
	}

	private AbstractInsnNode[] makeResult(int start, int end) {
		int startIndex = 0;
		int endIndex = -1;
		for (int i = 0; i < offsets.length - 1; i++) {
			int offset = offsets[i];
			if (offset == start)
				startIndex = i;
			if ((offset < end) && (offsets[i + 1] >= end)) {
				endIndex = i;
				break;
			}
		}
		if (endIndex == -1)
			endIndex = offsets.length - 1;
		int length = endIndex - startIndex + 1;
		AbstractInsnNode[] result = new AbstractInsnNode[length];
		System.arraycopy(instructions, startIndex, result, 0, length);
		return result;
	}

	/**
	 * Searches for a regex in the instruction list and returns the first match.
	 * 
	 * @param regex
	 *            the regular expression
	 * @return the matching instructions
	 */
	public AbstractInsnNode[] find(String regex) {
		try {
			Matcher regexMatcher = Pattern.compile(processRegex(regex),
					Pattern.MULTILINE).matcher(representation);
			if (regexMatcher.find())
				return makeResult(regexMatcher.start(), regexMatcher.end());
		} catch (PatternSyntaxException ex) {
			ex.printStackTrace();
		}
		return new AbstractInsnNode[0];
	}

	/**
	 * Searches a regex in an instruction list and returns all matches.
	 * 
	 * @param regex
	 *            the regular expression
	 * @return a list with all sets of matching instructions
	 */
	public List<AbstractInsnNode[]> findAll(String regex) {
		List<AbstractInsnNode[]> results = new ArrayList<AbstractInsnNode[]>();
		try {
			Matcher regexMatcher = Pattern.compile(processRegex(regex),
					Pattern.MULTILINE).matcher(representation);
			while (regexMatcher.find())
				results.add(makeResult(regexMatcher.start(), regexMatcher.end()));
		} catch (PatternSyntaxException ex) {
			ex.printStackTrace();
		}
		return results;
	}

	/**
	 * Searches for a regex in the instruction list and returns all groups for
	 * the first match.
	 * 
	 * @param regex
	 *            the regular expression
	 * @return the groups with matching instructions
	 */
	public AbstractInsnNode[][] findGroups(String regex) {
		try {
			Matcher regexMatcher = Pattern.compile(processRegex(regex),
					Pattern.MULTILINE).matcher(representation);
			if (regexMatcher.find()) {
				AbstractInsnNode[][] result = new AbstractInsnNode[regexMatcher
						.groupCount() + 1][0];
				for (int i = 0; i <= regexMatcher.groupCount(); i++)
					result[i] = makeResult(regexMatcher.start(i),
							regexMatcher.end(i));
				return result;
			}
		} catch (PatternSyntaxException ex) {
			ex.printStackTrace();
		}
		return new AbstractInsnNode[0][0];
	}

	/**
	 * Searches for a regex in the instruction list and returns all groups for
	 * all matches.
	 * 
	 * @param regex
	 *            the regular expression
	 * @return a list with all sets of groups with matching instructions
	 */
	public List<AbstractInsnNode[][]> findAllGroups(String regex) {
		List<AbstractInsnNode[][]> results = new ArrayList<AbstractInsnNode[][]>();
		try {
			Matcher regexMatcher = Pattern.compile(processRegex(regex),
					Pattern.MULTILINE).matcher(representation);
			if (regexMatcher.find()) {
				AbstractInsnNode[][] result = new AbstractInsnNode[regexMatcher
						.groupCount() + 1][0];
				for (int i = 0; i <= regexMatcher.groupCount(); i++)
					result[i] = makeResult(regexMatcher.start(i),
							regexMatcher.end(i));
				results.add(result);
			}
		} catch (PatternSyntaxException ex) {
			ex.printStackTrace();
		}
		return results;
	}
}
