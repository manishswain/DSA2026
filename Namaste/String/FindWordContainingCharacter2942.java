package Namaste.String;

import java.util.ArrayList;
import java.util.List;

public class FindWordContainingCharacter2942 {
    public static void main(String[] args) {
        String[] s = { "leet", "code" };
        char c = 'o';
        List<Integer> ans = findWordContainingCharacter(s, c);
        System.out.println("Word containing character '" + c + "': " + ans);
    }

    private static List<Integer> findWordContainingCharacter(String[] words, char x) {
        List<Integer> ans = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            for (int j = 0; j < words[i].length(); j++) {
                if (words[i].charAt(j) == x) {
                    ans.add(i);
                }
            }
        }
        return ans;
    }

}
