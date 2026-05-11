package Neetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
