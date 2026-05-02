package Warmup;

import java.util.Scanner;

public class ASum {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter the number of elements: ");
        int n = sc.nextInt();
        int[] arr = new int[n];
        System.out.println("Enter the elements:");
        for (int i = 0; i < n; i++) {
            arr[i] = sc.nextInt();
        }
        int result = sum(arr);
        System.out.println("Sum: " + result);
    }

    private static int sum(int[] arr) {
        return Math.toIntExact(java.util.Arrays.stream(arr).asLongStream().sum());
    }
}
