# Longest Common Prefix (LeetCode 14)

## Problem
Write a function to find the longest common prefix string amongst an array of strings. If there is no common prefix, return an empty string `""`.

Example: `strs = ["ab", "a"]` → `"a"`.

---

## Brute Force Approach
Compare every string against every other string character-by-character, or repeatedly shrink a candidate prefix (starting as `strs[0]`) by checking if it's a prefix of each remaining string, chopping off the last character whenever it isn't.

```java
String prefix = strs[0];
for (int i = 1; i < strs.length; i++) {
    while (!strs[i].startsWith(prefix)) {
        prefix = prefix.substring(0, prefix.length() - 1);
        if (prefix.isEmpty()) return "";
    }
}
return prefix;
```

- **Time Complexity:** O(S) worst case where S is the sum of all characters, but can involve repeated `substring`/`startsWith` calls, making it effectively O(n * m) with extra constant overhead (n = number of strings, m = length of shortest string).
- **Space Complexity:** O(1) extra (ignoring the prefix string itself).

---

## Optimal Approach (used in code)
```java
private static String longestCommonPrefix(String[] strs) {
    int x = 0;
    while (x < strs[0].length()) {
        char ch = strs[0].charAt(x);
        for (int i = 1; i < strs.length; i++) {
            if (x == strs[i].length() || ch != strs[i].charAt(x)) {
                return strs[0].substring(0, x);
            }
        }
        x++;
    }
    return strs[0];
}
```

### Intuition
This is "vertical scanning": instead of comparing whole strings against each other, walk one column (character index) at a time across all strings simultaneously. For column `x`, take the character from `strs[0]` as the reference and check that every other string has the same character at that position. The moment a string is too short (`x == strs[i].length()`) or has a different character, column `x` breaks the common prefix, so everything before it (`strs[0].substring(0, x)`) is the answer.

Using `strs[0]` as the reference is safe because the common prefix — by definition — must match every string including the first one; if any string diverges from `strs[0]` at position `x`, the common prefix can't extend past `x`. If the loop finishes without breaking, `strs[0]` itself is entirely a prefix of every other string.

- **Time Complexity:** O(S) — S is the sum of all characters across all strings; each character is visited at most once in the worst case before an early exit.
- **Space Complexity:** O(1) — no extra data structures, only the returned substring.

---

## Dry Run
`strs = ["ab", "a"]`

| x | ch = strs[0].charAt(x) | check strs[1] | result |
|---|------------------------|---------------|--------|
| 0 | 'a' | strs[1].charAt(0) = 'a', matches | continue, x=1 |
| 1 | 'b' | x == strs[1].length() (1 == 1) → break | return strs[0].substring(0,1) |

**Result:** `"a"` ✅
