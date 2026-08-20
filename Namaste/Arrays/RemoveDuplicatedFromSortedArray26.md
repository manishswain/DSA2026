# Remove Duplicates from Sorted Array (LeetCode 26)

## Problem
Given a sorted array `nums`, remove duplicates in-place so each unique element appears only once, and return the new length. The relative order of elements is kept, and elements beyond the returned length don't matter.

Example: `nums = [0,0,1,1,1,2,2,3,3,4]` → new length `5`, array becomes `[0,1,2,3,4,...]`.

---

## Brute Force Approach
Use a `Set` (or a temp list) to collect unique elements in order, then copy them back into the original array.

```java
LinkedHashSet<Integer> set = new LinkedHashSet<>();
for (int num : nums) set.add(num);
int idx = 0;
for (int num : set) nums[idx++] = num;
return idx;
```

- **Time Complexity:** O(n) average, but with extra overhead (hashing) and doesn't exploit sortedness.
- **Space Complexity:** O(n) — extra set.

---

## Optimal Approach (used in code)
```java
private static int removeDuplicates(int[] nums) {
    if (nums.length == 0) {
        return 0;
    }
    int j = 0;
    for (int i = 1; i < nums.length; i++) {
        if (nums[i] > nums[j]) {
            j++;
            nums[j] = nums[i];
        }
    }
    return j + 1;
}
```

### Intuition
Because the array is **sorted**, all duplicates of a value are guaranteed to sit next to each other. This means we don't need a set to track "have I seen this before" — we only need to compare each element to the *last unique element we've written* (`nums[j]`). If the current element (`nums[i]`) is strictly greater than `nums[j]`, it must be a new, never-before-seen value (since sorted order means nothing equal to it could appear later after a larger value), so we advance `j` and write it into place. If `nums[i] == nums[j]`, it's a duplicate and we simply skip it by not advancing `j`.

This is the classic slow-fast two-pointer technique: `j` tracks the "last confirmed unique" position, `i` scans ahead looking for the next distinct value.

- **Time Complexity:** O(n) — single pass.
- **Space Complexity:** O(1) — in-place, no extra data structure.

---

## Dry Run
`nums = [0, 0, 1, 1, 1, 2, 2, 3, 3, 4]`

| i | nums[i] | nums[j] | nums[i] > nums[j]? | action | j (after) | array snapshot |
|---|---------|---------|----------------------|--------|-----------|-----------------|
| 1 | 0       | 0 (j=0) | no                   | skip   | 0         | [0,0,1,1,1,2,2,3,3,4] |
| 2 | 1       | 0 (j=0) | yes                  | j=1, nums[1]=1 | 1 | [0,1,1,1,1,2,2,3,3,4] |
| 3 | 1       | 1 (j=1) | no                   | skip   | 1         | — |
| 4 | 1       | 1 (j=1) | no                   | skip   | 1         | — |
| 5 | 2       | 1 (j=1) | yes                  | j=2, nums[2]=2 | 2 | [0,1,2,1,1,2,2,3,3,4] |
| 6 | 2       | 2 (j=2) | no                   | skip   | 2         | — |
| 7 | 3       | 2 (j=2) | yes                  | j=3, nums[3]=3 | 3 | [0,1,2,3,1,2,2,3,3,4] |
| 8 | 3       | 3 (j=3) | no                   | skip   | 3         | — |
| 9 | 4       | 3 (j=3) | yes                  | j=4, nums[4]=4 | 4 | [0,1,2,3,4,2,2,3,3,4] |

**Result:** `j + 1 = 5` → new length is `5`, and `nums[0..4] = [0, 1, 2, 3, 4]` ✅
