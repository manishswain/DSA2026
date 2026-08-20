# Find Words Containing Character (LeetCode 2942)

## Problem
Given a 0-indexed array of strings `words` and a character `x`, return an array of indices representing the words that contain the character `x`. The order of indices may be in any order.

Example: `words = ["leet", "code"]`, `x = 'o'` → `[1]`.

---

## Brute Force Approach
For each word, use a built-in `contains`/`indexOf` check against the character.

```java
List<Integer> ans = new ArrayList<>();
for (int i = 0; i < words.length; i++) {
    if (words[i].indexOf(x) != -1) {
        ans.add(i);
    }
}
```

- **Time Complexity:** O(n * m) — n words, each of length up to m, since `indexOf` internally scans the word.
- **Space Complexity:** O(k) for the output list, k = number of matching words.

---

## Optimal Approach (used in code)
```java
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
```

### Intuition
This is fundamentally the same O(n * m) scan as the brute force — there's no way around inspecting each character of a word at least once to know if it contains `x`, since strings offer no shortcut for "does this character exist" other than a linear (or table-based) scan. The manual double loop here does the same work as `indexOf`, just spelled out explicitly with `charAt`.

The one subtlety worth flagging: this version doesn't `break` after finding a match, so if `x` appears multiple times in the same word, that word's index gets added to `ans` multiple times. Depending on the problem's exact contract, a `break` after `ans.add(i)` would be the tightened version to avoid duplicate indices — but it doesn't change the asymptotic complexity, since worst case still requires scanning the whole word.

- **Time Complexity:** O(n * m) — n words, m = average/max word length; every character may be inspected.
- **Space Complexity:** O(k) — output list of matching indices (with potential duplicates as implemented).

---

## Dry Run
`words = ["leet", "code"]`, `x = 'o'`

**i=0, word="leet":** chars l,e,e,t — none equal 'o' → no additions.

**i=1, word="code":**
| j | char | == 'o'? | action |
|---|------|---------|--------|
| 0 | c | no | — |
| 1 | o | yes | ans.add(1) |
| 2 | d | no | — |
| 3 | e | no | — |

**Result:** `[1]` ✅
