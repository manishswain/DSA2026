package Namaste.String;

import java.util.List;

public class FindMostFrequentVowelAndConsonant {
    public static void main(String[] args) {
        String s = "successes";
        // Implementation for finding most frequent vowel and consonant
        int res = findMostFrequentVowelAndConsonant(s);
        System.out.println("Most frequent vowel and consonant: " + res);
    }

    private static int findMostFrequentVowelAndConsonant(String s) {
        int[] vowel = new int[26];
        int[] consonant = new int[26];
        List<Character> vConstants = List.of('a', 'e', 'i', 'o', 'u');

        for (char c : s.toCharArray()) {
            if (vConstants.contains(c)) {
                vowel[c - 'a']++;
            } else {
                consonant[c - 'a']++;
            }
        }
        int maxVowel = 0, maxConsonant = 0;
        for (int i : vowel) {
            maxVowel = Math.max(maxVowel, i);
        }

        for (int i : consonant) {
            maxConsonant = Math.max(maxConsonant, i);
        }
        return maxVowel + maxConsonant;
    }
}
