package Neetcode;

//Two pointer approach
//Leetcode 11 - Container With Most Water
public class MContainerWithMostWater11 {
    public static void main(String[] args) {
        int[] height = { 1, 8, 6, 2, 5, 4, 8, 3, 7 };
        System.out.println(maxArea(height));
    }

    // Approach - Use two pointers, one at the beginning and one at the end of the
    // array. Calculate the area formed by the lines at these pointers and update
    // the maximum area. Move the pointer that has the shorter line towards the
    // other pointer, as this may increase the area. Continue this process until the
    // pointers meet. This approach efficiently finds the maximum area in O(n) time.
    private static int maxArea(int[] height) {
        int left = 0, right = height.length - 1;
        int maxArea = 0;
        int currentheight, curArea;
        while (left < right) {
            currentheight = Math.min(height[left], height[right]);
            curArea = currentheight * (right - left);
            maxArea = Math.max(curArea, maxArea);
            if (height[left] < height[right])
                left++;
            else
                right--;
        }
        return maxArea;
    }

}
