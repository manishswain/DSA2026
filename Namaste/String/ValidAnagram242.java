package Namaste.String;

import java.util.HashMap;
import java.util.Map;

public class ValidAnagram242 {
    public static void main(String[] args) {
        String s = "aacc";
        String t = "ccac";
        boolean isAnagram = isAnagram(s, t);
        System.out.println("Is Anagram: " + isAnagram);
    }

    private static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Integer> frequencyMap = new HashMap<>();

        for (char ch : s.toCharArray()) {
            frequencyMap.put(ch, frequencyMap.getOrDefault(ch, 0) + 1);
        }

        for (char ch : t.toCharArray()) {
            if (!frequencyMap.containsKey(ch)) {
                return false;
            }

            int updatedCount = frequencyMap.get(ch) - 1;
            if (updatedCount < 0) {
                return false;
            }

            frequencyMap.put(ch, updatedCount);
        }
        return true;
    }
}
