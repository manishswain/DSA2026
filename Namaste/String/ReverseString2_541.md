# Reverse String II (LeetCode 541)

## Problem
Given a string `s` and an integer `k`, reverse the first `k` characters for every `2k` characters counting from the start of the string. If there are fewer than `k` characters left in a chunk, reverse all of them. If there are at least `k` but fewer than `2k` characters left, reverse the first `k` and leave the rest as-is.

Example: `s = "abcdefg"`, `k = 2` → `"bacdfeg"`.

---

## Brute Force Approach
Build the result by manually slicing the string into `2k`-sized chunks, reversing the first `k` characters of each chunk using a helper (e.g. `StringBuilder.reverse()`), and concatenating everything back together.

```java
StringBuilder result = new StringBuilder();
for (int start = 0; start < s.length(); start += 2 * k) {
    int firstEnd = Math.min(start + k, s.length());
    String toReverse = s.substring(start, firstEnd);
    result.append(new StringBuilder(toReverse).reverse());
    int secondEnd = Math.min(start + 2 * k, s.length());
    if (firstEnd < secondEnd) {
        result.append(s, firstEnd, secondEnd);
    }
}
return result.toString();
```

- **Time Complexity:** O(n) — but with extra overhead from repeated `substring` calls and `StringBuilder` allocations per chunk.
- **Space Complexity:** O(n) — multiple intermediate strings/builders.

---

## Optimal Approach (used in code)
```java
private static String reverseStr(String s, int k) {
    char str[] = s.toCharArray();

    for (int x = 0; x < s.length(); x = x + 2 * k) {
        int i = x, j = Math.min(i + k - 1, s.length() - 1);
        while (i < j) {
            char temp = str[i];
            str[i] = str[j];
            str[j] = temp;
            i++;
            j--;
        }
    }

    return new String(str);
}
```

### Intuition
Convert the string to a mutable `char[]` once, then jump through it in strides of `2k`. For each stride starting at `x`, only the first `k` characters (indices `x` to `x + k - 1`, clamped to the string's actual end via `Math.min`) need reversing — the trailing `k` characters of the block (if they exist) are left untouched simply by not touching them. The two-pointer swap (`i` from the start, `j` from the clamped end, moving toward each other) reverses that sub-range in place without needing any substring extraction.

Clamping `j` with `Math.min(i + k - 1, s.length() - 1)` elegantly handles both edge cases at once: if fewer than `k` characters remain, `j` naturally stops at the last valid index and the whole remaining tail gets reversed; if `2k` or more characters remain, `j` stops exactly at `x + k - 1`, reversing precisely the first `k` and leaving the rest for the next stride (or untouched, if it falls in the "leave as-is" `k` to `2k-1` range).

- **Time Complexity:** O(n) — every character is visited a constant number of times across all strides.
- **Space Complexity:** O(n) for the output `char[]`/`String` (required for the result), O(1) extra auxiliary space.

---

## Dry Run
`s = "abcdefg"`, `k = 2` → `str = ['a','b','c','d','e','f','g']`

**Stride x=0:** i=0, j=min(0+1,6)=1 → swap str[0],str[1] → `['b','a','c','d','e','f','g']`

**Stride x=4:** i=4, j=min(4+1,6)=5 → swap str[4],str[5] → `['b','a','c','d','f','e','g']`

(x=2 is skipped because the stride increments by `2k=4`: x goes 0 → 4 → 8 (out of bounds); characters at indices 2,3 and 6 are left untouched as required.)

**Result:** `"bacdfeg"` ✅
