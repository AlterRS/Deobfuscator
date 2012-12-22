public class Test {
	static int[] anIntArray1 = new int[64];
	static int anInt1 = -30241709;
	public static void test(int i) {
		anIntArray1[(anInt1 += -30241709) * 1450153947 - 1] = 5;
		
		if(anInt1 * 1450153947 == i) {
			System.out.println("yay");
		} else {
			System.out.println("noo");
		}
	}
}
