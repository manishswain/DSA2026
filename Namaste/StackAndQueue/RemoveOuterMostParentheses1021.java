package Namaste.StackAndQueue;

public class RemoveOuterMostParentheses1021 {
    void main(String[] args) {
        String s = "(()())(())";
        System.out.println(removeOuterParentheses(s));
    }

    private String removeOuterParentheses(String s) {
        int depth = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(') {
                depth++;
                if (depth > 1) {
                    sb.append(c);
                }
            } else {
                if (depth > 1) {
                    sb.append(c);
                }
                depth--;
            }
        }
        return sb.toString();
    }

}
