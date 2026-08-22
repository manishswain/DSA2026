# LeetCode 128 - Longest Consecutive Sequence

Given an unsorted array of integers, find the length of the longest run of
consecutive integers (e.g., `[100, 4, 200, 1, 3, 2]` → the sequence
`1, 2, 3, 4` has length 4). The algorithm must run in O(n) time, which rules
out simply sorting as the "optimal" solution.

## Brute Force

For every number in the array, repeatedly check whether `num + 1`,
`num + 2`, ... exist in the array (via a linear scan or `contains` check),
counting how long the chain extends. The intuition is the direct
definition: "starting from each number, how far can I walk forward through
consecutive values that are present?" It's correct but wasteful because
membership checks against a plain array/list are O(n) each, and every
number in the array is treated as a potential sequence start even when it
clearly isn't one.

- Time: O(n^3) in the worst case (n starting points × up to n steps per
  chain × O(n) membership check per step), or O(n^2) if using a HashSet
  just for membership without the "is this a sequence start" pruning.
- Space: O(1) extra (or O(n) if a set is used only for lookups).

```java
private static int longestConsecutiveBruteForce(int[] nums) {
    int longest = 0;
    for (int num : nums) {
        int current = num;
        int length = 1;
        while (contains(nums, current + 1)) {
            current++;
            length++;
        }
        longest = Math.max(longest, length);
    }
    return longest;
}

private static boolean contains(int[] nums, int target) {
    for (int n : nums) if (n == target) return true;
    return false;
}
```

## Optimal Solution

Put all numbers into a `HashSet` for O(1) lookups. Then, for each number,
only start counting a sequence if that number is the *start* of a sequence,
i.e., `num - 1` is NOT in the set. This is the key intuition: a number can
only be the start of its sequence once, so by skipping numbers that have a
predecessor in the set, every element is visited a bounded number of times
overall (once as a potential start check, and only "start" numbers trigger
an inner while-loop walk). This turns an apparently O(n^2) nested-loop
shape into true O(n), because the total work done across all inner
while-loops sums to at most O(n) — each number is only ever counted once,
as part of the single sequence it belongs to.

- Time: O(n) — building the set is O(n), and each number is visited a
  constant number of times across the outer loop and any inner while-loop.
- Space: O(n) for the HashSet.

```java
private static int longestConsecutiveOptimal(int[] nums) {
    if (nums.length < 2) return nums.length;
    Set<Integer> set = Arrays.stream(nums).boxed().collect(Collectors.toSet());

    int largestSeq = 1;
    for (int num : set) {
        if (!set.contains(num - 1)) { // only start counting from sequence starts
            int currentSeq = 1, currentNum = num;
            while (set.contains(currentNum + 1)) {
                currentSeq++;
                currentNum++;
            }
            largestSeq = Math.max(currentSeq, largestSeq);
        }
    }
    return largestSeq;
}
```

### Sub-Optimal Alternative (Sort-based)

The file also contains a sort-based approach: sort the array, then walk
through it once counting consecutive runs (skipping duplicates). This is
simpler to reason about but is bounded by the sort.

- Time: O(n log n) due to sorting.
- Space: O(1) extra (ignoring sort's internal space), though it mutates the
  input array.
