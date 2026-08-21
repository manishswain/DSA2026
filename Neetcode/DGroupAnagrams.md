# LeetCode 49 - Group Anagrams

Given an array of strings `strs`, group the anagrams together. Anagrams can be returned in any order.

## Brute Force

Sort the characters of each string to produce a canonical key, then group strings sharing the same key using a hash map.

**Intuition:** Anagrams are just character-rearrangements of each other, and sorting removes the ordering, mapping every anagram of a word to the identical sorted string. That sorted string becomes a natural grouping key.

**Time Complexity:** O(n · k log k) — for `n` strings each of max length `k`, sorting each string costs O(k log k).
**Space Complexity:** O(n · k) — storing the sorted keys and grouped strings.

```java
private static List<List<String>> groupAnagrams(String[] strs) {
    List<List<String>> res = new ArrayList<>();
    Map<String, List<String>> map = new HashMap<>();
    for (String s : strs) {
        char[] ch = s.toCharArray();
        Arrays.sort(ch);
        String key = new String(ch);

        map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
    }
    res.addAll(map.values());
    return res;
}
```

## Optimal Solution

Instead of sorting, build a character-frequency count (26 counts for lowercase letters) for each string and use that count signature — encoded as a string like `"2.0.0.1..."` — as the hash map key.

**Intuition:** Sorting each string costs O(k log k) just to arrive at a canonical form. But anagrams are defined entirely by character *frequency*, which we can compute directly in O(k) with a counting pass — skipping the sort altogether and still producing an identical key for any two anagrams.

**Time Complexity:** O(n · k) — counting characters for each of `n` strings of max length `k`.
**Space Complexity:** O(n · k) — storing frequency-based keys and grouped anagrams.

```java
private static List<List<String>> groupAnagramsOptimal(String[] strs) {
    List<List<String>> res = new ArrayList<>();
    Map<String, List<String>> map = new HashMap<>();
    for (String s : strs) {
        int[] count = new int[26];
        for (char ch : s.toCharArray()) count[ch - 'a']++;

        StringBuilder sb = new StringBuilder();
        for (int c : count) sb.append(c).append(".");
        String key = sb.toString();

        map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
    }
    res.addAll(map.values());
    return res;
}
```
