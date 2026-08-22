# LeetCode 3 - Longest Substring Without Repeating Characters

Given a string `s`, find the length of the longest substring without repeating characters.

## Brute Force

**Intuition (as implemented in the file, `lengthOfLongestSubstringBruteForce`):** For every starting index `i`, extend a window to the right (`j`) adding characters to a set, stopping as soon as a duplicate is found. This directly checks, for each possible starting point, how long a repeat-free run can be — correct because it re-derives the answer from scratch at each starting position, but repeats a lot of work since each inner scan restarts from an empty set.

**Time Complexity:** O(n²) — for each of the n starting points, we may scan up to n characters.
**Space Complexity:** O(min(n, charset size)) for the set.

```java
private static int lengthOfLongestSubstringBruteForce(String s) {
    int longestSeq = 0;
    for (int i = 0; i < s.length(); i++) {
        Set<Character> set = new HashSet<>();
        int curLargest = 0;
        for (int j = i; j < s.length(); j++) {
            if (set.add(s.charAt(j))) {
                curLargest++;
                longestSeq = Math.max(curLargest, longestSeq);
            } else {
                break;
            }
        }
    }
    return longestSeq;
}
```

## Optimal Solution

**Intuition:** Use a sliding window with two pointers and a set of characters currently in the window. Expand the window by moving `right` and adding characters. When a duplicate character is encountered, we know the earliest possible valid window start must be *after* the previous occurrence of that character, so shrink the window from the left — removing characters from the set — until the duplicate is gone. Because both pointers only ever move forward, each character is added and removed from the set at most once, avoiding the brute force's repeated rescanning.

**Time Complexity:** O(n) — both pointers traverse the string at most once each.
**Space Complexity:** O(min(n, charset size)) for the set.

```java
private static int lengthOfLongestSubstring(String s) {
    if (s == null || s.length() == 0) return 0;
    int longestSeq = 0;
    Set<Character> set = new HashSet<>();
    int left = 0, right = 0;
    while (right < s.length()) {
        char c = s.charAt(right);
        while (set.contains(c)) {
            set.remove(s.charAt(left));
            left++;
        }
        set.add(c);
        longestSeq = Math.max(longestSeq, right - left + 1);
        right++;
    }
    return longestSeq;
}
```
