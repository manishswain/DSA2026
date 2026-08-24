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

**Example:** `height = [3, 0, 2, 0, 4]`

**Dry Run:**
```
i=0: leftMax(l=0..0)=3, rightMax(r=0..4)=4 -> total += min(3,4)-3=0   total=0
i=1: leftMax(l=0..1)=3, rightMax(r=1..4)=4 -> total += min(3,4)-0=3   total=3
i=2: leftMax(l=0..2)=3, rightMax(r=2..4)=4 -> total += min(3,4)-2=1   total=4
i=3: leftMax(l=0..3)=3, rightMax(r=3..4)=4 -> total += min(3,4)-0=3   total=7
i=4: leftMax(l=0..4)=4, rightMax(r=4..4)=4 -> total += min(4,4)-4=0   total=7
```
Output: `7`

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

**Example:** `height = [3, 0, 2, 0, 4]`

**Dry Run:**
```
left=0, right=4, leftMax=0, rightMax=0, total=0
height[left]=3 < height[right]=4 -> process left
  3 >= leftMax(0) -> leftMax=3
  left=1
height[left]=0 < height[right]=4 -> process left
  0 < leftMax(3) -> total += 3-0=3   total=3
  left=2
height[left]=2 < height[right]=4 -> process left
  2 < leftMax(3) -> total += 3-2=1   total=4
  left=3
height[left]=0 < height[right]=4 -> process left
  0 < leftMax(3) -> total += 3-0=3   total=7
  left=4
left==right(4) -> loop ends
```
Output: `7`
