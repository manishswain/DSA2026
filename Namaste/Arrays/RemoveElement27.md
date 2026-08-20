# Remove Element (LeetCode 27)

## Problem
Given an array `nums` and a value `val`, remove all occurrences of `val` in-place and return the new length. Order of remaining elements can change; elements beyond the returned length don't matter.

Example: `nums = [3, 2, 2, 3]`, `val = 3` → new length `2`, remaining elements are `[2, 2]` (in some order).

---

## Brute Force Approach
Build a new array containing only elements that don't equal `val`, then copy back.

```java
int[] temp = new int[nums.length];
int idx = 0;
for (int num : nums) {
    if (num != val) temp[idx++] = num;
}
System.arraycopy(temp, 0, nums, 0, idx);
return idx;
```

- **Time Complexity:** O(n).
- **Space Complexity:** O(n) — extra array.

---

## Optimal Approach (used in code)
```java
private int removeElement(int[] nums, int val) {
    if (nums.length == 0) {
        return 0;
    }
    int j = 0;
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] != val) {
            nums[j] = nums[i];
            j++;
        }
    }
    return j;
}
```

### Intuition
This is the same slow-fast two-pointer compaction pattern used in "Move Zeroes" and "Remove Duplicates": `i` scans every element, and `j` marks where the next "kept" element should be written. Whenever `nums[i]` is not the value we want removed, it's copied to position `j` and `j` advances. Elements equal to `val` are simply never copied — they get silently overwritten by later kept elements. Since `j` never exceeds `i`, we never overwrite an element before we've had the chance to read it, making the in-place rewrite safe.

Unlike "Remove Duplicates," there's no dependency on the array being sorted — this works for any array because we're filtering by a fixed value, not by comparing to a running previous value.

- **Time Complexity:** O(n) — single pass.
- **Space Complexity:** O(1) — in-place, no extra array.

---

## Dry Run
`nums = [3, 2, 2, 3]`, `val = 3`

| i | nums[i] | nums[i] != val? | action | j (after) | array snapshot |
|---|---------|-------------------|--------|-----------|-----------------|
| 0 | 3       | no                 | skip   | 0         | [3,2,2,3] |
| 1 | 2       | yes                | nums[0]=2, j++ | 1 | [2,2,2,3] |
| 2 | 2       | yes                | nums[1]=2, j++ | 2 | [2,2,2,3] |
| 3 | 3       | no                 | skip   | 2         | [2,2,2,3] |

**Result:** `j = 2` → new length is `2`, and `nums[0..1] = [2, 2]` ✅

**Note:** The `main` method in this file is missing `static`, so it won't run as a Java entry point as-is (`void main(String[] args)` needs to be `public static void main(String[] args)` to be a valid program entry point, though newer Java preview features do allow instance `main` methods).
