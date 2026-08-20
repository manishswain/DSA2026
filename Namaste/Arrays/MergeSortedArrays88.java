package Namaste.Arrays;

public class MergeSortedArrays88 {
    public static void main(String[] args) {
        int[] nums1 = { 0 };
        int m = 0;
        int[] nums2 = { 1 };
        int n = 1;
        merge(nums1, m, nums2, n);
        System.out.print("Merged array: ");
        for (int num : nums1) {
            System.out.print(num + " ");
        }

    }

    private static void merge(int[] nums1, int m, int[] nums2, int n) {
        int i = m - 1, j = n - 1;
        for (int p = m + n - 1; p >= 0; p--) {
            if (j < 0)
                break;
            if (i >= 0 && nums1[i] > nums2[j]) {
                nums1[p] = nums1[i--];
            } else {
                nums1[p] = nums2[j--];
            }
        }
    }
}
