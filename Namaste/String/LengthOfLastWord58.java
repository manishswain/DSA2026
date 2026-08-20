package Namaste.String;

public class LengthOfLastWord {
    public static void main(String[] args) {
        String s = " a";
        int length = lengthOfLastWord(s);
        System.out.println("Length of last word: " + length);
    }

    private static int lengthOfLastWord(String s) {
        int len = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (len < 1 && s.charAt(i) == ' ') {

            } else if (len > 0 && s.charAt(i) == ' ') {
                return len;
            } else {
                len++;
            }
        }
        return len;
    }
}
