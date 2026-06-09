package Neetcode;

//Array - Easy
public class OBestTimeToBuyAndSellStock {
    public static void main(String[] args) {
        int[] prices = { 7, 1, 5, 3, 6, 4 };
        System.out.println(maxProfit(prices));
    }

    // Thought Process - Iterate through the array and keep track of the minimum
    // price seen so far. For each price, calculate the potential profit by
    // subtracting the minimum price from the current price and update the maximum
    // profit if the potential profit is greater.
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
}
