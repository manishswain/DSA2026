# LeetCode 76 - Minimum Window Substring

Given two strings `s` and `t`, return the minimum-length substring of `s` that contains every character of `t` (including duplicates). Return an empty string if no such substring exists.

## Brute Force

**Intuition (as implemented in the file):** Generate every possible substring of `s` by trying all start/end index pairs, and for each one check whether it contains all characters of `t` (with correct multiplicity), tracking the shortest one that qualifies. This is correct because it exhaustively checks every candidate substring, but it repeats a full character-count comparison for every single candidate.

**Time Complexity:** O(n³) in the worst case — O(n²) substrings, each requiring O(n) to build and check character counts.
**Space Complexity:** O(1) extra (O(n) if counting the result string).

```java
private static String minWindowBruteForce(String s, String t) {
    String result = "";
    for (int i = 0; i < s.length(); i++) {
        for (int j = i + 1; j <= s.length(); j++) {
            String subStr = s.substring(i, j);
            if (containsAllChars(subStr, t) && (result.length() == 0 || subStr.length() < result.length())) {
                result = subStr;
            }
        }
    }
    return result;
}

private static boolean containsAllChars(String subStr, String t) {
    int[] charCount = new int[128];
    for (char c : subStr.toCharArray()) charCount[c]++;
    for (char c : t.toCharArray()) {
        if (charCount[c] == 0) return false;
        charCount[c]--;
    }
    return true;
}
```

## Optimal Solution

**Intuition:** Use a sliding window with two pointers over `s`, tracking how many of `t`'s required characters (with multiplicity) are currently satisfied by the window, via a frequency map for `t` and a counter for how many distinct required characters currently have enough occurrences. Expand the window with `right` until all of `t`'s characters are covered, then greedily shrink from `left` as much as possible while the window remains valid, recording the minimum-length valid window seen. Each character enters and leaves the window at most once, so unlike the brute force, we never re-derive counts from scratch — the "contains all of t" check becomes an O(1) comparison of a running "characters satisfied" counter instead of rescanning.

**Time Complexity:** O(n + m), where n = `s.length()`, m = `t.length()` — each pointer traverses `s` once, plus O(m) to build the initial frequency map.
**Space Complexity:** O(charset size) = O(1) for the frequency maps.

```java
private static String minWindow(String s, String t) {
    if (s.length() < t.length() || t.isEmpty()) return "";
    Map<Character, Integer> need = new HashMap<>();
    for (char c : t.toCharArray()) need.merge(c, 1, Integer::sum);

    Map<Character, Integer> window = new HashMap<>();
    int required = need.size(), formed = 0;
    int left = 0, bestLen = Integer.MAX_VALUE, bestStart = 0;

    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        window.merge(c, 1, Integer::sum);
        if (need.containsKey(c) && window.get(c).intValue() == need.get(c).intValue()) formed++;

        while (formed == required) {
            if (right - left + 1 < bestLen) {
                bestLen = right - left + 1;
                bestStart = left;
            }
            char lc = s.charAt(left);
            window.put(lc, window.get(lc) - 1);
            if (need.containsKey(lc) && window.get(lc) < need.get(lc)) formed--;
            left++;
        }
    }
    return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
}
```
