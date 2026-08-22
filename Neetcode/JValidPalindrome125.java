package Neetcode;

//Two Pointer Pattern Question
//Leetcode 125 - Valid Palindrome
public class JValidPalindrome125 {
    public static void main(String[] args) {
        String s = "A man, a plan, a canal: Panama";
        boolean result = isPalindromeOptimal(s);
        System.out.println("Is the string a valid palindrome? " + result);
    }

    private static boolean isPalindrome(String s) {
        s = s.toLowerCase();
        for (int i = 0, j = s.length() - 1; i < j;) {
            if (!Character.isLetterOrDigit(s.charAt(i))) {
                i++;
                continue;
            }
            if (!Character.isLetterOrDigit(s.charAt(j))) {
                j--;
                continue;
            }
            if (s.charAt(i) == s.charAt(j)) {
                i++;
                j--;
            } else {
                return false;
            }
        }
        return true;
    }

    // Optimal Approach: Two Pointer Pattern
    // We can use two pointers, one starting at the beginning of the string and the
    // other starting at the end. We can move the pointers towards each other while
    // skipping any non-alphanumeric characters. At each step, we compare the
    // characters at the two pointers. If they are not the same (ignoring case),
    // then the string is not a palindrome. If they are the same, we move the
    // pointers inward and continue the process until they meet or cross each other.
    // This approach has a time complexity of O(n) and a space complexity of O(1).
    private static boolean isPalindromeOptimal(String s) {
        int left = 0, right = s.length() - 1;
        while (left < right) {
            while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
                left++;
            }
            while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
                right--;
            }

            if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

}
