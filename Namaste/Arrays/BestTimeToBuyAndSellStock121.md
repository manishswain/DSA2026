# Best Time to Buy and Sell Stock (LeetCode 121)

## Problem
Given an array `prices` where `prices[i]` is the stock price on day `i`, find the maximum profit achievable by buying on one day and selling on a later day. If no profit is possible, return `0`.

Example: `prices = [7, 1, 5, 3, 6, 4]` → answer `5` (buy at 1, sell at 6).

---

## Brute Force Approach
Try every pair of buy day `i` and sell day `j > i`, compute `prices[j] - prices[i]`, and track the maximum.

```java
int maxProfit = 0;
for (int i = 0; i < prices.length; i++) {
    for (int j = i + 1; j < prices.length; j++) {
        maxProfit = Math.max(maxProfit, prices[j] - prices[i]);
    }
}
return maxProfit;
```

- **Time Complexity:** O(n²) — nested loop over all pairs.
- **Space Complexity:** O(1).

---

## Optimal Approach (used in code)
```java
private static int maxProfit(int[] prices) {
    int lowest = Integer.MAX_VALUE;
    int maxP = 0;
    for (int i = 0; i < prices.length; i++) {
        lowest = Math.min(lowest, prices[i]);
        maxP = Math.max(maxP, prices[i] - lowest);
    }
    return maxP;
}
```

### Intuition
The maximum profit on any day `i` (if selling that day) is `prices[i] - (minimum price seen so far before or at i)`. Instead of re-scanning for the minimum for every `i` (which is what makes brute force O(n²)), we can carry the running minimum forward as we scan left to right in a single pass. At each index we ask two questions simultaneously:
1. "Is this the new lowest buy point?" → update `lowest`.
2. "If I sold today, having bought at the lowest point so far, would that beat my best profit?" → update `maxP`.

Because `lowest` always reflects the best possible buy price *before* the current day, we never need to look backward or re-scan — one pass captures the answer.

- **Time Complexity:** O(n) — single pass.
- **Space Complexity:** O(1).

---

## Dry Run
`prices = [7, 1, 5, 3, 6, 4]`

| i | prices[i] | lowest (before) | lowest (after) | maxP (before) | prices[i]-lowest | maxP (after) |
|---|-----------|------------------|-----------------|----------------|-------------------|--------------|
| 0 | 7         | ∞                | 7               | 0              | 0                 | 0            |
| 1 | 1         | 7                | 1               | 0              | 0                 | 0            |
| 2 | 5         | 1                | 1               | 0              | 4                 | 4            |
| 3 | 3         | 1                | 1               | 4              | 2                 | 4            |
| 4 | 6         | 1                | 1               | 4              | 5                 | 5            |
| 5 | 4         | 1                | 1               | 5              | 3                 | 5            |

**Result:** `maxProfit = 5` (buy at price 1 on day 1, sell at price 6 on day 4).
