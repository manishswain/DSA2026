package Namaste.BinarySearch;

//Leetcode 81. Search in Rotated Sorted Array II
public class SearchInRotatedSortedArrayWithDuplicates {
    public static void main(String[] args) {
        int[] arr = { 2, 5, 6, 0, 0, 1, 2 };
        int target = 0;
        boolean ans = search(arr, target);
        System.out.println(ans);
    }

    private static boolean search(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) {
                return true;
            }
            // If we have duplicates, we can just move the left pointer
            if (arr[left] == arr[mid]) {
                left++;
                continue;
            }
            // Check if the left side is sorted
            if (arr[left] < arr[mid]) {
                // Check if the target is in the left side
                if (target >= arr[left] && target < arr[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {
                // Right side is sorted
                if (target > arr[mid] && target <= arr[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        return false;
    }
}
