package Namaste.String;

//Given a string s and an integer k, reverse the first k characters for every 2k characters counting from the start of the string.
//If there are fewer than k characters left, reverse all of them. If there are less than 2k but greater than or equal to k characters, then reverse the first k characters and leave the other as original.
public class ReverseString2 {
    public static void main(String[] args) {
        String s = "abcdefg";
        int k = 2;
        String result = reverseStr(s, k);
        System.out.println("Reversed string: " + result);
    }

    private static String reverseStr(String s, int k) {
        char str[] = s.toCharArray();

        for (int x = 0; x < s.length(); x = x + 2 * k) {
            int i = x, j = Math.min(i + k - 1, s.length() - 1);
            while (i < j) {
                char temp = str[i];
                str[i] = str[j];
                str[j] = temp;
                i++;
                j--;
            }
        }

        return new String(str);
    }
}
