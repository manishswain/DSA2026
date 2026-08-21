# LeetCode 217 - Contains Duplicate

Given an integer array `nums`, return `true` if any value appears **at least twice**, and `false` if every element is distinct.

## Brute Force

Compare every pair of elements. If any two match, the array has a duplicate.

**Intuition:** A duplicate simply means two indices hold the same value, so directly checking every pair is the most literal way to satisfy the definition — no extra data structures needed.

**Time Complexity:** O(n²) — nested loop over all pairs.
**Space Complexity:** O(1) — no auxiliary storage.

```java
private static boolean containsDuplicateBrute(int[] nums) {
    for (int i = 0; i < nums.length; i++) {
        for (int j = i + 1; j < nums.length; j++) {
            if (nums[i] == nums[j]) return true;
        }
    }
    return false;
}
```

## Optimal Solution

Use a `HashSet` to record numbers as we scan the array once. If an insertion fails (the value is already present), a duplicate exists.

**Intuition:** A set gives O(1) average membership checks, so instead of comparing each element against every other, we can ask "have I seen this before?" in constant time, trading the nested comparison for a single pass with lookup-as-you-go.

**Time Complexity:** O(n) — one pass, O(1) average per insertion.
**Space Complexity:** O(n) worst case (all elements unique end up stored); can be O(1) if a duplicate is found early.

```java
private static boolean containsDuplicate(int[] nums) {
    Set<Integer> set = new HashSet<>();
    for (int i : nums) {
        if (!set.add(i)) {
            return true;
        }
    }
    return false;
}
```
