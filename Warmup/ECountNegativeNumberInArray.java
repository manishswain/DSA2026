package Warmup;

public class ECountNegativeNumberInArray {
    public static void main(String[] args) {
        int[] arr = { -1, -2, 3, 4, -5 };
        int result = countNegatives(arr);
        System.out.println("Count of Negative Numbers: " + result);
    }

    private static int countNegatives(int[] arr) {
        int count = 0;
        for (int i : arr) {
            if (i < 0)
                count++;
        }
        return count;
    }

}