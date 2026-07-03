package Namaste.Arrays;

public class MoveZeroes {
    public static void main(String[] args) {
        int[] nums = { 4, 2, 4, 0, 0, 3, 0, 5, 1, 0 };
        moveZeroes(nums);
        System.out.print("Modified array: ");
        for (int num : nums) {
            System.out.print(num + " ");
        }
    }

    private static void moveZeroes(int[] nums) {
        if (nums.length < 2)
            return;

        int j = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) {
                nums[j] = nums[i];
                j++;
            }
        }
        for (int i = j; i < nums.length; i++) {
            nums[i] = 0;
        }
    }
}
