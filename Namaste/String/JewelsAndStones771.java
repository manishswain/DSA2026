package Namaste.String;

public class JewelsAndStones {
    public static void main(String[] args) {
        String jewels = "aA";
        String stones = "aAAbbbb";
        int count = numJewelsInStones(jewels, stones);
        System.out.println("Number of jewels in stones: " + count);
    }

    private static int numJewelsInStones(String jewels, String stones) {
        int ans[] = new int[52];
        for (char c : jewels.toCharArray()) {
            if ((int) c >= 97 && (int) c <= 122) {
                ans[c - 'a']++;
            } else {
                ans[c - 'A' + 26]++;
            }
        }
        int count = 0;
        for (char c : stones.toCharArray()) {
            if ((int) c >= 97 && (int) c <= 122) {
                if (ans[c - 'a'] != 0)
                    count++;
            } else {
                if (ans[c - 'A' + 26] != 0)
                    count++;
            }
        }
        return count;
    }
}
