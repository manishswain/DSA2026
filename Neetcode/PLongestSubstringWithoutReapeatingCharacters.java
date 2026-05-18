package Neetcode;

import java.util.HashSet;
import java.util.Set;

public class PLongestSubstringWithoutReapeatingCharacters {
    public static void main(String[] args) {
        String s = "abcabcbb";
        System.out.println(lengthOfLongestSubstring(s));
    }

    private static int lengthOfLongestSubstring(String s) {
        int curLargest = 0, longestSeq = 0;
        Set<Character> set = new HashSet<>();
        for (char c : s.toCharArray()) {
            if (set.add(c)) {
                curLargest++;
                longestSeq = Math.max(curLargest, longestSeq);
            } else {
                longestSeq = Math.max(curLargest, longestSeq);
                set = new HashSet<>();
                set.add(c);
                curLargest = 1;
            }
        }
        return longestSeq;
    }
}
