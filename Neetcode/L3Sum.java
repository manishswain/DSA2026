package Neetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Two pointer approach
//Leetcode 15 - 3Sum
public class L3Sum {
    public static void main(String[] args) {
        int[] nums = { -1, 0, 1, 2, -1, -4 };
        System.out.println(threeSumOptimal(nums));
    }

    private static List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        Arrays.sort(nums);
        for (int i = 0; i < nums.length && nums[i] <= 0; i++) {
            if (i == 0 || nums[i] != nums[i - 1]) {
                int j = i + 1, k = nums.length - 1;
                int target = nums[i] * -1;
                while (j < k) {
                    if (nums[j] + nums[k] < target) {
                        j++;
                    } else if (nums[j] + nums[k] > target) {
                        k--;
                    } else {
                        res.add(List.of(nums[i], nums[j], nums[k]));
                        while (j < k && nums[j] == nums[j - 1]) {
                            j++;
                        }
                    }
                }
            }
        }
        return res;
    }

    // Optimal solution
    // Aproach - Sort the array and use two pointers to find pairs that sum up to
    // the negative of the current element. This way, we can efficiently find
    // triplets that sum to zero while avoiding duplicates.
    //
    private static List<List<Integer>> threeSumOptimal(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        Arrays.sort(nums);
        for (int i = 0; i < nums.length && nums[i] <= 0; i++) {
            if (i == 0 || nums[i] != nums[i - 1]) {
                twoSum2(nums, i, res);
            }
        }
        return res;
    }

    // Helper method to find pairs that sum up to the negative of the current
    // element
    // This method uses two pointers to efficiently find pairs in the sorted array
    // It also handles duplicates to ensure that the same triplet is not added
    // multiple times to the result list.
    // The method takes the sorted array, the current index, and the result list as
    // parameters. It initializes two pointers, j and k, to find pairs that sum up
    // to the target value (negative of the current element). If a valid pair is
    // found, it adds the triplet to the result list and moves the pointers while
    // skipping duplicates.
    private static void twoSum2(int[] nums, int i, List<List<Integer>> res) {
        int j = i + 1, k = nums.length - 1;
        int target = nums[i] * -1;
        while (j < k) {
            if (nums[j] + nums[k] < target) {
                j++;
            } else if (nums[j] + nums[k] > target) {
                k--;
            } else {
                res.add(List.of(nums[i], nums[j], nums[k]));
                while (j < k && nums[j] == nums[j + 1]) {
                    j++;
                }
                j++;
                k--;
            }
        }
    }
}
