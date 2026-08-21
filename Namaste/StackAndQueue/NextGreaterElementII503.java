package Namaste.StackAndQueue;

import java.util.Stack;

public class NextGreaterElementII503 {
    public static void main(String[] args) {
        int[] nums = { 1, 2, 1 };
        int[] result = nextGreaterElementsUsingStack(nums);
        for (int num : result) {
            System.out.print(num + " ");
        }
    }

    private static int[] nextGreaterElementsUsingStack(int[] nums) {
        Stack<Integer> st = new Stack<>();
        int len = nums.length;
        int[] ans = new int[len];
        for (int i = 2 * len - 1; i >= 0; i--) {
            while (!st.isEmpty() && nums[i % len] >= st.peek()) {
                st.pop();
            }
            if (i < len) {
                ans[i] = st.isEmpty() ? -1 : st.peek();
            }
            st.push(nums[i % len]);
        }
        return ans;
    }
}
