# LeetCode 11 - Container With Most Water

Given `n` non-negative integers `height[i]` representing vertical lines drawn at position `i`, find two lines that together with the x-axis form a container that holds the most water. Return the maximum area.

## Brute Force

**Intuition:** The area between any two lines `i` and `j` is `min(height[i], height[j]) * (j - i)`. The most direct way to find the max area is to try every pair of lines and compute the area, keeping the best one. This works because it exhaustively checks every possible container, guaranteeing the correct answer, but it does redundant work by re-evaluating pairs that two-pointer logic could skip.

**Time Complexity:** O(n²) — every pair `(i, j)` is checked.
**Space Complexity:** O(1).

```java
private static int maxAreaBruteForce(int[] height) {
    int maxArea = 0;
    for (int i = 0; i < height.length; i++) {
        for (int j = i + 1; j < height.length; j++) {
            int area = Math.min(height[i], height[j]) * (j - i);
            maxArea = Math.max(maxArea, area);
        }
    }
    return maxArea;
}
```

## Optimal Solution

**Intuition:** Start with the widest possible container — pointers at both ends. The area is limited by the shorter of the two lines. If we move the pointer at the *taller* line inward, the width shrinks but the limiting (shorter) height can't increase, so the area can only get worse or stay the same. Therefore, the only way to potentially find a larger area is to move the pointer at the *shorter* line inward, hoping to find a taller line that improves the limiting height. This greedy elimination is always safe because moving the taller pointer inward can never produce a better result than what's already been considered, so we never miss the optimal pair.

**Time Complexity:** O(n) — each pointer moves at most n times.
**Space Complexity:** O(1).

```java
private static int maxArea(int[] height) {
    int left = 0, right = height.length - 1;
    int maxArea = 0;
    while (left < right) {
        int currentHeight = Math.min(height[left], height[right]);
        int curArea = currentHeight * (right - left);
        maxArea = Math.max(curArea, maxArea);
        if (height[left] < height[right])
            left++;
        else
            right--;
    }
    return maxArea;
}
```
