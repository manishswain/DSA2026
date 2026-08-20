# Max Consecutive Ones (LeetCode 485)

## Problem
Given a binary array `nums`, return the maximum number of consecutive `1`s in the array.

Example: `nums = [1, 0, 1, 1, 0, 1]` → answer `2`.

---

## Brute Force Approach
For every index where a run of 1s starts, walk forward counting consecutive 1s, then take the max over all starting points.

```java
int maxC = 0;
for (int i = 0; i < nums.length; i++) {
    int count = 0;
    int j = i;
    while (j < nums.length && nums[j] == 1) {
        count++;
        j++;
    }
    maxC = Math.max(maxC, count);
}
return maxC;
```

- **Time Complexity:** O(n²) worst case (e.g., all 1s — inner loop re-walks the same run from every starting index).
- **Space Complexity:** O(1).

---

## Optimal Approach (used in code)
```java
private static int findMaxConsecutiveOnes(int[] nums) {
    int currC = 1;
    int maxC = Integer.MIN_VALUE;
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] == 1) {
            currC++;
            maxC = Math.max(currC, maxC);
        } else {
            currC = 0;
        }
    }
    return maxC;
}
```

### Intuition
A single pass suffices because a "run" of 1s only grows while we keep seeing 1s, and resets the instant we see a 0. We don't need to know where a run started — only how long the *current* run is. So we keep a running counter `currC`: increment on a `1`, reset to `0` on a `0`, and after every step check whether the current run beat the best run seen so far (`maxC`). This avoids re-walking any run more than once.

- **Time Complexity:** O(n) — single pass.
- **Space Complexity:** O(1).

### ⚠️ Bug in current implementation
`currC` is initialized to `1` instead of `0`, so every run's length is over-counted by one. This happens to produce the correct answer `2` for the sample input `[1,0,1,1,0,1]` purely by coincidence (the actual longest run is `2`, and the off-by-one error still lands on `2` because of how the trace works out), but it will give **wrong answers** on other inputs — e.g. `nums = [1]` should return `1` but this code returns `2`, and `nums = [0,0,0]` should return `0` but this code returns `1` (since `maxC` starts at `MIN_VALUE` and is never updated when all values are 0, it would actually return `Integer.MIN_VALUE`, which is also wrong). The correct initialization is `currC = 0` and `maxC = 0`.

---

## Dry Run (as currently written, showing the bug)
`nums = [1, 0, 1, 1, 0, 1]`

| i | nums[i] | currC (before) | action | currC (after) | maxC (after) |
|---|---------|-----------------|--------|-----------------|--------------|
| 0 | 1       | 1               | ++     | 2               | 2            |
| 1 | 0       | 2               | reset  | 0               | 2            |
| 2 | 1       | 0               | ++     | 1               | 2            |
| 3 | 1       | 1               | ++     | 2               | 2            |
| 4 | 0       | 2               | reset  | 0               | 2            |
| 5 | 1       | 0               | ++     | 1               | 2            |

**Result:** `maxC = 2` (correct for this input, but only by coincidence — see bug note above).
