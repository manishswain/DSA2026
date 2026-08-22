package Neetcode;

//LeetCode 76 - Minimum Window Substring
//Sliding Window - Hard
public class SMinimumWindowSubstring76 {
    public static void main(String[] args) {
        String s = "ADOBECODEBANC";
        String t = "ABC";
        System.out.println(minWindowBruteForce(s, t));
    }

    // Brute Force Approach: Generate all possible substrings of s and check if they
    // contain all characters of t. Keep track of the minimum length substring that
    // satisfies the condition.
    // Time Complexity: O(n^3) in the worst case, where n is the length of string s
    // (since we generate O(n^2) substrings and checking each substring takes O(n)
    // time).
    // Space Complexity: O(1) if we ignore the space used for the output, otherwise
    // O(n) for storing the minimum window substring.
    private static String minWindowBruteForce(String s, String t) {
        String result = "";
        for (int i = 0; i < s.length(); i++) {
            for (int j = i + 1; j <= s.length(); j++) {
                String subStr = s.substring(i, j);
                if (containsAllChars(subStr, t) && (result.length() == 0 || subStr.length() < result.length())) {
                    result = subStr;
                }
            }
        }
        return result;
    }

    private static boolean containsAllChars(String subStr, String t) {
        int[] charCount = new int[128]; // Assuming ASCII character set
        for (char c : subStr.toCharArray()) {
            charCount[c]++;
        }
        for (char c : t.toCharArray()) {
            if (charCount[c] == 0) {
                return false;
            }
            charCount[c]--;
        }
        return true;
    }
}
