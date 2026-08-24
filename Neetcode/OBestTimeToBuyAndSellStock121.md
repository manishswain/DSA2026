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

**Example:** `prices = [7, 1, 5, 3, 6, 4]`

**Dry Run:**
```
i=0(7): j=1(1) -6 max=0 | j=2(5) -2 max=0 | j=3(3) -4 max=0 | j=4(6) -1 max=0 | j=5(4) -3 max=0
i=1(1): j=2(5)  4 max=4 | j=3(3)  2 max=4 | j=4(6)  5 max=5 | j=5(4)  3 max=5
i=2(5): j=3(3) -2 max=5 | j=4(6)  1 max=5 | j=5(4) -1 max=5
i=3(3): j=4(6)  3 max=5 | j=5(4)  1 max=5
i=4(6): j=5(4) -2 max=5
```
Output: `5`

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

**Example:** `prices = [7, 1, 5, 3, 6, 4]`

**Dry Run:**
```
minPrice=7, profit=0
i=0: prices[0]=7, not < minPrice; profit=max(0, 7-7=0)=0
i=1: prices[1]=1 < 7 -> minPrice=1; profit=max(0, 1-1=0)=0
i=2: prices[2]=5, not < 1; profit=max(0, 5-1=4)=4
i=3: prices[3]=3, not < 1; profit=max(4, 3-1=2)=4
i=4: prices[4]=6, not < 1; profit=max(4, 6-1=5)=5
i=5: prices[5]=4, not < 1; profit=max(5, 4-1=3)=5
```
Output: `5`
