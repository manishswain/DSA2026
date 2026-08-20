package Namaste.Arrays;

public class MaxConsecutiveOnes485 {
    public static void main(String[] args) {
        int[] nums = { 1, 0, 1, 1, 0, 1 };
        int maxConsecutiveOnes = findMaxConsecutiveOnes(nums);
        System.out.println("Maximum consecutive ones: " + maxConsecutiveOnes);
    }

    private static int findMaxConsecutiveOnes(int[] nums) {
        int currC = 1;
        int maxC = Integer.MIN_VALUE;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == 1) {
                currC++;
                maxC = Math.max(currC, maxC);
            } else {
                currC = 0;
            }
        }
        return maxC;
    }

}
