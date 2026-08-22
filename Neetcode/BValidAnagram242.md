# LeetCode 242 - Valid Anagram

Given two strings `s` and `t`, return `true` if `t` is an anagram of `s` (uses exactly the same characters with the same frequencies), and `false` otherwise.

## Brute Force

Sort both strings and compare them character by character (or check equality of the sorted strings).

**Intuition:** Two strings are anagrams exactly when they contain the same multiset of characters. Sorting normalizes both strings into a canonical order, so if they're anagrams, the sorted versions must be identical.

**Time Complexity:** O(n log n) — dominated by sorting both strings.
**Space Complexity:** O(n) — for the sorted character arrays.

```java
private static boolean isAnagramBrute(String s, String t) {
    if (s.length() != t.length()) return false;
    char[] sc = s.toCharArray();
    char[] tc = t.toCharArray();
    Arrays.sort(sc);
    Arrays.sort(tc);
    return Arrays.equals(sc, tc);
}
```

## Optimal Solution

Use a fixed-size count array (26 slots for lowercase letters). Increment counts for characters in `s`, decrement for characters in `t`. If every count returns to zero, the strings are anagrams.

**Intuition:** Instead of ordering the characters, we just need to verify the two strings share the same character frequencies. A single pass that increments for one string and decrements for the other lets both strings' frequency information cancel out into an all-zero array exactly when they match — avoiding the cost of sorting entirely.

**Time Complexity:** O(n) — single pass over both strings.
**Space Complexity:** O(1) — the count array has a fixed size of 26, independent of input length.

```java
private static boolean isAnagram(String s, String t) {
    int[] countArr = new int[26];
    if (s.length() != t.length()) return false;

    for (char c : s.toCharArray()) countArr[c - 'a']++;
    for (char c : t.toCharArray()) countArr[c - 'a']--;

    for (int i : countArr) {
        if (i != 0) return false;
    }
    return true;
}
```
