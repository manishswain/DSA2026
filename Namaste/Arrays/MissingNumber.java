package Namaste.Arrays;

public class MissingNumber {
    public static void main(String[] args) {
        int[] nums = { 3, 0, 1 };
        int missingNumber = findMissingNumber(nums);
        System.out.println("Missing number: " + missingNumber);
    }

    private static int findMissingNumber(int[] nums) {
        int xor = 0;
        for (int i = 0; i < nums.length; i++) {
            xor = xor ^ nums[i];
        }
        return xor;
    }

}
