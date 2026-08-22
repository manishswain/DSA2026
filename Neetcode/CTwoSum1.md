# LeetCode 1 - Two Sum

Given an array of integers `nums` and an integer `target`, return the indices of the two numbers that add up to `target`. Assume exactly one solution exists, and the same element cannot be used twice.

## Brute Force

Check every pair of indices `(i, j)` and see if `nums[i] + nums[j] == target`.

**Intuition:** The problem is literally asking for a pair summing to target, so the most direct approach is to test all possible pairs until one matches.

**Time Complexity:** O(n²) — nested loop over all pairs.
**Space Complexity:** O(1) — no extra storage.

```java
private static int[] twoSumBrute(int[] nums, int target) {
    for (int i = 0; i < nums.length; i++) {
        for (int j = i + 1; j < nums.length; j++) {
            if (nums[i] + nums[j] == target) {
                return new int[] { i, j };
            }
        }
    }
    return new int[] {};
}
```

## Optimal Solution

Use a `HashMap` to store each number's index as we iterate. For each number, compute its complement (`target - nums[i]`) and check if that complement is already in the map — if so, we've found our pair.

**Intuition:** Instead of searching for the second number after fixing the first (which requires scanning again), we can flip the question: "have I already seen the number that completes this pair?" A hash map lets us answer that in O(1), turning the search into a single pass where each number is checked against what came before it.

**Time Complexity:** O(n) — one pass, O(1) average lookup/insert per element.
**Space Complexity:** O(n) — worst case stores all elements in the map before finding a match.

```java
private static int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (map.containsKey(complement)) {
            return new int[] { map.get(complement), i };
        }
        map.put(nums[i], i);
    }
    return new int[] {};
}
```
