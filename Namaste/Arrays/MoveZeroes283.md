# Move Zeroes (LeetCode 283)

## Problem
Given an array `nums`, move all `0`s to the end while maintaining the relative order of the non-zero elements. Must be done in-place.

Example: `nums = [4, 2, 4, 0, 0, 3, 0, 5, 1, 0]` → `[4, 2, 4, 3, 5, 1, 0, 0, 0, 0]`.

---

## Brute Force Approach
Create a new array, copy non-zero elements into it in order, then fill the rest with zeros, and finally copy back into the original array.

```java
int[] temp = new int[nums.length];
int idx = 0;
for (int num : nums) {
    if (num != 0) temp[idx++] = num;
}
// remaining slots in temp are already 0 by default
System.arraycopy(temp, 0, nums, 0, nums.length);
```

- **Time Complexity:** O(n).
- **Space Complexity:** O(n) — extra array.

---

## Optimal Approach (used in code)
```java
private static void moveZeroes(int[] nums) {
    if (nums.length < 2)
        return;

    int j = 0;
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] != 0) {
            nums[j] = nums[i];
            j++;
        }
    }
    for (int i = j; i < nums.length; i++) {
        nums[i] = 0;
    }
}
```

### Intuition
This is a two-pointer / "slow-fast pointer" pattern. `i` scans the whole array looking for non-zero values; `j` marks the next position where a non-zero value should be written. Because `j` never moves faster than `i` (it only advances when we actually place a value), `nums[j]` is always a position we've already fully read past, so overwriting it never destroys unread data. After the scan, `j` equals the count of non-zero elements, and everything from index `j` onward is stale/duplicate data that needs to be zeroed out in a second pass.

This avoids extra space by reusing the array itself as both the read source and write destination, relying on the invariant that the write pointer never overtakes the read pointer.

- **Time Complexity:** O(n) — two linear passes (still O(n) overall).
- **Space Complexity:** O(1) — in-place, no auxiliary array.

---

## Dry Run
`nums = [4, 2, 4, 0, 0, 3, 0, 5, 1, 0]`

**Pass 1 — compaction (j = write pointer):**

| i | nums[i] | non-zero? | action | j (after) | array state |
|---|---------|-----------|--------|-----------|--------------|
| 0 | 4       | yes       | nums[0]=4 | 1 | [4,2,4,0,0,3,0,5,1,0] |
| 1 | 2       | yes       | nums[1]=2 | 2 | [4,2,4,0,0,3,0,5,1,0] |
| 2 | 4       | yes       | nums[2]=4 | 3 | [4,2,4,0,0,3,0,5,1,0] |
| 3 | 0       | no        | skip      | 3 | — |
| 4 | 0       | no        | skip      | 3 | — |
| 5 | 3       | yes       | nums[3]=3 | 4 | [4,2,4,3,0,3,0,5,1,0] |
| 6 | 0       | no        | skip      | 4 | — |
| 7 | 5       | yes       | nums[4]=5 | 5 | [4,2,4,3,5,3,0,5,1,0] |
| 8 | 1       | yes       | nums[5]=1 | 6 | [4,2,4,3,5,1,0,5,1,0] |
| 9 | 0       | no        | skip      | 6 | — |

After pass 1: `j = 6`, array = `[4,2,4,3,5,1,0,5,1,0]` (indices 6-9 still hold stale values).

**Pass 2 — zero fill (i from j=6 to end):**
`nums[6]=0, nums[7]=0, nums[8]=0, nums[9]=0`

**Result:** `[4, 2, 4, 3, 5, 1, 0, 0, 0, 0]` ✅
