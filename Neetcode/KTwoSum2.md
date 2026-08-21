# LeetCode 167 - Two Sum II - Input Array Is Sorted

Given a 1-indexed array of integers that is already sorted in non-decreasing
order, find two numbers that add up to a specific target and return their
1-indexed positions. Exactly one solution is guaranteed to exist, and the
same element cannot be used twice.

## Brute Force

Check every pair of indices `(i, j)` with `i < j` and see if
`numbers[i] + numbers[j] == target`. The intuition is the plain definition
of the problem — try every possible pair. It ignores the fact that the
array is sorted, which is what makes a faster approach possible.

- Time: O(n^2) — nested loop over all pairs.
- Space: O(1) extra.

```java
private static int[] twoSumBruteForce(int[] numbers, int target) {
    for (int i = 0; i < numbers.length; i++) {
        for (int j = i + 1; j < numbers.length; j++) {
            if (numbers[i] + numbers[j] == target) {
                return new int[] { i + 1, j + 1 };
            }
        }
    }
    return new int[] {};
}
```

## Optimal Solution

Since the array is sorted, use two pointers: `left` starting at index 0 and
`right` starting at the last index. If `numbers[left] + numbers[right]`
equals the target, we've found the answer. If the sum is too small, the
only way to increase it is to move `left` rightward (since the array is
sorted, increasing `left` can only increase or keep the value). If the sum
is too large, move `right` leftward for the symmetric reason. The
intuition: sortedness lets us discard one end of the search space on every
comparison instead of trying all pairs, because we know definitively
whether we need a bigger or smaller value next.

- Time: O(n) — the two pointers together traverse the array at most once.
- Space: O(1) — only the two pointer variables and the output array.

```java
private static int[] twoSum(int[] numbers, int target) {
    int left = 0, right = numbers.length - 1;
    while (left < right) {
        int sum = numbers[left] + numbers[right];
        if (sum == target) {
            return new int[] { left + 1, right + 1 };
        } else if (sum < target) {
            left++;
        } else {
            right--;
        }
    }
    return new int[] {};
}
```
