package Warmup;

public class CPalindrome {
    public static void main(String[] args) {
        int x = 10;
        boolean result = isPalindrome(x);
        System.out.println(result);
    }

    private static boolean isPalindrome(int x) {
        if (x < 0 || (x % 10 == 0 && x != 0)) {
            return false;
        }
        int reversed = 0;
        while (x > reversed) {
            int digit = x % 10;
            reversed = reversed * 10 + digit;
            x /= 10;
        }
        return x == reversed || x == reversed / 10;
    }
}
