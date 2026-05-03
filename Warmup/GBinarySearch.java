package Warmup;

public class GBinarySearch {
    public static void main(String[] args) {
        int[] arr = { -1, 0, 3, 5, 9, 12 };
        int target = 9;
        int result = binarySearch(arr, target);
        System.out.println("Element found at index: " + result);
    }

    private static int binarySearch(int[] nums, int target) {
        int start = 0, end = nums.length - 1;
        while (start <= end) {
            int mid = start + (end - start) / 2;
            if (nums[mid] == target)
                return mid;
            else if (nums[mid] < target) {
                start = mid + 1;
            } else
                end = mid - 1;
        }
        return -1;
    }
}
