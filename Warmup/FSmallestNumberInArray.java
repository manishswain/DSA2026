package Warmup;

public class FSmallestNumberInArray {
    public static void main(String[] args) {
        int[] arr = { 5, 2, 9, 1, 5, -6678678 };
        int result = findSmallest(arr);
        System.out.println("Smallest Number: " + result);
    }

    private static int findSmallest(int[] arr) {
        int smallest = Integer.MAX_VALUE;
        for (int i : arr) {
            if (i < smallest)
                smallest = i;
        }
        return smallest;
    }
}
