package Neetcode;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class EKFrequentElements {
    public static void main(String[] args) {
        int[] nums = { 1, 1, 1, 2, 2, 3 };
        int k = 2;

        int[] result = topKFrequent(nums, k);
        System.out.print("Top " + k + " Frequent Elements: [");
        for (int i = 0; i < result.length; i++) {
            System.out.print(result[i]);
            if (i < result.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
    }

    private static int[] topKFrequent(int[] nums, int k) {

        if (k == nums.length) {
            return nums;
        }
        Map<Integer, Integer> map = new HashMap<>();
        for (int i : nums) {
            map.put(i, map.getOrDefault(i, 0) + 1);
        }
        Queue<Integer> pq = new PriorityQueue<>(
                (a, b) -> map.get(a) - map.get(b));

        for (int n : map.keySet()) {
            pq.add(n);
            if (pq.size() > k) {
                pq.poll();
            }
        }
        return pq.stream().mapToInt(a -> a).toArray();
    }
}

// Approach: HashMap + Min Heap (Priority Queue)
// Time Complexity: O(n log k) where n is the number of elements in the input
// array and k is the number of top frequent elements we want to return. We
// iterate through the input array to build the frequency map, which takes O(n)
// time. Then we iterate through the keys
// of the frequency map and add them to the priority queue, which takes O(m log
// k) time, where m is the number of unique elements in the input array. Since m
// can be at most n, this step can be considered O(n log k) in the worst case.
// Space Complexity: O(n) in the worst case, if all elements in the input array
// are unique, we will store all n elements in the frequency map and the
// priority queue. In the best case, if there are only a few unique elements,
// the space complexity would be O(k) since we only store the top k elements in
// the priority queue.
