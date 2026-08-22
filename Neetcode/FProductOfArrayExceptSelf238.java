package Neetcode;

import java.util.Arrays;

//Array Auxiliary Space Question
//Leetcode 238 - Product of Array Except Self
public class FProductOfArrayExceptSelf238 {
    public static void main(String[] args) {
        int[] nums = { -1, 1, 0, -3, 3 };
        int[] result = productExceptSelfOptimal(nums);
        System.out.print("Product of Array Except Self:" + Arrays.toString(result));
    }

    // Brute Force Approach: For each element in the array, calculate the product of
    // all other elements. This would involve nested loops and result in O(n^2) time
    // complexity, which is inefficient for large arrays.
    private static int[] productExceptSelf(int[] nums) {
        int[] res = new int[nums.length];
        for (int i = 0; i < nums.length; i++) {
            int product = 1;
            for (int j = 0; j < nums.length; j++) {
                if (i != j) {
                    product *= nums[j];
                }
            }
            res[i] = product;
        }
        return res;
    }

    // Optimal Approach: We can solve this problem in O(n) time complexity without
    // using division. We can create two arrays, one to store the product of all
    // elements to the left of the current index and another to store the product of
    // all elements to the right of the current index. Finally, we can multiply
    // these two products for each index to get the final result.
    private static int[] productExceptSelfOptimal(int[] nums) {
        int res[] = new int[nums.length];
        Arrays.fill(res, 1);
        int pre = 1, post = 1;
        for (int i = 0; i < nums.length; i++) {
            res[i] = pre;
            pre = nums[i] * res[i];
        }

        for (int i = nums.length - 1; i >= 0; i--) {
            res[i] = res[i] * post;
            post = post * nums[i];
        }
        return res;
    }

    // Cheating Approach: We can calculate the product of all non-zero elements in
    // the array and count the number of zeros. If there are no zeros, we can simply
    // divide the product by each element to get the result. If there is one zero,
    // then the product for the index where the zero is located will be the product
    // of all non-zero elements, and for all other indices, the product will be
    // zero. If there are more than one zero, then the product for all indices will
    // be zero. This approach has a time complexity of O(n) but uses division, which
    // may not be allowed in some cases.
    private static int[] productExceptSelfCheat(int[] nums) {
        int prod = 1;
        int flag = 0;
        for (int i : nums) {
            if (i != 0) {
                prod *= i;
            } else {
                flag++;
            }
        }
        for (int i = 0; i < nums.length; i++) {
            switch (flag) {
                case 0:
                    nums[i] = prod / nums[i];
                    break;
                case 1:
                    if (nums[i] == 0)
                        nums[i] = prod;
                    else
                        nums[i] = 0;
                    break;
                default:
                    nums[i] = 0;
            }
        }
        return nums;
    }
}
