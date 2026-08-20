# Split a String in Balanced Strings (LeetCode 1221)

## Problem
Balanced strings contain an equal number of `'L'` and `'R'` characters. Given a balanced string `s`, split it into the maximum number of balanced substrings, and return that maximum number. Each character can only belong to one substring.

Example: `s = "RLRRLLRLRL"` → `4` (`"RL"`, `"RRLL"`, `"RL"`, `"RL"`).

---

## Brute Force Approach
Try every possible way to cut the string into substrings and check which combination yields the maximum count of balanced pieces — e.g. recursively try every split point and take the maximum, verifying balance with a helper that counts `L`/`R` in a substring.

```java
// Conceptually: for each prefix, if it's balanced, recurse on the remainder
// and take 1 + best(remainder); otherwise extend the prefix.
// This explores overlapping substring boundaries and is exponential without memoization.
```

- **Time Complexity:** Exponential (O(2^n)) without memoization, since every split point could be tried.
- **Space Complexity:** O(n) recursion depth (plus memo table if added).

---

## Optimal Approach (used in code)
```java
private static int balancedStringSplit(String s) {
    int count = 0;
    int ans = 0;
    for (char c : s.toCharArray()) {
        if (c == 'R') {
            count++;
        } else
            count--;

        if (count == 0) {
            ans++;
        }
    }
    return ans;
}
```

### Intuition
Treat `'R'` as `+1` and `'L'` as `-1`, and track a running balance (`count`) while scanning left to right. Whenever `count` returns to `0`, it means the characters seen since the *previous* balance point have an equal number of `L`s and `R`s — i.e., they form a balanced substring. Because we greedily cut the moment balance hits zero, this produces the maximum possible number of pieces: cutting as early as possible never hurts, since any balanced substring can always be decomposed into these earliest-possible balanced chunks (any later cut point would just merge two already-balanced pieces into one, reducing the count).

No explicit substrings ever need to be built — only the running balance and a counter of how many times it hit zero.

- **Time Complexity:** O(n) — single linear scan.
- **Space Complexity:** O(1) — two integer counters.

---

## Dry Run
`s = "RLRRLLRLRL"`

| char | count (after) | count==0? | ans |
|------|----------------|-----------|-----|
| R    | 1              | no        | 0   |
| L    | 0              | yes       | 1   |
| R    | 1              | no        | 1   |
| R    | 2              | no        | 1   |
| L    | 1              | no        | 1   |
| L    | 0              | yes       | 2   |
| R    | 1              | no        | 2   |
| L    | 0              | yes       | 3   |
| R    | 1              | no        | 3   |
| L    | 0              | yes       | 4   |

**Result:** `4` ✅
