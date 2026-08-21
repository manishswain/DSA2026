# Remove Outermost Parentheses (LeetCode 1021)

## Problem
A valid parenthesis string `s` can be uniquely decomposed into a sequence of *primitive* valid parenthesis strings (each primitive is non-empty and cannot be split further into two non-empty valid strings). Return `s` after removing the outermost parentheses of every primitive string in the decomposition.

Example: `s = "(()())(())"` → `"()()()"` (primitives are `"(()())"` and `"(())"`; strip the outer `(` `)` from each and concatenate: `"()()"+ "()"` = `"()()()"`).

---

## Brute Force Approach
Use a stack to find the matching index for every parenthesis, then explicitly identify each primitive block (a run that starts when depth goes `0 → 1` and ends when it returns `1 → 0`), and build the result by slicing off the first and last character of each block.

```java
private static String removeOuterParenthesesBruteForce(String s) {
    Stack<Integer> indices = new Stack<>();
    int[] match = new int[s.length()];
    for (int i = 0; i < s.length(); i++) {
        if (s.charAt(i) == '(') {
            indices.push(i);
        } else {
            int openIdx = indices.pop();
            match[openIdx] = i;
            match[i] = openIdx;
        }
    }

    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < s.length()) {
        int closeIdx = match[i];          // end of this primitive block
        sb.append(s, i + 1, closeIdx);    // everything except outer '(' and ')'
        i = closeIdx + 1;
    }
    return sb.toString();
}
```

- **Time Complexity:** O(n) — one pass to build the match array, one pass to build the result.
- **Space Complexity:** O(n) — for the stack and match array (extra bookkeeping beyond what's strictly necessary).

---

## Optimal Approach (used in code — single-pass depth counter)
```java
private static String removeOuterParentheses(String s) {
    int depth = 0;
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
        if (c == '(') {
            depth++;
            if (depth > 1) {
                sb.append(c);
            }
        } else {
            if (depth > 1) {
                sb.append(c);
            }
            depth--;
        }
    }
    return sb.toString();
}
```

### Intuition
The brute force builds an explicit match-index array just to know *where* each primitive block starts and ends — but we don't actually need to know the matching index at all. We only need to know **one bit of information at every character: "is this character part of the outermost pair of its primitive, or is it nested inside?"** — and that's exactly what a running depth counter tells us.

Track `depth` = how many unmatched `(` we're currently inside:
- Every `(` increases depth *first*. If depth becomes `1`, this `(` is the *outermost* opening bracket of a new primitive — skip it (don't append). If depth was already `≥ 1` before incrementing (i.e. `depth > 1` after), it's a nested bracket — keep it.
- Every `)` is checked *before* decrementing: if `depth > 1`, this closer still has an enclosing pair around it — keep it. If `depth == 1`, this `)` is the outermost closer of the current primitive — skip it, then decrement back to `0`, ready for the next primitive.

Because the decision for each character depends only on the current depth (a single integer), we never need to know *where* a match is — just whether we're currently "at the boundary" (depth 1) or "inside" (depth > 1). This collapses the two-pass, extra-array brute force into one pass with O(1) extra state.

- **Time Complexity:** O(n) — single pass, O(1) work per character.
- **Space Complexity:** O(1) extra (excluding the output `StringBuilder`), versus O(n) for brute force's match array.

---

## Dry Run
`s = "(()())(())"`

| char | depth before | action | depth after | appended? |
|------|---------------|--------|-------------|-----------|
| (    | 0 | outer open, skip | 1 | no |
| (    | 1 | nested open, keep | 2 | `(` |
| )    | 2 | depth>1, keep, then decrement | 1 | `)` |
| (    | 1 | nested open, keep | 2 | `(` |
| )    | 2 | depth>1, keep, then decrement | 1 | `)` |
| )    | 1 | outer close, skip, decrement | 0 | no |
| (    | 0 | outer open, skip | 1 | no |
| (    | 1 | nested open, keep | 2 | `(` |
| )    | 2 | depth>1, keep, then decrement | 1 | `)` |
| )    | 1 | outer close, skip, decrement | 0 | no |

**Result:** `"()()" + "()"` = `"()()()"`
