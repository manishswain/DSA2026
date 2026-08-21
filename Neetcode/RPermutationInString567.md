# LeetCode 567 - Permutation in String

Given two strings `s1` and `s2`, return true if `s2` contains a permutation of `s1` as a contiguous substring (i.e., one of `s1`'s permutations is a substring of `s2`).

## Brute Force

**Intuition:** A permutation of `s1` has the exact same character frequency counts as `s1`, just reordered. So the naive approach is to generate all permutations of `s1` and check whether any of them appear as a substring in `s2` — or equivalently, slide a window of length `s1.length()` across `s2` and, for each window, sort both the window and `s1` and compare. This works because two strings are permutations of each other iff their sorted forms match, but sorting repeatedly for every window is expensive.

**Time Complexity:** O(n · m log m), where n = `s2.length()` and m = `s1.length()` (n windows, each requiring an O(m log m) sort/compare).
**Space Complexity:** O(m) for the sorted window copy.

```java
private static boolean checkInclusionBruteForce(String s1, String s2) {
    int m = s1.length();
    char[] sorted1 = s1.toCharArray();
    Arrays.sort(sorted1);
    String target = new String(sorted1);
    for (int i = 0; i + m <= s2.length(); i++) {
        char[] window = s2.substring(i, i + m).toCharArray();
        Arrays.sort(window);
        if (new String(window).equals(target)) return true;
    }
    return false;
}
```

## Optimal Solution

**Intuition:** Instead of sorting, represent character composition with fixed-size (26-length) frequency arrays, since only lowercase letters are involved. Build a frequency map for `s1` and for the first window of `s2` of the same length. Then slide the window one character at a time: incrementing the count for the character entering the window and decrementing the count for the character leaving it. At each step, compare the two frequency arrays — equal arrays mean the current window is a permutation of `s1`. This avoids re-sorting or rebuilding counts from scratch for each window.

**Time Complexity:** O(n · 26) ≈ O(n), where n = `s2.length()` — sliding is O(n) windows, each comparison is O(26).
**Space Complexity:** O(26) = O(1) for the two frequency arrays.

```java
private static boolean checkInclusion(String s1, String s2) {
    if (s1.length() > s2.length()) return false;
    int[] s1Map = new int[26];
    int[] s2Map = new int[26];
    for (int i = 0; i < s1.length(); i++) {
        s1Map[s1.charAt(i) - 'a']++;
        s2Map[s2.charAt(i) - 'a']++;
    }
    for (int i = 0; i < s2.length() - s1.length(); i++) {
        if (matches(s1Map, s2Map)) return true;
        s2Map[s2.charAt(s1.length() + i) - 'a']++;
        s2Map[s2.charAt(i) - 'a']--;
    }
    return matches(s1Map, s2Map);
}
```
