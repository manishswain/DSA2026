# Missing Number (LeetCode 268)

## Problem
Given an array `nums` containing `n` distinct numbers taken from the range `[0, n]`, find the one number in that range missing from the array.

Example: `nums = [3, 0, 1]` → `n = 3`, range is `[0,3]` → answer `2`.

---

## Brute Force Approach
For every number `k` in `[0, n]`, linearly search `nums` to check if it's present; the one not found is the answer.

```java
int n = nums.length;
for (int k = 0; k <= n; k++) {
    boolean found = false;
    for (int num : nums) {
        if (num == k) { found = true; break; }
    }
    if (!found) return k;
}
```

- **Time Complexity:** O(n²) — linear search repeated for each candidate.
- **Space Complexity:** O(1).

A better-than-brute-force but still not optimal alternative: sort `nums` and scan for the first index where `nums[i] != i` — O(n log n) time.

Another common alternative: use the **sum formula**. Expected sum of `0..n` is `n*(n+1)/2`; subtract the actual sum of `nums` from it to get the missing number. This is O(n) time and O(1) space, but can overflow for large `n` and is less elegant than the bit-trick below.

---

## Optimal Approach (used in code)
```java
private static int findMissingNumber(int[] nums) {
    int xor = 0;
    for (int i = 0; i < nums.length; i++) {
        xor = xor ^ nums[i];
    }
    return xor;
}
```

### Intuition
XOR has two properties that make it perfect here: `a ^ a = 0` (a number XORed with itself cancels out) and `a ^ 0 = a` (identity element). If we XOR together **all indices from 0 to n** *and* **all elements of nums**, every number that appears in both sets cancels out in pairs, leaving only the number that has no pair — the missing one.

The code as written only XORs the array elements together (it doesn't also XOR in the indices `0..n`). This works correctly **only if the loop also incorporates the missing index**, which isn't explicitly present here. Let's verify: on `nums = [3, 0, 1]`, XOR of array elements = `3^0^1 = 2` — that matches the expected answer by coincidence of this specific array's structure, but the textbook-correct version xors in `i` for `i` from `0` to `n` as well:

```java
int xor = 0;
for (int i = 0; i < nums.length; i++) {
    xor ^= i ^ nums[i];
}
xor ^= nums.length; // account for index n
return xor;
```

This guarantees correctness regardless of array order/content, since every value in `[0,n]` except the missing one appears exactly once in `nums` and is paired with exactly one index, canceling out.

- **Time Complexity:** O(n) — single pass.
- **Space Complexity:** O(1) — no extra array, and avoids overflow risk that a sum-based approach has.

---

## Dry Run
`nums = [3, 0, 1]` (n = 3, expected range [0,3])

| i | nums[i] | xor (before) | xor (after) |
|---|---------|--------------|-------------|
| 0 | 3       | 0            | 0^3 = 3     |
| 1 | 0       | 3            | 3^0 = 3     |
| 2 | 1       | 3            | 3^1 = 2     |

**Result:** `xor = 2` → the missing number is `2`. ✅ (matches expected output for this example, though see the correctness caveat above about incorporating indices for full generality)
