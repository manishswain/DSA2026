package Neetcode;

//String Question
//Leetcode 271 - Encode and Decode Strings
public class HEncodeAndDecodeString271 {
    public static void main(String[] args) {
        String[] s = { "Hello, World!", "Neetcode is great!", "Encode and Decode String" };
        String encoded = encode(s);
        String decoded = decode(encoded);

        System.out.println("Original: " + java.util.Arrays.toString(s));
        System.out.println("Encoded: " + encoded);
        System.out.println("Decoded: " + decoded);
    }

    // Approach: We can encode the list of strings by concatenating each string with
    // its length followed by a delimiter (e.g., '#'). To decode, we can split the
    // encoded string using the delimiter and extract the original strings based on
    // their lengths. This approach has a time complexity of O(n) for encoding and
    // O(n) for decoding, where n is the total length of all strings in the list.
    private static String encode(String[] strs) {
        StringBuilder sb = new StringBuilder();
        for (String s : strs) {
            sb.append(s.length()).append("#").append(s);
        }
        return sb.toString();
    }

    private static String decode(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int j = s.indexOf('#', i);
            int length = Integer.parseInt(s.substring(i, j));
            sb.append(s.substring(j + 1, j + 1 + length)).append(" ");
            i = j + 1 + length;
        }
        return sb.toString().trim();
    }

}