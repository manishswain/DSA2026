# Next Greater Element II (LeetCode 503)

## Problem
Given a **circular** array `nums` (the last element's next neighbor wraps around to the first), return an array where `ans[i]` is the next greater element of `nums[i]`, searching circularly. If none exists, `-1`.

Example: `nums = [1,2,1]` → `[2,-1,2]` (the last `1` wraps around and finds `2` at index 0).

---

## Brute Force Approach
For each index `i`, walk forward up to `n` steps (wrapping with modulo) looking for a greater element.

```java
private static int[] nextGreaterElementsBruteForce(int[] nums) {
    int n = nums.length;
    int[] ans = new int[n];
    for (int i = 0; i < n; i++) {
        ans[i] = -1;
        for (int step = 1; step < n; step++) {
            int j = (i + step) % n;
            if (nums[j] > nums[i]) {
                ans[i] = nums[j];
                break;
            }
        }
    }
    return ans;
}
```

- **Time Complexity:** O(n²) — for every index, up to n-1 steps of circular scanning.
- **Space Complexity:** O(1) extra (excluding output).

---

## Optimal Approach (used in code — monotonic stack over doubled array)
```java
private static int[] nextGreaterElementsUsingStack(int[] nums) {
    Stack<Integer> st = new Stack<>();
    int len = nums.length;
    int[] ans = new int[len];
    for (int i = 2 * len - 1; i >= 0; i--) {
        while (!st.isEmpty() && nums[i % len] >= st.peek()) {
            st.pop();
        }
        if (i < len) {
            ans[i] = st.isEmpty() ? -1 : st.peek();
        }
        st.push(nums[i % len]);
    }
    return ans;
}
```

### Intuition
This is the same monotonic-stack idea as [[NextGreaterElementI496]], extended to handle the circular wraparound: instead of physically duplicating the array, we simulate a virtual array of length `2n` by indexing with `i % len`, and scan it right to left from `2n - 1` down to `0`.

Why `2n` iterations are enough: an element's next greater element (if it exists at all) is always within one full lap of the array, so scanning indices `[0, 2n)` (mod `n`) guarantees every element gets a chance to "see" every other element that could possibly be its answer, without literally rotating the array per index.

- We only *record* an answer (`ans[i]`) during the **second pass** (`i < len`, i.e. the "real" indices) — the first pass over indices `[n, 2n)` exists purely to *prime the stack* with the circular wraparound candidates before we start reading real answers.
- The stack itself behaves exactly like the standard monotonic decreasing stack from [[NextGreaterElementI496]] / [[DailyTemperatures739]]: pop anything ≤ current (it's now permanently dominated by a closer, at-least-as-large value), then whatever remains on top is the next greater element.

- **Time Complexity:** O(n) — 2n iterations, each index pushed/popped at most once.
- **Space Complexity:** O(n) — stack can hold up to n elements.

---

## Dry Run
`nums = [1, 2, 1]`, `len = 3`, scanning `i` from `5` down to `0` (`i % len` in parentheses)

| i | i%len | nums[i%len] | stack before | action | stack after | ans set? |
|---|-------|-------------|--------------|--------|-------------|----------|
| 5 | 2 | 1 | [] | push 1 | [1] | — (i ≥ len) |
| 4 | 1 | 2 | [1] | 1 ≤ 2 → pop 1; stack empty → push 2 | [2] | — (i ≥ len) |
| 3 | 0 | 1 | [2] | 2 > 1, keep; push 1 | [2,1] | — (i ≥ len) |
| 2 | 2 | 1 | [2,1] | 1 ≥ 1 → pop 1; 2>1 keep | [2] | ans[2] = 2; push 1 → [2,1] |
| 1 | 1 | 2 | [2,1] | 1≤2 pop; 2≤2 pop; stack empty | [] | ans[1] = -1; push 2 → [2] |
| 0 | 0 | 1 | [2] | 2 > 1, keep | [2] | ans[0] = 2; push 1 → [2,1] |

**Result:** `[2, -1, 2]`
