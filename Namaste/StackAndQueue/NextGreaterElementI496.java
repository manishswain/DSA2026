package Namaste.StackAndQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class NextGreaterElementI496 {
    public static void main(String[] args) {
        int[] nums1 = { 4, 1, 2 };
        int[] nums2 = { 1, 3, 4, 2 };
        int[] result = nextGreaterElement(nums1, nums2);
        for (int num : result) {
            System.out.print(num + " ");
        }
    }

    private static int[] nextGreaterElement(int[] nums1, int[] nums2) {
        Map<Integer, Integer> map = new HashMap<>();
        Stack<Integer> st = new Stack<>();
        int len2 = nums2.length;
        map.put(nums2[len2 - 1], -1);
        st.push(nums2[len2 - 1]);
        for (int i = nums2.length - 2; i >= 0; i--) {
            while (!st.isEmpty()) {
                if (st.peek() < nums2[i]) {
                    st.pop();
                } else if (st.peek() > nums2[i]) {
                    map.put(nums2[i], st.peek());
                    break;
                }
            }
            if (st.isEmpty()) {
                map.put(nums2[i], -1);
            }
            st.push(nums2[i]);
        }

        for (int i = 0; i < nums1.length; i++) {
            nums1[i] = map.get(nums1[i]);
        }
        return nums1;
    }
}
