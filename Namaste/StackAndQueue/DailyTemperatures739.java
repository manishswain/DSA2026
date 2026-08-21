package Namaste.StackAndQueue;

import java.util.Stack;

public class DailyTemperatures739 {
    public static void main(String[] args) {
        int[] temperatures = { 34, 80, 80, 34, 34, 80, 80, 80, 80, 34 };
        // int[] temperatures = { 73, 74, 75, 71, 69, 72, 76, 73 };

        int[] result = dailyTemperatures(temperatures);
        for (int days : result) {
            System.out.print(days + " ");
        }

    }

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

    private static int[] dailyTemperaturesWithoutStack(int[] temperatures) {
        int len = temperatures.length;
        int[] ans = new int[len];
        ans[len - 1] = 0;
        for (int i = len - 2; i >= 0; i--) {
            int j = i + 1;
            while (j < len) {
                if (temperatures[i] < temperatures[j]) {
                    ans[i] = j - i;
                    break;
                } else if (ans[j] == 0) {
                    ans[i] = 0;
                    break;
                }
                j += ans[j];
            }
        }
        return ans;
    }
}
