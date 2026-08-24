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

**Example:** `height = [1, 8, 6, 2, 5, 4, 8, 3, 7]`

**Dry Run:**
```
(i=1, j=6): min(8,8)*(6-1) = 8*5 = 40   -> maxArea = 40
(i=1, j=8): min(8,7)*(8-1) = 7*7 = 49   -> maxArea = 49
... (all other pairs checked, none exceed 49) ...
```
Output: `49`

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

**Example:** `height = [1, 8, 6, 2, 5, 4, 8, 3, 7]`

**Dry Run:**
```
left=0 (1), right=8 (7): area = min(1,7)*8 = 8   -> maxArea=8
  height[left] < height[right] -> left++ (left=1)
left=1 (8), right=8 (7): area = min(8,7)*7 = 49  -> maxArea=49
  height[left] >= height[right] -> right-- (right=7)
left=1 (8), right=7 (3): area = min(8,3)*6 = 18  -> maxArea=49
  height[left] >= height[right] -> right-- (right=6)
left=1 (8), right=6 (8): area = min(8,8)*5 = 40  -> maxArea=49
  height[left] >= height[right] -> right-- (right=5)
left=1 (8), right=5 (4): area = min(8,4)*4 = 16  -> maxArea=49
  height[left] >= height[right] -> right-- (right=4)
left=1 (8), right=4 (5): area = min(8,5)*3 = 15  -> maxArea=49
  height[left] >= height[right] -> right-- (right=3)
left=1 (8), right=3 (2): area = min(8,2)*2 = 4   -> maxArea=49
  height[left] >= height[right] -> right-- (right=2)
left=1 (8), right=2 (6): area = min(8,6)*1 = 6   -> maxArea=49
  height[left] >= height[right] -> right-- (right=1)
left=1, right=1: loop ends (left < right is false)
```
Output: `49`
