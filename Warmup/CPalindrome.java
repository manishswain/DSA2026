package Warmup;

public class CPalindrome {
    public static void main(String[] args) {
        int x = 1;
        System.out.println(1 / 10);
        boolean result = isPalindrome(x);
        System.out.println(result);
    }

    private static boolean isPalindrome(int x) {
        if (x < 0 || (x % 10 == 0 && x != 0)) {
            return false;
        }
        int reverse = 0, original = x;
        while (reverse < original) {
            reverse = reverse * 10 + x % 10;
            x /= 10;
        }
        return reverse == original || reverse / 10 == original;
    }
}
