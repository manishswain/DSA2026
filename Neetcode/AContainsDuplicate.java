package Neetcode;

import java.util.HashSet;
import java.util.Set;

public class AContainsDuplicate {
    public static void main(String[] args) {
        int[] nums = { 1, 2, 3 };
        boolean result = containsDuplicate(nums);
        System.out.println(result);
    }

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
