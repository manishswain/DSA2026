package Warmup;

public class BSecondLargestDigitInAString {
    public static void main(String[] args) {
        String s = "vwkxfq9791769";
        int result = secondLargestDigit(s);
        System.out.println("Second Largest Digit: " + result);
    }

    private static int secondLargestDigit(String s) {
        int largest = -1, slargest = -1;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                int num = c - '0';
                if (num > largest) {
                    slargest = largest;
                    largest = num;
                } else if (num > slargest && num != largest) {
                    slargest = num;
                }
            }
        }
        return slargest;
    }

    private static int secondLargestDigit2(String s) {
        int count = 1;
        for (char c = '9'; c >= '0'; c--) {
            if (s.indexOf(c) != -1) {
                if (count-- <= 0) {
                    return c - '0';
                }
            }
        }
        return -1;
    }
}

// Thinking behind both approaches:
// 1. The first approach iterates through the string once, keeping track of the
// largest and second largest digits found.
// This is efficient with a time complexity of O(n) and space complexity of
// O(1).
// 2. The second approach iterates through the digits from '9' to '0', checking
// if each digit is present in the string. This also has a time complexity of
// O(n) due to the indexOf method, but it may be less efficient than the first
// approach due to multiple calls to indexOf. The space complexity is O(1).
