# Length of Last Word (LeetCode 58)

## Problem
Given a string `s` consisting of words and spaces, return the length of the last word in the string. A word is a maximal substring of non-space characters.

Example: `s = " a"` → `1`.

---

## Brute Force Approach
Split the string on whitespace using `String.split` (or `trim` + `split`), then return the length of the last element in the resulting array.

```java
String[] words = s.trim().split("\\s+");
return words[words.length - 1].length();
```

- **Time Complexity:** O(n) — but with higher constants due to regex splitting and creating an array of substrings.
- **Space Complexity:** O(n) — for the array of split words.

---

## Optimal Approach (used in code)
```java
private static int lengthOfLastWord(String s) {
    int len = 0;
    for (int i = s.length() - 1; i >= 0; i--) {
        if (len < 1 && s.charAt(i) == ' ') {

        } else if (len > 0 && s.charAt(i) == ' ') {
            return len;
        } else {
            len++;
        }
    }
    return len;
}
```

### Intuition
Instead of splitting the whole string, scan backward from the end. Trailing spaces (encountered before we've started counting, i.e. `len < 1`) are simply skipped — they're just padding after the last word. Once we hit the first non-space character, `len` starts incrementing for every character of the last word. The moment we hit a space *after* `len` has become positive, that space marks the start boundary of the last word, so we return immediately without scanning any further left. If we reach the beginning of the string without hitting that boundary space, the whole scanned string was one word, and `len` is returned as-is.

This avoids building any intermediate array/string — it's a single reverse pass with O(1) extra state (`len`), stopping as early as possible once the last word's start is found.

- **Time Complexity:** O(n) worst case, but often better in practice since it stops as soon as the last word's leading boundary is found.
- **Space Complexity:** O(1) — only a counter is used.

---

## Dry Run
`s = " a"` (indices: 0=' ', 1='a')

| i | s.charAt(i) | len (before) | condition matched | action | len (after) |
|---|-------------|---------------|--------------------|--------|--------------|
| 1 | 'a' | 0 | else (not space) | len++ | 1 |
| 0 | ' ' | 1 | `len > 0 && space` | return len | returns 1 |

**Result:** `1` ✅
