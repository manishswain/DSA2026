# LeetCode 424 - Longest Repeating Character Replacement

Given a string `s` of uppercase letters and an integer `k`, you may replace up to `k` characters in the string with any other uppercase letter. Return the length of the longest substring containing a single repeating character achievable after at most `k` replacements.

## Brute Force

**Intuition:** For every possible substring, count the frequency of each character within it, find the most frequent character, and check whether replacing all the *other* characters (`length - maxFreq`) costs at most `k`. Track the longest substring where this holds. This is correct because it directly evaluates the replacement-cost condition for every window, but it recomputes character counts from scratch for every substring.

**Time Complexity:** O(n³) (or O(n² · 26) with an optimized count) — O(n²) substrings, each needing an O(n) or O(26) scan to find max frequency.
**Space Complexity:** O(26) = O(1) per substring check.

```java
private static int characterReplacementBruteForce(String s, int k) {
    int result = 0;
    for (int i = 0; i < s.length(); i++) {
        int[] count = new int[26];
        int maxFreq = 0;
        for (int j = i; j < s.length(); j++) {
            count[s.charAt(j) - 'A']++;
            maxFreq = Math.max(maxFreq, count[s.charAt(j) - 'A']);
            int windowLen = j - i + 1;
            if (windowLen - maxFreq <= k) {
                result = Math.max(result, windowLen);
            }
        }
    }
    return result;
}
```

## Optimal Solution

**Intuition:** Use a sliding window with a 26-size frequency array and track `maxOccurrence`, the count of the most frequent character seen in the current window. A window of length `windowSize` is valid (achievable with ≤ k replacements) exactly when `windowSize - maxOccurrence <= k`, since every character other than the most frequent one would need to be replaced. When the window becomes invalid, shrink it from the left by one. Notably, `maxOccurrence` is never decremented on shrink — this is safe because the algorithm only cares about the *longest* valid window ever seen; even if `maxOccurrence` becomes stale (too high) after a shrink, the window simply won't grow again until a genuinely higher frequency is found, so the recorded max window length is never overstated.

**Time Complexity:** O(n) — each pointer moves forward at most n times.
**Space Complexity:** O(26) = O(1) for the frequency array.

```java
private int characterReplacement(String s, int k) {
    int[] occurrence = new int[26];
    int left = 0, result = 0, maxOccurrence = 0;
    for (int right = 0; right < s.length(); right++) {
        maxOccurrence = Math.max(maxOccurrence, ++occurrence[s.charAt(right) - 'A']);
        if (right - left + 1 - maxOccurrence > k) {
            occurrence[s.charAt(left) - 'A']--;
            left++;
        }
        result = Math.max(result, right - left + 1);
    }
    return result;
}
```
