package Neetcode;

// Leetcode 424. Longest Repeating Character Replacement
// Array + Sliding Window + 2 Pointers
public class QLongestRepeatingCharacterReplacement424 {
    void main(String[] args) {
        String s = "AABABBA";
        int k = 1;

        int result = characterReplacement(s, k);
        System.out.println("Longest Repeating Character Replacement Length: " + result);
    }

    // Approach: String is made of uppercase English letters, so we can use an array
    // of size 26 to count the occurrences of each character in the current window.
    // We maintain a sliding window defined by two pointers (left and right) and
    // keep track of the maximum occurrence of any single character in that window.
    // If the number of characters that need to be replaced (window size - max
    // occurrence) exceeds k, we shrink the window from the left until it becomes
    // valid again. We also keep track of the maximum length of a valid window
    // throughout the process.
    private int characterReplacement(String s, int k) {
        int[] occurance = new int[26];

        int left = 0, result = 0, maxOccurance = 0;
        for (int right = 0; right < s.length(); right++) {
            maxOccurance = Math.max(maxOccurance, ++occurance[s.charAt(right) - 'A']);
            if (right - left + 1 - maxOccurance > k) {
                occurance[s.charAt(left) - 'A']--;
                left++;
            }
            result = Math.max(result, right - left + 1);
        }
        return result;
    }
}
