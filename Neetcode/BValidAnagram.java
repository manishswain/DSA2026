package Neetcode;

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
