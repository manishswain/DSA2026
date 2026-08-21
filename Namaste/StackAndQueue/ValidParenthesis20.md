# Valid Parenthesis (LeetCode 20)

## Problem
Given a string `s` containing just the characters `(`, `)`, `{`, `}`, `[`, `]`, determine if the input string is valid: every opening bracket must be closed by the same type of bracket, and in the correct order.

Example: `s = "({)}"` → `false`. `s = "()[]{}"` → `true`.

---

## Brute Force Approach
Repeatedly scan the string and remove any adjacent matching pair (`"()"`, `"{}"`, `"[]"`) until no more removals are possible. If the string becomes empty, it was valid.

```java
private static boolean isValidBruteForce(String s) {
    boolean changed = true;
    while (changed) {
        String before = s;
        s = s.replace("()", "").replace("{}", "").replace("[]", "");
        changed = !s.equals(before);
    }
    return s.isEmpty();
}
```

- **Time Complexity:** O(n²) — each pass over the string is O(n), and up to O(n) passes may be needed in the worst case (e.g. `"((((...))))"`).
- **Space Complexity:** O(n) — for the intermediate strings created by `replace`.

---

## Optimal Approach (used in code)
```java
private static boolean isValid(String s) {
    Stack<Character> stack = new Stack<>();
    for (char c : s.toCharArray()) {
        if (c == '(' || c == '{' || c == '[') {
            stack.push(c);
        } else {
            if (stack.isEmpty()) {
                return false;
            }
            char top = stack.pop();
            if (!isMatchingPair(top, c)) {
                return false;
            }
        }
    }
    return stack.isEmpty();
}

private static boolean isMatchingPair(char top, char c) {
    return (top == '(' && c == ')') || (top == '{' && c == '}') || (top == '[' && c == ']');
}
```

### Intuition
Brackets nest like a call stack: the *most recently opened* bracket must be the *next one closed*. That "last opened, first closed" behavior is exactly a stack's LIFO property, so we never need to rescan the string repeatedly the way brute force does.

Scan left to right:
1. Every opening bracket is pushed — it's a "promise" that must be fulfilled by a matching close later.
2. Every closing bracket must fulfill the *most recent* unfulfilled promise — so we pop the stack and check the popped bracket matches the current closer's type. If the stack is empty, there was no open bracket to close, so it's invalid immediately.
3. At the end, if the stack isn't empty, some opening brackets were never closed — invalid.

This one left-to-right pass is enough because each character is examined exactly once, unlike brute force which repeatedly re-examines the whole (shrinking) string.

- **Time Complexity:** O(n) — one pass, each character pushed/popped at most once.
- **Space Complexity:** O(n) — worst case (all opening brackets) the stack holds every character.

---

## Dry Run
`s = "({)}"`

| char | action | stack (top → right) | result |
|------|--------|----------------------|--------|
| `(`  | push   | `(`                  | —      |
| `{`  | push   | `( {`                | —      |
| `)`  | pop `{`, check match with `)` → not matching (`{` closes with `}`) | `(` | **return false** |

**Result:** `false` ✅ (matches expected output)
