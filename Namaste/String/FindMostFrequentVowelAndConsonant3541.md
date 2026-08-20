# Find Most Frequent Vowel and Consonant (LeetCode 3541)

## Problem
Given a string `s` consisting of lowercase English letters (`'a'` to `'z'`), find the vowel (one of `'a'`, `'e'`, `'i'`, `'o'`, `'u'`) with the maximum frequency and the consonant (any other letter) with the maximum frequency in `s`. Return the sum of these two frequencies. If multiple vowels/consonants have the same maximum frequency, any can be chosen; if a category is absent, its contribution is 0.

Example: `s = "successes"` → `6` (most frequent vowel `'e'` or `'u'`... let's verify: s-u-c-c-e-s-s-e-s → vowels: u(1), e(2); consonants: s(5), c(2). Max vowel = 2 (`e`), max consonant = 5 (`s`), sum = 7). The code computes this sum generically.

---

## Brute Force Approach
For each of the 26 letters, count its occurrences in `s` with a nested scan, classify it as vowel or consonant, and track running maximums.

```java
int maxVowel = 0, maxConsonant = 0;
for (char letter = 'a'; letter <= 'z'; letter++) {
    int freq = 0;
    for (char c : s.toCharArray()) {
        if (c == letter) freq++;
    }
    if (isVowel(letter)) maxVowel = Math.max(maxVowel, freq);
    else maxConsonant = Math.max(maxConsonant, freq);
}
return maxVowel + maxConsonant;
```

- **Time Complexity:** O(26 * n) = O(n) but with a larger constant factor, since `s` is rescanned once per letter of the alphabet.
- **Space Complexity:** O(1).

---

## Optimal Approach (used in code)
```java
private static int findMostFrequentVowelAndConsonant(String s) {
    int[] vowel = new int[26];
    int[] consonant = new int[26];
    List<Character> vConstants = List.of('a', 'e', 'i', 'o', 'u');

    for (char c : s.toCharArray()) {
        if (vConstants.contains(c)) {
            vowel[c - 'a']++;
        } else {
            consonant[c - 'a']++;
        }
    }
    int maxVowel = 0, maxConsonant = 0;
    for (int i : vowel) {
        maxVowel = Math.max(maxVowel, i);
    }

    for (int i : consonant) {
        maxConsonant = Math.max(maxConsonant, i);
    }
    return maxVowel + maxConsonant;
}
```

### Intuition
Instead of rescanning `s` once per alphabet letter, make a single pass and bucket every character's frequency into one of two fixed-size 26-slot arrays based on whether it's a vowel or a consonant — indexed by `c - 'a'` so each letter has a dedicated slot regardless of category. After this single counting pass, finding the maximum frequency within each category is just a linear scan over 26 fixed-size arrays (effectively O(1), since the alphabet size is constant).

This is the same "counting array" pattern as [[JewelsAndStones]] and [[ValidAnagram]] — precompute frequencies in one pass, then answer aggregate questions (max, presence, etc.) in O(1) per query afterward, instead of paying for a fresh scan of `s` every time.

- **Time Complexity:** O(n) — one pass to build frequency arrays, O(26) to find each max (constant).
- **Space Complexity:** O(1) — two fixed 26-length arrays regardless of input size.

---

## Dry Run
`s = "successes"` (s,u,c,c,e,s,s,e,s)

**Frequency build:**
- s: consonant, consonant['s'-'a']=consonant[18] → ends at 5
- u: vowel[20] → 1
- c: consonant[2] → 2
- e: vowel[4] → 2

**vowel array (nonzero slots):** `vowel['a']=0, vowel['e']=2, vowel['u']=1` → max = 2

**consonant array (nonzero slots):** `consonant['c']=2, consonant['s']=5` → max = 5

**Result:** `2 + 5 = 7` ✅
