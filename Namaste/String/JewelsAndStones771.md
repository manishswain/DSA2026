# Jewels and Stones (LeetCode 771)

## Problem
You're given strings `jewels` and `stones`. Every character in `jewels` is a type of stone that is a jewel, and you want to know how many of the characters in `stones` are also jewels. Letters are case sensitive, so `"a"` and `"A"` are different types.

Example: `jewels = "aA"`, `stones = "aAAbbbb"` → `3`.

---

## Brute Force Approach
For every character in `stones`, scan through `jewels` looking for a match.

```java
int count = 0;
for (char s : stones.toCharArray()) {
    for (char j : jewels.toCharArray()) {
        if (s == j) {
            count++;
            break;
        }
    }
}
```

- **Time Complexity:** O(n * m), where n = length of `stones`, m = length of `jewels`.
- **Space Complexity:** O(1).

---

## Optimal Approach (used in code)
```java
private static int numJewelsInStones(String jewels, String stones) {
    int ans[] = new int[52];
    for (char c : jewels.toCharArray()) {
        if ((int) c >= 97 && (int) c <= 122) {
            ans[c - 'a']++;
        } else {
            ans[c - 'A' + 26]++;
        }
    }
    int count = 0;
    for (char c : stones.toCharArray()) {
        if ((int) c >= 97 && (int) c <= 122) {
            if (ans[c - 'a'] != 0)
                count++;
        } else {
            if (ans[c - 'A' + 26] != 0)
                count++;
        }
    }
    return count;
}
```

### Intuition
Instead of re-scanning `jewels` for every character of `stones`, build a presence array once: 26 slots for lowercase letters and 26 more for uppercase, indexed by `c - 'a'` or `c - 'A' + 26`. Marking a jewel character turns lookups from O(m) linear scans into O(1) array reads. Then a single pass over `stones` checks each character's slot directly.

This is the classic "trade a pass for a lookup table" trick — since the alphabet is fixed-size (52 possible characters), an array beats a HashSet/HashMap on constant factors while keeping the same O(1) lookup guarantee.

- **Time Complexity:** O(n + m) — one pass to build the table, one pass to count.
- **Space Complexity:** O(1) — fixed-size 52-slot array regardless of input size.

---

## Dry Run
`jewels = "aA"`, `stones = "aAAbbbb"`

**Build table from jewels:**
- `'a'` → index 0 → `ans[0] = 1`
- `'A'` → index 26 → `ans[26] = 1`

**Scan stones:**

| char | index | ans[index] | jewel? | count |
|------|-------|------------|--------|-------|
| a    | 0     | 1          | yes    | 1     |
| A    | 26    | 1          | yes    | 2     |
| A    | 26    | 1          | yes    | 3     |
| b    | 1     | 0          | no     | 3     |
| b    | 1     | 0          | no     | 3     |
| b    | 1     | 0          | no     | 3     |
| b    | 1     | 0          | no     | 3     |

**Result:** `3` ✅
