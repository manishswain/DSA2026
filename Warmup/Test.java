package Warmup;

public class Test {
    public static void main(String[] args) {
        int[] i = { -1, 1, 2 };
        int mis = 1;
        for (int x : i) {
            mis = mis ^ x;
        }
        System.out.println(mis);
    }
}
