# LeetCode 424 - Longest Repeating Character Replacement

Given a string `s` of uppercase letters and an integer `k`, you may replace up to `k` characters in the string with any other uppercase letter. Return the length of the longest substring containing a single repeating character achievable after at most `k` replacements.

## Brute Force

**Intuition:** For every possible substring, count the frequency of each character within it, find the most frequent character, and check whether replacing all the _other_ characters (`length - maxFreq`) costs at most `k`. Track the longest substring where this holds. This is correct because it directly evaluates the replacement-cost condition for every window, but it recomputes character counts from scratch for every substring.

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

**Example:** `s = "ABAB", k = 2`

**Dry Run:**

```
i=0: j=0'A' count[A]=1 maxFreq=1 len=1 (1-1=0<=2) result=1
     j=1'B' count[B]=1 maxFreq=1 len=2 (2-1=1<=2) result=2
     j=2'A' count[A]=2 maxFreq=2 len=3 (3-2=1<=2) result=3
     j=3'B' count[B]=2 maxFreq=2 len=4 (4-2=2<=2) result=4
i=1: j=1'B' count[B]=1 maxFreq=1 len=1 result=4
     j=2'A' count[A]=1 maxFreq=1 len=2 result=4
     j=3'B' count[B]=2 maxFreq=2 len=3 result=4
i=2: j=2'A' count[A]=1 len=1 result=4 | j=3'B' count[B]=1 maxFreq=1 len=2 result=4
i=3: j=3'B' count[B]=1 len=1 result=4
```

Output: `4`

## Optimal Solution

**Intuition:** Use a sliding window with a 26-size frequency array and track `maxOccurrence`, the count of the most frequent character seen in the current window. A window of length `windowSize` is valid (achievable with ≤ k replacements) exactly when `windowSize - maxOccurrence <= k`, since every character other than the most frequent one would need to be replaced. When the window becomes invalid, shrink it from the left by one. Notably, `maxOccurrence` is never decremented on shrink — this is safe because the algorithm only cares about the _longest_ valid window ever seen; even if `maxOccurrence` becomes stale (too high) after a shrink, the window simply won't grow again until a genuinely higher frequency is found, so the recorded max window length is never overstated.

**Time Complexity:** O(n) — each pointer moves forward at most n times.
**Space Complexity:** O(26) = O(1) for the frequency array.

```java
private int characterReplacement(String s, int k) {
    int[] occurrence = new int[26];
    int left = 0, result = 0, maxOccurrence = 0;
    while(right < s.length()) {
        maxOccurrence = Math.max(maxOccurrence, ++occurrence[s.charAt(right) - 'A']);
        if (right - left + 1 - maxOccurrence > k) {
            occurrence[s.charAt(left) - 'A']--;
            left++;
        }
        result = Math.max(result, right - left + 1);
        right++;
    }
    return result;
}
```

**Example:** `s = "ABAB", k = 2`

**Dry Run:**

```
occurrence[26]=0, left=0, result=0, maxOccurrence=0
right=0 'A': occurrence[A]=1, maxOccurrence=1, windowSize=1 (1-1=0<=2) result=max(0,1)=1
right=1 'B': occurrence[B]=1, maxOccurrence=max(1,1)=1, windowSize=2 (2-1=1<=2) result=max(1,2)=2
right=2 'A': occurrence[A]=2, maxOccurrence=max(1,2)=2, windowSize=3 (3-2=1<=2) result=max(2,3)=3
right=3 'B': occurrence[B]=2, maxOccurrence=max(2,2)=2, windowSize=4 (4-2=2<=2) result=max(3,4)=4
```

Output: `4`
