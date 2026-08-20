# Reverse String (LeetCode 344)

## Problem
Given a character array `s`, reverse it in-place.

Example: `s = ['h','e','l','l','o']` → `['o','l','l','e','h']`.

---

## Brute Force Approach
Build a new array by reading `s` from the end to the start, then copy it back (or use a `StringBuilder.reverse()` if working with `String`).

```java
char[] temp = new char[s.length];
for (int i = 0; i < s.length; i++) {
    temp[i] = s[s.length - 1 - i];
}
System.arraycopy(temp, 0, s, 0, s.length);
```

- **Time Complexity:** O(n).
- **Space Complexity:** O(n) — extra array.

---

## Optimal Approach (used in code)
```java
private static void reverseString(char[] s) {
    int i = 0, j = s.length - 1;
    while (i < j) {
        char temp = s[i];
        s[i] = s[j];
        s[j] = temp;
        i++;
        j--;
    }
}
```

### Intuition
Reversing an array is fundamentally about swapping mirrored pairs: the first element with the last, the second with the second-to-last, and so on, until the two pointers meet (or cross) in the middle. Using two pointers moving toward each other from opposite ends lets us do all these swaps in-place with a single pass, without needing anywhere to "stage" the reversed result — each swap only touches elements that haven't been finalized yet, and once `i` and `j` cross, every pair has been swapped exactly once.

- **Time Complexity:** O(n/2) → O(n) — each pointer traverses half the array.
- **Space Complexity:** O(1) — only a single temp variable for swapping, no extra array.

---

## Dry Run
`s = ['h', 'e', 'l', 'l', 'o']`

| step | i | j | s[i] | s[j] | swap result | array after swap |
|------|---|---|------|------|-------------|--------------------|
| 1    | 0 | 4 | h    | o    | swap        | [o,e,l,l,h] |
| 2    | 1 | 3 | e    | l    | swap        | [o,l,l,e,h] |
| 3    | 2 | 2 | —    | —    | i == j, loop condition `i < j` fails | [o,l,l,e,h] |

**Result:** `s = ['o', 'l', 'l', 'e', 'h']` → `"olleh"` ✅
