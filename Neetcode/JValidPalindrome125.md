# LeetCode 125 - Valid Palindrome

Given a string, determine if it is a palindrome after converting all
uppercase letters to lowercase and removing all non-alphanumeric characters
(spaces, punctuation, etc).

## Brute Force

Build a cleaned string by iterating through the input once, keeping only
alphanumeric characters and lowercasing them, then compare that cleaned
string to its reverse. The intuition is the literal definition of a
palindrome applied after normalization: strip out everything that
shouldn't count, then check symmetry directly. It works but pays for an
extra pass and extra memory to build the reversed copy.

- Time: O(n) to clean + O(n) to reverse/compare = O(n).
- Space: O(n) for the cleaned string and its reverse.

```java
private static boolean isPalindromeBruteForce(String s) {
    StringBuilder cleaned = new StringBuilder();
    for (char c : s.toCharArray()) {
        if (Character.isLetterOrDigit(c)) {
            cleaned.append(Character.toLowerCase(c));
        }
    }
    String forward = cleaned.toString();
    String reversed = cleaned.reverse().toString();
    return forward.equals(reversed);
}
```

**Example:** `s = "A man, a plan, a canal: Panama"`

**Dry Run:**
```
Scan and filter/lowercase each character, keeping only letters/digits:
cleaned = "amanaplanacanalpanama"

forward = "amanaplanacanalpanama"
reversed = "amanaplanacanalpanama" (cleaned.reverse())

forward.equals(reversed) -> true
```
Output: `true`

## Optimal Solution

Use two pointers, one starting at the beginning and one at the end of the
string. Advance the `left` pointer forward and the `right` pointer backward,
skipping over any non-alphanumeric characters on either side. At each valid
pair of characters, compare them case-insensitively; if they differ, the
string isn't a palindrome. The intuition: a palindrome check only needs to
compare pairs of symmetric characters — we don't need to materialize a
cleaned string first, we can filter and compare in the same pass, saving
the extra allocation and second traversal.

- Time: O(n) — each character is visited at most once by either pointer.
- Space: O(1) — no extra string is built, only pointer variables are used.

```java
private static boolean isPalindromeOptimal(String s) {
    int left = 0, right = s.length() - 1;
    while (left < right) {
        while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
            left++;
        }
        while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
            right--;
        }
        if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
            return false;
        }
        left++;
        right--;
    }
    return true;
}
```

**Example:** `s = "A man, a plan, a canal: Panama"`

**Dry Run:**
```
left=0 ('A'), right=29 ('a'): both alphanumeric, 'a'=='a' -> match; left=1, right=28
left=1 (' '): not alphanumeric -> left=2 ('m')
right=28 ('m'): alphanumeric already
  'm'=='m' -> match; left=3, right=27
left=3 ('a'), right=27 ('a'): match; left=4, right=26
left=4 ('n'), right=26 ('n'): match; left=5, right=25
left=5 (','): not alphanumeric -> left=6 (' ') -> not alphanumeric -> left=7 ('a')
right=25 ('a'): alphanumeric already
  'a'=='a' -> match; left=8, right=24
... (continues comparing symmetric pairs, skipping spaces/commas/colon) ...
eventually left >= right -> loop ends, no mismatch found
```
Output: `true`
