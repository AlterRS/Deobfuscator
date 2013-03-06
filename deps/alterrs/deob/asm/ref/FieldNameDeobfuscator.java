package alterrs.deob.asm.ref;

import java.util.Set;

import alterrs.asm.tree.ClassNode;
import alterrs.asm.tree.FieldNode;
import alterrs.deob.asm.Renamer;
import alterrs.deob.asm.utility.AbstractTransformer;
import alterrs.deob.asm.utility.ClassCollection;
import alterrs.deob.asm.utility.FieldID;
import alterrs.deob.asm.utility.ID;
import alterrs.deob.asm.utility.IdentifierUtil;

/**
 * @author Lazaro Brito
 */
public class FieldNameDeobfuscator extends AbstractTransformer {
	private int fieldId = 1;

	public FieldNameDeobfuscator() {
		super(true);
	}

	@Override
	public void transform(ClassCollection cc) {
		for (int level = 0; level < 32; level++) {
			for (ClassNode c : cc.getClasses().values()) {
				if (cc.getLevel(c) == level) {
					fieldLoop: for (Object fo : c.fields) {
						FieldNode f = (FieldNode) fo;

						if (f.name.length() <= 3
								&& Renamer.ctx.module.getRefactorer().getField(
										new FieldID(c, f)) == null) {
							boolean illegal = IdentifierUtil
									.illegalIdentifier(f.name);
							boolean force = false;
							if (Renamer.ctx.atr.<Set<ID>> get("ignoredClasses")
									.contains(new ID(c.name))) {
								if (!illegal)
									continue fieldLoop;
								force = true;
							}

							String intro;
							String type;
							String array = "";
							String simpleDesc = f.desc.replace("[", "");

							if (simpleDesc.equals("Z")) {
								type = "Bool";
							} else if (simpleDesc.equals("B")) {
								type = "Byte";
							} else if (simpleDesc.equals("C")) {
								type = "Char";
							} else if (simpleDesc.equals("S")) {
								type = "Short";
							} else if (simpleDesc.equals("I")) {
								type = "Int";
							} else if (simpleDesc.equals("J")) {
								type = "Long";
							} else if (simpleDesc.equals("F")) {
								type = "Float";
							} else if (simpleDesc.equals("D")) {
								type = "Double";
							} else if (simpleDesc.startsWith("L")) {
								type = simpleDesc.substring(1,
										simpleDesc.length() - 1);

								if (Renamer.ctx.module.getRefactorer().getClass(
										new ID(type)) != null) {
									type = Renamer.ctx.module.getRefactorer()
											.getClass(new ID(type));
								}

								if (type.contains("/")) {
									type = type
											.substring(type.lastIndexOf("/") + 1);
								}

							} else {
								return;
							}

							int dimensions = 1;
							for (char ch : f.desc.toCharArray()) {
								if (ch == '[') {
									dimensions++;
								}
							}

							for (int i = 0; i < dimensions - 1; i++) {
								array += "Array";
							}

							if (startsWithVowel(type)) {
								intro = "an";
							} else {
								intro = "a";
							}

							String name = intro
									+ type
									+ array
									+ ((dimensions == 1 && endsWithNumber(type)) ? "_"
											: "") + fieldId++;
							Renamer.ctx.module.getRefactorer().refactorField(
									new FieldID(c, f), name);

							if (force) {
								System.out.println(" ^ Forced to rename field : "
										+ new FieldID(c, f));
							}
						}
					}
				}
			}
		}
	}

	private static char[] VOWELS = { 'a', 'e', 'i', 'o', 'u', 'A', 'E', 'I',
			'O', 'U' };

	private static boolean startsWithVowel(String string) {
		char[] stringChars = string.toCharArray();
		vowelLoop: for (char c : VOWELS) {
			for (char c2 : stringChars) {
				if (Character.isLetter(c2)) {
					if (c2 == c) {
						return true;
					} else {
						continue vowelLoop;
					}
				}
			}
		}
		return false;
	}

	private static boolean endsWithNumber(String string) {
		return Character.isDigit(string.toCharArray()[string.length() - 1]);
	}

	@Override
	public void onFinish() {
		System.out.println(" ^ Refactored " + (fieldId - 1) + " fields!");
	}
}
