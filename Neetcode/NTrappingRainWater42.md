# LeetCode 42 - Trapping Rain Water

Given `n` non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.

## Brute Force

**Intuition:** The water trapped above any bar `i` is determined by the shorter of the tallest bar to its left and the tallest bar to its right, minus the bar's own height (`min(leftMax, rightMax) - height[i]`, floored at 0). Water can't rise higher than the shorter of the two "walls" surrounding it, since it would simply spill over the shorter side. The brute force approach directly computes `leftMax` and `rightMax` for each index by scanning outward from it every time.

**Time Complexity:** O(n²) — for every index, we scan left and scan right.
**Space Complexity:** O(1).

```java
private static int trapBruteForce(int[] height) {
    int total = 0;
    for (int i = 0; i < height.length; i++) {
        int leftMax = 0, rightMax = 0;
        for (int l = 0; l <= i; l++) leftMax = Math.max(leftMax, height[l]);
        for (int r = i; r < height.length; r++) rightMax = Math.max(rightMax, height[r]);
        total += Math.min(leftMax, rightMax) - height[i];
    }
    return total;
}
```

## Optimal Solution

The file contains two solutions: a precompute-arrays version (`trap`) and a two-pointer version (`trapOptimal`), which is the one actually run.

**Intuition (two pointers):** Instead of precomputing `leftMax`/`rightMax` arrays for every index, track running maxima from both ends simultaneously. At each step, whichever side has the smaller current bar (`height[left]` vs `height[right]`) is the side whose water level is actually determined — because the *other* side is guaranteed to have a max at least that large (since we always advance from the smaller side, the untouched side's max can only be ≥ the smaller side's height). So we can safely compute trapped water for the smaller side using only its own running max, without needing the true max on the far side.

**Time Complexity:** O(n) — single pass with two pointers.
**Space Complexity:** O(1) — no auxiliary arrays needed (improves on the O(n) space precompute version).

```java
private static int trapOptimal(int[] height) {
    int left = 0, right = height.length - 1;
    int leftMax = 0, rightMax = 0;
    int total = 0;
    while (left < right) {
        if (height[left] < height[right]) {
            if (height[left] >= leftMax) leftMax = height[left];
            else total += leftMax - height[left];
            left++;
        } else {
            if (height[right] >= rightMax) rightMax = height[right];
            else total += rightMax - height[right];
            right--;
        }
    }
    return total;
}
```
