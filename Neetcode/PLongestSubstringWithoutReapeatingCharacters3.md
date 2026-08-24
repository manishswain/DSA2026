# LeetCode 3 - Longest Substring Without Repeating Characters

Given a string `s`, find the length of the longest substring without repeating characters.

## Brute Force

**Intuition (as implemented in the file, `lengthOfLongestSubstringBruteForce`):** For every starting index `i`, extend a window to the right (`j`) adding characters to a set, stopping as soon as a duplicate is found. This directly checks, for each possible starting point, how long a repeat-free run can be — correct because it re-derives the answer from scratch at each starting position, but repeats a lot of work since each inner scan restarts from an empty set.

**Time Complexity:** O(n²) — for each of the n starting points, we may scan up to n characters.
**Space Complexity:** O(min(n, charset size)) for the set.

```java
private static int lengthOfLongestSubstringBruteForce(String s) {
    int longestSeq = 0;
    for (int i = 0; i < s.length(); i++) {
        Set<Character> set = new HashSet<>();
        int curLargest = 0;
        for (int j = i; j < s.length(); j++) {
            if (set.add(s.charAt(j))) {
                curLargest++;
                longestSeq = Math.max(curLargest, longestSeq);
            } else {
                break;
            }
        }
    }
    return longestSeq;
}
```

**Example:** `s = "abcabcbb"`

**Dry Run:**
```
i=0: j=0'a' add(cur=1,longest=1) | j=1'b' add(cur=2,longest=2) | j=2'c' add(cur=3,longest=3) | j=3'a' dup -> break
i=1: j=1'b' add(1,longest=3) | j=2'c' add(2) | j=3'a' add(3) | j=4'b' dup -> break
i=2: j=2'c' add(1) | j=3'a' add(2) | j=4'b' add(3) | j=5'c' dup -> break
i=3: j=3'a' add(1) | j=4'b' add(2) | j=5'c' add(3) | j=6'b' dup -> break
i=4: j=4'b' add(1) | j=5'c' add(2) | j=6'b' dup -> break
i=5: j=5'c' add(1) | j=6'b' add(2) | j=7'b' dup -> break
i=6: j=6'b' add(1) | j=7'b' dup -> break
i=7: j=7'b' add(1)
```
Output: `3` (the substring `"abc"`)

## Optimal Solution

**Intuition:** Use a sliding window with two pointers and a set of characters currently in the window. Expand the window by moving `right` and adding characters. When a duplicate character is encountered, we know the earliest possible valid window start must be *after* the previous occurrence of that character, so shrink the window from the left — removing characters from the set — until the duplicate is gone. Because both pointers only ever move forward, each character is added and removed from the set at most once, avoiding the brute force's repeated rescanning.

**Time Complexity:** O(n) — both pointers traverse the string at most once each.
**Space Complexity:** O(min(n, charset size)) for the set.

```java
private static int lengthOfLongestSubstring(String s) {
    if (s == null || s.length() == 0) return 0;
    int longestSeq = 0;
    Set<Character> set = new HashSet<>();
    int left = 0, right = 0;
    while (right < s.length()) {
        char c = s.charAt(right);
        while (set.contains(c)) {
            set.remove(s.charAt(left));
            left++;
        }
        set.add(c);
        longestSeq = Math.max(longestSeq, right - left + 1);
        right++;
    }
    return longestSeq;
}
```

**Example:** `s = "abcabcbb"`

**Dry Run:**
```
left=0, right=0, longestSeq=0, set={}
right=0 'a' not in set -> set={a}         longestSeq=max(0,1)=1  right=1
right=1 'b' not in set -> set={a,b}       longestSeq=max(1,2)=2  right=2
right=2 'c' not in set -> set={a,b,c}     longestSeq=max(2,3)=3  right=3
right=3 'a' in set -> remove s[0]='a', left=1, set={b,c}
        'a' not in set now -> set={b,c,a} longestSeq=max(3,3)=3  right=4
right=4 'b' in set -> remove s[1]='b', left=2, set={c,a}
        'b' not in set now -> set={c,a,b} longestSeq=max(3,3)=3  right=5
right=5 'c' in set -> remove s[2]='c', left=3, set={a,b}
        'c' not in set now -> set={a,b,c} longestSeq=max(3,3)=3  right=6
right=6 'b' in set -> remove s[3]='a', left=4, set={b,c}
        'b' still in set -> remove s[4]='b', left=5, set={c}
        'b' not in set now -> set={c,b}   longestSeq=max(3,2)=3  right=7
right=7 'b' in set -> remove s[5]='c', left=6, set={b}
        'b' still in set -> remove s[6]='b', left=7, set={}
        'b' not in set now -> set={b}     longestSeq=max(3,1)=3  right=8 (loop ends)
```
Output: `3` (the substring `"abc"`)
