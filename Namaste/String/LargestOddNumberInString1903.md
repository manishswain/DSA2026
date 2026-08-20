# Largest Odd Number in String (LeetCode 1903)

## Problem
You're given a string `num`, representing a large integer. Return the largest-valued odd integer (as a string) that is a non-empty substring of `num`, formed by removing some suffix (possibly none) from `num`. Return an empty string `""` if no odd integer exists.

Example: `num = "35427"` → `"35427"` (already ends in an odd digit `7`).

---

## Brute Force Approach
Generate every prefix of `num` (from longest to shortest), and return the first one whose last digit is odd.

```java
for (int len = num.length(); len >= 1; len--) {
    String prefix = num.substring(0, len);
    char last = prefix.charAt(prefix.length() - 1);
    if ((last - '0') % 2 != 0) {
        return prefix;
    }
}
return "";
```

- **Time Complexity:** O(n^2) — creating a new substring of up to length n for each of the n possible prefixes.
- **Space Complexity:** O(n) — for the substrings created (discarded except the answer).

---

## Optimal Approach (used in code)
```java
private static String largestOddNumber(String num) {
    for (int i = num.length() - 1; i >= 0; i--) {
        if ((int) num.charAt(i) % 2 != 0) {
            return num.substring(0, i + 1);
        }
    }
    return "";
}
```

### Intuition
A numeric string's largest odd-valued prefix is entirely determined by where the rightmost odd digit sits: dropping the suffix after any odd digit yields a valid odd number (since a number's parity is only determined by its last digit), and keeping the prefix as long as possible maximizes its value (a longer numeric prefix, with no leading-zero concerns here, is always larger). So instead of testing every prefix, scan `num` from the right once, and the first odd digit found immediately gives the cut point — `num.substring(0, i + 1)` keeps everything up to and including that digit.

If no digit is ever odd, every prefix ends in an even digit, so no valid answer exists and `""` is returned.

- **Time Complexity:** O(n) — single reverse scan, stopping at the first odd digit.
- **Space Complexity:** O(1) extra (excluding the returned substring).

---

## Dry Run
`num = "35427"` (indices: 0='3',1='5',2='4',3='2',4='7')

| i | num.charAt(i) | digit % 2 | odd? | action |
|---|----------------|-----------|------|--------|
| 4 | '7' | 1 | yes | return num.substring(0, 5) |

**Result:** `"35427"` ✅ (the rightmost digit was already odd, so the whole string is returned)
