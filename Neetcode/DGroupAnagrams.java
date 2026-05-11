package Neetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//HashMap Question
public class DGroupAnagrams {
    public static void main(String[] args) {
        String[] strs = { "bdddddddddd", "bbbbbbbbbbc" };

        System.out.println("Grouped Anagrams: " + groupAnagramsOptimal(strs));
    }

    // Brute Force Approach: Sort each string and use the sorted string as a key in
    // a hash map to group anagrams together.
    // Time Complexity: O(n * k log k), where n is the number of strings and k is
    // the maximum length of a string (due to sorting).
    // Space Complexity: O(n * k), since we are storing the sorted strings as keys
    // in the hash map and the grouped
    private static List<List<String>> groupAnagrams(String[] strs) {
        List<List<String>> res = new ArrayList<>();
        Map<String, List<String>> map = new HashMap<>();
        for (String s : strs) {
            char[] ch = s.toCharArray();
            Arrays.sort(ch);
            String key = new String(ch);

            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(s);
        }
        res.addAll(map.values());
        return res;
    }

    // Optimal Approach: Count the frequency of each character in the string and use
    // the frequency count as a key in a hash map to group anagrams together.
    // Time Complexity: O(n * k), where n is the number of strings and k is the
    // maximum length of a string (since we count the frequency of characters).
    // Space Complexity: O(n * k), since we are storing the frequency counts as keys
    // in the hash map and the grouped anagrams as values.
    private static List<List<String>> groupAnagramsOptimal(String[] strs) {
        List<List<String>> res = new ArrayList<>();
        Map<String, List<String>> map = new HashMap<>();
        int[] count = new int[26];
        for (String s : strs) {
            Arrays.fill(count, 0);
            for (char ch : s.toCharArray()) {
                count[ch - 'a']++;
            }
            StringBuilder sb = new StringBuilder();
            for (int i : count) {
                sb.append(i).append(".");
            }
            String key = sb.toString();
            System.out.println(key);
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(s);
        }
        res.addAll(map.values());
        return res;
    }
}