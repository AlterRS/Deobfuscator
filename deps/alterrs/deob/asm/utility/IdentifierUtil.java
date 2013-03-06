package alterrs.deob.asm.utility;

/**
 * @author Lazaro Brito
 */
public class IdentifierUtil {
	public static boolean illegalIdentifier(String identifier) {
		return identifier.equals("do") || identifier.equals("if")
				|| identifier.equals("for") || identifier.equals("try")
				|| identifier.equals("int");
	}
}
