package Namaste.BinarySearch;

public class SquareRoot {
    public static void main(String[] args) {
        int result = squareRoot(2147395599);
        System.out.println("Square root " + result);
    }

    private static int squareRoot(int i) {
        int start = 0;
        int end = i / 2;

        while (start <= end) {
            int mid = start + (end - start) / 2;
            long square = (long) mid * mid; // Use long to avoid overflow

            if (square == i) {
                return mid; // Found the exact square root
            } else if (square < i) {
                start = mid + 1; // Search in the right half
            } else {
                end = mid - 1; // Search in the left half
            }
        }

        return end; // Return the integer part of the square root
    }
}
