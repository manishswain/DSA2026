package Neetcode;

import java.util.HashSet;
import java.util.Set;

//Leetcode 217 - Contains Duplicate
public class AContainsDuplicate {
    public static void main(String[] args) {
        int[] nums = { 1, 2, 3, 3 };
        boolean result = containsDuplicate(nums);
        System.out.println(result);
    }

    // Approach: HashSet
    // Time Complexity: O(n) where n is the length of the input array. We iterate
    // through the array once, and each insertion into the HashSet takes O(1) time
    // on average.
    // Space Complexity: O(n) in the worst case, if all elements in the array are
    // unique, we will store all n elements in the HashSet. In the best case, if
    // there is a duplicate early on, the space complexity would be O(1) since we
    // would only store a few elements before finding the duplicate.
    private static boolean containsDuplicate(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int i : nums) {
            if (!set.add(i)) {
                return true;
            }
        }
        return false;
    }
}
