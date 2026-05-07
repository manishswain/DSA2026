package Warmup;

public class CPalindrome {
    public static void main(String[] args) {
        int x = 1331;
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
// Approach: We can reverse the second half of the number and compare it with
// the first half. If they are the same, then the number is a palindrome.
// We also need to handle edge cases such as negative numbers and numbers that
// end with 0 (except for 0 itself).
// Time Complexity: O(log10(n)) where n is the input number, because we are
// processing each digit of the number at most once.
// Space Complexity: O(1) because we are using only a constant amount of extra
// space.
