# Daily Temperatures (LeetCode 739)

## Problem
Given an array `temperatures`, return an array `answer` where `answer[i]` is the number of days you have to wait after day `i` to get a warmer temperature. If there is no future day with a warmer temperature, `answer[i] = 0`.

Example: `temperatures = [73,74,75,71,69,72,76,73]` → `[1,1,4,2,1,1,0,0]`.

---

## Brute Force Approach
For each day `i`, scan forward day by day until a strictly warmer temperature is found.

```java
private static int[] dailyTemperaturesBruteForce(int[] temperatures) {
    int len = temperatures.length;
    int[] ans = new int[len];
    for (int i = 0; i < len; i++) {
        for (int j = i + 1; j < len; j++) {
            if (temperatures[j] > temperatures[i]) {
                ans[i] = j - i;
                break;
            }
        }
    }
    return ans;
}
```

- **Time Complexity:** O(n²) — for every day, potentially scan all remaining days.
- **Space Complexity:** O(1) extra (excluding output array).

---

## Optimal Approach (used in code — monotonic stack)
```java
private static int[] dailyTemperatures(int[] temperatures) {
    Stack<Integer> st = new Stack<>();
    int len = temperatures.length;
    int[] ans = new int[len];
    ans[len - 1] = 0;
    st.push(len - 1);
    for (int i = len - 2; i >= 0; i--) {
        while (!st.isEmpty()) {
            if (temperatures[i] >= temperatures[st.peek()]) {
                st.pop();
            } else {
                ans[i] = st.peek() - i;
                break;
            }
        }
        st.push(i);
    }
    return ans;
}
```

### Intuition
The brute force wastes time because, once we know day `k` is not warmer than day `i`, that same fact was already "learned" when we processed a day even further right — we shouldn't have to re-derive it.

Scan from **right to left**, keeping a stack of day-indices that are still "unresolved candidates" for being someone's next-warmer day. For the current day `i`:
- Any index sitting on top of the stack whose temperature is **≤** `temperatures[i]` can never be the answer for any day to the *left* of `i` either — because `i` is both closer and at least as warm, so `i` itself is always a strictly better candidate. Those indices are permanently useless from this point on, so we pop and discard them.
- Once we hit an index on the stack whose temperature is strictly greater than `temperatures[i]`, that's the closest future day that's warmer — record the distance and stop.
- Push `i` onto the stack (it now becomes a candidate for days further left).

Because every index is pushed once and popped at most once, the total work across the whole scan is linear, not quadratic.

- **Time Complexity:** O(n) — each index pushed and popped at most once.
- **Space Complexity:** O(n) — worst case, stack holds all indices (strictly decreasing temperatures left to right, e.g. `[80,70,60]`).

---

## Dry Run
`temperatures = [34, 80, 80, 34, 34, 80, 80, 80, 80, 34]` (indices 0-9)

Start: `ans[9] = 0`, stack = `[9]`

| i | temperatures[i] | stack before | action | stack after | ans[i] |
|---|------------------|--------------|--------|-------------|--------|
| 8 | 80 | [9(34)] | 80 ≥ 34 → pop 9; stack empty → no ans set (stays 0) | [8] | 0 |
| 7 | 80 | [8(80)] | 80 ≥ 80 → pop 8; stack empty | [7] | 0 |
| 6 | 80 | [7(80)] | 80 ≥ 80 → pop 7; stack empty | [6] | 0 |
| 5 | 80 | [6(80)] | 80 ≥ 80 → pop 6; stack empty | [5] | 0 |
| 4 | 34 | [5(80)] | 34 < 80 → ans[4] = 5-4 = 1 | [5,4] | 1 |
| 3 | 34 | [5(80),4(34)] | 34 ≥ 34 → pop 4; then 34 < 80 → ans[3] = 5-3 = 2 | [5,3] | 2 |
| 2 | 80 | [5(80),3(34)] | 80 ≥ 34 → pop 3; 80 ≥ 80 → pop 5; stack empty | [2] | 0 |
| 1 | 80 | [2(80)] | 80 ≥ 80 → pop 2; stack empty | [1] | 0 |
| 0 | 34 | [1(80)] | 34 < 80 → ans[0] = 1-0 = 1 | [1,0] | 1 |

**Result:** `[1, 0, 0, 2, 1, 0, 0, 0, 0, 0]`
