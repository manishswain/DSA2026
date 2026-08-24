package Neetcode;

import java.util.Arrays;

//Array - Medium - Two Pointers
//LeetCode 42 - Trapping Rain Water
public class NTrappingRainWater42 {
    public static void main(String[] args) {
        int[] height = { 0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1 };
        System.out.println(trapOptimalPractice(height));
    }

    // Approach- Precompute the maximum height to the left and right of each bar,
    // then calculate the trapped water at each position.
    // Time Complexity: O(n), where n is the length of the input array (since we
    // traverse the array a few times).
    // Space Complexity: O(n), since we use two additional arrays to store the
    // maximum heights to the left and right of each bar.
    private static int trap(int[] height) {
        int[] leftArr = new int[height.length];
        int[] rightArr = new int[height.length];
        int leftMax = 0, rightMax = 0;
        for (int i = 0, j = height.length - 1; i < height.length && j >= 0; i++, j--) {
            leftArr[i] = Math.max(height[i], leftMax);
            leftMax = Math.max(leftMax, leftArr[i]);

            rightArr[j] = Math.max(height[j], rightMax);
            rightMax = Math.max(rightMax, rightArr[j]);
        }
        System.out.println("Left Max Array: " + Arrays.toString(leftArr));
        System.out.println("Right Max Array: " + Arrays.toString(rightArr));
        int total = 0;
        for (int i = 0; i < height.length; i++) {
            total += Math.min(leftArr[i], rightArr[i]) - height[i];
        }
        return total;
    }

    // Approach - Two Pointers: Use two pointers to traverse the array from both
    // ends, keeping track of the maximum height seen so far from both sides and
    // calculating the trapped water on the fly.
    // Time Complexity: O(n), where n is the length of the input array (since we
    // traverse the array once).
    // Space Complexity: O(1), since we only use a constant amount of extra space to
    // store the pointers and maximum heights.
    private static int trapOptimal(int[] height) {
        int left = 0, right = height.length - 1;
        int leftMax = 0, rightMax = 0;
        int total = 0;

        while (left < right) {
            if (height[left] < height[right]) {
                if (height[left] >= leftMax) {
                    leftMax = height[left];
                } else {
                    total += leftMax - height[left];
                }
                left++;
            } else {
                if (height[right] >= rightMax) {
                    rightMax = height[right];
                } else {
                    total += rightMax - height[right];
                }
                right--;
            }
        }
        return total;
    }

    private static int trapOptimalPractice(int[] h) {
        int j = 0, k = h.length - 1;
        int leftMax = 0, rightMax = 0, total = 0;
        while (j < k) {
            if (h[j] < h[k]) {
                if (h[j] >= leftMax) {
                    leftMax = h[j];
                } else {
                    total += leftMax - h[j];
                }
                j++;
            } else {
                if (h[k] >= rightMax) {
                    rightMax = h[k];
                } else {
                    total += rightMax - h[k];
                }
                k--;
            }
        }
        return total;
    }
}
