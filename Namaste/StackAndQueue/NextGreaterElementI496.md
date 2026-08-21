# Next Greater Element I (LeetCode 496)

## Problem
`nums1` is a subset of `nums2` (both have distinct elements). For each element in `nums1`, find its "next greater element" in `nums2` — the first element to its right in `nums2` that is greater than it. If none exists, use `-1`.

Example: `nums1 = [4,1,2]`, `nums2 = [1,3,4,2]` → `[-1,3,-1]`.

---

## Brute Force Approach
For each element of `nums1`, locate it inside `nums2`, then scan forward from that position until a greater element is found.

```java
private static int[] nextGreaterElementBruteForce(int[] nums1, int[] nums2) {
    int[] result = new int[nums1.length];
    for (int i = 0; i < nums1.length; i++) {
        int target = nums1[i];
        int startIdx = -1;
        for (int j = 0; j < nums2.length; j++) {
            if (nums2[j] == target) {
                startIdx = j;
                break;
            }
        }
        int ans = -1;
        for (int j = startIdx + 1; j < nums2.length; j++) {
            if (nums2[j] > target) {
                ans = nums2[j];
                break;
            }
        }
        result[i] = ans;
    }
    return result;
}
```

- **Time Complexity:** O(n × m) — for every element of `nums1` (size n), search `nums2` (size m) for its position, then scan forward again.
- **Space Complexity:** O(1) extra (excluding output).

---

## Optimal Approach (used in code — monotonic stack + hashmap)
```java
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
```

### Intuition
The next-greater-element answer for *every* value in `nums2` can be precomputed **once**, in a single pass, instead of re-searching `nums2` from scratch for each query in `nums1` — the two queries in `nums1` don't need independent O(m) scans if we already know the answer for all of `nums2`.

Scan `nums2` from right to left, keeping a monotonic stack of "candidates that could be the next-greater-element for something further left":
- Any candidate on the stack that is **smaller** than the current value can never be the answer for anything to the left either (the current, closer value is at least as good a lower bound) — pop and discard it.
- Once we find a candidate **greater** than the current value, that's the closest next-greater element — record `map[current] = candidate`.
- If the stack empties out, there's no greater element to the right → `-1`.
- Push the current value so it becomes a candidate for elements further left.

Once this map is built for all of `nums2` (O(m)), answering each `nums1[i]` is a single O(1) hashmap lookup, so the total combined cost is O(n + m) instead of O(n × m).

- **Time Complexity:** O(n + m) — O(m) to build the map, O(n) to answer all queries.
- **Space Complexity:** O(m) — for the hashmap and stack.

---

## Dry Run
`nums2 = [1, 3, 4, 2]` (indices 0-3)

Start: `map[2] = -1`, stack = `[2]`

| i | nums2[i] | stack before | action | stack after | map update |
|---|----------|--------------|--------|-------------|-------------|
| 2 | 4 | [2] | 2 < 4 → pop 2; stack empty | [] → push 4 | map[4] = -1 |
| 1 | 3 | [4] | 4 > 3 → map[3]=4 | [4,3] | map[3] = 4 |
| 0 | 1 | [4,3] | 3 > 1 → map[1]=3 | [4,3,1] | map[1] = 3 |

Final map: `{2:-1, 4:-1, 3:4, 1:3}`

`nums1 = [4, 1, 2]` → lookup each: `map[4]=-1, map[1]=3, map[2]=-1`

**Result:** `[-1, 3, -1]`
