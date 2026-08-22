package Neetcode;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

//HashSet Question
//Leetcode 128 - Longest Consecutive Sequence
public class ILongestConsecutiveSequence128 {
    public static void main(String[] args) {
        int[] nums = { 1, 2, 6, 7, 8 };

        int result = longestConsecutiveOptimal(nums);
        System.out.println("Longest Consecutive Sequence Length: " + result);
    }

    // Sub-Optimal Approach: Sort the array and then iterate through it to find the
    // longest consecutive sequence. This approach has a time complexity of O(n log
    // n) due to the sorting step and a space complexity of O(1) if we ignore the
    // space used by the sorting algorithm.
    private static int longestConsecutive(int[] nums) {
        if (nums.length == 0)
            return 0;
        Arrays.sort(nums);
        int largestSeq = 1, currentSeq = 1;
        for (int i = 0; i < nums.length - 1; i++) {
            if (nums[i] == nums[i + 1])
                continue;
            if (nums[i] + 1 == nums[i + 1]) {
                currentSeq++;
            } else {
                largestSeq = Math.max(currentSeq, largestSeq);
                currentSeq = 1;
            }
        }
        largestSeq = Math.max(currentSeq, largestSeq);
        return largestSeq;
    }

    // Optimal Approach: Use a HashSet to store the numbers and then iterate through
    // the set to find the longest consecutive sequence. For each number, we check
    // if it is the start of a sequence (i.e., if the number one less than it is not
    // in the set). If it is the start of a sequence, we keep checking for the next
    // numbers in the sequence until we find a number that is not in the set. This
    // approach has a time complexity of O(n) since we are doing a single pass
    // through the array to build the set and then another pass through the set to
    // find the longest sequence. The space complexity is O(n) in the worst case if
    // all numbers are unique, as we would store all numbers in the set. In the best
    // case, if there are many duplicates, the space complexity could be less than
    // O(n).
    private static int longestConsecutiveOptimal(int[] nums) {
        if (nums.length < 2)
            return nums.length;
        Set<Integer> set = Arrays.stream(nums).boxed().collect(Collectors.toSet());

        int largestSeq = 1;
        for (int i : set) {
            if (set.contains(i - 1)) {
            } else {
                int currentSeq = 1, currentNum = i;
                while (set.contains(currentNum + 1)) {
                    currentSeq++;
                    currentNum++;
                }
                largestSeq = Math.max(currentSeq, largestSeq);
            }
        }
        return largestSeq;
    }
}
