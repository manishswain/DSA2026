# Valid Anagram (LeetCode 242)

## Problem
Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise. An anagram uses exactly the same characters with the same frequencies, just rearranged.

Example: `s = "aacc"`, `t = "ccac"` → `false` (frequencies differ: `s` has two `a`s and two `c`s, `t` has one `a` and three `c`s... wait `t = "ccac"` has 1 `a`, 3 `c`s → not equal to `s`'s 2 `a`, 2 `c` → `false`).

---

## Brute Force Approach
Sort both strings and compare them character by character (or compare the sorted strings directly).

```java
char[] sArr = s.toCharArray();
char[] tArr = t.toCharArray();
Arrays.sort(sArr);
Arrays.sort(tArr);
boolean isAnagram = Arrays.equals(sArr, tArr);
```

- **Time Complexity:** O(n log n) — dominated by sorting.
- **Space Complexity:** O(n) — for the sorted copies (or O(log n) if sort is in-place and we ignore output arrays).

---

## Optimal Approach (used in code)
```java
private static boolean isAnagram(String s, String t) {
    if (s.length() != t.length()) {
        return false;
    }

    Map<Character, Integer> frequencyMap = new HashMap<>();

    for (char ch : s.toCharArray()) {
        frequencyMap.put(ch, frequencyMap.getOrDefault(ch, 0) + 1);
    }

    for (char ch : t.toCharArray()) {
        if (!frequencyMap.containsKey(ch)) {
            return false;
        }

        int updatedCount = frequencyMap.get(ch) - 1;
        if (updatedCount < 0) {
            return false;
        }

        frequencyMap.put(ch, updatedCount);
    }
    return true;
}
```

### Intuition
Two strings are anagrams exactly when every character occurs the same number of times in both. Instead of sorting (which reorders data just to compare it), count character frequencies from `s` into a map, then "consume" those counts while walking `t`. If `t` ever needs a character that isn't in the map, or needs more of a character than `s` provided (count would go negative), the strings can't be anagrams. If `t` is fully consumed without any violation, and lengths matched upfront, the frequency multisets are identical.

The length check upfront is a cheap short-circuit — two strings of different lengths can never be anagrams, so there's no need to build the map at all in that case.

- **Time Complexity:** O(n) — two linear passes plus O(1) average HashMap operations.
- **Space Complexity:** O(k) — where k is the number of distinct characters (bounded by alphabet size, so effectively O(1) for typical inputs).

---

## Dry Run
`s = "aacc"`, `t = "ccac"`

**Build frequency map from s:**
`{a: 2, c: 2}`

**Consume using t:**

| ch | map before | action | map after | violation? |
|----|------------|--------|-----------|------------|
| c  | {a:2, c:2} | c: 2→1 | {a:2, c:1} | no |
| c  | {a:2, c:1} | c: 1→0 | {a:2, c:0} | no |
| a  | {a:2, c:0} | a: 2→1 | {a:1, c:0} | no |
| c  | {a:1, c:0} | c: 0→-1 | — | **yes, negative** |

**Result:** `false` — matches expected output since `s` and `t` don't have matching character frequencies. ✅
