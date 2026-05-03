package Warmup;

//Given a signed 32-bit integer x, return x with its digits reversed. If reversing x causes the value to go outside the signed 32-bit integer range [-231, 231 - 1], then return 0.
public class DReverseInteger {
    public static void main(String[] args) {
        int x = 1534236469;
        int result = reverse(x);
        System.out.println(result);
    }

    private static int reverse(int x) {
        long reversed = 0;

        while (x != 0) {
            reversed = reversed * 10 + x % 10;
            x /= 10;
        }

        // Check 32-bit integer overflow
        if (reversed > Integer.MAX_VALUE || reversed < Integer.MIN_VALUE)
            return 0;

        return (int) reversed;
    }
}
