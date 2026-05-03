package Warmup;

public class IPowerOfTwo {
    public static void main(String[] args) {
        int n = 0;
        boolean result = isPowerOfTwo(n);
        System.out.println(result);
    }

    private static boolean isPowerOfTwo(int n) {
        if (n == 0)
            return false;
        while (n != 1) {
            if (checkDivisibleBy2(n)) {
                n /= 2;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean checkDivisibleBy2(int n) {
        return n % 2 == 0;
    }

}
