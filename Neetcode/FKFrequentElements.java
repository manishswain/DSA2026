package Neetcode;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class FKFrequentElements {
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
