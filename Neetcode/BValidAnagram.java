package Neetcode;

//Leetcode 242 - Valid Anagram
public class BValidAnagram {
    public static void main(String[] args) {
        String s = "anagrams";
        String t = "nagaram";

        boolean isAnagram = isAnagram(s, t);
        System.out.println("Is Anagram: " + isAnagram);
    }

    private static boolean isAnagram(String s, String t) {
        int[] countArr = new int[26];

        if (s.length() != t.length()) {
            return false;
        }

        for (char c : s.toCharArray()) {
            countArr[c - 'a']++;
        }

        for (char c : t.toCharArray()) {
            countArr[c - 'a']--;
        }

        for (int i : countArr) {
            if (i != 0) {
                return false;
            }
        }
        return true;
    }
}

// Approach: Count the frequency of each character in both strings and compare
// the counts. If they match, the strings are anagrams.
// Time Complexity: O(n), where n is the length of the strings (since we
// traverse both strings once).
// Space Complexity: O(1), since the count array has a fixed size of 26 (for
// lowercase English letters).
