package Namaste.StackAndQueue;

import java.util.Stack;

public class ValidParenthesis20 {
    public static void main(String[] args) {
        String s = "({)}";
        System.out.println(isValid(s));
    }

    private static boolean isValid(String s) {
        Stack<Character> stack = new Stack<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '{' || c == '[') {
                stack.push(c);
            } else {
                if (stack.isEmpty()) {
                    return false;
                }
                char top = stack.pop();
                if (!isMatchingPair(top, c)) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    private static boolean isMatchingPair(char top, char c) {
        return (top == '(' && c == ')') || (top == '{' && c == '}') || (top == '[' && c == ']');
    }
}
