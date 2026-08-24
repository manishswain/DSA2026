# LeetCode 238 - Product of Array Except Self

Given an integer array `nums`, return an array `answer` such that `answer[i]` equals the product of all elements of `nums` except `nums[i]`, without using the division operator.

## Brute Force

For each index `i`, loop over the whole array again and multiply together every element except `nums[i]`.

**Intuition:** The definition directly tells us what to compute for each index — the product of everything else — so recomputing that product from scratch for every index satisfies the requirement without needing division.

**Time Complexity:** O(n²) — an inner loop over all elements for each of the `n` outer indices.
**Space Complexity:** O(1) extra (excluding the output array).

```java
private static int[] productExceptSelf(int[] nums) {
    int[] res = new int[nums.length];
    for (int i = 0; i < nums.length; i++) {
        int product = 1;
        for (int j = 0; j < nums.length; j++) {
            if (i != j) product *= nums[j];
        }
        res[i] = product;
    }
    return res;
}
```

**Example:** `nums = [1, 2, 3, 4]`

**Dry Run:**
```
i=0: product of nums[1..3] = 2*3*4 = 24 -> res[0] = 24
i=1: product of nums[0,2,3] = 1*3*4 = 12 -> res[1] = 12
i=2: product of nums[0,1,3] = 1*2*4 = 8  -> res[2] = 8
i=3: product of nums[0,1,2] = 1*2*3 = 6  -> res[3] = 6
```
Output: `[24, 12, 8, 6]`

## Optimal Solution

Make two passes over the array. In the first (left-to-right) pass, `res[i]` accumulates the product of all elements to the *left* of `i`. In the second (right-to-left) pass, multiply `res[i]` by the running product of all elements to the *right* of `i`.

**Intuition:** "Everything except `nums[i]`" is just "everything to its left" multiplied by "everything to its right." Instead of recomputing that product from scratch each time (which repeats work across indices), we can build the left-products and right-products incrementally in two linear sweeps, reusing a single running total, and combine them at each index — avoiding both division and the O(n) recomputation per index.

**Time Complexity:** O(n) — two linear passes over the array.
**Space Complexity:** O(1) extra (excluding the output array); only two running products (`pre`, `post`) are kept.

```java
private static int[] productExceptSelfOptimal(int[] nums) {
    int[] res = new int[nums.length];
    Arrays.fill(res, 1);
    int pre = 1, post = 1;

    for (int i = 0; i < nums.length; i++) {
        res[i] = pre;
        pre *= nums[i];
    }
    for (int i = nums.length - 1; i >= 0; i--) {
        res[i] *= post;
        post *= nums[i];
    }
    return res;
}
```

**Example:** `nums = [1, 2, 3, 4]`

**Dry Run:**
```
Left pass (pre starts at 1):
  i=0: res[0] = pre = 1; pre *= 1 -> pre = 1
  i=1: res[1] = pre = 1; pre *= 2 -> pre = 2
  i=2: res[2] = pre = 2; pre *= 3 -> pre = 6
  i=3: res[3] = pre = 6; pre *= 4 -> pre = 24
  res after left pass = [1, 1, 2, 6]

Right pass (post starts at 1):
  i=3: res[3] *= post (1) -> res[3] = 6;  post *= nums[3] (4) -> post = 4
  i=2: res[2] *= post (4) -> res[2] = 8;  post *= nums[2] (3) -> post = 12
  i=1: res[1] *= post (12) -> res[1] = 12; post *= nums[1] (2) -> post = 24
  i=0: res[0] *= post (24) -> res[0] = 24; post *= nums[0] (1) -> post = 24
  res after right pass = [24, 12, 8, 6]
```
Output: `[24, 12, 8, 6]`
