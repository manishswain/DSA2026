# LeetCode 121 - Best Time to Buy and Sell Stock

Given an array `prices` where `prices[i]` is the stock price on day `i`, find the maximum profit achievable by buying on one day and selling on a later day. Return 0 if no profit is possible.

## Brute Force

**Intuition:** Try every possible pair of buy day `i` and sell day `j` (with `j > i`), compute the profit for each, and keep the maximum. This is correct because it enumerates every valid buy/sell combination directly, but it's wasteful — for a fixed sell day, we don't need to check every previous buy day individually, just the minimum one.

**Time Complexity:** O(n²) — every pair of days is checked.
**Space Complexity:** O(1).

```java
private static int maxProfitBruteForce(int[] prices) {
    int maxProfit = 0;
    for (int i = 0; i < prices.length; i++) {
        for (int j = i + 1; j < prices.length; j++) {
            maxProfit = Math.max(maxProfit, prices[j] - prices[i]);
        }
    }
    return maxProfit;
}
```

## Optimal Solution

**Intuition:** To maximize profit for a sale on day `i`, we only care about the *lowest* price seen at any day before `i` — buying at any higher earlier price can never be better. So as we scan left to right, we track the minimum price seen so far, and for each day compute the profit if we sold today after having bought at that minimum. This collapses the need to check every earlier buy day into a single running value.

**Time Complexity:** O(n) — single pass through the array.
**Space Complexity:** O(1).

```java
private static int maxProfit(int[] prices) {
    int minPrice = prices[0];
    int profit = 0;
    for (int i = 0; i < prices.length; i++) {
        if (prices[i] < minPrice) {
            minPrice = prices[i];
        }
        profit = Math.max(profit, prices[i] - minPrice);
    }
    return profit;
}
```
