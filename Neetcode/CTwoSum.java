package Neetcode;

import java.util.HashMap;
import java.util.Map;

//HashMap Question
//Leetcode 1 - Two Sum
public class CTwoSum {
    public static void main(String[] args) {
        int[] nums = { 2, 7, 11, 15 };
        int target = 9;

        int[] result = twoSum(nums, target);
        System.out.println("Indices: [" + result[0] + ", " + result[1] + "]");
    }

    private static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }
            map.put(nums[i], i);
        }
        return new int[] {};

    }
}

// Approach: Use a hash map to store the indices of the numbers as we iterate
// through the array. For each number, calculate its complement (target -
// current number) and check if it exists in the map. If it does, we have found
// the two numbers that add up to the target, and we return their indices.
// Time Complexity: O(n), where n is the length of the input array (since we
// traverse the array once).
// Space Complexity: O(n), since in the worst case, we may store all the numbers
// in the hash map.
