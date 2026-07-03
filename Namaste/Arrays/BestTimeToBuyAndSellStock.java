package Namaste.Arrays;

public class BestTimeToBuyAndSellStock {
    public static void main(String[] args) {
        int[] prices = { 7, 1, 5, 3, 6, 4 };
        int maxProfit = maxProfit(prices);
        System.out.println("Maximum profit: " + maxProfit);
    }

    private static int maxProfit(int[] prices) {
        int lowest = Integer.MAX_VALUE;
        int maxP = 0;
        for (int i = 0; i < prices.length; i++) {
            lowest = Math.min(lowest, prices[i]);
            maxP = Math.max(maxP, prices[i] - lowest);
        }
        return maxP;
    }

}
