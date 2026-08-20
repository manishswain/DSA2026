# Merge Sorted Arrays (LeetCode 88)

## Problem
Given two sorted arrays `nums1` (with length `m + n`, where the last `n` slots are placeholder space) and `nums2` (length `n`), merge `nums2` into `nums1` in-place so `nums1` becomes one sorted array of length `m + n`.

Example: `nums1 = [0]`, `m = 0`, `nums2 = [1]`, `n = 1` → `nums1` becomes `[1]`.

---

## Brute Force Approach
Copy all of `nums2` into the tail of `nums1`, then sort the whole array.

```java
for (int i = 0; i < n; i++) {
    nums1[m + i] = nums2[i];
}
Arrays.sort(nums1);
```

- **Time Complexity:** O((m+n) log(m+n)) due to sorting.
- **Space Complexity:** O(1) extra (in-place sort) or O(m+n) depending on sort implementation.

This ignores the fact that both arrays are *already sorted*, wasting the opportunity to merge in linear time.

---

## Optimal Approach (used in code)
```java
private static void merge(int[] nums1, int m, int[] nums2, int n) {
    int i = m - 1, j = n - 1;
    for (int p = m + n - 1; p >= 0; p--) {
        if (j < 0)
            break;
        if (i >= 0 && nums1[i] > nums2[j]) {
            nums1[p] = nums1[i--];
        } else {
            nums1[p] = nums2[j--];
        }
    }
}
```

### Intuition
This is the classic two-pointer merge from merge sort, but run **backwards** instead of forwards. The forward merge (as in merge sort's merge step) needs a separate buffer because writing the smallest element to the front would overwrite data in `nums1` that hasn't been read yet. But `nums1` has extra empty space at the *end* (`m + n` total length, only `m` used). So instead of merging from the front, we merge from the **back**: compare the largest remaining elements of each array (`nums1[i]` and `nums2[j]`), and place the bigger one at the last unfilled position `p`. Since we always write to a slot at or after the elements we still need to read, we never overwrite unread data — this eliminates the need for extra space entirely.

If `nums2` is exhausted first (`j < 0`), `nums1`'s remaining prefix is already in the correct place, so we can stop. If `nums1` is exhausted first (`i < 0`), whatever remains of `nums2` is copied over by falling into the `else` branch.

- **Time Complexity:** O(m + n) — single backward pass.
- **Space Complexity:** O(1) — merges in-place, no extra array.

---

## Dry Run
`nums1 = [1,2,3,0,0,0]`, `m = 3`, `nums2 = [2,5,6]`, `n = 3`
(the code file's own sample is trivial: `nums1=[0], m=0, nums2=[1], n=1`, so here's a richer trace)

Initial: `i = 2` (nums1[2]=3), `j = 2` (nums2[2]=6), `p = 5`

| p | i | j | nums1[i] | nums2[j] | comparison | write nums1[p] | new i | new j |
|---|---|---|----------|----------|------------|-----------------|-------|-------|
| 5 | 2 | 2 | 3        | 6        | 3 < 6      | 6               | 2     | 1     |
| 4 | 2 | 1 | 3        | 5        | 3 < 5      | 5               | 2     | 0     |
| 3 | 2 | 0 | 3        | 2        | 3 > 2      | 3               | 1     | 0     |
| 2 | 1 | 0 | 2        | 2        | 2 not > 2  | 2 (from nums2)  | 1     | -1    |
| 1 | 1 | -1| —        | —        | j < 0      | break           | —     | —     |

**Result:** `nums1 = [1, 2, 2, 3, 5, 6]` ✅ sorted merge.

For the trivial sample in the file (`nums1=[0], m=0, nums2=[1], n=1`): `i=-1, j=0, p=0` → since `i < 0`, falls to else branch → `nums1[0] = nums2[0] = 1`, `j` becomes `-1`. Loop ends. **Result:** `nums1 = [1]`.
