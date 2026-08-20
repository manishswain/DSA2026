package Namaste.String;

public class LargestOddNumberInString1903 {
    public static void main(String[] args) {
        String num = "35427";
        String largestOdd = largestOddNumber(num);
        System.out.println("Largest odd number: " + largestOdd);
    }

    private static String largestOddNumber(String num) {
        for (int i = num.length() - 1; i >= 0; i--) {
            if ((int) num.charAt(i) % 2 != 0) {
                return num.substring(0, i + 1);
            }
        }
        return "";
    }
}
