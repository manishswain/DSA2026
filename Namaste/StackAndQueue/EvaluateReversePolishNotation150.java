package Namaste.StackAndQueue;

import java.util.Set;
import java.util.Stack;

public class EvaluateReversePolishNotation150 {
    public static void main(String[] args) {
        String[] tokens = { "2", "1", "+", "3", "*" };
        System.out.println(evalRPN(tokens));
    }

    private static int evalRPN(String[] tokens) {
        Stack<Integer> st = new Stack<>();
        Set<String> set = Set.of("+", "-", "*", "/");
        for (String s : tokens) {
            if (!set.contains(s)) {
                st.push(Integer.valueOf(s));
            } else {
                int b = st.pop();
                int a = st.pop();
                switch (s) {
                    case "+" -> st.push((a + b));
                    case "-" -> st.push((a - b));
                    case "*" -> st.push((a * b));
                    case "/" -> st.push((a / b));
                    default -> {
                    }
                }

            }
        }
        return st.pop();
    }
}
