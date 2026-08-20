# Single Number (LeetCode 136)

## Problem
Given a non-empty array `nums` where every element appears **twice** except for one, find that single element. Must run in linear time and ideally without extra memory.

Example: `nums = [2, 2, 1]` → answer `1`.

---

## Brute Force Approach
For each element, count how many times it appears in the array (or use a `HashMap` to count occurrences); return the one with count `1`.

```java
HashMap<Integer, Integer> count = new HashMap<>();
for (int num : nums) {
    count.put(num, count.getOrDefault(num, 0) + 1);
}
for (int num : count.keySet()) {
    if (count.get(num) == 1) return num;
}
```

- **Time Complexity:** O(n).
- **Space Complexity:** O(n) — extra hash map.

---

## Optimal Approach (used in code)
```java
private static int findSingleNumber(int[] nums) {
    int result = 0;
    for (int num : nums) {
        result ^= num;
    }
    return result;
}
```

### Intuition
XOR is commutative, associative, and self-canceling: `a ^ a = 0` and `a ^ 0 = a`. If every number except one appears exactly twice, then XOR-ing the entire array together causes every paired number to cancel itself out (`a ^ a = 0`), regardless of the order they appear in. What survives at the end is exactly the one number with no pair, since XOR-ing it with the accumulated `0` just returns itself. This turns a "count occurrences" problem into a single running XOR — no hashing, no extra memory, and it naturally handles any order or arrangement of the pairs.

- **Time Complexity:** O(n) — single pass.
- **Space Complexity:** O(1) — one accumulator variable, no auxiliary structure.

---

## Dry Run
`nums = [2, 2, 1]`

| num | result (before) | result (after) |
|-----|-------------------|-----------------|
| 2   | 0                 | 0^2 = 2         |
| 2   | 2                 | 2^2 = 0         |
| 1   | 0                 | 0^1 = 1         |

**Result:** `result = 1` ✅
