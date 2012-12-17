

public class Test {

	static int a_decoder = 1;
	public void testArithmetic() {
		int x = 1;
		int y = 12;
		int c = -x + y;
		
		if (-(c + -x + y) + x - -y - -x + 2 == 12 - -x) {
			System.out.println("...");
		}
		
	}
	
	public void test2() {
	}
	
}
