package Namaste.String;

public class SplitAStringinBalancedStrings {
    public static void main(String[] args) {
        String s = "RLRRLLRLRL";
        int count = balancedStringSplit(s);
        System.out.println("Number of balanced strings: " + count);
    }

    private static int balancedStringSplit(String s) {
        int count = 0;
        int ans = 0;
        for (char c : s.toCharArray()) {
            if (c == 'R') {
                count++;
            } else
                count--;

            if (count == 0) {
                ans++;
            }

        }
        return ans;
    }
}
