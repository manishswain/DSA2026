# LeetCode 347 - Top K Frequent Elements

Given an integer array `nums` and an integer `k`, return the `k` most frequent elements.

## Brute Force

Count the frequency of every element with a hash map, then sort the unique elements by frequency (descending) and take the first `k`.

**Intuition:** Once we know how often each value appears, the "top k" ones are simply whichever have the highest counts — sorting all of them by frequency and slicing the top `k` directly answers that.

**Time Complexity:** O(n log n) — dominated by sorting the unique elements by frequency.
**Space Complexity:** O(n) — for the frequency map and the list being sorted.

```java
private static int[] topKFrequentBrute(int[] nums, int k) {
    Map<Integer, Integer> map = new HashMap<>();
    for (int i : nums) map.put(i, map.getOrDefault(i, 0) + 1);

    List<Integer> unique = new ArrayList<>(map.keySet());
    unique.sort((a, b) -> map.get(b) - map.get(a));

    int[] res = new int[k];
    for (int i = 0; i < k; i++) res[i] = unique.get(i);
    return res;
}
```

**Example:** `nums = [1, 1, 1, 2, 2, 3]`, `k = 2`

**Dry Run:**
```
map = {1:3, 2:2, 3:1}
unique = [1, 2, 3]
sorted by frequency descending -> [1, 2, 3]  (freq 3, 2, 1)
res = first 2 elements = [1, 2]
```
Output: `[1, 2]`

## Optimal Solution

Build the frequency map as before, then maintain a **min-heap** of size `k` (ordered by frequency). Push each unique element; whenever the heap exceeds size `k`, pop the smallest-frequency element. What remains are the `k` most frequent elements.

**Intuition:** We don't need a full sort of all unique elements — we only care about the top `k`. A min-heap capped at size `k` naturally discards the least frequent candidate whenever a more frequent one arrives, so by the end only the `k` largest-frequency elements survive, at a cheaper `log k` cost per operation instead of `log n` for a full sort.

**Time Complexity:** O(n log k) — building the frequency map is O(n); each of the up-to-n heap operations costs O(log k).
**Space Complexity:** O(n) worst case for the frequency map (O(k) for the heap itself).

```java
private static int[] topKFrequent(int[] nums, int k) {
    if (k == nums.length) return nums;

    Map<Integer, Integer> map = new HashMap<>();
    for (int i : nums) map.put(i, map.getOrDefault(i, 0) + 1);

    Queue<Integer> pq = new PriorityQueue<>((a, b) -> map.get(a) - map.get(b));

    for (int n : map.keySet()) {
        pq.add(n);
        if (pq.size() > k) pq.poll();
    }
    return pq.stream().mapToInt(a -> a).toArray();
}
```

**Example:** `nums = [1, 1, 1, 2, 2, 3]`, `k = 2`

**Dry Run:**
```
map = {1:3, 2:2, 3:1}
pq = [] (min-heap ordered by frequency)
add 1 (freq 3): pq = [1]                       size 1 <= k
add 2 (freq 2): pq = [2, 1]                    size 2 <= k
add 3 (freq 1): pq = [3, 1, 2]                 size 3 > k -> poll smallest freq (3) -> pq = [1, 2]
Remaining heap: {1, 2}
```
Output: `[1, 2]`
