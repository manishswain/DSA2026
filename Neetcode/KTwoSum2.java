package Neetcode;

//Two Pointers Approach
//Leetcode 167 - Two Sum II - Input Array Is Sorted
public class KTwoSum2 {
    public static void main(String[] args) {
        int[] numbers = { 1, 2, 3, 4, 5 };
        int target = 5;
        int[] result = twoSum(numbers, target);
        System.out.println("Indices: " + result[0] + ", " + result[1]);
    }

    // Approach: Two Pointers
    // Time Complexity: O(n) where n is the length of the input array. We traverse
    // the array at most once with two pointers.
    // Space Complexity: O(1) since we are using only a constant amount of extra
    // space for the two pointers and the output array.
    private static int[] twoSum(int[] numbers, int target) {
        int left = 0, right = numbers.length - 1;
        while (left < right) {
            if (numbers[left] + numbers[right] == target) {
                return new int[] { left + 1, right + 1 };
            } else if (numbers[left] + numbers[right] < target) {
                left++;
            } else {
                right--;
            }
        }
        return new int[] {};
    }

}
