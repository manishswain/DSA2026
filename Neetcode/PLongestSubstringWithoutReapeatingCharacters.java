package Neetcode;

import java.util.HashSet;
import java.util.Set;

// Array - Medium - Sliding Window + 2 Pointers
public class PLongestSubstringWithoutReapeatingCharacters {
    public static void main(String[] args) {
        String s = "abatman";
        System.out.println(lengthOfLongestSubstring(s));
    }

    // Optimal Solution - Sliding Window + 2 Pointers
    // Thought Process - Use two pointers to create a sliding window. Move the right
    // pointer to expand the window and add characters to a set until we encounter a
    // duplicate character. When we encounter a duplicate character, move the left
    // pointer to shrink the window until we remove the duplicate character from the
    // set. Keep track of the longest sequence found so far.
    private static int lengthOfLongestSubstring(String s) {
        if (s == null || s.length() == 0)
            return 0;
        if (s.length() == 1)
            return 1;
        int longestSeq = 0;
        Set<Character> set = new HashSet<>();
        int left = 0, right = 0;
        while (right < s.length()) {
            char c = s.charAt(right);
            while (set.contains(c)) {
                set.remove(s.charAt(left));
                left++;
            }
            set.add(c);
            longestSeq = Math.max(longestSeq, right - left + 1);
            right++;
        }
        return longestSeq;

    }

    // Brute Force Solution -
    // Thought Process - Iterate through the string and for each character, add it
    // to a set until we encounter a duplicate character. Keep track of the longest
    // sequence found so far.
    private static int lengthOfLongestSubstringBruteForce(String s) {
        int longestSeq = 0;
        for (int i = 0; i < s.length(); i++) {
            Set<Character> set = new HashSet<>();
            int curLargest = 0;
            for (int j = i; j < s.length(); j++) {
                if (set.add(s.charAt(j))) {
                    curLargest++;
                    longestSeq = Math.max(curLargest, longestSeq);
                } else {
                    longestSeq = Math.max(curLargest, longestSeq);
                    break;
                }
            }
        }
        return longestSeq;
    }
}
